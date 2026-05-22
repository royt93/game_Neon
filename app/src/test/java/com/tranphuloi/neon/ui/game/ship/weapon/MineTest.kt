package com.tranphuloi.neon.ui.game.ship.weapon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MineTest {

    @Test
    fun `constants are all positive`() {
        assertTrue("SIZE > 0", Mine.SIZE > 0f)
        assertTrue("EXPLOSION_DAMAGE > 0", Mine.EXPLOSION_DAMAGE > 0f)
        assertTrue("AOE_RADIUS > 0", Mine.AOE_RADIUS > 0f)
        assertTrue("TRIGGER_RADIUS > 0", Mine.TRIGGER_RADIUS > 0f)
        assertTrue("LIFETIME_MS > 0", Mine.LIFETIME_MS > 0L)
    }

    @Test
    fun `TRIGGER_RADIUS is smaller than AOE_RADIUS`() {
        // Invariant: any enemy close enough to trigger must also be inside the
        // AoE blast — otherwise the mine fires but does no damage.
        assertTrue(
            "TRIGGER_RADIUS (${Mine.TRIGGER_RADIUS}) must be < AOE_RADIUS (${Mine.AOE_RADIUS})",
            Mine.TRIGGER_RADIUS < Mine.AOE_RADIUS,
        )
    }

    @Test
    fun `LIFETIME is several seconds (not too short)`() {
        // If lifetime is too short the mine is unusable as a delayed trap.
        assertTrue("LIFETIME_MS should be at least 3 seconds", Mine.LIFETIME_MS >= 3_000L)
    }

    @Test
    fun `Mine constructed with sane defaults`() {
        val m = Mine(
            id = "test-1",
            xOffset = 100f,
            yOffset = 200f,
            createdAtMillis = 1_000_000L,
        )
        assertEquals("test-1", m.id)
        assertEquals(100f, m.xOffset, 0f)
        assertEquals(200f, m.yOffset, 0f)
        assertEquals(1_000_000L, m.createdAtMillis)
        assertFalse("Mine starts un-detonated", m.detonated)
    }

    @Test
    fun `detonated flag is mutable so loop can mark for removal`() {
        val m = Mine(id = "x", xOffset = 0f, yOffset = 0f, createdAtMillis = 0L)
        assertFalse(m.detonated)
        m.detonated = true
        assertTrue(m.detonated)
    }
}
