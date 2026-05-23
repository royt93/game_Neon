package com.tranphuloi.neon.ui.game.ship.ship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 61 — verify Ship.phaseShieldEndMillis state field added for the
 * PHASE_SHIELD ghost visual. Simple round-trip + default checks; the actual
 * GameWorld rendering is verified at runtime.
 */
class ShipPhaseShieldTest {

    private fun newShip() = Ship(xOffset = 0f, yOffset = 0f)

    @Test
    fun `phaseShieldEndMillis defaults to 0 (inactive)`() {
        assertEquals(0L, newShip().phaseShieldEndMillis)
    }

    @Test
    fun `phaseShieldEndMillis preserved through copy`() {
        val s = newShip().copy(phaseShieldEndMillis = 12345L)
        assertEquals(12345L, s.phaseShieldEndMillis)
    }

    @Test
    fun `phaseShieldEndMillis independent from iframes (separate concerns)`() {
        // Phase shield is a 5s gameplay buff — Ship.phaseShieldEndMillis surfaces
        // it to GameWorld for ghost rendering. The 600ms damage-iframes timer
        // lives in ShipController (not exposed on Ship). These are independent
        // bookkeeping channels.
        val s = newShip().copy(phaseShieldEndMillis = 99999L)
        // No iframe field on Ship to compare — assertion is that copy() handles
        // the new field cleanly without affecting other props.
        assertEquals(99999L, s.phaseShieldEndMillis)
        assertEquals(newShip().hp, s.hp)                                 // unchanged
        assertEquals(newShip().shieldEnabled, s.shieldEnabled)           // unchanged
    }

    @Test
    fun `four round 60 booster Boolean fields default to false`() {
        val s = newShip()
        assertFalse(s.spreadShotEnabled)
        assertFalse(s.doubleFireEnabled)
        assertFalse(s.critSurgeEnabled)
        assertFalse(s.berserkEnabled)
    }

    @Test
    fun `Boolean buff fields toggle independently via copy`() {
        val s = newShip().copy(spreadShotEnabled = true, berserkEnabled = true)
        assertTrue(s.spreadShotEnabled)
        assertTrue(s.berserkEnabled)
        assertFalse(s.doubleFireEnabled)
        assertFalse(s.critSurgeEnabled)
    }

    @Test
    fun `Ship copy with new fields preserves identity equality`() {
        val s1 = newShip().copy(spreadShotEnabled = true)
        val s2 = newShip().copy(spreadShotEnabled = true)
        // Data-class equality should hold across copies with same params.
        assertEquals(s1, s2)
    }

    @Test
    fun `Ship copy with different phaseShield values are non-equal`() {
        val s1 = newShip().copy(phaseShieldEndMillis = 1000L)
        val s2 = newShip().copy(phaseShieldEndMillis = 2000L)
        assertNotEquals(s1, s2)
    }
}
