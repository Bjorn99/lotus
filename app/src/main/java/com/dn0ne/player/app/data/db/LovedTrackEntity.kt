package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loved_tracks")
data class LovedTrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
