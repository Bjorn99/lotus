package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dn0ne.player.app.di.MIGRATION_1_2
import com.dn0ne.player.app.di.MIGRATION_2_3
import com.dn0ne.player.app.di.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LotusDatabaseMigrationTest {

    // Uses MigrationTestHelper with auto-migration specs empty (no auto-
    // migrations) and FrameworkSQLiteOpenHelperFactory so Room creates
    // databases directly on-device. Schema JSON files for versions 1 and 2
    // were hand-crafted from the v3 export by removing the tables added in
    // each subsequent version, with identityHash updated to match.
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LotusDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate_1_to_2_creates_loved_tracks_table() {
        val db = helper.createDatabase(LotusDatabase.NAME, 1)
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            2,
            true,
            MIGRATION_1_2,
        )

        val cursor = migrated.query("SELECT uri, added_at FROM loved_tracks")
        assertEquals(2, cursor.columnCount)
        assertEquals("uri", cursor.getColumnName(0))
        assertEquals("added_at", cursor.getColumnName(1))
        cursor.close()
        migrated.close()
    }

    @Test
    fun migrate_2_to_3_creates_track_stats_table() {
        val db = helper.createDatabase(LotusDatabase.NAME, 2)
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            3,
            true,
            MIGRATION_2_3,
        )

        val cursor = migrated.query(
            "SELECT uri, play_count, skip_count, total_listening_ms, " +
                "first_played_at, last_played_at FROM track_stats"
        )
        assertEquals(6, cursor.columnCount)
        assertEquals("uri", cursor.getColumnName(0))
        assertEquals("play_count", cursor.getColumnName(1))
        assertEquals("skip_count", cursor.getColumnName(2))
        assertEquals("total_listening_ms", cursor.getColumnName(3))
        assertEquals("first_played_at", cursor.getColumnName(4))
        assertEquals("last_played_at", cursor.getColumnName(5))
        cursor.close()
        migrated.close()
    }

    @Test
    fun migrate_1_to_3_applies_both_migrations() {
        val db = helper.createDatabase(LotusDatabase.NAME, 1)
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
        )

        val lovedCursor = migrated.query("SELECT uri, added_at FROM loved_tracks")
        assertEquals(2, lovedCursor.columnCount)
        lovedCursor.close()

        val statsCursor = migrated.query(
            "SELECT uri, play_count, skip_count, total_listening_ms, " +
                "first_played_at, last_played_at FROM track_stats"
        )
        assertEquals(6, statsCursor.columnCount)
        statsCursor.close()

        migrated.close()
    }

    @Test
    fun migrate_4_to_5_creates_cover_art_colors_table() {
        val db = helper.createDatabase(LotusDatabase.NAME, 4)
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            LotusDatabase.NAME,
            5,
            true,
            MIGRATION_4_5,
        )

        val cursor = migrated.query(
            "SELECT cover_art_uri, dominant_color FROM cover_art_colors"
        )
        assertEquals(2, cursor.columnCount)
        assertEquals("cover_art_uri", cursor.getColumnName(0))
        assertEquals("dominant_color", cursor.getColumnName(1))
        cursor.close()
        migrated.close()
    }
}
