package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.LovedTrackDao
import com.dn0ne.player.app.data.db.LovedTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LovedTracksRepository(
    private val dao: LovedTrackDao,
) {

    fun observeLovedUris(): Flow<Set<String>> =
        dao.observeUris().map { it.toSet() }

    suspend fun isLoved(uri: String): Boolean = dao.isLoved(uri)

    suspend fun add(uri: String) {
        dao.insert(LovedTrackEntity(uri = uri, addedAt = System.currentTimeMillis()))
    }

    suspend fun remove(uri: String) {
        dao.deleteByUri(uri)
    }
}
