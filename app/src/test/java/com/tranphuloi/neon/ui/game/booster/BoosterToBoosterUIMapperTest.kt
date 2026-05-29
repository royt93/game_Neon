package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round 54 — verifies the type-discriminator overlay (tint + glyph) applied by
 * [BoosterToBoosterUIMapper] only to PIERCING/PLASMA, since their drawables
 * collide with LASER_BOOSTER / ULTIMATE_WEAPON_BOOSTER.
 *
 * NOTE: Cannot construct [Booster] directly here because its `type` and
 * `rarity` fields are rolled via [kotlin.random.Random] inside the body. We
 * use reflection-friendly assertions on the mapper output shape only.
 */
class BoosterToBoosterUIMapperTest {

    private val mapper = BoosterToBoosterUIMapper()

    /**
     * Build a booster instance and re-roll until we get the requested type.
     * For test stability — bounded retries to avoid CI flake.
     */
    private fun boosterOfType(target: BoosterType): Booster {
        repeat(MAX_RETRY) {
            val b = Booster(
                id = "test-$it",
                xOffset = 100f,
                size = 40f,
                screenHeight = 800f,
            )
            if (b.type == target) return b
        }
        throw AssertionError("Could not produce $target in $MAX_RETRY attempts — RNG biased?")
    }

    @Test
    fun `PIERCING booster gets magenta tint + arrow glyph`() {
        val b = boosterOfType(BoosterType.PIERCING_BOOSTER)
        val ui = mapper(b)
        assertEquals(BoosterToBoosterUIMapper.PIERCING_TINT_ARGB, ui.tintColorHex)
        assertEquals("→", ui.glyph)
    }

    @Test
    fun `PLASMA booster gets cyan tint + circle glyph`() {
        val b = boosterOfType(BoosterType.PLASMA_BOOSTER)
        val ui = mapper(b)
        assertEquals(BoosterToBoosterUIMapper.PLASMA_TINT_ARGB, ui.tintColorHex)
        assertEquals("◯", ui.glyph)
    }

    @Test
    fun `LASER_BOOSTER has no tint and no glyph (renders as raw NeonGold)`() {
        val b = boosterOfType(BoosterType.LASER_BOOSTER)
        val ui = mapper(b)
        assertEquals(0L, ui.tintColorHex)
        assertNull(ui.glyph)
    }

    @Test
    fun `ULTIMATE_WEAPON_BOOSTER has no tint and no glyph (renders as raw NeonGold)`() {
        val b = boosterOfType(BoosterType.ULTIMATE_WEAPON_BOOSTER)
        val ui = mapper(b)
        assertEquals(0L, ui.tintColorHex)
        assertNull(ui.glyph)
    }

    @Test
    fun `PIERCING and PLASMA tints are distinct (cannot be confused)`() {
        assertNotEquals(
            BoosterToBoosterUIMapper.PIERCING_TINT_ARGB,
            BoosterToBoosterUIMapper.PLASMA_TINT_ARGB,
        )
    }

    @Test
    fun `PIERCING tint is fully opaque (alpha=FF)`() {
        val argb = BoosterToBoosterUIMapper.PIERCING_TINT_ARGB
        val alpha = (argb ushr 24) and 0xFFL
        assertEquals(0xFFL, alpha)
    }

    @Test
    fun `PLASMA tint is fully opaque (alpha=FF)`() {
        val argb = BoosterToBoosterUIMapper.PLASMA_TINT_ARGB
        val alpha = (argb ushr 24) and 0xFFL
        assertEquals(0xFFL, alpha)
    }

    @Test
    fun `mapper preserves position, size, drawableId from booster`() {
        val b = boosterOfType(BoosterType.HEALTH_BOOSTER)
        val ui = mapper(b)
        assertEquals(b.xOffset, ui.xOffset, 0f)
        assertEquals(b.yOffset, ui.yOffset, 0f)
        assertEquals(b.size, ui.size, 0f)
        assertEquals(b.type.drawableId, ui.drawableId)
    }

    @Test
    fun `rarity ring color flows through unchanged`() {
        val b = boosterOfType(BoosterType.LASER_BOOSTER)
        val ui = mapper(b)
        assertEquals(b.rarity.ringColorHex, ui.rarityRingColorHex)
        assertEquals(b.rarity != BoosterRarity.COMMON, ui.isEliteRarity)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Round 60 (38x) — verify each new booster's glyph + tint mapping. 10
    // assertions, one per type. Glyph is the literal char rendered as overlay
    // in GameWorld; tint is the ARGB applied to the recycled drawable so
    // player can tell e.g. MAGNET_BOOST (shield drawable + violet tint + ⊕
    // glyph) apart from SHIELD_BOOSTER (shield drawable, no tint, no glyph).
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `MAGNET_BOOST gets violet tint + plus-circled glyph`() {
        val ui = mapper(boosterOfType(BoosterType.MAGNET_BOOST))
        assertEquals(BoosterToBoosterUIMapper.MAGNET_BOOST_TINT_ARGB, ui.tintColorHex)
        assertEquals("⊕", ui.glyph)
    }

    @Test
    fun `CRIT_SURGE gets amber tint + asterisk glyph`() {
        val ui = mapper(boosterOfType(BoosterType.CRIT_SURGE))
        assertEquals(BoosterToBoosterUIMapper.CRIT_SURGE_TINT_ARGB, ui.tintColorHex)
        assertEquals("✱", ui.glyph)
    }

    @Test
    fun `SPREAD_SHOT gets teal-green tint + star glyph`() {
        val ui = mapper(boosterOfType(BoosterType.SPREAD_SHOT))
        assertEquals(BoosterToBoosterUIMapper.SPREAD_SHOT_TINT_ARGB, ui.tintColorHex)
        assertEquals("☆", ui.glyph)
    }

    @Test
    fun `BERSERK gets blood-red tint + lightning glyph`() {
        val ui = mapper(boosterOfType(BoosterType.BERSERK))
        assertEquals(BoosterToBoosterUIMapper.BERSERK_TINT_ARGB, ui.tintColorHex)
        assertEquals("⚡", ui.glyph)
    }

    @Test
    fun `PHASE_SHIELD gets pale-cyan tint + diamond glyph`() {
        val ui = mapper(boosterOfType(BoosterType.PHASE_SHIELD))
        assertEquals(BoosterToBoosterUIMapper.PHASE_SHIELD_TINT_ARGB, ui.tintColorHex)
        assertEquals("◇", ui.glyph)
    }

    @Test
    fun `SCORE_X3 gets gold tint + dollar glyph`() {
        val ui = mapper(boosterOfType(BoosterType.SCORE_X3))
        assertEquals(BoosterToBoosterUIMapper.SCORE_X3_TINT_ARGB, ui.tintColorHex)
        assertEquals("$", ui.glyph)
    }

    @Test
    fun `QUICK_HEAL gets bright-green tint + cross glyph`() {
        val ui = mapper(boosterOfType(BoosterType.QUICK_HEAL))
        assertEquals(BoosterToBoosterUIMapper.QUICK_HEAL_TINT_ARGB, ui.tintColorHex)
        assertEquals("✚", ui.glyph)
    }

    @Test
    fun `MINERAL_SUPERCHARGE gets orange tint + sparkle glyph`() {
        val ui = mapper(boosterOfType(BoosterType.MINERAL_SUPERCHARGE))
        assertEquals(BoosterToBoosterUIMapper.MINERAL_SUPERCHARGE_TINT_ARGB, ui.tintColorHex)
        assertEquals("✦", ui.glyph)
    }

    @Test
    fun `HEALING_AURA gets mint tint + plus glyph`() {
        val ui = mapper(boosterOfType(BoosterType.HEALING_AURA))
        assertEquals(BoosterToBoosterUIMapper.HEALING_AURA_TINT_ARGB, ui.tintColorHex)
        assertEquals("+", ui.glyph)
    }

    @Test
    fun `DOUBLE_FIRE gets pink tint + double-circle glyph`() {
        val ui = mapper(boosterOfType(BoosterType.DOUBLE_FIRE))
        assertEquals(BoosterToBoosterUIMapper.DOUBLE_FIRE_TINT_ARGB, ui.tintColorHex)
        assertEquals("⚯", ui.glyph)
    }

    @Test
    fun `FIRE_BOOSTER gets orange tint + steam glyph`() {
        val ui = mapper(boosterOfType(BoosterType.FIRE_BOOSTER))
        assertEquals(BoosterToBoosterUIMapper.FIRE_TINT_ARGB, ui.tintColorHex)
        assertEquals("♨", ui.glyph)
    }

    @Test
    fun `HOMING_BOOSTER gets hot-pink tint + bullseye glyph`() {
        val ui = mapper(boosterOfType(BoosterType.HOMING_BOOSTER))
        assertEquals(BoosterToBoosterUIMapper.HOMING_TINT_ARGB, ui.tintColorHex)
        assertEquals("◎", ui.glyph)
    }

    @Test
    fun `BOUNCE_BOOSTER gets mint tint + double-arrow glyph`() {
        val ui = mapper(boosterOfType(BoosterType.BOUNCE_BOOSTER))
        assertEquals(BoosterToBoosterUIMapper.BOUNCE_TINT_ARGB, ui.tintColorHex)
        assertEquals("⇄", ui.glyph)
    }

    @Test
    fun `GIANT_BOOSTER gets gold tint + filled-circle glyph`() {
        val ui = mapper(boosterOfType(BoosterType.GIANT_BOOSTER))
        assertEquals(BoosterToBoosterUIMapper.GIANT_TINT_ARGB, ui.tintColorHex)
        assertEquals("⬤", ui.glyph)
    }

    @Test
    fun `Round 67 bullet-type tints distinct + opaque`() {
        val tints = listOf(
            BoosterToBoosterUIMapper.FIRE_TINT_ARGB,
            BoosterToBoosterUIMapper.HOMING_TINT_ARGB,
            BoosterToBoosterUIMapper.BOUNCE_TINT_ARGB,
        )
        assertEquals("tints should be unique", tints.size, tints.toSet().size)
        tints.forEach {
            val alpha = (it ushr 24) and 0xFFL
            assertEquals("tint should be opaque", 0xFFL, alpha)
        }
    }

    @Test
    fun `all 10 round 60 booster tints are distinct + opaque`() {
        val tints = listOf(
            BoosterToBoosterUIMapper.MAGNET_BOOST_TINT_ARGB,
            BoosterToBoosterUIMapper.CRIT_SURGE_TINT_ARGB,
            BoosterToBoosterUIMapper.SPREAD_SHOT_TINT_ARGB,
            BoosterToBoosterUIMapper.BERSERK_TINT_ARGB,
            BoosterToBoosterUIMapper.PHASE_SHIELD_TINT_ARGB,
            BoosterToBoosterUIMapper.SCORE_X3_TINT_ARGB,
            BoosterToBoosterUIMapper.QUICK_HEAL_TINT_ARGB,
            BoosterToBoosterUIMapper.MINERAL_SUPERCHARGE_TINT_ARGB,
            BoosterToBoosterUIMapper.HEALING_AURA_TINT_ARGB,
            BoosterToBoosterUIMapper.DOUBLE_FIRE_TINT_ARGB,
        )
        assertEquals("tints should be unique", tints.size, tints.toSet().size)
        tints.forEach {
            val alpha = (it ushr 24) and 0xFFL
            assertEquals("tint should be opaque (alpha=FF)", 0xFFL, alpha)
        }
    }

    companion object {
        // Bounded retries: with 8/116 = 6.9% probability, expected E[X]=15 attempts
        // for PIERCING/PLASMA. 200 gives ~99.999...% confidence we hit at least once.
        // Round 60 — 10 new boosters at weight 6/184 = 3.3% each; 200 retries gives
        // ~99.87% (1 - 0.967^200) confidence per hit. Bump if flake observed.
        // Wave 11a Phase 4 — increased from 200 to 1000. With 36 BoosterTypes
        // (Wave 11a complete), individual booster spawn probability dropped to
        // ~2-6.5% per try. 200 attempts had 1-2% miss rate (occasional CI flake).
        // 1000 attempts → ~0.0001% miss for the rarest type (REVIVE weight 5/292).
        private const val MAX_RETRY = 1000
    }
}
