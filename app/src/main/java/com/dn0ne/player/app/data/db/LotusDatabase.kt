package com.dn0ne.player.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        LyricsEntity::class,
        LovedTrackEntity::class,
        TrackStatsEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun lovedTrackDao(): LovedTrackDao
    abstract fun trackStatsDao(): TrackStatsDao

    companion object {
        const val NAME = "lotus.db"
    }
}
