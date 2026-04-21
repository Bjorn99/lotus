package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.track.Playlist
import com.dn0ne.player.app.domain.track.Track
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RealmPlaylistRepository(
    private val realm: Realm
): PlaylistRepository {
    override fun getPlaylists(): Flow<List<Playlist>> {
        return realm.query<PlaylistJson>().asFlow().map { it.list.reversed().map { it.toPlaylist() } }
    }

    override suspend fun insertPlaylist(playlist: Playlist) {
        // Realm's PK is a non-null String; a playlist without a name can't be
        // persisted. Drop silently rather than crash — the UI layer enforces
        // non-empty names when users create playlists.
        if (playlist.name == null) return
        realm.write {
            copyToRealm(instance = playlist.toPlaylistJson(), updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun updatePlaylistTrackList(playlist: Playlist, trackList: List<Track>) {
        val name = playlist.name ?: return
        realm.write {
            val queryResult = query<PlaylistJson>(query = "name = $0", name).find().firstOrNull()
            queryResult?.let {
                findLatest(it)?.json = playlist.copy(trackList = trackList).toPlaylistJson().json
            }
        }
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        val name = playlist.name ?: return
        realm.write {
            val stored = query<PlaylistJson>(query = "name = $0", name)
            delete(stored)
        }
    }

    override suspend fun renamePlaylist(playlist: Playlist, name: String) {
        deletePlaylist(playlist)
        insertPlaylist(playlist.copy(name = name))
    }
}

class PlaylistJson(): RealmObject {
    @PrimaryKey
    var name: String = ""
    var json: String = ""

    fun toPlaylist(): Playlist = Json.decodeFromString(json)
}

fun Playlist.toPlaylistJson(): PlaylistJson = PlaylistJson().apply {
    // Callers (RealmPlaylistRepository) filter out null-name playlists before
    // reaching here; orEmpty() is belt-and-braces to keep this non-crashing
    // even if a future caller forgets.
    this.name = this@toPlaylistJson.name.orEmpty()
    json = Json.encodeToString(this@toPlaylistJson)
}