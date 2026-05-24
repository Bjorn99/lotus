package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.PlaylistDao
import com.dn0ne.player.app.data.db.PlaylistEntity
import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlaylistRepository(
    private val dao: PlaylistDao,
) {

    fun getPlaylists(): Flow<List<Playlist>> =
        dao.observeAll().map { rows -> rows.map { it.toPlaylist() } }

    suspend fun insertPlaylist(playlist: Playlist) {
        val name = playlist.name ?: return
        dao.upsert(PlaylistEntity(name = name, json = Json.encodeToString(playlist)))
    }

    suspend fun updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>) {
        val name = playlist.name ?: return
        val updated = playlist.copy(trackList = trackList)
        dao.updateJson(name = name, json = Json.encodeToString(updated))
    }

    suspend fun renamePlaylist(playlist: Playlist, name: String) {
        deletePlaylist(playlist)
        insertPlaylist(playlist.copy(name = name))
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        val name = playlist.name ?: return
        dao.deleteByName(name)
    }
}

private fun PlaylistEntity.toPlaylist(): Playlist = Json.decodeFromString(json)
