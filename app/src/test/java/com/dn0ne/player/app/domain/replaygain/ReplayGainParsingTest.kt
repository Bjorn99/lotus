package com.dn0ne.player.app.domain.replaygain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplayGainParsingTest {

    private val eps = 1e-4f

    @Test
    fun `gain accepts the usual spellings`() {
        assertEquals(-7.85f, parseRgGainDb("-7.85 dB")!!, eps)
        assertEquals(3.2f, parseRgGainDb("+3.2 dB")!!, eps)
        assertEquals(-7.85f, parseRgGainDb("-7.85")!!, eps)
        assertEquals(-7.85f, parseRgGainDb(" -7.85dB ")!!, eps)
        assertEquals(-7.85f, parseRgGainDb("-7.85 DB")!!, eps)
        assertEquals(0.5f, parseRgGainDb("0,5 dB")!!, eps)
    }

    @Test
    fun `zero gain is a real value, not absent`() {
        assertEquals(0f, parseRgGainDb("0.00 dB")!!, 0f)
    }

    @Test
    fun `gain rejects garbage and absurd values`() {
        listOf("", "abc", "NaN", "Infinity", "1e3", "-7.85 dB extra", "--3", "-900 dB", "60.5")
            .forEach { assertNull(it, parseRgGainDb(it)) }
        assertEquals(60f, parseRgGainDb("60")!!, 0f)
    }

    @Test
    fun `peak parses, above 1 is valid, zero and negative are not`() {
        assertEquals(0.988525f, parseRgPeak("0.988525")!!, eps)
        assertEquals(1.2f, parseRgPeak("1.2")!!, eps)
        assertNull(parseRgPeak("0"))
        assertNull(parseRgPeak("-0.5"))
        assertNull(parseRgPeak("NaN"))
    }

    @Test
    fun `r128 is q7_8 plus the 5 dB reference offset`() {
        assertEquals(-573f / 256f + 5f, parseR128GainDb("-573")!!, eps)
        assertEquals(10f, parseR128GainDb("1280")!!, eps)
        assertEquals(-7f, parseR128GainDb("-3072")!!, eps)
        assertEquals(10f, parseR128GainDb("+1280")!!, eps)
        assertNull(parseR128GainDb("-5.5"))
        assertNull(parseR128GainDb("40000"))
    }

    @Test
    fun `fields build both tiers from vorbis keys in any case`() {
        val state = tagStateFromFields(
            listOf(
                "replaygain_track_gain" to "-7.00 dB",
                "REPLAYGAIN_TRACK_PEAK" to "0.881049",
                "ReplayGain_Album_Gain" to "-6.50 dB",
                "REPLAYGAIN_ALBUM_PEAK" to "0.95",
            )
        )
        assertEquals(
            TagState.Tagged(ReplayGainTags(GainPeak(-7f, 0.881049f), GainPeak(-6.5f, 0.95f))),
            state,
        )
    }

    @Test
    fun `txxx and mp4 freeform prefixes are ignored`() {
        val mp3 = tagStateFromFields(listOf("TXXX:REPLAYGAIN_TRACK_GAIN" to "+16.30 dB", "TXXX:REPLAYGAIN_TRACK_PEAK" to "0.5"))
        val m4a = tagStateFromFields(listOf("----:com.apple.iTunes:replaygain_track_gain" to "+18.00 dB"))
        assertEquals(TagState.Tagged(ReplayGainTags(GainPeak(16.3f, 0.5f), null)), mp3)
        assertEquals(TagState.Tagged(ReplayGainTags(GainPeak(18f, null), null)), m4a)
    }

    @Test
    fun `r128 wins over replaygain when both exist`() {
        val state = tagStateFromFields(listOf("REPLAYGAIN_TRACK_GAIN" to "-3.00 dB", "R128_TRACK_GAIN" to "-3072"))
        assertEquals(TagState.Tagged(ReplayGainTags(GainPeak(-7f, null), null)), state)
    }

    @Test
    fun `first valid value wins over duplicates and garbage`() {
        val state = tagStateFromFields(
            listOf(
                "REPLAYGAIN_TRACK_GAIN" to "garbage",
                "REPLAYGAIN_TRACK_GAIN" to "-5.00 dB",
                "REPLAYGAIN_TRACK_GAIN" to "-9.00 dB",
            )
        )
        assertEquals(TagState.Tagged(ReplayGainTags(GainPeak(-5f, null), null)), state)
    }

    @Test
    fun `a peak without a gain is not a tier`() {
        assertEquals(TagState.Untagged, tagStateFromFields(listOf("REPLAYGAIN_TRACK_PEAK" to "0.9")))
    }

    @Test
    fun `no relevant fields is untagged`() {
        assertEquals(TagState.Untagged, tagStateFromFields(emptyList()))
        assertEquals(TagState.Untagged, tagStateFromFields(listOf("TITLE" to "Song", "REPLAYGAIN_TRACK_GAIN" to "junk")))
    }
}
