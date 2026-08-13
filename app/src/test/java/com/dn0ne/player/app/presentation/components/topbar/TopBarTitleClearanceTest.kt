package com.dn0ne.player.app.presentation.components.topbar

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression cover for #139 — the playlist title sliding under the top bar
 * icons partway through a collapse.
 *
 * Numbers below are the real ones: the bar runs 60dp collapsed to 250dp
 * expanded, the icon row is 56dp tall, and the title is around 28dp at its
 * collapsed size. Densities cancel out, so px and dp are interchangeable here.
 */
class TopBarTitleClearanceTest {

    private val iconRow = 56f
    private val title = 28f

    private fun clearanceAtBarHeight(bar: Float) =
        titleClearanceFraction(bar, title, iconRow)

    // ---- the bug ----

    @Test
    fun `fully collapsed bar needs all the clearance`() {
        assertEquals(1f, clearanceAtBarHeight(60f), 0.001f)
    }

    @Test
    fun `the height that used to overlap now keeps full clearance`() {
        // Bar at 80dp is collapseFraction 0.105, where the old code had already
        // shrunk the start padding to 99.6dp against icons occupying 108dp.
        assertEquals(1f, clearanceAtBarHeight(80f), 0.001f)
    }

    @Test
    fun `clearance is still full across the whole former overlap band`() {
        var bar = 60f
        while (bar <= 140f) {
            assertEquals(
                "bar height $bar",
                1f, clearanceAtBarHeight(bar), 0.001f
            )
            bar += 5f
        }
    }

    // ---- releasing it once the title has risen clear ----

    @Test
    fun `fully expanded bar needs no clearance`() {
        assertEquals(0f, clearanceAtBarHeight(250f), 0.001f)
    }

    @Test
    fun `clearance is gone once the title has risen well clear`() {
        // titleBottom = 200/2 + 14 = 114; iconRowTop = 200 - 56 = 144, so the
        // title sits a full title-height above the icons.
        assertEquals(0f, clearanceAtBarHeight(200f), 0.001f)
    }

    @Test
    fun `clearance decreases monotonically as the bar grows`() {
        var previous = clearanceAtBarHeight(60f)
        var bar = 65f
        while (bar <= 250f) {
            val current = clearanceAtBarHeight(bar)
            assertTrue(
                "clearance rose at bar height $bar ($previous -> $current)",
                current <= previous + 0.0001f
            )
            previous = current
            bar += 5f
        }
    }

    @Test
    fun `the ramp is partial in between rather than snapping`() {
        val mid = clearanceAtBarHeight(160f)
        assertTrue("expected a partial ramp, got $mid", mid > 0f && mid < 1f)
    }

    // ---- degenerate input ----

    @Test
    fun `an unmeasured title asks for full clearance`() {
        assertEquals(1f, titleClearanceFraction(0f, 0f, iconRow), 0.001f)
    }

    @Test
    fun `a negative title height is treated as unmeasured`() {
        assertEquals(1f, titleClearanceFraction(200f, -5f, iconRow), 0.001f)
    }

    @Test
    fun `result never leaves the unit range`() {
        var bar = 0f
        while (bar <= 400f) {
            val f = clearanceAtBarHeight(bar)
            assertTrue("out of range at $bar: $f", f in 0f..1f)
            bar += 10f
        }
    }

    @Test
    fun `a taller title keeps clearance for longer`() {
        // A large font scale makes the title taller, so it stays on the icon
        // line further into the expansion. That is exactly why this is measured
        // rather than assumed.
        val small = titleClearanceFraction(150f, 20f, iconRow)
        val large = titleClearanceFraction(150f, 44f, iconRow)
        assertTrue("large=$large should exceed small=$small", large > small)
    }

    // ---- cluster widths replace the hardcoded 108 / 156 ----

    @Test
    fun `a two button cluster is the old 108dp`() {
        assertEquals(108.dp, iconClusterWidth(2))
    }

    @Test
    fun `a three button cluster is the old 156dp`() {
        assertEquals(156.dp, iconClusterWidth(3))
    }

    @Test
    fun `an added button widens the cluster by one button`() {
        assertEquals(iconClusterWidth(3) + 48.dp, iconClusterWidth(4))
    }
}
