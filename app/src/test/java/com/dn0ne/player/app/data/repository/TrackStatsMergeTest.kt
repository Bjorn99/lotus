package com.dn0ne.player.app.data.repository

import com.dn0ne.player.app.domain.track.TrackStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TrackStatsMergeTest {

    private val uri = "content://media/external/audio/media/42"

    private fun stats(
        playCount: Int = 0,
        skipCount: Int = 0,
        firstPlayedAt: Long? = null,
        lastPlayedAt: Long? = null,
        totalListeningMs: Long = 0L,
    ) = TrackStats(
        uri = uri,
        playCount = playCount,
        skipCount = skipCount,
        firstPlayedAt = firstPlayedAt,
        lastPlayedAt = lastPlayedAt,
        totalListeningMs = totalListeningMs,
    )

    @Test
    fun missing_local_returns_incoming_unchanged() {
        val incoming = stats(playCount = 3, totalListeningMs = 60_000L)
        val merged = mergeStats(local = null, incoming = incoming)
        assertEquals(incoming, merged)
    }

    @Test
    fun counters_take_max_per_field() {
        val local = stats(playCount = 5, skipCount = 2, totalListeningMs = 90_000L)
        val incoming = stats(playCount = 3, skipCount = 7, totalListeningMs = 60_000L)
        val merged = mergeStats(local, incoming)
        assertEquals(5, merged.playCount)
        assertEquals(7, merged.skipCount)
        assertEquals(90_000L, merged.totalListeningMs)
    }

    @Test
    fun first_played_takes_minimum_last_played_takes_maximum() {
        val local = stats(firstPlayedAt = 2_000L, lastPlayedAt = 9_000L)
        val incoming = stats(firstPlayedAt = 1_000L, lastPlayedAt = 5_000L)
        val merged = mergeStats(local, incoming)
        assertEquals(1_000L, merged.firstPlayedAt)
        assertEquals(9_000L, merged.lastPlayedAt)
    }

    @Test
    fun null_timestamps_defer_to_whichever_side_has_a_value() {
        val local = stats(firstPlayedAt = null, lastPlayedAt = null)
        val incoming = stats(firstPlayedAt = 1_000L, lastPlayedAt = 5_000L)
        val merged = mergeStats(local, incoming)
        assertEquals(1_000L, merged.firstPlayedAt)
        assertEquals(5_000L, merged.lastPlayedAt)

        val flipped = mergeStats(local = incoming, incoming = local)
        assertEquals(1_000L, flipped.firstPlayedAt)
        assertEquals(5_000L, flipped.lastPlayedAt)
    }

    @Test
    fun re_importing_same_backup_is_a_no_op() {
        // The user runs export, then immediately re-imports the same file:
        // the merged row should equal the local row that produced the export.
        val local = stats(
            playCount = 5,
            skipCount = 2,
            firstPlayedAt = 1_000L,
            lastPlayedAt = 9_000L,
            totalListeningMs = 90_000L,
        )
        val merged = mergeStats(local = local, incoming = local)
        assertEquals(local, merged)
    }

    @Test
    fun older_backup_never_erases_newer_local_activity() {
        // User exported a backup last month, listened to a track 5 more
        // times since, then restored. The merge must not roll their
        // counters back to the older numbers.
        val newerLocal = stats(
            playCount = 10,
            skipCount = 3,
            firstPlayedAt = 1_000L,
            lastPlayedAt = 50_000L,
            totalListeningMs = 200_000L,
        )
        val olderBackup = stats(
            playCount = 5,
            skipCount = 1,
            firstPlayedAt = 1_000L,
            lastPlayedAt = 20_000L,
            totalListeningMs = 100_000L,
        )
        val merged = mergeStats(local = newerLocal, incoming = olderBackup)
        assertEquals(newerLocal, merged)
    }

    @Test
    fun merge_picks_up_earliest_first_play_from_either_side() {
        // Two devices that started tracking the same track at different
        // times: the cross-imported view should reflect the earliest.
        val deviceA = stats(playCount = 3, firstPlayedAt = 5_000L, lastPlayedAt = 7_000L)
        val deviceB = stats(playCount = 4, firstPlayedAt = 2_000L, lastPlayedAt = 6_000L)
        val merged = mergeStats(local = deviceA, incoming = deviceB)
        assertNotNull(merged.firstPlayedAt)
        assertEquals(2_000L, merged.firstPlayedAt)
        assertEquals(7_000L, merged.lastPlayedAt)
        assertEquals(4, merged.playCount)
    }
}
