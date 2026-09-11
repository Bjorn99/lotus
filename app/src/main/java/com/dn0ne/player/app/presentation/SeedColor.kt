package com.dn0ne.player.app.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastFirstOrNull
import com.kmpalette.color
import com.kmpalette.palette.graphics.Palette
import com.materialkolor.ktx.toHct

/**
 * Chooses the colour the whole app is themed from, given the swatches Palette
 * found in the current track's artwork.
 *
 * Lifted out of `PlayerScreen` because the version that lived inline called
 * `swatches.first()` on a list that had only been null-checked. Palette can
 * return a result whose swatch list is present but **empty**, and an empty list
 * survives both `sortedByDescending` and `?.let`, so the `?: fallback` that was
 * meant to catch this never ran and `first()` threw
 * `NoSuchElementException: List is empty` instead. That threw during
 * recomposition of the theme wrapping the entire UI, so it killed the app
 * rather than degrading the colour.
 *
 * Taking a plain `List` makes the empty case reachable from a unit test, which
 * is the whole point — the crashing path needs no Palette, no bitmap and no
 * emulator to exercise.
 *
 * Picking rule, unchanged: prefer the most populous swatch, unless another one
 * is markedly more colourful (at least 30 chroma above it) while still carrying
 * at least a tenth of its population. That keeps a vivid accent from being lost
 * to a large, dull background, without letting a handful of stray pixels pick
 * the theme.
 */
internal fun pickSeedColor(swatches: List<Palette.Swatch>, fallback: Color): Color {
    val byPopulation = swatches.sortedByDescending { it.population }
    val dominant = byPopulation.firstOrNull() ?: return fallback

    val dominantChroma = dominant.color.toHct().chroma
    val moreChromatic = byPopulation.fastFirstOrNull {
        it.color.toHct().chroma - dominantChroma >= 30 &&
                it.population.toFloat() / dominant.population >= .1f
    }

    return moreChromatic?.color ?: dominant.color
}
