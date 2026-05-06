package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    @ColumnInfo(name = "skip_count")
    val skipCount: Int = 0,
    @ColumnInfo(name = "total_listening_ms")
    val totalListeningMs: Long = 0,
    @ColumnInfo(name = "first_played_at")
    val firstPlayedAt: Long? = null,
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null,
)
