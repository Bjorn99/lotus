package com.dn0ne.player.app.presentation.components.playback

/**
 * Clock readouts for the player's seek bar and the track info sheet.
 *
 * This replaces three copies of the same inline arithmetic, two of which were
 * wrong in the same way (#159): they derived an hours field with
 * `ms / 1000 / 60 / 60` and a minutes field with `ms / 1000 / 60`, so the
 * minutes were *total* elapsed minutes rather than minutes within the hour.
 * Below an hour that is indistinguishable from correct, which is why it
 * survived to 1.9.0; above an hour both fields render at once and the output
 * becomes `HH:<total minutes>:SS`. The reporter's six-hour mixtape read
 * `06:372:49` at a position of `04:281:15`.
 *
 * The `% 60` that reduces the seconds field was simply never written on the
 * minutes lines. Deriving all three fields here, once, is what stops that
 * happening a fourth time.
 */

/**
 * Renders [millis] as a playback clock: `MM:SS` below an hour, `H:MM:SS` at an
 * hour and above.
 *
 * Hours are deliberately *not* zero-padded — `1:30:00`, not `01:30:00` — while
 * minutes and seconds always are. A leading field needs no padding to stay
 * aligned, and every other player renders it this way.
 *
 * Truncates rather than rounds, matching the integer division it replaces. A
 * readout that rounded up would show a duration the track never actually
 * reaches, and a position one second ahead of the audio.
 *
 * Non-positive input renders `00:00`. MediaStore reports 0 for a track whose
 * duration it could not determine, and that reaches this function unchanged;
 * the guard also covers a negative, which nothing produces today. Media3's
 * `C.TIME_UNSET` is *not* a case here — both call sites read `Track.duration`,
 * an `Int` of MediaStore milliseconds, never a player sentinel.
 *
 * Built from string templates and [padStart] rather than `String.format`. That
 * is not an oversight to tidy up: `String.format` applies the default locale's
 * numbering system, so `%02d:%02d` on 5 and 7 renders `٠٥:٠٧` under `ar-EG` and
 * `ar-SA`, `۰۵:۰۷` under `fa-IR`, and `০৫:০৭` under `bn-IN` — measured on this
 * project's JDK, not assumed. A clock readout has to stay ASCII whatever the
 * device language is set to.
 */
internal fun formatPlaybackTime(millis: Long): String {
    if (millis <= 0L) return "00:00"

    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = totalSeconds / 60L % 60L
    val seconds = totalSeconds % 60L

    return buildString {
        if (hours > 0L) {
            append(hours)
            append(':')
        }
        append("$minutes".padStart(2, '0'))
        append(':')
        append("$seconds".padStart(2, '0'))
    }
}
