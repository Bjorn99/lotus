package com.dn0ne.player.app.data.replaygain

/** Which reader can pull ReplayGain tags out of a file, decided from its extension. */
enum class TagReaderKind {
    /** OpusTagEditor. jaudiotagger 3.0.1 can't open .opus at all. */
    OPUS,

    /** .ogg/.oga may hold Opus or Vorbis: sniff the first page, see [isOpusStream]. */
    OGG,

    JAUDIOTAGGER,

    /** Nothing here can read it. Treated as untagged, without copying the file. */
    NONE,
}

// jaudiotagger 3.0.1's SupportedFileFormat, read from the jar with javap, minus the
// Ogg suffixes handled above.
private val JAUDIOTAGGER_EXTENSIONS = setOf(
    "mp3", "flac", "mp4", "m4a", "m4p", "m4b", "wma", "wav",
    "ra", "rm", "aif", "aiff", "aifc", "dsf", "dff",
)

fun tagReaderFor(extension: String): TagReaderKind =
    when (val ext = extension.lowercase()) {
        "opus" -> TagReaderKind.OPUS
        "ogg", "oga" -> TagReaderKind.OGG
        in JAUDIOTAGGER_EXTENSIONS -> TagReaderKind.JAUDIOTAGGER
        else -> TagReaderKind.NONE
    }

private val OPUS_HEAD = "OpusHead".toByteArray(Charsets.US_ASCII)

/**
 * True when the first Ogg page carries an Opus identification header. [head] is the
 * start of the file; the first page's payload begins within its first ~300 bytes.
 */
fun isOpusStream(head: ByteArray): Boolean =
    (0..head.size - OPUS_HEAD.size).any { start ->
        OPUS_HEAD.indices.all { head[start + it] == OPUS_HEAD[it] }
    }
