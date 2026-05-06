package com.dn0ne.player.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dn0ne.player.app.data.repository.LovedTracksRepository
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.TrackStats
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Read/write the user-data backup file (playlists, loved tracks, listening
 * stats) over the URIs the user picks via Storage Access Framework.
 *
 * Import policy is *merge*, never replace:
 *
 * - Loved set: union with existing entries.
 * - Playlists: add by name; if a playlist with the same name already exists,
 *   the imported one is skipped (the result tells the caller how many).
 * - Track stats: merged per-URI with max for counters/timestamps and min for
 *   firstPlayedAt — re-importing the same backup is a no-op, importing an
 *   older backup never erases newer activity. Skipped entirely if the
 *   listening-stats privacy toggle is currently off.
 *
 * That means a restore against an empty database brings everything back, and
 * a restore against an in-use database can't silently destroy work the user
 * created since the backup.
 */
class BackupManager(
    private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val lovedTracksRepository: LovedTracksRepository,
    private val trackStatsRepository: TrackStatsRepository,
    private val trackRepository: TrackRepository,
    private val settings: Settings,
    private val appVersionName: String,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val playlists = playlistRepository.getPlaylists().first()
            val lovedUris = lovedTracksRepository.observeLovedUris().first()
            val tracks = trackRepository.getTracks()
            val byUri = tracks.associateBy { it.uri.toString() }

            // Stats: read every row, drop ones whose track isn't in the
            // library at export time (we can't write a usable data path).
            val statsRows = trackStatsRepository
                .observeTopByPlayCount(limit = Int.MAX_VALUE)
                .first()
            val backupStats = statsRows.mapNotNull { row ->
                val track = byUri[row.uri] ?: return@mapNotNull null
                BackupTrackStats(
                    uri = row.uri,
                    data = track.data,
                    playCount = row.playCount,
                    skipCount = row.skipCount,
                    firstPlayedAt = row.firstPlayedAt,
                    lastPlayedAt = row.lastPlayedAt,
                    totalListeningMs = row.totalListeningMs,
                )
            }

            val backup = BackupData(
                exportedAt = System.currentTimeMillis(),
                appVersionName = appVersionName,
                playlists = playlists
                    .filter { it.name != null }
                    .map { p ->
                        BackupPlaylist(
                            name = p.name!!,
                            tracks = p.trackList.map {
                                BackupTrackRef(
                                    uri = it.uri.toString(),
                                    data = it.data,
                                )
                            },
                        )
                    },
                lovedTracks = lovedUris.mapNotNull { uri ->
                    val track = byUri[uri] ?: return@mapNotNull BackupTrackRef(
                        uri = uri,
                        data = "",
                    )
                    BackupTrackRef(uri = uri, data = track.data)
                },
                trackStats = backupStats,
            )

            val bytes = json.encodeToString(backup).toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("openOutputStream returned null for $uri")

            ExportResult.Ok(
                playlists = backup.playlists.size,
                lovedTracks = backup.lovedTracks.size,
                trackStats = backup.trackStats.size,
            )
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Backup export failed", t)
            ExportResult.Failure(t)
        }
    }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("openInputStream returned null for $uri")
            val backup = json.decodeFromString<BackupData>(text)

            // Schema-version forward-compat: refuse to import a newer file
            // than we know how to read, rather than silently dropping fields.
            if (backup.schemaVersion > BackupData.SCHEMA_VERSION) {
                return@withContext ImportResult.Failure(
                    IllegalStateException(
                        "Backup schemaVersion=${backup.schemaVersion} is newer " +
                            "than supported (${BackupData.SCHEMA_VERSION}). Update Lotus and try again."
                    )
                )
            }

            val tracks = trackRepository.getTracks()
            val byUri = tracks.associateBy { it.uri.toString() }
            val byPath = tracks.associateBy { it.data }

            val existingPlaylistNames = playlistRepository.getPlaylists()
                .first()
                .mapNotNull { it.name }
                .toSet()

            var unresolved = 0
            fun resolve(ref: BackupTrackRef): Track? {
                val match = byUri[ref.uri] ?: byPath[ref.data]
                if (match == null) unresolved++
                return match
            }

            // Playlists: skip name conflicts. Empty resolved-track lists are
            // still imported so the user sees the playlist exists, even if
            // none of the source files survived.
            var playlistsAdded = 0
            var playlistsSkipped = 0
            for (bp in backup.playlists) {
                if (bp.name in existingPlaylistNames) {
                    playlistsSkipped++
                    continue
                }
                val resolved = bp.tracks.mapNotNull(::resolve)
                playlistRepository.insertPlaylist(
                    Playlist(name = bp.name, trackList = resolved)
                )
                playlistsAdded++
            }

            // Loved: union — only add ones that resolve to a real track.
            // Storing dangling loved URIs would just clutter the table.
            var lovedAdded = 0
            for (ref in backup.lovedTracks) {
                val track = resolve(ref) ?: continue
                val uriStr = track.uri.toString()
                if (!lovedTracksRepository.isLoved(uriStr)) {
                    lovedTracksRepository.add(uriStr)
                    lovedAdded++
                }
            }

            // Track stats: only honoured if the user has stats recording on.
            // Off means "stop tracking, both directions" — importing into a
            // disabled feature would be confusing.
            val statsEnabled = settings.trackPlayStats.value
            var statsImported = 0
            if (statsEnabled && backup.trackStats.isNotEmpty()) {
                val resolvedStats = backup.trackStats.mapNotNull { row ->
                    val track = byUri[row.uri] ?: byPath[row.data]
                    if (track == null) {
                        unresolved++
                        return@mapNotNull null
                    }
                    TrackStats(
                        uri = track.uri.toString(),
                        playCount = row.playCount,
                        skipCount = row.skipCount,
                        firstPlayedAt = row.firstPlayedAt,
                        lastPlayedAt = row.lastPlayedAt,
                        totalListeningMs = row.totalListeningMs,
                    )
                }
                trackStatsRepository.mergeFromBackup(resolvedStats)
                statsImported = resolvedStats.size
            }

            ImportResult.Ok(
                playlistsAdded = playlistsAdded,
                playlistsSkipped = playlistsSkipped,
                lovedTracksAdded = lovedAdded,
                tracksUnresolved = unresolved,
                statsImported = statsImported,
                statsSkippedDueToToggle = !statsEnabled && backup.trackStats.isNotEmpty(),
            )
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Backup import failed", t)
            ImportResult.Failure(t)
        }
    }

    companion object {
        private const val LOG_TAG = "BackupManager"
    }
}
