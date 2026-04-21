package com.dn0ne.player.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, LyricsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LotusDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        const val NAME = "lotus.db"
    }
}
