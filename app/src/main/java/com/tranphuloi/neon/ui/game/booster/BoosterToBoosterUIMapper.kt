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
            // Round 60 (38x) — 10 new boosters reuse 6 base drawables. Each
            // gets a distinct glyph + tint so player can identify the effect
            // even though the sprite is recycled. Glyph is rendered top-right
            // of the icon (12-14sp) via existing overlay; tint modulates the
            // sprite + glow color.
            BoosterType.MAGNET_BOOST -> MAGNET_BOOST_TINT_ARGB to "⊕"
            BoosterType.CRIT_SURGE -> CRIT_SURGE_TINT_ARGB to "✱"
            BoosterType.SPREAD_SHOT -> SPREAD_SHOT_TINT_ARGB to "☆"
            BoosterType.BERSERK -> BERSERK_TINT_ARGB to "⚡"
            BoosterType.PHASE_SHIELD -> PHASE_SHIELD_TINT_ARGB to "◇"
            BoosterType.SCORE_X3 -> SCORE_X3_TINT_ARGB to "$"
            BoosterType.QUICK_HEAL -> QUICK_HEAL_TINT_ARGB to "✚"
            BoosterType.MINERAL_SUPERCHARGE -> MINERAL_SUPERCHARGE_TINT_ARGB to "✦"
            BoosterType.HEALING_AURA -> HEALING_AURA_TINT_ARGB to "+"
            BoosterType.DOUBLE_FIRE -> DOUBLE_FIRE_TINT_ARGB to "⚯"
            // Round 67 (Wave 10a) — 3 bullet-type boosters with full behaviors.
            // Unicode-only glyphs (no emoji) to match neon vector aesthetic.
            BoosterType.FIRE_BOOSTER -> FIRE_TINT_ARGB to "♨"
            BoosterType.HOMING_BOOSTER -> HOMING_TINT_ARGB to "◎"
            BoosterType.BOUNCE_BOOSTER -> BOUNCE_TINT_ARGB to "⇄"
            BoosterType.GIANT_BOOSTER -> GIANT_TINT_ARGB to "⬤"
            BoosterType.SMOKE_BOOSTER -> SMOKE_TINT_ARGB to "❍"
            BoosterType.ZIGZAG_BOOSTER -> ZIGZAG_TINT_ARGB to "⌇"
            BoosterType.KAMEHAMEHA_BOOSTER -> KAMEHAMEHA_TINT_ARGB to "⊛"
            BoosterType.ATOMIC_BOOSTER -> ATOMIC_TINT_ARGB to "⊙"
            BoosterType.SPLIT_BOOSTER -> SPLIT_TINT_ARGB to "Ѱ"
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

        // Round 60 (38x) — distinct tints for 10 new boosters. Hue picked for
        // contrast against the reused base drawable's hue (red/yellow/green/
        // gold heart). All fully opaque (alpha=FF).
        const val MAGNET_BOOST_TINT_ARGB: Long = 0xFF8A2BE2L            // violet (magnet field)
        const val CRIT_SURGE_TINT_ARGB: Long = 0xFFFFC020L              // amber (critical strike)
        const val SPREAD_SHOT_TINT_ARGB: Long = 0xFF00E5A0L             // teal-green (fan spread)
        const val BERSERK_TINT_ARGB: Long = 0xFFFF3030L                 // blood red (rage)
        const val PHASE_SHIELD_TINT_ARGB: Long = 0xFFB0E8FFL            // pale-cyan (phase ghost)
        const val SCORE_X3_TINT_ARGB: Long = 0xFFFFD700L                // gold (score)
        const val QUICK_HEAL_TINT_ARGB: Long = 0xFFA8FF60L              // bright green (instant heal)
        const val MINERAL_SUPERCHARGE_TINT_ARGB: Long = 0xFFFF9050L     // orange (energy flash)
        const val HEALING_AURA_TINT_ARGB: Long = 0xFF60FFAAL            // mint (continuous heal)
        const val DOUBLE_FIRE_TINT_ARGB: Long = 0xFFFF80E0L             // pink (double rate)

        // Round 67 (Wave 10a) — 3 bullet-type tints.
        const val FIRE_TINT_ARGB: Long = 0xFFFF6020L                    // bright orange (fire)
        const val HOMING_TINT_ARGB: Long = 0xFFFF40A0L                  // hot pink (lock-on)
        const val BOUNCE_TINT_ARGB: Long = 0xFF40FFD0L                  // mint (rubber bounce)
        const val GIANT_TINT_ARGB: Long = 0xFFFFD040L                   // gold (heavyweight)
        // Round 68 (Wave 10 finish) — 5 tints cho 5 bullets còn lại.
        const val SMOKE_TINT_ARGB: Long = 0xFFA0A0B0L                   // gray-blue smoke
        const val ZIGZAG_TINT_ARGB: Long = 0xFFFFE040L                  // electric yellow
        const val KAMEHAMEHA_TINT_ARGB: Long = 0xFF60E0FFL              // sky cyan beam
        const val ATOMIC_TINT_ARGB: Long = 0xFF80FF80L                  // radioactive green
        const val SPLIT_TINT_ARGB: Long = 0xFFB060FFL                   // purple multi-shard
    }
}
