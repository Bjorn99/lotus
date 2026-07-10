package com.dn0ne.player.app.domain.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LyricsParserTest {

    @Test
    fun parses_standard_mm_ss_xx_timestamps() {
        val input = listOf(
            "[00:00.00] Intro",
            "[00:12.34] First line",
            "[01:23.45] Second line",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(3, result.size)
        assertEquals(0 to "Intro", result[0])
        assertEquals(12_340 to "First line", result[1])
        assertEquals(83_450 to "Second line", result[2])
    }

    @Test
    fun parses_single_digit_minutes() {
        val result = "[1:23.45] Line".toSyncedLyrics()

        assertEquals(1, result.size)
        assertEquals(83_450 to "Line", result[0])
    }

    @Test
    fun parses_three_digit_milliseconds() {
        val result = "[00:12.345] Line".toSyncedLyrics()

        assertEquals(12_345 to "Line", result[0])
    }

    @Test
    fun skips_lines_that_are_not_timestamped() {
        val input = listOf(
            "[ti: Song]",
            "[ar: Artist]",
            "[00:10.00] First actual line",
            "just a comment",
            "[00:20.00] Second actual line",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(2, result.size)
        assertEquals(10_000 to "First actual line", result[0])
        assertEquals(20_000 to "Second actual line", result[1])
    }

    @Test
    fun trims_whitespace_from_lyric_text() {
        val result = "[00:10.00]    indented line   ".toSyncedLyrics()

        assertEquals(10_000 to "indented line", result[0])
    }

    @Test
    fun empty_input_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            "".toSyncedLyrics()
        }
    }

    @Test
    fun only_metadata_throws() {
        val input = listOf(
            "[ti: Song]",
            "[ar: Artist]",
            "no tracks here",
        ).joinToString("\n")

        assertThrows(IllegalArgumentException::class.java) {
            input.toSyncedLyrics()
        }
    }

    // ---- Same-timestamp grouping (#102) ----

    // Reporter's exact example: original + Cyrillic translation on the
    // same time cue. Under the old parser these were two entries at the
    // same timestamp, which the downstream renderer treated as
    // sequential lines — the second one erased the first as it scrolled
    // past.
    @Test
    fun consecutive_same_timestamp_lines_merge_into_one_entry() {
        val input = listOf(
            "[01:05.99]Sin is broken, You have saved me",
            "[01:05.99]греха разчупи и спаси ме",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(1, result.size)
        assertEquals(
            65_990 to "Sin is broken, You have saved me\nгреха разчупи и спаси ме",
            result[0],
        )
    }

    @Test
    fun three_same_timestamp_lines_merge_preserving_source_order() {
        val input = listOf(
            "[00:10.00]First",
            "[00:10.00]Second",
            "[00:10.00]Third",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(1, result.size)
        assertEquals(10_000 to "First\nSecond\nThird", result[0])
    }

    @Test
    fun mixed_same_and_distinct_timestamps_are_grouped_and_sorted() {
        val input = listOf(
            "[00:20.00]Line B at 20s",
            "[00:10.00]Line A at 10s (original)",
            "[00:10.00]Line A at 10s (translation)",
            "[00:30.00]Line C at 30s",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(3, result.size)
        assertEquals(10_000 to "Line A at 10s (original)\nLine A at 10s (translation)", result[0])
        assertEquals(20_000 to "Line B at 20s", result[1])
        assertEquals(30_000 to "Line C at 30s", result[2])
    }

    @Test
    fun distinct_timestamps_are_unchanged_by_grouping() {
        // Regression guard: the grouping pass must not corrupt normal
        // LRC files where every line has its own timestamp.
        val input = listOf(
            "[00:10.00]Line A",
            "[00:20.00]Line B",
            "[00:30.00]Line C",
        ).joinToString("\n")

        val result = input.toSyncedLyrics()

        assertEquals(3, result.size)
        assertEquals(10_000 to "Line A", result[0])
        assertEquals(20_000 to "Line B", result[1])
        assertEquals(30_000 to "Line C", result[2])
    }
}
