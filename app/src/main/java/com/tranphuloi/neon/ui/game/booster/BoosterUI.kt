package com.tranphuloi.neon.ui.game.booster

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Keep
@Immutable
data class BoosterUI(
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
    @DrawableRes val drawableId: Int,
    /**
     * Round 43 (39x) — ARGB ring color for the rarity tier overlay.
     * 0 = no ring (defensive default; not used by current callers).
     */
    val rarityRingColorHex: Long = 0L,
    /** Round 43 (39x) — true if rarity is RARE or EPIC; drives ring pulse intensity. */
    val isEliteRarity: Boolean = false,
    /**
     * Round 54 — ARGB tint applied via ColorFilter.Modulate on the booster icon.
     * 0 = no tint (most booster types render raw). Used to distinguish
     * PIERCING/PLASMA boosters whose drawables collide with LASER/ULTIMATE.
     */
    val tintColorHex: Long = 0L,
    /**
     * Round 54 — single-character glyph rendered top-right of the booster icon
     * for unambiguous identification (PIERCING "→", PLASMA "◯"). Null = no badge.
     */
    val glyph: String? = null,
) : Serializable
