package com.dn0ne.player.app.presentation.components.playlist

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dn0ne.player.R
import com.dn0ne.player.app.domain.playlist.M3uExporter
import com.dn0ne.player.app.domain.track.Track

/**
 * Returns a trigger function for exporting a list of tracks to an M3U8 file.
 *
 * Usage:
 *
 *   val exportM3u = rememberM3uExport()
 *   IconButton(onClick = { exportM3u(playlist.name ?: "playlist", playlist.trackList) }) { ... }
 *
 * The user picks the destination via Storage Access Framework
 * ([ActivityResultContracts.CreateDocument] with `audio/x-mpegurl`).
 *
 * A cached list is kept between "launch picker" and "picker returned" because
 * the system may recreate the activity during the flow; storing the intent in
 * Compose state is simpler than threading it through Parcelables.
 */
private const val LOG_TAG = "M3uExport"

@Composable
fun rememberM3uExport(): (String, List<Track>) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<List<Track>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val tracks = pending
        pending = null
        if (uri == null || tracks == null) return@rememberLauncherForActivityResult
        val body = M3uExporter.export(tracks).toByteArray(Charsets.UTF_8)
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(body) }
                ?: error("openOutputStream returned null for $uri")
            Toast.makeText(context, R.string.export_m3u_success, Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            // No point crashing: SAF can fail for a dozen reasons (removed SD
            // card, revoked permission, read-only destination). Tell the user
            // it didn't work so they can pick a different folder.
            Log.w(LOG_TAG, "Failed to write M3U to $uri", t)
            Toast.makeText(context, R.string.export_m3u_failure, Toast.LENGTH_SHORT).show()
        }
    }

    return { suggestedName, tracks ->
        pending = tracks
        launcher.launch(sanitiseFilename(suggestedName) + ".m3u8")
    }
}

// SAF accepts most characters but some file managers / filesystems choke on
// the classics. Strip those, collapse runs of whitespace, cap the length.
private fun sanitiseFilename(raw: String): String {
    val cleaned = raw
        .map { c -> if (c in INVALID_FILENAME_CHARS || c.isISOControl()) '_' else c }
        .joinToString("")
        .trim()
        .ifEmpty { "playlist" }
    return if (cleaned.length > MAX_NAME) cleaned.take(MAX_NAME) else cleaned
}

private val INVALID_FILENAME_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
private const val MAX_NAME = 120
