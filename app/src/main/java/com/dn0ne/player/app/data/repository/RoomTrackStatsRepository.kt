package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.TrackStatsDao
import com.dn0ne.player.app.data.db.TrackStatsEntity
import com.dn0ne.player.app.domain.track.TrackStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTrackStatsRepository(
    private val dao: TrackStatsDao,
) : TrackStatsRepository {

    override fun observeAll(): Flow<List<TrackStats>> =
        dao.observeAll().map { it.map(TrackStatsEntity::toDomain) }

    override fun observeTopByPlayCount(limit: Int): Flow<List<TrackStats>> =
        dao.observeTopByPlayCount(limit).map { it.map(TrackStatsEntity::toDomain) }

    override fun observeRecentlyPlayed(limit: Int): Flow<List<TrackStats>> =
        dao.observeRecentlyPlayed(limit).map { it.map(TrackStatsEntity::toDomain) }

    override suspend fun statsFor(uri: String): TrackStats? =
        dao.getByUri(uri)?.toDomain()

    override suspend fun recordPlay(uri: String, now: Long) {
        dao.recordPlay(uri = uri, now = now)
    }

    override suspend fun recordSkip(uri: String) {
        dao.recordSkip(uri = uri)
    }

    override suspend fun addListenedMs(uri: String, ms: Long) {
        dao.addListenedMs(uri = uri, ms = ms)
    }

    override suspend fun mergeFromBackup(rows: List<TrackStats>) {
        for (incoming in rows) {
            val local = dao.getByUri(incoming.uri)?.toDomain()
            val merged = mergeStats(local = local, incoming = incoming)
            dao.upsertReplacing(merged.toEntity())
        }
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}

private fun TrackStatsEntity.toDomain() = TrackStats(
    uri = uri,
    playCount = playCount,
    skipCount = skipCount,
    firstPlayedAt = firstPlayedAt,
    lastPlayedAt = lastPlayedAt,
    totalListeningMs = totalListeningMs,
)

private fun TrackStats.toEntity() = TrackStatsEntity(
    uri = uri,
    playCount = playCount,
    skipCount = skipCount,
    firstPlayedAt = firstPlayedAt,
    lastPlayedAt = lastPlayedAt,
    totalListeningMs = totalListeningMs,
)
