package com.tranphuloi.neon.ui.game.booster

import androidx.compose.runtime.Immutable

/**
 * Wave 6 (39x) round 43 — booster rarity tiers. Rolled independently of
 * [BoosterType] at booster spawn time. The rarity scales the booster's effect
 * (longer duration / more healing) via [multiplier] and renders a colored ring
 * around the sprite to signal pickup value.
 *
 * Weight rolls produce roughly: 75% COMMON, 20% RARE, 5% EPIC.
 *
 * Effect scaling:
 *  - COMMON × 1.0 (baseline — unchanged behavior).
 *  - RARE × 1.5 (50% longer / stronger).
 *  - EPIC × 2.0 (double).
 *
 * REVIVE_TOKEN bypasses scaling — it's a binary effect (you have it or you don't).
 */
@Immutable
enum class BoosterRarity(
    val key: String,
    val displayName: String,
    val weight: Int,
    val multiplier: Float,
    val ringColorHex: Long,
) {
    COMMON(key = "common", displayName = "Thường", weight = 75, multiplier = 1.0f, ringColorHex = 0xFFB0B0B0),
    RARE(key = "rare", displayName = "Hiếm", weight = 20, multiplier = 1.5f, ringColorHex = 0xFF56B4E9),
    EPIC(key = "epic", displayName = "Sử thi", weight = 5, multiplier = 2.0f, ringColorHex = 0xFFFFCB47);

    companion object {
        fun fromKey(key: String?): BoosterRarity =
            entries.firstOrNull { it.key == key } ?: COMMON
    }
}
