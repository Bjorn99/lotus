package com.dn0ne.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayThresholdTest {

    @Test
    fun zero_duration_is_indeterminate_event_none() {
        // Live streams and broken-metadata files report duration <= 0; we
        // shouldn't lie about play vs skip when we can't tell.
        assertEquals(TransitionEvent.NONE, classifyTransition(maxReachedPositionMs = 0, durationMs = 0))
        assertEquals(TransitionEvent.NONE, classifyTransition(maxReachedPositionMs = 1_000, durationMs = 0))
        assertEquals(TransitionEvent.NONE, classifyTransition(maxReachedPositionMs = 999_999, durationMs = -1))
    }

    @Test
    fun position_below_half_is_skip() {
        assertEquals(TransitionEvent.SKIP, classifyTransition(maxReachedPositionMs = 0, durationMs = 200_000))
        assertEquals(TransitionEvent.SKIP, classifyTransition(maxReachedPositionMs = 99_999, durationMs = 200_000))
    }

    @Test
    fun position_at_or_past_half_is_play() {
        assertEquals(TransitionEvent.PLAY, classifyTransition(maxReachedPositionMs = 100_000, durationMs = 200_000))
        assertEquals(TransitionEvent.PLAY, classifyTransition(maxReachedPositionMs = 200_000, durationMs = 200_000))
        assertEquals(TransitionEvent.PLAY, classifyTransition(maxReachedPositionMs = 250_000, durationMs = 200_000))
    }

    @Test
    fun threshold_uses_integer_floor_for_odd_durations() {
        // duration=3 → threshold = duration/2 = 1 (integer floor). Position 1
        // crosses the bar, position 0 doesn't.
        assertEquals(TransitionEvent.PLAY, classifyTransition(maxReachedPositionMs = 1, durationMs = 3))
        assertEquals(TransitionEvent.SKIP, classifyTransition(maxReachedPositionMs = 0, durationMs = 3))
    }

    @Test
    fun very_short_track_one_ms_long() {
        // duration=1 → threshold = 0. Even being at position 0 counts as a
        // play, which is the right behaviour for a track that's effectively
        // a click — there's no room to "skip" something that short.
        assertEquals(TransitionEvent.PLAY, classifyTransition(maxReachedPositionMs = 0, durationMs = 1))
    }
}
