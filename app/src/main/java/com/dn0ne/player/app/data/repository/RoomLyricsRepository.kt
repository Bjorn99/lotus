package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.LyricsDao
import com.dn0ne.player.app.data.db.LyricsEntity
import com.dn0ne.player.app.domain.lyrics.Lyrics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room-backed replacement for RealmLyricsRepository. Same JSON-blob shape
 * as the legacy Realm store so the migrator can copy rows verbatim.
 */
class RoomLyricsRepository(
    private val dao: LyricsDao,
) : LyricsRepository {

    override suspend fun getLyricsByUri(uri: String): Lyrics? =
        dao.getByUri(uri)?.toLyrics()

    override suspend fun insertLyrics(lyrics: Lyrics) {
        dao.upsert(LyricsEntity(uri = lyrics.uri, json = Json.encodeToString(lyrics)))
    }

    override suspend fun updateLyrics(lyrics: Lyrics) {
        dao.updateJson(uri = lyrics.uri, json = Json.encodeToString(lyrics))
    }

    override suspend fun deleteLyrics(lyrics: Lyrics) {
        dao.deleteByUri(lyrics.uri)
    }
}

private fun LyricsEntity.toLyrics(): Lyrics = Json.decodeFromString(json)
