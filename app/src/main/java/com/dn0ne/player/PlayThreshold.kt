package com.dn0ne.player

// Pure classifier for a track transition. Kept here (not nested inside the
// listener) so it can be unit-tested without a Player.
//
// Rules:
//   - duration <= 0 → NONE (live stream / broken file: don't lie about
//     play vs skip when we can't tell)
//   - maxReachedPositionMs >= duration / 2 → PLAY
//   - otherwise → SKIP
//
// `maxReachedPositionMs` is the high-water mark of the playhead seen
// during the session, so seeking *backward* after crossing 50 % doesn't
// undo the play.
internal enum class TransitionEvent { PLAY, SKIP, NONE }

internal fun classifyTransition(
    maxReachedPositionMs: Long,
    durationMs: Long,
): TransitionEvent = when {
    durationMs <= 0L -> TransitionEvent.NONE
    maxReachedPositionMs >= durationMs / 2L -> TransitionEvent.PLAY
    else -> TransitionEvent.SKIP
}
