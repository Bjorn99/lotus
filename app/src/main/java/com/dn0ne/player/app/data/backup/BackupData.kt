package com.dn0ne.player.app.data.backup

import kotlinx.serialization.Serializable

/**
 * Wire format for the user-controlled backup file. Plain JSON so a curious
 * user can open it in any text editor and see exactly what's in it.
 *
 * Each track is captured as a (uri, data-path) pair. On restore we try to
 * resolve by URI first, then fall back to the absolute file path — handles
 * the common "reinstalled the app, MediaStore reassigned IDs" case as long
 * as the audio files are still in the same place on disk.
 */
@Serializable
data class BackupData(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersionName: String,
    val playlists: List<BackupPlaylist>,
    val lovedTracks: List<BackupTrackRef>,
    // Defaulted so v1 backups still parse cleanly. Bumped to v2 when this
    // field was added in v1.5.6.
    val trackStats: List<BackupTrackStats> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 2
    }
}

@Serializable
data class BackupPlaylist(
    val name: String,
    val tracks: List<BackupTrackRef>,
)

@Serializable
data class BackupTrackRef(
    val uri: String,
    val data: String,
)

@Serializable
data class BackupTrackStats(
    val uri: String,
    val data: String,
    val playCount: Int,
    val skipCount: Int,
    // Nullable so a stats row with only skips (no play timestamps yet)
    // round-trips faithfully. Older backups that lacked this field are
    // covered by the schema-default at parse time.
    val firstPlayedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val totalListeningMs: Long,
)

sealed interface ExportResult {
    data class Ok(
        val playlists: Int,
        val lovedTracks: Int,
        val trackStats: Int,
    ) : ExportResult
    data class Failure(val cause: Throwable) : ExportResult
}

sealed interface ImportResult {
    data class Ok(
        val playlistsAdded: Int,
        val playlistsSkipped: Int,
        val lovedTracksAdded: Int,
        val tracksUnresolved: Int,
        val statsImported: Int,
        val statsSkippedDueToToggle: Boolean,
    ) : ImportResult
    data class Failure(val cause: Throwable) : ImportResult
}
