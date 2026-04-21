package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    /** Most-recently-inserted first, matching the original Realm behaviour. */
    @Query("SELECT * FROM playlists ORDER BY ROWID DESC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getAll(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PlaylistEntity>)

    @Query("UPDATE playlists SET json = :json WHERE name = :name")
    suspend fun updateJson(name: String, json: String)

    @Query("DELETE FROM playlists WHERE name = :name")
    suspend fun deleteByName(name: String)
}
