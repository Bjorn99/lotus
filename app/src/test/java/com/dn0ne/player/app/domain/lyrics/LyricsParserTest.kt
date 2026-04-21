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
}
