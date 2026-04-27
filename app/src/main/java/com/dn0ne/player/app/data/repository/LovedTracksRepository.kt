package com.dn0ne.player.app.data.repository

import kotlinx.coroutines.flow.Flow

interface LovedTracksRepository {
    fun observeLovedUris(): Flow<Set<String>>
    suspend fun isLoved(uri: String): Boolean
    suspend fun add(uri: String)
    suspend fun remove(uri: String)
}
