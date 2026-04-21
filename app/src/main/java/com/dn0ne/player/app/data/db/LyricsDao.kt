package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics_cache WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): LyricsEntity?

    @Query("SELECT * FROM lyrics_cache")
    suspend fun getAll(): List<LyricsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LyricsEntity>)

    @Query("UPDATE lyrics_cache SET json = :json WHERE uri = :uri")
    suspend fun updateJson(uri: String, json: String)

    @Query("DELETE FROM lyrics_cache WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
