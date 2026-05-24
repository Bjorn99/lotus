package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.LyricsDao
import com.dn0ne.player.app.data.db.LyricsEntity
import com.dn0ne.player.app.domain.lyrics.Lyrics
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LyricsRepository(
    private val dao: LyricsDao,
) {

    suspend fun getLyricsByUri(uri: String): Lyrics? =
        dao.getByUri(uri)?.toLyrics()

    suspend fun insertLyrics(lyrics: Lyrics) {
        dao.upsert(LyricsEntity(uri = lyrics.uri, json = Json.encodeToString(lyrics)))
    }

    suspend fun updateLyrics(lyrics: Lyrics) {
        dao.updateJson(uri = lyrics.uri, json = Json.encodeToString(lyrics))
    }

    suspend fun deleteLyrics(lyrics: Lyrics) {
        dao.deleteByUri(lyrics.uri)
    }
}

private fun LyricsEntity.toLyrics(): Lyrics = Json.decodeFromString(json)
