package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.lyrics.Lyrics

interface LyricsRepository {
    suspend fun getLyricsByUri(uri: String): Lyrics?
    suspend fun insertLyrics(lyrics: Lyrics)
    suspend fun updateLyrics(lyrics: Lyrics)
    suspend fun deleteLyrics(lyrics: Lyrics)
}