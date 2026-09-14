package com.dn0ne.player.app.presentation.components.playback

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class SeekDirectionTest {

    private val threshold = 50f

    @Test
    fun `drag below threshold is none regardless of direction`() {
        assertEquals(
            SeekDirection.None,
            seekDirectionForDrag(totalDrag = -49f, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
        assertEquals(
            SeekDirection.None,
            seekDirectionForDrag(totalDrag = 49f, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
        assertEquals(
            SeekDirection.None,
            seekDirectionForDrag(totalDrag = -1f, thresholdPx = threshold, layoutDirection = LayoutDirection.Rtl)
        )
        assertEquals(
            SeekDirection.None,
            seekDirectionForDrag(totalDrag = 1f, thresholdPx = threshold, layoutDirection = LayoutDirection.Rtl)
        )
    }

    @Test
    fun `no drag is none`() {
        assertEquals(
            SeekDirection.None,
            seekDirectionForDrag(totalDrag = 0f, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
    }

    @Test
    fun `ltr swipe left past threshold is next`() {
        assertEquals(
            SeekDirection.Next,
            seekDirectionForDrag(totalDrag = -threshold, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
        assertEquals(
            SeekDirection.Next,
            seekDirectionForDrag(totalDrag = -200f, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
    }

    @Test
    fun `ltr swipe right past threshold is previous`() {
        assertEquals(
            SeekDirection.Previous,
            seekDirectionForDrag(totalDrag = threshold, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
        assertEquals(
            SeekDirection.Previous,
            seekDirectionForDrag(totalDrag = 200f, thresholdPx = threshold, layoutDirection = LayoutDirection.Ltr)
        )
    }

    @Test
    fun `rtl mirrors the seek direction`() {
        assertEquals(
            SeekDirection.Previous,
            seekDirectionForDrag(totalDrag = -200f, thresholdPx = threshold, layoutDirection = LayoutDirection.Rtl)
        )
        assertEquals(
            SeekDirection.Next,
            seekDirectionForDrag(totalDrag = 200f, thresholdPx = threshold, layoutDirection = LayoutDirection.Rtl)
        )
    }
}