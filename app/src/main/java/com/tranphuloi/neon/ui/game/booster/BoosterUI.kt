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
) : Serializable
