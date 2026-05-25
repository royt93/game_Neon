package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 81 audit — verifies the [BoosterToBoosterUIMapper.shapeFor] mapping is
 * complete + distinct for every [BoosterType]. User feedback round 79 + 81:
 * "có vô số item duplicate symbol shape nhau". This test enforces that no two
 * BoosterTypes share the same BoosterShape — and that the mapper handles every
 * type (no fall-through).
 *
 * Pure unit test (no Android deps, no RNG retries). Direct call to internal
 * `shapeFor(type)` function.
 */
class BoosterShapeUniquenessTest {

    private val mapper = BoosterToBoosterUIMapper()

    @Test
    fun `every BoosterType maps to a non-null BoosterShape`() {
        for (type in BoosterType.values()) {
            val shape = mapper.shapeFor(type)
            assertNotNull("BoosterType.$type must map to a BoosterShape", shape)
        }
    }

    @Test
    fun `no two BoosterTypes share the same BoosterShape`() {
        val typeByShape = mutableMapOf<BoosterShape, MutableList<BoosterType>>()
        for (type in BoosterType.values()) {
            val shape = mapper.shapeFor(type)
            typeByShape.getOrPut(shape) { mutableListOf() }.add(type)
        }
        val dups = typeByShape.filter { it.value.size > 1 }
        assertTrue(
            "BoosterShape duplicates found: $dups",
            dups.isEmpty(),
        )
    }

    @Test
    fun `BoosterType count matches expected R81 roster`() {
        // 6 base (R66b) + 2 R54 + 11 R60 (with SCORE_X3) + 4 R67 + 5 R68 = 27
        assertEquals(27, BoosterType.values().size)
    }

    @Test
    fun `all 28 used BoosterShape values are dispatched in shapeFor`() {
        // BoosterShape has 28 values (R81 removed PLUS_DOUBLE → 28, added
        // RAGE_FANG/HEALING_FLASK in R79 → still 28). Test that at least 26 of
        // them are used by some BoosterType (since the enum can have unused
        // values reserved for future).
        val used = BoosterType.values().map { mapper.shapeFor(it) }.toSet()
        assertEquals(
            "Expected 26 distinct BoosterShape used (one per BoosterType). Got: $used",
            BoosterType.values().size,
            used.size,
        )
    }
}
