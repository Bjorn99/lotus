package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the legacy Realm `LyricsJson` schema one-to-one. The `json` column
 * holds the serialized `Lyrics` domain object.
 */
@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "json")
    val json: String,
)
