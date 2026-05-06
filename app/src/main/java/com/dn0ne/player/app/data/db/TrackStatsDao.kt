package com.dn0ne.player.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entity: TrackStatsEntity)

    @Query("SELECT * FROM track_stats WHERE uri = :uri")
    suspend fun getByUri(uri: String): TrackStatsEntity?

    // Drives the stats screen — every section is derived in Kotlin from
    // this single observation so we don't issue four parallel queries
    // that all return overlapping subsets of the same table.
    @Query("SELECT * FROM track_stats")
    fun observeAll(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats ORDER BY play_count DESC, last_played_at DESC LIMIT :limit")
    fun observeTopByPlayCount(limit: Int): Flow<List<TrackStatsEntity>>

    @Query(
        "SELECT * FROM track_stats " +
            "WHERE last_played_at IS NOT NULL " +
            "ORDER BY last_played_at DESC LIMIT :limit"
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<TrackStatsEntity>>

    // Play event: bumps play_count, sets first_played_at if absent, advances
    // last_played_at monotonically. Listening time is owned by addListenedMs.
    @Query(
        "UPDATE track_stats " +
            "SET play_count = play_count + 1, " +
            "first_played_at = COALESCE(first_played_at, :now), " +
            "last_played_at = MAX(COALESCE(last_played_at, 0), :now) " +
            "WHERE uri = :uri"
    )
    suspend fun applyPlay(uri: String, now: Long)

    @Transaction
    suspend fun recordPlay(uri: String, now: Long) {
        insertIfMissing(TrackStatsEntity(uri = uri))
        applyPlay(uri = uri, now = now)
    }

    // Skip: only the count moves. A skip isn't a "play," so it shouldn't
    // touch first_played_at or last_played_at — those are reserved for
    // intentional listening per the design spec.
    @Query("UPDATE track_stats SET skip_count = skip_count + 1 WHERE uri = :uri")
    suspend fun applySkip(uri: String)

    @Transaction
    suspend fun recordSkip(uri: String) {
        insertIfMissing(TrackStatsEntity(uri = uri))
        applySkip(uri = uri)
    }

    @Query(
        "UPDATE track_stats " +
            "SET total_listening_ms = total_listening_ms + :ms " +
            "WHERE uri = :uri"
    )
    suspend fun applyListenedMs(uri: String, ms: Long)

    @Transaction
    suspend fun addListenedMs(uri: String, ms: Long) {
        if (ms <= 0L) return
        insertIfMissing(TrackStatsEntity(uri = uri))
        applyListenedMs(uri = uri, ms = ms)
    }

    // Used by backup-import after the merge math in TrackStatsMerge has
    // already produced the target row state. INSERT-or-REPLACE is fine
    // here because the caller is reconciling a complete row, not nudging
    // a counter.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReplacing(entity: TrackStatsEntity)

    @Query("DELETE FROM track_stats")
    suspend fun clearAll()
}
