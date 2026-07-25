package com.dn0ne.player.app.data

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.text.Normalizer

class LyricsSidecarReader(private val contentResolver: ContentResolver) {

    // Look up a `<basename>.lrc` sidecar in the SAF tree the user picked.
    //
    // The obvious query — `WHERE COLUMN_DISPLAY_NAME = '<basename>.lrc'` —
    // works on the default Android DocumentsProvider but is silently
    // ignored by some OEM providers (notably Samsung's OneUI 5.1
    // provider on Android 13, per issues #100/#101). Those providers
    // return every child of the tree regardless of the selection
    // argument, which then causes:
    //   - #100: the caller always gets the first child's document ID,
    //     so every audio file's lyrics are the same wrong `.lrc` file.
    //   - #101: if the first child happens to be a subdirectory, the
    //     caller opens a directory as an input stream and the read()
    //     throws IOException(EISDIR) at the syscall layer.
    //
    // Defensive fix: don't rely on the provider honouring WHERE. Read
    // every child, skip directories by MIME type, compare display
    // names in Kotlin (which sidesteps any provider-side charset
    // quirks with non-Latin filenames), and swallow IOException on the
    // read so a mis-typed row can't crash the app.
    fun readSidecarLyrics(
        audioFilePath: String,
        treeUri: Uri?,
        title: String? = null,
    ): String? {
        if (treeUri == null) return null

        val targets = sidecarTargets(audioFilePath, title)

        val documentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return null
        }

        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, documentId
        )

        val cursor = try {
            contentResolver.query(
                childUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            ) ?: return null
        } catch (_: SecurityException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }

        return cursor.use { c ->
            val docId = findMatchingDocumentId(c.asDocumentRows(), targets) ?: return@use null
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, docId
            )
            try {
                contentResolver.openInputStream(fileUri)?.use { stream ->
                    stream.readBytes().decodeLyrics()
                }
            } catch (_: SecurityException) {
                null
            } catch (_: IOException) {
                // EISDIR if the row somehow refers to a directory
                // despite the mime filter, or any other transient I/O
                // failure. Don't crash — no lyrics available.
                null
            }
        }
    }

    private fun Cursor.asDocumentRows(): Sequence<DocumentRow> = sequence {
        while (moveToNext()) {
            yield(
                DocumentRow(
                    documentId = getString(0),
                    displayName = getString(1),
                    mimeType = getString(2),
                )
            )
        }
    }

    internal data class DocumentRow(
        val documentId: String?,
        val displayName: String?,
        val mimeType: String?,
    )

    companion object {
        // Builds the set of normalized candidate `<name>.lrc` sidecar names
        // for a track. The audio filename base is the primary convention; the
        // track title is a fallback, because a metadata/title edit does NOT
        // rename the file, so a user who names the .lrc after the displayed
        // title would otherwise never match the (unchanged) filename. Names
        // are normalized (NFC + trim + lowercase) so composition form,
        // surrounding whitespace, and case can't defeat the match. Extracted
        // as a pure function so the candidate logic is unit-testable.
        internal fun sidecarTargets(audioFilePath: String, title: String?): Set<String> {
            val filenameBase = audioFilePath.substringAfterLast('/')
                .substringBeforeLast('.')
            return buildSet {
                add("$filenameBase.lrc".normalizeForLrcMatch())
                if (!title.isNullOrBlank()) {
                    add("$title.lrc".normalizeForLrcMatch())
                }
            }
        }

        // Scans a sequence of (documentId, displayName, mimeType) rows for
        // the first non-directory row whose normalized display name is one of
        // [targets] (the targets are already normalized by the caller).
        // Returns the matching document ID, or null if nothing matches. Kept
        // separate from readSidecarLyrics so tests can exercise the
        // row-filtering logic without needing Robolectric to fake
        // DocumentsContract statics.
        internal fun findMatchingDocumentId(
            rows: Sequence<DocumentRow>,
            targets: Set<String>,
        ): String? {
            return rows.firstOrNull { row ->
                row.mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                    row.documentId != null &&
                    row.displayName != null &&
                    row.displayName.normalizeForLrcMatch() in targets
            }?.documentId
        }

        // Single-target convenience: normalizes [target] and delegates. Kept
        // so the existing regression tests and any single-name caller stay
        // simple.
        internal fun findMatchingDocumentId(
            rows: Sequence<DocumentRow>,
            target: String,
        ): String? = findMatchingDocumentId(rows, setOf(target.normalizeForLrcMatch()))
    }
}

// NFC-normalize + trim + lowercase so visually-identical names match
// regardless of Unicode composition form (a .lrc renamed in another app can
// land in NFD while the audio file is NFC, or vice versa), stray surrounding
// whitespace, or letter case. File-private top-level so it is in scope for
// both the instance reader and the companion matcher.
private fun String.normalizeForLrcMatch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFC).lowercase()
