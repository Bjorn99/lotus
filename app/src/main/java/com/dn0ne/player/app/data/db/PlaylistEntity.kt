package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the legacy Realm `PlaylistJson` schema one-to-one so the
 * RealmToRoomMigrator can copy rows verbatim. The `json` column holds the
 * serialized `Playlist` domain object — we intentionally keep the JSON-blob
 * shape rather than decomposing into columns so the migration is mechanical
 * and the serialization format stays the single source of truth.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "json")
    val json: String,
)
