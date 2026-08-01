package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Test

/** Task 33 — verifies [fusedStats] averaging formula. */
class ShipFusionTest {

    @Test
    fun `fusedStats averages hpMul speedMul damageMul of both ships`() {
        val a = ShipShape.FIGHTER
        val b = ShipShape.entries.first { it != a }

        val fused = fusedStats(a, b)

        assertEquals((a.hpMul + b.hpMul) / 2f, fused.hpMul, 0.0001f)
        assertEquals((a.speedMul + b.speedMul) / 2f, fused.speedMul, 0.0001f)
        assertEquals((a.damageMul + b.damageMul) / 2f, fused.damageMul, 0.0001f)
    }

    @Test
    fun `fusedStats is symmetric regardless of argument order`() {
        val a = ShipShape.FIGHTER
        val b = ShipShape.entries.first { it != a }

        val fusedAB = fusedStats(a, b)
        val fusedBA = fusedStats(b, a)

        assertEquals(fusedAB.hpMul, fusedBA.hpMul, 0.0001f)
        assertEquals(fusedAB.speedMul, fusedBA.speedMul, 0.0001f)
        assertEquals(fusedAB.damageMul, fusedBA.damageMul, 0.0001f)
    }

    @Test
    fun `fusing a ship with itself returns its own stats unchanged`() {
        val a = ShipShape.FIGHTER

        val fused = fusedStats(a, a)

        assertEquals(a.hpMul, fused.hpMul, 0.0001f)
        assertEquals(a.speedMul, fused.speedMul, 0.0001f)
        assertEquals(a.damageMul, fused.damageMul, 0.0001f)
    }
}
