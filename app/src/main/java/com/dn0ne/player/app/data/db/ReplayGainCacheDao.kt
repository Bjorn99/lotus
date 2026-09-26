package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReplayGainCacheDao {

    @Query("SELECT * FROM replaygain_cache WHERE uri = :uri")
    suspend fun get(uri: String): ReplayGainCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReplayGainCacheEntity)
}
