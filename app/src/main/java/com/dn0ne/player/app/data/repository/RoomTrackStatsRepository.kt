package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.TrackStatsDao

class RoomTrackStatsRepository(
    private val dao: TrackStatsDao,
) : TrackStatsRepository {

    override suspend fun recordEvent(uri: String, isPlay: Boolean, now: Long) {
        dao.recordEvent(uri = uri, isPlay = isPlay, now = now)
    }

    override suspend fun addListeningMs(uri: String, ms: Long) {
        dao.addListeningMs(uri = uri, ms = ms)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
