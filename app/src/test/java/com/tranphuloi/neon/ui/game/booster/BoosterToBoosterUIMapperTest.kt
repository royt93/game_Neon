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

    companion object {
        // Bounded retries: with 8/116 = 6.9% probability, expected E[X]=15 attempts
        // for PIERCING/PLASMA. 200 gives ~99.999...% confidence we hit at least once.
        private const val MAX_RETRY = 200
    }
}
