package com.dn0ne.player.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsCharsetDecoderTest {

    @Test
    fun ascii_returns_utf8() {
        val input = "Hello, world! Line 1\nLine 2".toByteArray()
        assertEquals("Hello, world! Line 1\nLine 2", input.decodeLyrics())
    }

    @Test
    fun utf8_with_non_ascii_stays_utf8() {
        val input = "日本語の歌詞\nこんにちは".toByteArray(Charsets.UTF_8)
        assertEquals("日本語の歌詞\nこんにちは", input.decodeLyrics())
    }

    @Test
    fun utf8_bom_stays_utf8() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                "Lyrics content".toByteArray()
        assertEquals("﻿Lyrics content", bytes.decodeLyrics())
    }

    @Test
    fun iso_8859_1_falls_back_when_utf8_fails() {
        val input = byteArrayOf('L'.code.toByte(), 0xF6.toByte(), 'v'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte())
        assertEquals("Lövel", input.decodeLyrics())
    }

    @Test
    fun empty_input_stays_utf8() {
        assertEquals("", ByteArray(0).decodeLyrics())
    }

    @Test
    fun single_replacement_char_below_threshold() {
        val builder = StringBuilder()
        repeat(199) { builder.append('a') }
        builder.append('�')
        val input = builder.toString().toByteArray(Charsets.UTF_8)
        val result = input.decodeLyrics()
        assertEquals(200, result.length)
        assertEquals('�', result.last())
    }

    @Test
    fun many_replacement_chars_triggers_fallback() {
        val invalidUtf8 = ByteArray(100) { 0xF6.toByte() }
        val result = invalidUtf8.decodeLyrics()
        assertEquals(100, result.length)
        assertTrue(result.all { it == 'ö' })
    }
}
