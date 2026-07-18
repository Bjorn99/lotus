package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CoverArtColorDao {
    @Query("SELECT * FROM cover_art_colors WHERE cover_art_uri = :coverArtUri")
    suspend fun getColor(coverArtUri: String): CoverArtColorEntity?

    @Query("SELECT * FROM cover_art_colors")
    suspend fun getAllColors(): List<CoverArtColorEntity>

    @Query("SELECT cover_art_uri FROM cover_art_colors")
    suspend fun getCachedUris(): List<String>

    @Upsert
    suspend fun upsert(entity: CoverArtColorEntity)
}
