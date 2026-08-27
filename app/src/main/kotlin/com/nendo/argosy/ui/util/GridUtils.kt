package com.nendo.argosy.ui.util

import com.nendo.argosy.data.preferences.GridDensity
import kotlin.math.pow

object GridUtils {

    private const val WIDE_SCREEN_THRESHOLD_DP = 900
    private const val WIDE_SCREEN_MULTIPLIER = 1.5f

    /**
     * Below this width the column counts stop being a landscape handheld's and start being a
     * portrait phone's. 600dp is the standard Android compact-width boundary: every handheld and TV
     * Argosy targets is wider, so the narrow-screen path below is phones only and changes nothing
     * about the layouts this launcher was designed for.
     */
    private const val NARROW_SCREEN_THRESHOLD_DP = 600

    /**
     * The narrowest a cover may be drawn at, per density. The column counts above are absolute, so
     * on a ~410dp phone six columns produce roughly 60dp covers - unreadable art on the screen where
     * the art is the whole interface. On a narrow screen the count is derived from these instead,
     * which keeps density meaningful (compact still fits more than spacious) while holding the cover
     * at a size worth looking at.
     */
    private const val MIN_GAME_CELL_COMPACT_DP = 88
    private const val MIN_GAME_CELL_NORMAL_DP = 116
    private const val MIN_GAME_CELL_SPACIOUS_DP = 160

    private const val MIN_APP_CELL_COMPACT_DP = 84
    private const val MIN_APP_CELL_NORMAL_DP = 104
    private const val MIN_APP_CELL_SPACIOUS_DP = 132

    private const val MIN_COLUMNS = 2

    /**
     * The width the minimum cover sizes above are stated at - a typical phone in portrait.
     */
    private const val TOUCH_REFERENCE_WIDTH_DP = 400f

    /**
     * How much of a screen's extra width goes into bigger covers rather than more of them.
     *
     * A fixed cover size would put every extra pixel into extra columns, and a fixed column count
     * would put it all into size; neither is what a larger screen should do. Raising the width ratio
     * to a fractional power splits it: from a 411dp phone to a 1280dp tablet the covers grow from
     * about 137dp to 183dp AND the columns go from three to seven. Below the reference width the
     * ratio is clamped, so a small phone keeps the stated minimum rather than shrinking further.
     */
    private const val TOUCH_GROWTH_EXPONENT = 0.35f

    fun getGameGridColumns(density: GridDensity, screenWidthDp: Int): Int {
        val baseColumns = when (density) {
            GridDensity.COMPACT -> 8
            GridDensity.NORMAL -> 6
            GridDensity.SPACIOUS -> 5
        }
        val minCellDp = when (density) {
            GridDensity.COMPACT -> MIN_GAME_CELL_COMPACT_DP
            GridDensity.NORMAL -> MIN_GAME_CELL_NORMAL_DP
            GridDensity.SPACIOUS -> MIN_GAME_CELL_SPACIOUS_DP
        }
        return fitNarrowScreen(
            applyWideScreenMultiplier(baseColumns, screenWidthDp),
            screenWidthDp,
            minCellDp
        )
    }

    fun getAppGridColumns(density: GridDensity, screenWidthDp: Int): Int {
        val baseColumns = when (density) {
            GridDensity.COMPACT -> 5
            GridDensity.NORMAL -> 4
            GridDensity.SPACIOUS -> 3
        }
        val minCellDp = when (density) {
            GridDensity.COMPACT -> MIN_APP_CELL_COMPACT_DP
            GridDensity.NORMAL -> MIN_APP_CELL_NORMAL_DP
            GridDensity.SPACIOUS -> MIN_APP_CELL_SPACIOUS_DP
        }
        return fitNarrowScreen(
            applyWideScreenMultiplier(baseColumns, screenWidthDp),
            screenWidthDp,
            minCellDp
        )
    }

    /**
     * Columns for the touch layouts, derived from a cover size that grows with the screen rather
     * than from a fixed count.
     *
     * The controller layouts above start from a column count because a d-pad moves between cells and
     * the count is what the user is really navigating. Touch has no such constraint: a finger cares
     * how big the art is, and how much of it fits follows from that. So this inverts the calculation
     * - pick the cover size for this screen, then fit as many as the width allows.
     *
     * Scoped to touch deliberately. Applying it everywhere would redraw the handhelds and TVs Argosy
     * was built for, which is not a change to make as a side effect of a phone layout.
     */
    fun getTouchGameGridColumns(density: GridDensity, screenWidthDp: Int): Int {
        if (screenWidthDp <= 0) return getGameGridColumns(density, screenWidthDp)
        val minCellDp = when (density) {
            GridDensity.COMPACT -> MIN_GAME_CELL_COMPACT_DP
            GridDensity.NORMAL -> MIN_GAME_CELL_NORMAL_DP
            GridDensity.SPACIOUS -> MIN_GAME_CELL_SPACIOUS_DP
        }
        val widthRatio = (screenWidthDp / TOUCH_REFERENCE_WIDTH_DP).coerceAtLeast(1f)
        val targetCellDp = minCellDp * widthRatio.pow(TOUCH_GROWTH_EXPONENT)
        return (screenWidthDp / targetCellDp).toInt().coerceAtLeast(MIN_COLUMNS)
    }

    /**
     * Caps [columns] at what fits on a narrow screen without shrinking a cell past [minCellDp].
     *
     * Only ever reduces, and only below [NARROW_SCREEN_THRESHOLD_DP]. A zero or unknown width means
     * the screen has not been measured yet, which is not the same as a narrow one, so it is left
     * alone rather than clamped to the minimum.
     */
    private fun fitNarrowScreen(columns: Int, screenWidthDp: Int, minCellDp: Int): Int {
        if (screenWidthDp <= 0 || screenWidthDp >= NARROW_SCREEN_THRESHOLD_DP) return columns
        val fits = screenWidthDp / minCellDp
        return columns.coerceAtMost(fits).coerceAtLeast(MIN_COLUMNS)
    }

    fun getGridSpacingDp(density: GridDensity): Int = when (density) {
        GridDensity.COMPACT -> 4
        GridDensity.NORMAL -> 6
        GridDensity.SPACIOUS -> 8
    }

    private fun applyWideScreenMultiplier(baseColumns: Int, screenWidthDp: Int): Int {
        return if (screenWidthDp > WIDE_SCREEN_THRESHOLD_DP) {
            (baseColumns * WIDE_SCREEN_MULTIPLIER).toInt()
        } else {
            baseColumns
        }
    }
}
