package com.dn0ne.player.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        LyricsEntity::class,
        LovedTrackEntity::class,
        TrackStatsEntity::class,
        TrackMetadataEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun lovedTrackDao(): LovedTrackDao
    abstract fun trackStatsDao(): TrackStatsDao
    abstract fun trackMetadataDao(): TrackMetadataDao

    companion object {
        const val NAME = "lotus.db"
    }
}
