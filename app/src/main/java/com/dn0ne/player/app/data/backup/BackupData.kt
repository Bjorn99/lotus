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
) {
    companion object {
        const val SCHEMA_VERSION = 1
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

sealed interface ExportResult {
    data class Ok(val playlists: Int, val lovedTracks: Int) : ExportResult
    data class Failure(val cause: Throwable) : ExportResult
}

sealed interface ImportResult {
    data class Ok(
        val playlistsAdded: Int,
        val playlistsSkipped: Int,
        val lovedTracksAdded: Int,
        val tracksUnresolved: Int,
    ) : ImportResult
    data class Failure(val cause: Throwable) : ImportResult
}
