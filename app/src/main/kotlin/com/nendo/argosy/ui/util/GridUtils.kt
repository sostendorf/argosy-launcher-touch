package com.nendo.argosy.ui.util

import com.nendo.argosy.data.preferences.GridDensity

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
