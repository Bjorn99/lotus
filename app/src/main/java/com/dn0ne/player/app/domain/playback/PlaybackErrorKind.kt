package com.dn0ne.player.app.domain.playback

/**
 * Maps a Media3 [PlaybackException.errorCode] to a user-facing error category.
 *
 * Code ranges (from Media3's documented constants):
 * - 2xxx: I/O errors (file not found, permission denied, network)
 * - 3xxx: parsing/container errors (malformed, unsupported container)
 * - 4xxx: decoder errors (codec not found, format unsupported, init failed)
 * - 5xxx: audio renderer errors (not reliably about the file)
 * - 6xxx: DRM errors (not reliably about the file)
 *
 * Renderer (5xxx) and DRM (6xxx) failures map to [PlaybackErrorKind.Unknown]
 * because telling the user "this format can't be played" would misinform them
 * — those failures aren't about the file itself.
 */
enum class PlaybackErrorKind {
    UnsupportedFormat,
    FileUnreadable,
    Unknown,
}

fun playbackErrorKind(errorCode: Int): PlaybackErrorKind = when (errorCode) {
    in 2000..2999 -> PlaybackErrorKind.FileUnreadable
    in 3000..4999 -> PlaybackErrorKind.UnsupportedFormat
    else -> PlaybackErrorKind.Unknown
}
