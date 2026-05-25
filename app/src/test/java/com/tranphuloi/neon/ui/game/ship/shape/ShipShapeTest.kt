package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 81 audit — verifies [ShipShape] enum integrity + stat sanity.
 *
 * Roster: 5 base (R68) + 12 R79 + 5 R79-followup = 22 total.
 */
class ShipShapeTest {

    @Test
    fun `ShipShape has 22 entries across R68 R79 R79-followup`() {
        assertEquals(22, ShipShape.values().size)
    }

    @Test
    fun `all ShipShape keys are unique`() {
        val keys = ShipShape.values().map { it.key }
        assertEquals(
            "ShipShape has duplicate keys: $keys",
            keys.size, keys.toSet().size,
        )
    }

    @Test
    fun `all ShipShape displayNames are non-blank`() {
        for (shape in ShipShape.values()) {
            assertTrue(
                "ShipShape.$shape has blank displayName",
                shape.displayName.isNotBlank(),
            )
        }
    }

    @Test
    fun `FIGHTER is the only free ship default`() {
        val free = ShipShape.values().filter { it.unlockMinerals == 0 }
        assertEquals(
            "Expected exactly 1 free ship (FIGHTER), got: ${free.map { it.key }}",
            1, free.size,
        )
        assertEquals(ShipShape.FIGHTER, free.single())
    }

    @Test
    fun `all stat multipliers are within sensible range`() {
        for (shape in ShipShape.values()) {
            assertTrue(
                "ShipShape.$shape.hpMul=${shape.hpMul} outside [0.5, 2.0]",
                shape.hpMul in 0.5f..2.0f,
            )
            assertTrue(
                "ShipShape.$shape.speedMul=${shape.speedMul} outside [0.5, 2.0]",
                shape.speedMul in 0.5f..2.0f,
            )
            assertTrue(
                "ShipShape.$shape.damageMul=${shape.damageMul} outside [0.5, 2.0]",
                shape.damageMul in 0.5f..2.0f,
            )
        }
    }

    @Test
    fun `fromKey returns FIGHTER for unknown or null key`() {
        assertEquals(ShipShape.FIGHTER, ShipShape.fromKey(null))
        assertEquals(ShipShape.FIGHTER, ShipShape.fromKey("unknown_key_xyz"))
    }

    @Test
    fun `fromKey roundtrips for every ShipShape`() {
        for (shape in ShipShape.values()) {
            assertEquals(
                "fromKey('${shape.key}') should return ${shape.name}",
                shape, ShipShape.fromKey(shape.key),
            )
        }
    }

    @Test
    fun `pricing rebalance R79 — non-default unlockMinerals at most 7500`() {
        // R79 pricing range. Max is AO_GIAP_THIET=7500. Future ships above
        // 7500 should bump this expected max.
        val maxPrice = ShipShape.values().maxOf { it.unlockMinerals }
        assertTrue("Max unlockMinerals=$maxPrice exceeds R79 ceiling 7500", maxPrice <= 7500)
    }
}
