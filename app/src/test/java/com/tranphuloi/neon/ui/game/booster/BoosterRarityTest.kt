package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterRarityTest {

    @Test
    fun `enum has 3 tiers`() {
        assertEquals(3, BoosterRarity.entries.size)
    }

    @Test
    fun `weights sum to a tidy 100`() {
        val total = BoosterRarity.entries.sumOf { it.weight }
        // 75 + 20 + 5 = 100 — easy to reason about as percentages.
        assertEquals(100, total)
    }

    @Test
    fun `weight ordering reflects rarity COMMON gt RARE gt EPIC`() {
        assertTrue(
            "COMMON.weight (${BoosterRarity.COMMON.weight}) must be > RARE.weight (${BoosterRarity.RARE.weight})",
            BoosterRarity.COMMON.weight > BoosterRarity.RARE.weight,
        )
        assertTrue(
            "RARE.weight (${BoosterRarity.RARE.weight}) must be > EPIC.weight (${BoosterRarity.EPIC.weight})",
            BoosterRarity.RARE.weight > BoosterRarity.EPIC.weight,
        )
    }

    @Test
    fun `multiplier ordering reflects rarity COMMON less than RARE less than EPIC`() {
        // Higher rarity ⇒ stronger effect.
        assertTrue(
            BoosterRarity.COMMON.multiplier < BoosterRarity.RARE.multiplier &&
                BoosterRarity.RARE.multiplier < BoosterRarity.EPIC.multiplier
        )
    }

    @Test
    fun `COMMON multiplier is exactly 1_0 (baseline behavior)`() {
        // Critical: round 43 changes ShipController to apply rarity.multiplier in
        // every booster handler. COMMON must be 1.0 so non-elite drops keep the
        // exact pre-round-43 behavior.
        assertEquals(1.0f, BoosterRarity.COMMON.multiplier, 0f)
    }

    @Test
    fun `every rarity has unique ring color`() {
        val colors = BoosterRarity.entries.map { it.ringColorHex }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `every rarity has fully opaque ring color`() {
        BoosterRarity.entries.forEach {
            val alpha = (it.ringColorHex ushr 24) and 0xFF
            assertEquals("$it alpha should be 0xFF", 0xFFL, alpha)
        }
    }

    @Test
    fun `every rarity has non-empty displayName`() {
        BoosterRarity.entries.forEach {
            assertNotEquals("$it has empty displayName", "", it.displayName)
        }
    }

    @Test
    fun `fromKey roundtrips for every rarity`() {
        BoosterRarity.entries.forEach {
            assertEquals(it, BoosterRarity.fromKey(it.key))
        }
    }

    @Test
    fun `fromKey returns COMMON for null and unknown`() {
        assertEquals(BoosterRarity.COMMON, BoosterRarity.fromKey(null))
        assertEquals(BoosterRarity.COMMON, BoosterRarity.fromKey("not_a_rarity"))
    }
}
