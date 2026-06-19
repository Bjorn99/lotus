package com.dn0ne.player.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

class LyricsSidecarReader(private val contentResolver: ContentResolver) {

    fun readSidecarLyrics(
        audioFilePath: String,
        treeUri: Uri?,
    ): String? {
        if (treeUri == null) return null

        val basename = audioFilePath.substringAfterLast('/')
            .substringBeforeLast('.')
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)

        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, documentId
        )

        val cursor = try {
            contentResolver.query(
                childUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} = ?",
                arrayOf("$basename.lrc"),
                null,
            ) ?: return null
        } catch (_: SecurityException) {
            return null
        }

        return cursor.use { c ->
            if (c.moveToFirst()) {
                val docId = c.getString(0)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri, docId
                )
                try {
                    contentResolver.openInputStream(fileUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        bytes.decodeLyrics()
                    }
                } catch (_: SecurityException) {
                    null
                }
            } else null
        }
    }
}
