package com.tranphuloi.neon.ui.game.booster

class BoosterToBoosterUIMapper {

    operator fun invoke(booster: Booster): BoosterUI {
        // Round 54 — PIERCING + PLASMA reuse existing drawables (booster_red_lasers /
        // booster_ultimate_weapon) and were visually indistinguishable from
        // LASER_BOOSTER / ULTIMATE_WEAPON_BOOSTER. Apply a discriminating tint
        // (modulated against the raw icon) + glyph badge so the player can tell
        // them apart without dedicated art assets.
        val (tintHex, glyph) = when (booster.type) {
            BoosterType.PIERCING_BOOSTER -> PIERCING_TINT_ARGB to "→"
            BoosterType.PLASMA_BOOSTER -> PLASMA_TINT_ARGB to "◯"
            else -> 0L to null
        }
        return with(booster) {
            BoosterUI(
                xOffset = xOffset,
                yOffset = yOffset,
                size = size,
                drawableId = type.drawableId,
                rarityRingColorHex = rarity.ringColorHex,
                isEliteRarity = rarity != BoosterRarity.COMMON,
                tintColorHex = tintHex,
                glyph = glyph,
            )
        }
    }

    companion object {
        // ARGB long values — kept here (not in Color.kt) because they're a
        // gameplay-discriminator concern, not a theme palette concern. Picked
        // to maximise contrast against the icons' base hues:
        //   NeonMagenta (0xFFFF2DE0) over red booster_red_lasers → magenta wins
        //   NeonCyan    (0xFF00F0FF) over yellow booster_ultimate_weapon → cyan wins
        const val PIERCING_TINT_ARGB: Long = 0xFFFF2DE0L
        const val PLASMA_TINT_ARGB: Long = 0xFF00F0FFL
    }
}
