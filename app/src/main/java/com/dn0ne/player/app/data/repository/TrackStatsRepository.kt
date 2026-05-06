package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.track.TrackStats
import kotlinx.coroutines.flow.Flow

interface TrackStatsRepository {
    fun observeAll(): Flow<List<TrackStats>>
    fun observeTopByPlayCount(limit: Int): Flow<List<TrackStats>>
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackStats>>
    suspend fun statsFor(uri: String): TrackStats?
    suspend fun recordPlay(uri: String, now: Long)
    suspend fun recordSkip(uri: String)
    suspend fun addListenedMs(uri: String, ms: Long)
    suspend fun mergeFromBackup(rows: List<TrackStats>)
    suspend fun clearAll()
}
