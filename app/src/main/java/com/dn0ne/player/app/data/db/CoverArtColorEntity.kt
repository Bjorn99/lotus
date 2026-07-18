package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cover_art_colors")
data class CoverArtColorEntity(
    @PrimaryKey
    @ColumnInfo(name = "cover_art_uri")
    val coverArtUri: String,
    @ColumnInfo(name = "dominant_color")
    val dominantColor: Int,
)
