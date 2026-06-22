package com.dn0ne.player.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_metadata")
data class TrackMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_data")
    val trackData: String,
    @ColumnInfo(name = "mb_album_id")
    val mbAlbumId: String? = null,
    @ColumnInfo(name = "mb_release_group_id")
    val mbReleaseGroupId: String? = null,
    @ColumnInfo(name = "mb_album_artist_id")
    val mbAlbumArtistId: String? = null,
)
