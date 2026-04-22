package com.dn0ne.player.app.domain.playlist

import com.dn0ne.player.app.domain.track.Track
import kotlin.math.roundToInt

/**
 * Serializes a list of tracks into Extended M3U8 (UTF-8) format.
 *
 * Spec reference: https://en.wikipedia.org/wiki/M3U#Extended_M3U
 *
 *   #EXTM3U
 *   #EXTINF:<seconds>,<artist> - <title>
 *   <absolute-file-path>
 *
 * We use absolute paths from [Track.data] rather than relative paths because
 * Storage Access Framework abstracts away the final destination folder, so
 * we have nowhere stable to make paths relative to. Absolute paths are also
 * what other music players expect when the M3U is opened elsewhere.
 *
 * The formatter operates on a pure data [Entry] so it can be unit-tested
 * without Android types (Track holds Uri / MediaItem which require a device).
 */
object M3uExporter {

    private const val NEWLINE = "\n"
    private const val HEADER = "#EXTM3U"

    data class Entry(
        val path: String,
        val durationMillis: Int,
        val title: String?,
        val artist: String?,
    )

    fun export(tracks: List<Track>): String =
        exportEntries(tracks.map { it.toEntry() })

    fun exportEntries(entries: List<Entry>): String = buildString {
        append(HEADER)
        append(NEWLINE)
        entries.forEach { entry ->
            append("#EXTINF:")
            append(entry.durationSeconds())
            append(',')
            append(entry.extinfLabel())
            append(NEWLINE)
            append(entry.path)
            append(NEWLINE)
        }
    }

    private fun Track.toEntry() = Entry(
        path = data,
        durationMillis = duration,
        title = title,
        artist = artist,
    )

    private fun Entry.durationSeconds(): Int {
        // Duration is in milliseconds; EXTINF wants seconds, -1 if unknown.
        if (durationMillis <= 0) return -1
        return (durationMillis / 1000.0).roundToInt()
    }

    private fun Entry.extinfLabel(): String {
        val cleanArtist = artist?.sanitise()?.takeIf { it.isNotBlank() }
        val cleanTitle = title?.sanitise()?.takeIf { it.isNotBlank() }
            ?: path.substringAfterLast('/').substringBeforeLast('.')
        return if (cleanArtist != null) "$cleanArtist - $cleanTitle" else cleanTitle
    }

    // Sanitise so a single track's metadata can't break the multi-line format.
    private fun String.sanitise(): String =
        replace('\n', ' ').replace('\r', ' ').trim()
}
