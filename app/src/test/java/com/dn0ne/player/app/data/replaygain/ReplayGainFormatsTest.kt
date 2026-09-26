package com.dn0ne.player.app.data.replaygain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainFormatsTest {

    @Test
    fun `opus files go to the opus reader`() {
        assertEquals(TagReaderKind.OPUS, tagReaderFor("opus"))
    }

    @Test
    fun `ogg containers are sniffed, since they may hold opus`() {
        assertEquals(TagReaderKind.OGG, tagReaderFor("ogg"))
        assertEquals(TagReaderKind.OGG, tagReaderFor("oga"))
    }

    @Test
    fun `formats jaudiotagger can open go to jaudiotagger, in any case`() {
        listOf("mp3", "flac", "m4a", "mp4", "m4b", "wav", "aiff", "wma", "dsf", "FLAC", "Mp3")
            .forEach { assertEquals(it, TagReaderKind.JAUDIOTAGGER, tagReaderFor(it)) }
    }

    @Test
    fun `formats nothing can read are skipped without a copy`() {
        // jaudiotagger 3.0.1 has no reader for these; copying them only to fail left
        // them uncached, re-copied on every prefetch and without the untagged fallback.
        listOf("aac", "wv", "ape", "mka", "webm", "amr", "3gp", "mpc", "")
            .forEach { assertEquals(it, TagReaderKind.NONE, tagReaderFor(it)) }
    }

    @Test
    fun `an ogg stream starting with OpusHead is opus`() {
        val page = "OggS".toByteArray() + ByteArray(24) + "OpusHead".toByteArray() + ByteArray(11)
        assertTrue(isOpusStream(page))
    }

    @Test
    fun `a vorbis ogg stream is not opus`() {
        val page = "OggS".toByteArray() + ByteArray(24) + byteArrayOf(1) + "vorbis".toByteArray()
        assertFalse(isOpusStream(page))
        assertFalse(isOpusStream(ByteArray(0)))
    }
}
