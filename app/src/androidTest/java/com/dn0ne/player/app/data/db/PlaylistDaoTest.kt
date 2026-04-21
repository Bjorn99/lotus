package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_and_getAll_round_trip() = runBlocking {
        dao.upsert(PlaylistEntity(name = "mix", json = """{"name":"mix","trackList":[]}"""))

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("mix", rows[0].name)
    }

    @Test
    fun upsert_replaces_on_same_primary_key() = runBlocking {
        dao.upsert(PlaylistEntity(name = "mix", json = "first"))
        dao.upsert(PlaylistEntity(name = "mix", json = "second"))

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("second", rows[0].json)
    }

    @Test
    fun observeAll_returns_rows_most_recent_first() = runBlocking {
        dao.upsert(PlaylistEntity(name = "first", json = "{}"))
        dao.upsert(PlaylistEntity(name = "second", json = "{}"))
        dao.upsert(PlaylistEntity(name = "third", json = "{}"))

        val rows = dao.observeAll().first()
        assertEquals(listOf("third", "second", "first"), rows.map { it.name })
    }

    @Test
    fun updateJson_changes_payload_not_name() = runBlocking {
        dao.upsert(PlaylistEntity(name = "mix", json = "old"))

        dao.updateJson(name = "mix", json = "new")

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("new", rows[0].json)
    }

    @Test
    fun deleteByName_removes_only_matching_row() = runBlocking {
        dao.upsert(PlaylistEntity(name = "keep", json = "{}"))
        dao.upsert(PlaylistEntity(name = "drop", json = "{}"))

        dao.deleteByName("drop")

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals("keep", rows[0].name)
    }

    @Test
    fun upsertAll_inserts_all_rows() = runBlocking {
        dao.upsertAll(
            listOf(
                PlaylistEntity(name = "a", json = "{}"),
                PlaylistEntity(name = "b", json = "{}"),
            )
        )

        assertEquals(2, dao.getAll().size)
    }

    @Test
    fun getAll_on_empty_returns_empty_list() = runBlocking {
        assertTrue(dao.getAll().isEmpty())
        assertNull(dao.getAll().firstOrNull())
    }
}
