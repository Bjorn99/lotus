package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LovedTrackDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: LovedTrackDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lovedTrackDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun isLoved_returns_true_after_insert() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 1000L))

        assertTrue(dao.isLoved("content://song/1"))
    }

    @Test
    fun isLoved_returns_false_for_missing_uri() = runBlocking {
        assertFalse(dao.isLoved("content://missing"))
    }

    @Test
    fun deleteByUri_removes_loved_track() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 1000L))
        assertTrue(dao.isLoved("content://song/1"))

        dao.deleteByUri("content://song/1")

        assertFalse(dao.isLoved("content://song/1"))
    }

    @Test
    fun observeUris_emits_updated_set_after_insert_and_delete() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/a", addedAt = 1000L))

        val first = dao.observeUris().first()
        assertEquals(listOf("content://song/a"), first)

        dao.insert(LovedTrackEntity(uri = "content://song/b", addedAt = 2000L))

        val second = dao.observeUris().first()
        assertEquals(listOf("content://song/b", "content://song/a"), second)

        dao.deleteByUri("content://song/a")

        val third = dao.observeUris().first()
        assertEquals(listOf("content://song/b"), third)
    }

    @Test
    fun insert_same_uri_twice_is_idempotent() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 1000L))
        dao.insert(LovedTrackEntity(uri = "content://song/1", addedAt = 9999L))

        val uris = dao.observeUris().first()
        assertEquals(1, uris.size)
        assertEquals("content://song/1", uris[0])
    }

    @Test
    fun observeUris_returns_most_recently_added_first() = runBlocking {
        dao.insert(LovedTrackEntity(uri = "content://oldest", addedAt = 1000L))
        dao.insert(LovedTrackEntity(uri = "content://middle", addedAt = 2000L))
        dao.insert(LovedTrackEntity(uri = "content://newest", addedAt = 3000L))

        val uris = dao.observeUris().first()

        assertEquals(
            listOf("content://newest", "content://middle", "content://oldest"),
            uris,
        )
    }
}
