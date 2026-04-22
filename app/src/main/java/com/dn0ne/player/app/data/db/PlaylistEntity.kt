package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// The `json` column holds the serialized `Playlist` domain object — we keep
// the JSON-blob shape (rather than decomposing into columns) so the
// serialization format stays the single source of truth for both on-disk
// storage and the import / export code paths.
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "json")
    val json: String,
)
