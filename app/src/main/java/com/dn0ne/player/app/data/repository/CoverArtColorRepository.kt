package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.data.db.CoverArtColorDao
import com.dn0ne.player.app.data.db.CoverArtColorEntity

class CoverArtColorRepository(
    private val dao: CoverArtColorDao,
) {
    suspend fun getDominantColor(coverArtUri: String): Int? {
        return dao.getColor(coverArtUri)?.dominantColor
    }

    suspend fun getAllDominantColors(): Map<String, Int> {
        return dao.getAllColors().associate { it.coverArtUri to it.dominantColor }
    }

    suspend fun getCachedUris(): Set<String> {
        return dao.getCachedUris().toSet()
    }

    suspend fun cacheDominantColor(coverArtUri: String, color: Int) {
        dao.upsert(CoverArtColorEntity(coverArtUri, color))
    }
}
