package com.dn0ne.player.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dn0ne.player.app.data.repository.LyricsJson
import com.dn0ne.player.app.data.repository.PlaylistJson
import com.dn0ne.player.core.data.Settings
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealmToRoomMigratorTest {

    private lateinit var context: Context
    private lateinit var db: LotusDatabase
    private lateinit var settings: Settings

    private val realmConfig by lazy {
        RealmConfiguration.create(schema = setOf(LyricsJson::class, PlaylistJson::class))
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().apply()
        runCatching { Realm.deleteRealm(realmConfig) }

        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = Settings(context)
    }

    @After
    fun tearDown() {
        db.close()
        runCatching { Realm.deleteRealm(realmConfig) }
    }

    @Test
    fun fresh_install_without_realm_file_marks_migration_done() = runBlocking {
        val migrator = RealmToRoomMigrator(context, db, settings)

        migrator.migrateIfNeeded()

        assertTrue(settings.realmToRoomMigrationDone)
        assertTrue(db.playlistDao().getAll().isEmpty())
        assertTrue(db.lyricsDao().getAll().isEmpty())
    }

    @Test
    fun existing_realm_rows_are_copied_to_room() = runBlocking {
        seedRealm(
            playlists = listOf("mix-a" to "{json-a}", "mix-b" to "{json-b}"),
            lyrics = listOf("content://song/1" to "lrc-1", "content://song/2" to "lrc-2"),
        )

        RealmToRoomMigrator(context, db, settings).migrateIfNeeded()

        val playlists = db.playlistDao().getAll().associate { it.name to it.json }
        assertEquals(mapOf("mix-a" to "{json-a}", "mix-b" to "{json-b}"), playlists)

        val lyrics = db.lyricsDao().getAll().associate { it.uri to it.json }
        assertEquals(mapOf("content://song/1" to "lrc-1", "content://song/2" to "lrc-2"), lyrics)

        assertTrue(settings.realmToRoomMigrationDone)
    }

    @Test
    fun migration_is_a_noop_after_flag_is_set() = runBlocking {
        seedRealm(playlists = listOf("original" to "{}"), lyrics = emptyList())
        RealmToRoomMigrator(context, db, settings).migrateIfNeeded()
        assertEquals(1, db.playlistDao().getAll().size)

        // Mutate the Room store to something the second run shouldn't touch.
        db.playlistDao().deleteByName("original")

        RealmToRoomMigrator(context, db, settings).migrateIfNeeded()

        assertTrue(db.playlistDao().getAll().isEmpty())
    }

    private fun seedRealm(
        playlists: List<Pair<String, String>>,
        lyrics: List<Pair<String, String>>,
    ) {
        val realm = Realm.open(realmConfig)
        try {
            realm.writeBlocking {
                playlists.forEach { (name, json) ->
                    copyToRealm(
                        PlaylistJson().apply {
                            this.name = name
                            this.json = json
                        },
                        UpdatePolicy.ALL,
                    )
                }
                lyrics.forEach { (uri, json) ->
                    copyToRealm(
                        LyricsJson().apply {
                            this.uri = uri
                            this.json = json
                        },
                        UpdatePolicy.ALL,
                    )
                }
            }
        } finally {
            realm.close()
        }
    }
}
