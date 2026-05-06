package com.dn0ne.player.app.data.repository

interface TrackStatsRepository {
    suspend fun recordEvent(uri: String, isPlay: Boolean, now: Long)
    suspend fun addListeningMs(uri: String, ms: Long)
    suspend fun clearAll()
}
