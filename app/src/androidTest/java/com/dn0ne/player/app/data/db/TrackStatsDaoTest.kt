package com.dn0ne.player.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackStatsDaoTest {

    private lateinit var db: LotusDatabase
    private lateinit var dao: TrackStatsDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LotusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.trackStatsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun recordPlay_sets_first_and_last_played_at() = runBlocking {
        dao.recordPlay("content://song/1", now = 5000L)

        val row = dao.getByUri("content://song/1")
        assertNotNull(row)
        assertEquals(1, row!!.playCount)
        assertEquals(5000L, row.firstPlayedAt)
        assertEquals(5000L, row.lastPlayedAt)
        assertEquals(0, row.skipCount)
    }

    @Test
    fun second_recordPlay_increments_count_updates_last_played_at_leaves_first_alone() = runBlocking {
        dao.recordPlay("content://song/1", now = 1000L)
        dao.recordPlay("content://song/1", now = 2000L)

        val row = dao.getByUri("content://song/1")
        assertNotNull(row)
        assertEquals(2, row!!.playCount)
        assertEquals(1000L, row.firstPlayedAt)
        assertEquals(2000L, row.lastPlayedAt)
    }

    @Test
    fun recordSkip_increments_skip_count_does_not_touch_play_timestamps() = runBlocking {
        dao.recordPlay("content://song/1", now = 1000L)
        dao.recordSkip("content://song/1")

        val row = dao.getByUri("content://song/1")
        assertNotNull(row)
        assertEquals(1, row!!.playCount)
        assertEquals(1, row.skipCount)
        assertEquals(1000L, row.firstPlayedAt)
        assertEquals(1000L, row.lastPlayedAt)
    }

    @Test
    fun addListenedMs_sums_correctly_across_multiple_calls() = runBlocking {
        dao.addListenedMs("content://song/1", ms = 30_000L)
        dao.addListenedMs("content://song/1", ms = 15_000L)

        val row = dao.getByUri("content://song/1")
        assertNotNull(row)
        assertEquals(45_000L, row!!.totalListeningMs)
    }

    @Test
    fun addListenedMs_zero_or_negative_is_no_op() = runBlocking {
        dao.addListenedMs("content://song/1", ms = 0L)
        dao.addListenedMs("content://song/1", ms = -1L)

        // The guard returns before insertIfMissing, so no row is created at all
        val row = dao.getByUri("content://song/1")
        assertNull(row)
    }

    @Test
    fun observeTopByPlayCount_returns_limited_rows_ordered_by_play_count_desc() = runBlocking {
        dao.recordPlay("a", now = 0L)
        dao.recordPlay("b", now = 0L); dao.recordPlay("b", now = 0L)
        dao.recordPlay("c", now = 0L); dao.recordPlay("c", now = 0L); dao.recordPlay("c", now = 0L)

        val top = dao.observeTopByPlayCount(2).first()

        assertEquals(2, top.size)
        assertEquals("c", top[0].uri)
        assertEquals(3, top[0].playCount)
        assertEquals("b", top[1].uri)
        assertEquals(2, top[1].playCount)
    }

    @Test
    fun observeRecentlyPlayed_returns_only_rows_with_non_null_last_played_at() = runBlocking {
        // recordSkip alone does not set last_played_at
        dao.recordSkip("skip-only")

        // recordPlay sets last_played_at
        dao.recordPlay("played-a", now = 2000L)
        dao.recordPlay("played-b", now = 1000L)

        val recent = dao.observeRecentlyPlayed(limit = 10).first()

        assertEquals(2, recent.size)
        assertEquals("played-a", recent[0].uri) // most recent first
        assertEquals("played-b", recent[1].uri)
    }

    @Test
    fun observeAll_emits_on_insert_and_update() = runBlocking {
        val initial = dao.observeAll().first()
        assertTrue(initial.isEmpty())

        dao.recordPlay("a", now = 0L)

        val afterInsert = dao.observeAll().first()
        assertEquals(1, afterInsert.size)

        dao.recordPlay("a", now = 0L)

        val afterUpdate = dao.observeAll().first()
        assertEquals(1, afterUpdate.size)
        assertEquals(2, afterUpdate[0].playCount)
    }

    @Test
    fun upsertReplacing_overwrites_existing_row() = runBlocking {
        dao.recordPlay("uri", now = 1000L)

        dao.upsertReplacing(
            TrackStatsEntity(
                uri = "uri",
                playCount = 99,
                skipCount = 1,
                totalListeningMs = 500_000L,
                firstPlayedAt = 500L,
                lastPlayedAt = 2000L,
            )
        )

        val row = dao.getByUri("uri")
        assertNotNull(row)
        assertEquals(99, row!!.playCount)
        assertEquals(1, row.skipCount)
        assertEquals(500_000L, row.totalListeningMs)
        assertEquals(500L, row.firstPlayedAt)
        assertEquals(2000L, row.lastPlayedAt)
    }
}
