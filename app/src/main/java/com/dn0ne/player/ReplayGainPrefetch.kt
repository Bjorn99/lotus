package com.dn0ne.player

import androidx.media3.common.Player

/**
 * Player events after which the playing or next item may have changed, so ReplayGain
 * tags must be read ahead of the sink. nextMediaItemIndex depends on shuffle and repeat,
 * not just the timeline: a toggle without a prefetch leaves the new next track unread at
 * its flush, and it then plays its whole length at unity.
 */
val REPLAYGAIN_PREFETCH_EVENTS = intArrayOf(
    Player.EVENT_TIMELINE_CHANGED,
    Player.EVENT_MEDIA_ITEM_TRANSITION,
    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
    Player.EVENT_REPEAT_MODE_CHANGED,
)
