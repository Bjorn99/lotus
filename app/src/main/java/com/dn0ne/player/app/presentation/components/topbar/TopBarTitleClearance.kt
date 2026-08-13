package com.dn0ne.player.app.presentation.components.topbar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometry for a collapsible top bar whose title is centred in the bar while
 * its icon rows are pinned to the bottom.
 *
 * The two move independently: the title tracks the vertical centre, so it
 * rises as the bar grows, while the icons stay where they are. For a band of
 * bar heights near the collapsed end they occupy the same line, and the title
 * has to keep clear of them horizontally. Once the bar is tall enough that the
 * title has risen above the icons, that clearance is dead space and the title
 * should have the full width.
 *
 * Tying the clearance to the collapse fraction instead — which is what this
 * replaces — gets the two endpoints right and everything between them wrong:
 * the padding starts shrinking immediately while the icons have not moved at
 * all, so the title slides underneath them mid-scroll (#139).
 */

/** A 48dp [androidx.compose.material3.IconButton] plus the row's 4dp vertical padding. */
internal val TOP_BAR_ICON_ROW_HEIGHT = 56.dp

/** Breathing room for the title once it no longer shares a line with the icons. */
internal val TOP_BAR_TITLE_RELAXED_PADDING = 28.dp

private val ICON_BUTTON_WIDTH = 48.dp
private val ICON_ROW_HORIZONTAL_PADDING = 12.dp

/**
 * Width a cluster of [buttonCount] icon buttons occupies, including the row's
 * own horizontal padding.
 *
 * Derived rather than written down so that adding a button to a top bar cannot
 * silently leave the title overlapping it — which is how the hardcoded 108dp
 * and 156dp came to be wrong in the first place.
 */
internal fun iconClusterWidth(buttonCount: Int): Dp =
    ICON_ROW_HORIZONTAL_PADDING + ICON_BUTTON_WIDTH * buttonCount

/**
 * How much of the icon clearance the title still needs: 1 for as long as it
 * shares a line with the icon row, then easing to 0 once it has risen clear.
 *
 * Clearance is held at full while the two overlap by any amount at all. Easing
 * it off during the overlap — which is the mistake the first attempt at this
 * made — leaves a partial padding that is still smaller than the icons are
 * wide, so the title goes right on overlapping them, just less of the time.
 *
 * The ramp afterwards runs over the title's own height, so the padding changes
 * smoothly rather than snapping as the bar is dragged.
 *
 * Returns full clearance for a not-yet-measured title, so the first frame errs
 * towards too much space rather than an overlap.
 */
internal fun titleClearanceFraction(
    barHeightPx: Float,
    titleHeightPx: Float,
    iconRowHeightPx: Float,
): Float {
    if (titleHeightPx <= 0f) return 1f

    val iconRowTop = barHeightPx - iconRowHeightPx
    val titleBottom = barHeightPx / 2f + titleHeightPx / 2f
    val gap = iconRowTop - titleBottom

    if (gap <= 0f) return 1f
    return (1f - gap / titleHeightPx).coerceIn(0f, 1f)
}
