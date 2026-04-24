package com.dn0ne.player.app.domain.playlist

import com.dn0ne.player.R
import com.dn0ne.player.app.data.repository.PlaylistRepository
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent

/**
 * Thin wrapper around the playlist repository for the mutation-side
 * operations (create, rename, delete, add/remove/reorder tracks). Extracted
 * from `PlayerViewModel` so the VM can shrink — behaviour is identical to
 * the inline branches it replaces, including the "already on playlist"
 * snackbar that fires even when we proceed to add the remaining tracks.
 *
 * All methods are `suspend`; the VM keeps responsibility for `viewModelScope`
 * so cancellation semantics don't change.
 */
class PlaylistEditor(
    private val repository: PlaylistRepository,
) {

    /**
     * @return `true` if inserted, `false` if another playlist with that name
     * already exists (duplicate names are not allowed at the UI layer).
     */
    suspend fun create(name: String, existingNames: List<String?>): Boolean {
        if (name in existingNames) return false
        repository.insertPlaylist(Playlist(name = name, trackList = emptyList()))
        return true
    }

    /**
     * @return `true` if renamed, `false` if another playlist already has the
     * new name.
     */
    suspend fun rename(
        playlist: Playlist,
        newName: String,
        existingNames: List<String?>,
    ): Boolean {
        if (newName in existingNames) return false
        repository.renamePlaylist(playlist = playlist, name = newName)
        return true
    }

    suspend fun delete(playlist: Playlist) {
        repository.deletePlaylist(playlist = playlist)
    }

    /**
     * Appends [tracks] to [playlist]. If any requested track is already
     * present, emits a snackbar; the non-duplicate tracks are still added
     * (preserved behaviour from the original VM branch).
     *
     * @return the playlist's new full track list.
     */
    suspend fun addTracks(playlist: Playlist, tracks: List<Track>): List<Track> {
        if (playlist.trackList.any { it in tracks }) {
            SnackbarController.sendEvent(
                SnackbarEvent(message = R.string.track_is_already_on_playlist)
            )
        }
        val newTrackList = (playlist.trackList.toMutableSet() + tracks).toList()
        repository.updatePlaylistTrackList(playlist = playlist, trackList = newTrackList)
        return newTrackList
    }

    /** @return the playlist's track list with [tracks] removed. */
    suspend fun removeTracks(playlist: Playlist, tracks: List<Track>): List<Track> {
        val newTrackList = playlist.trackList.toMutableList().apply { removeAll(tracks) }
        repository.updatePlaylistTrackList(playlist = playlist, trackList = newTrackList)
        return newTrackList
    }

    /** No-op if [newOrder] is structurally identical to the existing list. */
    suspend fun reorder(playlist: Playlist, newOrder: List<Track>) {
        if (playlist.trackList == newOrder) return
        repository.updatePlaylistTrackList(playlist = playlist, trackList = newOrder)
    }
}
