package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricsDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: LyricsDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lyricsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getByUri_returns_inserted_row() = runBlocking {
        dao.upsert(LyricsEntity(uri = "content://song/1", json = "payload"))

        val row = dao.getByUri("content://song/1")
        assertEquals("payload", row?.json)
    }

    @Test
    fun getByUri_returns_null_for_missing_row() = runBlocking {
        assertNull(dao.getByUri("content://missing"))
    }

    @Test
    fun upsert_replaces_on_same_uri() = runBlocking {
        dao.upsert(LyricsEntity(uri = "u", json = "first"))
        dao.upsert(LyricsEntity(uri = "u", json = "second"))

        assertEquals("second", dao.getByUri("u")?.json)
    }

    @Test
    fun updateJson_changes_payload() = runBlocking {
        dao.upsert(LyricsEntity(uri = "u", json = "old"))

        dao.updateJson(uri = "u", json = "new")

        assertEquals("new", dao.getByUri("u")?.json)
    }

    @Test
    fun deleteByUri_removes_row() = runBlocking {
        dao.upsert(LyricsEntity(uri = "u", json = "x"))

        dao.deleteByUri("u")

        assertNull(dao.getByUri("u"))
    }

    @Test
    fun upsertAll_then_getAll_returns_all_rows() = runBlocking {
        dao.upsertAll(
            listOf(
                LyricsEntity(uri = "a", json = "1"),
                LyricsEntity(uri = "b", json = "2"),
            )
        )

        assertEquals(2, dao.getAll().size)
    }
}
