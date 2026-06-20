package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrackMetadataEntity)

    @Query("SELECT * FROM track_metadata WHERE track_data = :trackData")
    suspend fun getByTrackData(trackData: String): TrackMetadataEntity?
}
