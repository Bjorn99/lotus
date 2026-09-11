package com.dn0ne.player.app.presentation

import androidx.compose.ui.graphics.Color
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression cover for the launch crash: Palette returning a swatch list that
 * is present but empty made the inline version call `first()` on it and throw
 * `NoSuchElementException: List is empty`, taking the whole app down because
 * the colour seeds the theme wrapping every screen.
 */
class SeedColorTest {

    private val fallback = Color(0xFF123456)

    // ---- the crash ----

    @Test
    fun `empty swatch list returns the fallback instead of throwing`() {
        assertEquals(fallback, pickSeedColor(emptyList(), fallback))
    }

    // ---- the picking rule still holds ----

    @Test
    fun `a single swatch is used as-is`() {
        val only = swatch(0xFF884422.toInt(), population = 100)
        assertEquals(only.color, pickSeedColor(listOf(only), fallback))
    }

    @Test
    fun `the most populous swatch wins when nothing is markedly more colourful`() {
        val dominant = swatch(0xFF808080.toInt(), population = 1000)
        val minor = swatch(0xFF888888.toInt(), population = 10)
        assertEquals(dominant.color, pickSeedColor(listOf(minor, dominant), fallback))
    }

    @Test
    fun `the result does not depend on the order the swatches arrive in`() {
        val dominant = swatch(0xFF808080.toInt(), population = 1000)
        val minor = swatch(0xFF888888.toInt(), population = 10)
        assertEquals(
            pickSeedColor(listOf(dominant, minor), fallback),
            pickSeedColor(listOf(minor, dominant), fallback),
        )
    }

    private fun swatch(argb: Int, population: Int) = Palette.Swatch(argb, population)
}
