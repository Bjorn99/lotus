package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrackStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entity: TrackStatsEntity)

    @Query(
        "UPDATE track_stats " +
            "SET play_count = play_count + :playInc, " +
            "skip_count = skip_count + :skipInc, " +
            "first_played_at = COALESCE(first_played_at, :now), " +
            "last_played_at = MAX(COALESCE(last_played_at, 0), :now) " +
            "WHERE uri = :uri"
    )
    suspend fun applyTally(uri: String, playInc: Int, skipInc: Int, now: Long)

    @Query(
        "UPDATE track_stats " +
            "SET total_listening_ms = total_listening_ms + :ms " +
            "WHERE uri = :uri"
    )
    suspend fun applyListeningMs(uri: String, ms: Long)

    @Transaction
    suspend fun recordEvent(uri: String, isPlay: Boolean, now: Long) {
        insertIfMissing(TrackStatsEntity(uri = uri))
        applyTally(
            uri = uri,
            playInc = if (isPlay) 1 else 0,
            skipInc = if (isPlay) 0 else 1,
            now = now,
        )
    }

    @Transaction
    suspend fun addListeningMs(uri: String, ms: Long) {
        insertIfMissing(TrackStatsEntity(uri = uri))
        applyListeningMs(uri = uri, ms = ms)
    }

    @Query("DELETE FROM track_stats")
    suspend fun clearAll()
}
