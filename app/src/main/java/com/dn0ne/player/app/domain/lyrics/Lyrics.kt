package com.dn0ne.player.app.domain.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val uri: String,
    val areFromRemote: Boolean = true,
    val plain: List<String>? = null,
    val synced: List<Pair<Int, String>>? = null
)

// [mm:ss.xx] or [m:ss.xx] or [mm:ss.xxx] — LRC timestamps in the wild have
// variable digit widths, so rely on the regex groups instead of fixed offsets.
private val SYNCED_LINE_REGEX = Regex("""^\[(\d{1,3}):(\d{1,2})\.(\d{1,3})](.*)$""")

fun String.toSyncedLyrics(): List<Pair<Int, String>> {
    val lines = split('\n')
        .mapNotNull { line ->
            val match = SYNCED_LINE_REGEX.matchEntire(line.trim()) ?: return@mapNotNull null
            val minutes = match.groupValues[1].toInt()
            val seconds = match.groupValues[2].toInt()
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                1 -> fraction.toInt() * 100
                2 -> fraction.toInt() * 10
                3 -> fraction.toInt()
                else -> return@mapNotNull null
            }
            val timestamp = minutes * 60_000 + seconds * 1_000 + fractionMs
            timestamp to match.groupValues[4].trim()
        }

    if (lines.isEmpty()) {
        throw IllegalArgumentException("Synced lines not found.")
    }

    // Merge lines that share a timestamp into a single entry (e.g.
    // bilingual LRCs where the original and the translation land on
    // the same cue). Compose Text renders '\n' as a soft break, so
    // downstream shows these as one paragraph — which is what issue
    // #102 asked for. groupBy preserves insertion order within each
    // group, so the source ordering (English first, translation
    // second — or whatever the file has) is preserved.
    return lines
        .groupBy { it.first }
        .map { (timestamp, group) ->
            timestamp to group.joinToString("\n") { it.second }
        }
        .sortedBy { it.first }
}
