package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Parsed ReplayGain tags per file, so each file is copied and read once. All four
 * values null means the file was read and has no tags: caching that is the point,
 * or every untagged file would be copied again on every play.
 */
@Entity(tableName = "replaygain_cache")
data class ReplayGainCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long,
    @ColumnInfo(name = "size")
    val size: Long,
    @ColumnInfo(name = "track_gain_db")
    val trackGainDb: Float? = null,
    @ColumnInfo(name = "track_peak")
    val trackPeak: Float? = null,
    @ColumnInfo(name = "album_gain_db")
    val albumGainDb: Float? = null,
    @ColumnInfo(name = "album_peak")
    val albumPeak: Float? = null,
)
