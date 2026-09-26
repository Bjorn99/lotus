package com.dn0ne.player

import androidx.media3.common.Player
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainPrefetchTest {

    @Test
    fun `every event that can change the next item triggers a prefetch`() {
        // nextMediaItemIndex depends on shuffle and repeat too. Toggling either without a
        // prefetch left the new next track unread at its flush, at unity for its whole length.
        listOf(
            Player.EVENT_TIMELINE_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            Player.EVENT_REPEAT_MODE_CHANGED,
        ).forEach { assertTrue("event $it", it in REPLAYGAIN_PREFETCH_EVENTS) }
    }
}
