package com.tranphuloi.neon.common

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Legacy palette (still referenced by some screens/transitively)
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Blue = Color(0xFF020037)
val Pink = Color(0xFF810885)
val Green = Color(0xFF007400)
val Teal = Color(0xFF22B9D2)
val ShipShieldOne = Color(0xFF00FF0D)
val ShipShieldTwo = Color(0xFFCE00FC)

// Cyberpunk neon palette
val NeonBgDeep = Color(0xFF05010F)
val NeonBgMid = Color(0xFF0F0529)
val NeonBgEdge = Color(0xFF26033C)
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF2DE0)
val NeonViolet = Color(0xFFB14CFF)
val NeonGold = Color(0xFFFFCB47)
val NeonRedAlert = Color(0xFFFF2D55)

/**
 * Wave 6 (27x) round 39 — runtime-swappable palette for accessibility mode.
 * Compose code that reads `LocalNeonPalette.current.cyan` (etc.) instead of
 * the top-level [NeonCyan] constant will follow the user's COLOR_BLIND_MODE
 * setting. Code that imports the constants directly stays on the standard
 * palette — that's intentional for round-39 scope (game entity bitmaps and
 * many decorative surfaces don't need the swap).
 */
@Immutable
data class NeonPalette(
    val cyan: Color,
    val magenta: Color,
    val violet: Color,
    val gold: Color,
    val redAlert: Color,
) {
    companion object {
        /** Original cyberpunk neon palette (same as the top-level Neon* vals). */
        val NORMAL = NeonPalette(
            cyan = NeonCyan,
            magenta = NeonMagenta,
            violet = NeonViolet,
            gold = NeonGold,
            redAlert = NeonRedAlert,
        )

        /**
         * Wong-derived colorblind-safe palette. Distinguishable under
         * deuteranopia / protanopia / tritanopia. Keeps the "bright glowing
         * neon" feel while swapping the red/magenta channel to orange/vermillion
         * (the most-affected hue under red-green colorblindness).
         */
        val COLORBLIND_SAFE = NeonPalette(
            cyan = Color(0xFF56B4E9),         // sky blue — universally safe
            magenta = Color(0xFFE69F00),      // orange — replaces the red-confusable pink
            violet = Color(0xFFCC79A7),       // muted pink — distinguishable from orange
            gold = Color(0xFFF0E442),         // pure yellow — high contrast
            redAlert = Color(0xFFD55E00),     // vermillion — visible warning under all CB types
        )
    }
}

/**
 * CompositionLocal carrying the active palette. MainActivity provides it from
 * the `settings.colorBlindMode` flow; consumers read via [LocalNeonPalette.current].
 * Defaults to [NeonPalette.NORMAL] when not provided (e.g., previews, tests).
 */
val LocalNeonPalette = staticCompositionLocalOf { NeonPalette.NORMAL }
