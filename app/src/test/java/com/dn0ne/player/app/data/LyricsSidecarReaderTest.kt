package com.dn0ne.player.app.data

import android.provider.DocumentsContract
import com.dn0ne.player.app.data.LyricsSidecarReader.Companion.findMatchingDocumentId
import com.dn0ne.player.app.data.LyricsSidecarReader.Companion.sidecarTargets
import com.dn0ne.player.app.data.LyricsSidecarReader.DocumentRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The regressions this test suite pins:
//
// #101 — App crashed when the picked lyrics folder contained only
// subfolders; the sidecar reader iterated onto a directory row and
// tried to open it as an InputStream, which threw IOException(EISDIR)
// on the syscall layer and killed the app.
//
// #100 — On Samsung's DocumentsProvider (OneUI 5.1 on Android 13),
// the WHERE clause in the previous query was silently ignored, so
// every audio file's lookup returned the same "first child" of the
// tree regardless of what basename was requested. Cyrillic-named
// files that weren't the first child never matched.
//
// The fix scans all children client-side, skips directories by MIME
// type, and matches display name case-insensitively — behaviour these
// tests lock in.
class LyricsSidecarReaderTest {

    private val fileMime = "text/plain"
    private val dirMime = DocumentsContract.Document.MIME_TYPE_DIR

    @Test
    fun empty_cursor_returns_null() {
        val result = findMatchingDocumentId(emptySequence(), "song.lrc")

        assertNull(result)
    }

    // #101 regression pin. Folder-of-subfolders must not crash and
    // must not return any match.
    @Test
    fun folder_of_only_directories_returns_null() {
        val rows = sequenceOf(
            DocumentRow("dir-1", "Albums", dirMime),
            DocumentRow("dir-2", "Playlists", dirMime),
            DocumentRow("dir-3", "song.lrc", dirMime), // pathological: directory named like a lyric file
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertNull(result)
    }

    @Test
    fun matching_file_returns_its_document_id() {
        val rows = sequenceOf(
            DocumentRow("doc-A", "song.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("doc-A", result)
    }

    // #100 regression pin. When the WHERE clause is ignored and every
    // row is returned, the caller must match by name — NOT just take
    // the first row.
    @Test
    fun non_matching_first_row_does_not_win_over_matching_later_row() {
        val rows = sequenceOf(
            DocumentRow("doc-1", "unrelated.lrc", fileMime),
            DocumentRow("doc-2", "target.lrc", fileMime),
            DocumentRow("doc-3", "another.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "target.lrc")

        assertEquals("doc-2", result)
    }

    // #100 regression pin. Cyrillic display names must compare
    // correctly in Kotlin, which was the observable failure on
    // Samsung's provider (where the equivalent SQLite comparison was
    // silently broken for non-Latin names).
    @Test
    fun cyrillic_display_name_matches() {
        val rows = sequenceOf(
            DocumentRow("doc-1", "Kalitka.lrc", fileMime),
            DocumentRow("doc-2", "Калитка.lrc", fileMime),
            DocumentRow("doc-3", "Дом.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "Дом.lrc")

        assertEquals("doc-3", result)
    }

    // #101 mixed-case scenario: a subdirectory happens to be named
    // the same as the target lyric file. The reader must skip the
    // directory by MIME type and return the real file's ID.
    @Test
    fun directory_with_matching_name_is_skipped_and_file_wins() {
        val rows = sequenceOf(
            DocumentRow("dir-shadow", "song.lrc", dirMime),
            DocumentRow("file-real", "song.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("file-real", result)
    }

    @Test
    fun display_name_match_is_case_insensitive() {
        val rows = sequenceOf(
            DocumentRow("doc-1", "SONG.LRC", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("doc-1", result)
    }

    @Test
    fun null_display_name_row_is_skipped() {
        val rows = sequenceOf(
            DocumentRow("doc-1", null, fileMime),
            DocumentRow("doc-2", "song.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("doc-2", result)
    }

    @Test
    fun null_document_id_row_is_skipped_even_if_name_matches() {
        val rows = sequenceOf(
            DocumentRow(null, "song.lrc", fileMime),
            DocumentRow("doc-real", "song.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("doc-real", result)
    }

    @Test
    fun no_matching_row_returns_null() {
        val rows = sequenceOf(
            DocumentRow("doc-1", "other.lrc", fileMime),
            DocumentRow("doc-2", "another.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, "target.lrc")

        assertNull(result)
    }

    // A .lrc renamed in another app can land in NFD (decomposed) while the
    // audio filename is NFC (composed), or vice versa. Visually identical
    // names must still match after normalization.
    @Test
    fun nfc_and_nfd_forms_of_the_same_name_match() {
        val composed = "caf" + "\u00E9" + ".lrc"      // NFC: precomposed e-acute U+00E9
        val decomposed = "cafe" + "\u0301" + ".lrc"   // NFD: e + combining acute U+0301
        val rows = sequenceOf(
            DocumentRow("doc-1", decomposed, fileMime),
        )

        val result = findMatchingDocumentId(rows, composed)

        assertEquals("doc-1", result)
    }

    @Test
    fun surrounding_whitespace_is_ignored() {
        val rows = sequenceOf(
            DocumentRow("doc-1", "  song.lrc  ", fileMime),
        )

        val result = findMatchingDocumentId(rows, "song.lrc")

        assertEquals("doc-1", result)
    }

    // The reported sidecar-rename case: the audio file keeps its original
    // name, but the user names the .lrc after the (edited) song title — a
    // title edit does NOT rename the file. The title candidate must match
    // even though the filename candidate doesn't.
    @Test
    fun title_candidate_matches_when_filename_does_not() {
        val targets = sidecarTargets("/music/01 - track.mp3", "Actual Title")
        val rows = sequenceOf(
            DocumentRow("doc-1", "Actual Title.lrc", fileMime),
        )

        val result = findMatchingDocumentId(rows, targets)

        assertEquals("doc-1", result)
    }

    @Test
    fun sidecar_targets_include_filename_and_title_candidates() {
        val targets = sidecarTargets("/music/01 - track.mp3", "Actual Title")

        assertTrue(targets.contains("01 - track.lrc"))
        assertTrue(targets.contains("actual title.lrc"))
    }

    @Test
    fun sidecar_targets_omit_blank_title() {
        assertEquals(setOf("song.lrc"), sidecarTargets("/music/song.mp3", null))
        assertEquals(setOf("song.lrc"), sidecarTargets("/music/song.mp3", "   "))
    }
}
