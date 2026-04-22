package com.dn0ne.player.app.domain.playlist

import com.dn0ne.player.app.domain.playlist.M3uExporter.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uExporterTest {

    @Test
    fun empty_playlist_still_has_header() {
        val result = M3uExporter.exportEntries(emptyList())
        assertEquals("#EXTM3U\n", result)
    }

    @Test
    fun single_entry_renders_extinf_and_path() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry(
                    path = "/storage/emulated/0/Music/song.mp3",
                    durationMillis = 184_000,
                    title = "Hello",
                    artist = "Adele",
                )
            )
        )
        val expected = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXTINF:184,Adele - Hello")
            appendLine("/storage/emulated/0/Music/song.mp3")
        }
        assertEquals(expected, result)
    }

    @Test
    fun missing_artist_falls_back_to_title_only() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry(
                    path = "/music/instrumental.flac",
                    durationMillis = 60_000,
                    title = "Reverie",
                    artist = null,
                )
            )
        )
        assertTrue("expected label without dash, got:\n$result", result.contains("#EXTINF:60,Reverie\n"))
    }

    @Test
    fun missing_title_falls_back_to_filename_without_extension() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry(
                    path = "/music/track 09.ogg",
                    durationMillis = 0,
                    title = null,
                    artist = null,
                )
            )
        )
        // Duration is 0 (unknown) so we emit -1 per EXTM3U spec.
        assertTrue(result.contains("#EXTINF:-1,track 09\n"))
        assertTrue(result.contains("/music/track 09.ogg"))
    }

    @Test
    fun blank_metadata_is_treated_like_missing() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry(
                    path = "/music/a.mp3",
                    durationMillis = 1_000,
                    title = "   ",
                    artist = "",
                )
            )
        )
        // Both artist and title are blank; label falls back to filename.
        assertTrue(result.contains("#EXTINF:1,a\n"))
    }

    @Test
    fun newlines_in_metadata_do_not_break_format() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry(
                    path = "/m/x.mp3",
                    durationMillis = 30_000,
                    title = "Line1\nLine2",
                    artist = "Bad\r\nArtist",
                )
            )
        )
        // Line count must match: header + extinf + path = 3 newlines terminating 3 lines.
        assertEquals(3, result.count { it == '\n' })
        assertTrue(result.contains("#EXTINF:30,Bad  Artist - Line1 Line2\n"))
    }

    @Test
    fun duration_rounds_to_nearest_second() {
        assertTrue(
            M3uExporter.exportEntries(
                listOf(Entry("/x.mp3", 1_499, "t", "a"))
            ).contains("#EXTINF:1,")
        )
        assertTrue(
            M3uExporter.exportEntries(
                listOf(Entry("/x.mp3", 1_500, "t", "a"))
            ).contains("#EXTINF:2,")
        )
    }

    @Test
    fun negative_duration_reports_unknown() {
        val result = M3uExporter.exportEntries(
            listOf(Entry("/x.mp3", -5, "t", "a"))
        )
        assertTrue(result.contains("#EXTINF:-1,"))
    }

    @Test
    fun multiple_entries_are_written_in_order() {
        val result = M3uExporter.exportEntries(
            listOf(
                Entry("/one.mp3", 1_000, "One", "A"),
                Entry("/two.mp3", 2_000, "Two", "B"),
                Entry("/three.mp3", 3_000, "Three", "C"),
            )
        )
        val expected = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXTINF:1,A - One")
            appendLine("/one.mp3")
            appendLine("#EXTINF:2,B - Two")
            appendLine("/two.mp3")
            appendLine("#EXTINF:3,C - Three")
            appendLine("/three.mp3")
        }
        assertEquals(expected, result)
    }

    @Test
    fun output_uses_lf_not_crlf() {
        val result = M3uExporter.exportEntries(
            listOf(Entry("/x.mp3", 1_000, "t", "a"))
        )
        assertTrue(
            "output should not contain CR — M3U readers expect LF-terminated lines",
            !result.contains('\r')
        )
    }
}
