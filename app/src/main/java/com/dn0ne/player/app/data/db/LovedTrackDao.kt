package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LovedTrackDao {

    @Query("SELECT uri FROM loved_tracks ORDER BY added_at DESC")
    fun observeUris(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM loved_tracks WHERE uri = :uri)")
    suspend fun isLoved(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: LovedTrackEntity)

    @Query("DELETE FROM loved_tracks WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}
