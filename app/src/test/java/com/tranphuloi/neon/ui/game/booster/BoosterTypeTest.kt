package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterTypeTest {

    @Test
    fun `every booster type has a positive weight`() {
        BoosterType.entries.forEach {
            assertTrue("$it should have positive weight, got ${it.weight}", it.weight > 0)
        }
    }

    @Test
    fun `every booster type has a non-zero drawableId`() {
        BoosterType.entries.forEach {
            assertNotEquals("$it has 0 drawableId", 0, it.drawableId)
        }
    }

    @Test
    fun `REVIVE_TOKEN is the rarest drop`() {
        val others = BoosterType.entries.filter { it != BoosterType.REVIVE_TOKEN }
        others.forEach {
            assertTrue(
                "$it (weight ${it.weight}) should be more common than REVIVE_TOKEN (${BoosterType.REVIVE_TOKEN.weight})",
                it.weight > BoosterType.REVIVE_TOKEN.weight,
            )
        }
    }

    @Test
    fun `weight distribution sums to expected total`() {
        val total = BoosterType.entries.sumOf { it.weight }
        // 5 base @ 19 + REVIVE @ 5 + 2 bullet-type @ 8 = 95 + 5 + 16 = 116
        assertEquals(116, total)
    }

    @Test
    fun `bullet-type boosters are rarer than base boosters but commoner than revive`() {
        val piercing = BoosterType.PIERCING_BOOSTER.weight
        val plasma = BoosterType.PLASMA_BOOSTER.weight
        val baseExample = BoosterType.HEALTH_BOOSTER.weight
        val revive = BoosterType.REVIVE_TOKEN.weight
        assertTrue("piercing < base", piercing < baseExample)
        assertTrue("plasma < base", plasma < baseExample)
        assertTrue("piercing > revive", piercing > revive)
        assertTrue("plasma > revive", plasma > revive)
    }

    @Test
    fun `weighted random pick is deterministic given the roll-loop algorithm`() {
        // Mirror the algorithm from Booster.kt:18-32 with a deterministic roll
        // and verify the function maps roll 0 → first type, total-1 → last type.
        val all = BoosterType.entries
        val totalWeight = all.sumOf { it.weight }

        fun pickFor(roll: Int): BoosterType {
            var r = roll
            for (t in all) {
                if (r < t.weight) return t
                r -= t.weight
            }
            return all.last()
        }

        // roll = 0 → first type
        assertEquals(all.first(), pickFor(0))
        // roll = totalWeight - 1 → last type
        assertEquals(all.last(), pickFor(totalWeight - 1))
        // roll inside first bucket → first type
        assertEquals(all.first(), pickFor(all.first().weight - 1))
        // roll just past first bucket → second type
        assertEquals(all[1], pickFor(all.first().weight))
    }

    @Test
    fun `bullet-type boosters are reachable in weighted pick`() {
        // Sum weights up to PIERCING_BOOSTER — roll inside its slot must pick it.
        val all = BoosterType.entries
        val piercingIdx = all.indexOf(BoosterType.PIERCING_BOOSTER)
        val cumulativeBefore = all.take(piercingIdx).sumOf { it.weight }
        val plasmaIdx = all.indexOf(BoosterType.PLASMA_BOOSTER)
        val cumulativeBeforePlasma = all.take(plasmaIdx).sumOf { it.weight }

        // Smallest valid roll inside the piercing slot.
        var r = cumulativeBefore
        var pick: BoosterType = all.first()
        for (t in all) {
            if (r < t.weight) { pick = t; break }
            r -= t.weight
        }
        assertEquals(BoosterType.PIERCING_BOOSTER, pick)

        r = cumulativeBeforePlasma
        pick = all.first()
        for (t in all) {
            if (r < t.weight) { pick = t; break }
            r -= t.weight
        }
        assertEquals(BoosterType.PLASMA_BOOSTER, pick)
    }
}
