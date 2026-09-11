package com.dn0ne.player.app.presentation.components.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Regression cover for #159 — the seek bar rendering total elapsed minutes in
 * the minutes field, so a track over an hour showed an hours field and an
 * unreduced minutes field at the same time.
 *
 * The two `screenshot` cases are not invented. They are the exact values
 * visible in the reporter's screenshot on #159, worked back to milliseconds:
 * a 6h12m49s mixtape displaying `06:372:49`, at a position of 4h41m15s
 * displaying `04:281:15`.
 */
class PlaybackTimeFormatTest {

    // ---- the bug ----

    @Test
    fun `an hour exactly rolls the minutes over instead of showing sixty`() {
        assertEquals("1:00:00", formatPlaybackTime(3_600_000L))
    }

    @Test
    fun `ninety minutes reads as an hour and a half`() {
        assertEquals("1:30:00", formatPlaybackTime(5_400_000L))
    }

    @Test
    fun `screenshot duration - six hour mixtape`() {
        assertEquals("6:12:49", formatPlaybackTime(22_369_000L))
    }

    @Test
    fun `screenshot position - four hours and change into it`() {
        assertEquals("4:41:15", formatPlaybackTime(16_875_000L))
    }

    /**
     * Pins the reported symptom itself, not just the corrected contract. If
     * the minutes field ever goes back to holding total minutes, these fail
     * even if someone has changed what "correct" looks like elsewhere.
     */
    @Test
    fun `the exact strings from the bug report are no longer produced`() {
        assertNotEquals("06:372:49", formatPlaybackTime(22_369_000L))
        assertNotEquals("04:281:15", formatPlaybackTime(16_875_000L))
    }

    // ---- the 60:00 boundary ----

    @Test
    fun `one second under an hour has no hours field`() {
        assertEquals("59:59", formatPlaybackTime(3_599_000L))
    }

    @Test
    fun `one second over an hour gains one`() {
        assertEquals("1:00:01", formatPlaybackTime(3_601_000L))
    }

    // ---- short durations, unchanged from 1_9_0 ----

    @Test
    fun `under a minute`() {
        assertEquals("00:01", formatPlaybackTime(1_000L))
        assertEquals("00:59", formatPlaybackTime(59_000L))
    }

    @Test
    fun `a minute exactly`() {
        assertEquals("01:00", formatPlaybackTime(60_000L))
    }

    @Test
    fun `minutes and seconds are both padded to two digits`() {
        assertEquals("05:07", formatPlaybackTime(307_000L))
    }

    // ---- edges ----

    @Test
    fun `unknown duration renders as zero rather than blank`() {
        assertEquals("00:00", formatPlaybackTime(0L))
    }

    @Test
    fun `negative input is clamped, not rendered with a minus sign`() {
        assertEquals("00:00", formatPlaybackTime(-1L))
        assertEquals("00:00", formatPlaybackTime(-5_400_000L))
    }

    @Test
    fun `sub-second input truncates to zero`() {
        assertEquals("00:00", formatPlaybackTime(999L))
    }

    @Test
    fun `truncates rather than rounds up`() {
        // 6:12:49.999 must not tip into 6:12:50 — the readout would be a
        // second ahead of the audio, and the duration would name a point the
        // track never reaches.
        assertEquals("6:12:49", formatPlaybackTime(22_369_999L))
    }

    @Test
    fun `hours stay unpadded past nine`() {
        assertEquals("10:05:00", formatPlaybackTime(36_300_000L))
    }

    @Test
    fun `very long durations do not wrap`() {
        // Track.duration is an Int of milliseconds; this is close to its
        // ceiling, around 596 hours.
        assertEquals("596:31:23", formatPlaybackTime(2_147_483_647L))
    }
}
