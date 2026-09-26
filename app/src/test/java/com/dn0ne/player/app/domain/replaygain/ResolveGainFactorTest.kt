package com.dn0ne.player.app.domain.replaygain

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveGainFactorTest {

    private val eps = 1e-4f
    private fun tagged(track: GainPeak? = null, album: GainPeak? = null) =
        TagState.Tagged(ReplayGainTags(track, album))

    @Test
    fun `off is unity whatever the tags`() {
        assertEquals(1f, resolveGainFactor(tagged(GainPeak(-7f, 0.9f)), ReplayGainMode.OFF, 6f, -6f), 0f)
        assertEquals(1f, resolveGainFactor(TagState.Untagged, ReplayGainMode.OFF, 0f, -6f), 0f)
    }

    @Test
    fun `unknown is unity and never gets the fallback`() {
        assertEquals(1f, resolveGainFactor(TagState.Unknown, ReplayGainMode.TRACK, 3f, -6f), 0f)
    }

    @Test
    fun `untagged gets the fallback and not the pre-amp`() {
        assertEquals(dbToFactor(-6f), resolveGainFactor(TagState.Untagged, ReplayGainMode.TRACK, 5f, -6f), eps)
    }

    @Test
    fun `untagged fallback never boosts`() {
        assertEquals(1f, resolveGainFactor(TagState.Untagged, ReplayGainMode.ALBUM, 0f, 4f), 0f)
    }

    @Test
    fun `track mode uses the track tier`() {
        val s = tagged(GainPeak(-7f, 0.88f), GainPeak(-3f, 0.95f))
        assertEquals(dbToFactor(-7f), resolveGainFactor(s, ReplayGainMode.TRACK, 0f, 0f), eps)
    }

    @Test
    fun `album mode uses the album tier`() {
        val s = tagged(GainPeak(-7f, 0.88f), GainPeak(-3f, 0.95f))
        assertEquals(dbToFactor(-3f), resolveGainFactor(s, ReplayGainMode.ALBUM, 0f, 0f), eps)
    }

    @Test
    fun `album mode falls back to the whole track tier, peak included`() {
        // track peak 0.5 caps +10 dB at x2.0; if the album path paired track gain with
        // some other peak the cap would differ
        val s = tagged(track = GainPeak(10f, 0.5f), album = null)
        assertEquals(2f, resolveGainFactor(s, ReplayGainMode.ALBUM, 0f, 0f), eps)
    }

    @Test
    fun `track mode falls back to the whole album tier, peak included`() {
        val s = tagged(track = null, album = GainPeak(10f, 0.25f))
        assertEquals(dbToFactor(10f), resolveGainFactor(s, ReplayGainMode.TRACK, 0f, 0f), eps)
        val capped = tagged(track = null, album = GainPeak(20f, 0.25f))
        assertEquals(4f, resolveGainFactor(capped, ReplayGainMode.TRACK, 0f, 0f), eps)
    }

    @Test
    fun `peak caps a boost`() {
        assertEquals(2f, resolveGainFactor(tagged(GainPeak(16.3f, 0.5f)), ReplayGainMode.TRACK, 0f, 0f), eps)
    }

    @Test
    fun `no peak caps a boost at unity`() {
        assertEquals(1f, resolveGainFactor(tagged(GainPeak(10f, null)), ReplayGainMode.TRACK, 0f, 0f), 0f)
    }

    @Test
    fun `no peak still attenuates`() {
        assertEquals(dbToFactor(-7f), resolveGainFactor(tagged(GainPeak(-7f, null)), ReplayGainMode.TRACK, 0f, 0f), eps)
    }

    @Test
    fun `pre-amp adds to tagged gain and the cap still holds`() {
        assertEquals(dbToFactor(-4f), resolveGainFactor(tagged(GainPeak(-7f, 0.88f)), ReplayGainMode.TRACK, 3f, 0f), eps)
        assertEquals(2f, resolveGainFactor(tagged(GainPeak(5f, 0.5f)), ReplayGainMode.TRACK, 10f, 0f), eps)
    }

    @Test
    fun `result is floored at -60 dB`() {
        assertEquals(MIN_GAIN_FACTOR, resolveGainFactor(tagged(GainPeak(-60f, null)), ReplayGainMode.TRACK, -15f, 0f), 0f)
    }

    @Test
    fun `stored mode names round trip and unknown names mean off`() {
        ReplayGainMode.entries.forEach { assertEquals(it, ReplayGainMode.fromStoredName(it.name)) }
        assertEquals(ReplayGainMode.OFF, ReplayGainMode.fromStoredName(null))
        assertEquals(ReplayGainMode.OFF, ReplayGainMode.fromStoredName("DYNAMIC"))
    }
}
