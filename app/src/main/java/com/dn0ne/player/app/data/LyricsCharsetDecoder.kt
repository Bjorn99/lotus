package com.dn0ne.player.app.data

internal fun ByteArray.decodeLyrics(): String {
    val utf8 = toString(Charsets.UTF_8)
    // U+FFFD appears when a byte sequence is invalid for the chosen charset.
    // Legitimate U+FFFD in song lyrics is vanishingly rare, so >1% is virtually
    // always a charset mismatch, not real replacement characters.
    val replacementRatio = utf8.count { it == '�' }.toDouble() / utf8.length.coerceAtLeast(1)
    return if (replacementRatio < 0.01) utf8
    else toString(Charsets.ISO_8859_1)
}
