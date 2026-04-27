package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.LovedTrackDao
import com.dn0ne.player.app.data.db.LovedTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLovedTracksRepository(
    private val dao: LovedTrackDao,
) : LovedTracksRepository {

    override fun observeLovedUris(): Flow<Set<String>> =
        dao.observeUris().map { it.toSet() }

    override suspend fun isLoved(uri: String): Boolean = dao.isLoved(uri)

    override suspend fun add(uri: String) {
        dao.insert(LovedTrackEntity(uri = uri, addedAt = System.currentTimeMillis()))
    }

    override suspend fun remove(uri: String) {
        dao.deleteByUri(uri)
    }
}
