package com.dn0ne.player.app.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        LyricsEntity::class,
        LovedTrackEntity::class,
        TrackStatsEntity::class,
        TrackMetadataEntity::class,
        CoverArtColorEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
    ],
    exportSchema = true,
)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun lovedTrackDao(): LovedTrackDao
    abstract fun trackStatsDao(): TrackStatsDao
    abstract fun trackMetadataDao(): TrackMetadataDao
    abstract fun coverArtColorDao(): CoverArtColorDao

    companion object {
        const val NAME = "lotus.db"
    }
}
