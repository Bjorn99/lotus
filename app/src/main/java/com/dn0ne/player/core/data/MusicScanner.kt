package com.dn0ne.player.core.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.media.MediaScannerConnection
import android.os.Environment
import com.dn0ne.player.R
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarAction
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarController
import com.dn0ne.player.app.presentation.components.snackbar.SnackbarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicScanner(
    private val context: Context,
    private val settings: Settings
) {
    private val allowedExtensions = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a")

    suspend fun refreshMedia(showMessages: Boolean = true, onComplete: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                val isScanModeInclusive = settings.isScanModeInclusive.value

                val directoriesToScan = if (isScanModeInclusive) {
                    settings.extraScanFolders.value.map { File(it) }.toMutableList().apply {
                        if (settings.scanMusicFolder.value) {
                            add(
                                Environment
                                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                            )
                        }
                    }
                } else {
                    listOf(
                        Environment.getExternalStorageDirectory()
                    )
                }

                val excludedFromScan = settings.excludedScanFolders.value


                val paths = directoriesToScan.flatMap { directory ->
                    directory.walkTopDown()
                        .onEnter { if (isScanModeInclusive) true else it.absolutePath !in excludedFromScan }
                        .filter { it.isFile && it.extension.lowercase() in allowedExtensions }
                        .map { it.absolutePath }
                }.toTypedArray()

                if (paths.isEmpty()) {
                    if (showMessages) {
                        SnackbarController.sendEvent(
                            event = SnackbarEvent(
                                message = R.string.nothing_to_refresh
                            )
                        )
                    }
                } else {
                    MediaScannerConnection.scanFile(
                        context,
                        paths,
                        arrayOf("audio/*"),
                        null
                    )

                    if (showMessages) {
                        SnackbarController.sendEvent(
                            event = SnackbarEvent(
                                message = R.string.refreshed_successfully
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                if (!showMessages) return@withContext
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = R.string.failed_to_refresh,
                        action = SnackbarAction(
                            name = R.string.copy_error,
                            action = {
                                val clipboardManager =
                                    context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip =
                                    ClipData.newPlainText(
                                        null,
                                        e.message + "\n" + e.stackTrace.joinToString("\n")
                                    )
                                clipboardManager?.setPrimaryClip(clip)
                            }
                        )
                    )
                )
            }
            onComplete()
        }
    }

    suspend fun scanFolder(path: String, onComplete: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    arrayOf("audio/*"),
                    null
                )

                SnackbarController.sendEvent(
                    event = SnackbarEvent(
                        message = R.string.scanned_successfully
                    )
                )
            } catch (e: Exception) {
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = R.string.failed_to_scan,
                        action = SnackbarAction(
                            name = R.string.copy_error,
                            action = {
                                val clipboardManager =
                                    context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip =
                                    ClipData.newPlainText(
                                        null,
                                        e.message + "\n" + e.stackTrace.joinToString("\n")
                                    )
                                clipboardManager?.setPrimaryClip(clip)
                            }
                        )
                    )
                )
            }
            onComplete()
        }
    }
}