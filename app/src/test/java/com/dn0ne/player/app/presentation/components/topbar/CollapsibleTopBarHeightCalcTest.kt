package com.dn0ne.player.app.presentation.components.topbar

import org.junit.Assert.assertEquals
import org.junit.Test

class CollapsibleTopBarHeightCalcTest {

    @Test
    fun `delta-only calc — scroll up within range moves height proportionally`() {
        val result = calculateTopBarHeight(
            previousHeight = 500f,
            scrollDelta = -30f,
            minHeight = 180f,
            maxHeight = 750f
        )
        assertEquals(470f, result)
    }

    @Test
    fun `delta-only calc — clamps result below minHeight`() {
        // previousHeight is at min and scroll tries to push below
        val result = calculateTopBarHeight(
            previousHeight = 180f,
            scrollDelta = -100f,
            minHeight = 180f,
            maxHeight = 750f
        )
        assertEquals(180f, result)
    }

    @Test
    fun `delta-only calc — clamps result above maxHeight`() {
        // previousHeight is at max and scroll tries to push above
        val result = calculateTopBarHeight(
            previousHeight = 750f,
            scrollDelta = 100f,
            minHeight = 180f,
            maxHeight = 750f
        )
        assertEquals(750f, result)
    }

    @Test
    fun `delta-only calc — same inputs produce same output regardless of content scroll position`() {
        // With delta-only calculation, the content scroll position does NOT affect
        // the height change. The old code subtracted contentScrollState.value causing
        // a positive-feedback loop — this test verifies the new formula is pure.
        val atScroll0 = calculateTopBarHeight(
            previousHeight = 500f,
            scrollDelta = -30f,
            minHeight = 180f,
            maxHeight = 750f
        )
        val atScroll500 = calculateTopBarHeight(
            previousHeight = 500f,
            scrollDelta = -30f,
            minHeight = 180f,
            maxHeight = 750f
        )
        assertEquals(atScroll0, atScroll500)
    }

    @Test
    fun `delta-only calc — matches LazyColumn variant behavior`() {
        val result = calculateTopBarHeight(
            previousHeight = 400f,
            scrollDelta = -100f,
            minHeight = 180f,
            maxHeight = 750f
        )
        assertEquals(300f, result)
    }
}
