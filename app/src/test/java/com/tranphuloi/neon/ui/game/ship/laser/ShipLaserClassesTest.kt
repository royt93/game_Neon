package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipLaserClassesTest {

    private val EPS = 0.001f

    @Test
    fun `PiercingShipLaser initial pierceRemaining matches BulletType_PIERCING_pierceCount`() {
        val l = PiercingShipLaser(id = "p1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertEquals(BulletType.PIERCING.pierceCount, l.pierceRemaining)
        assertEquals(3, l.pierceRemaining)
    }

    @Test
    fun `PiercingShipLaser pierceRemaining decrements on assignment`() {
        val l = PiercingShipLaser(id = "p1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        l.pierceRemaining -= 1
        assertEquals(2, l.pierceRemaining)
        l.pierceRemaining -= 1
        l.pierceRemaining -= 1
        assertEquals(0, l.pierceRemaining)
    }

    @Test
    fun `PiercingShipLaser carries PIERCING bullet type tag`() {
        val l = PiercingShipLaser(id = "p1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertEquals(BulletType.PIERCING, l.bulletType)
    }

    @Test
    fun `PiercingShipLaser starts not destroyed`() {
        val l = PiercingShipLaser(id = "p1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertFalse(l.destroyed)
    }

    @Test
    fun `PiercingShipLaser moveLaser decreases yOffset by movement speed`() {
        val l = PiercingShipLaser(id = "p1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        l.moveLaser()
        assertEquals(100f - l.yOffsetMovementSpeed, l.yOffset, EPS)
        l.moveLaser()
        assertEquals(100f - 2 * l.yOffsetMovementSpeed, l.yOffset, EPS)
    }

    @Test
    fun `PlasmaShipLaser carries PLASMA bullet type tag`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertEquals(BulletType.PLASMA, l.bulletType)
    }

    @Test
    fun `PlasmaShipLaser pierceRemaining default is 0 (non-piercing)`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        // Plasma uses the interface default (0) — does NOT pierce.
        assertEquals(0, l.pierceRemaining)
    }

    @Test
    fun `PlasmaShipLaser width matches PLASMA_WIDTH constant`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertEquals(PlasmaShipLaser.PLASMA_WIDTH, l.width, EPS)
        assertEquals(16f, l.width, EPS)
    }

    @Test
    fun `PlasmaShipLaser impactPower reflects damage multiplier`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        // 25 × 1.6 = 40 per source comment
        assertEquals(40f, l.impactPower, EPS)
        assertTrue(
            "plasma should hit harder than a normal 25-power shot",
            l.impactPower > 25f,
        )
    }

    @Test
    fun `PlasmaShipLaser moveLaser uses slow vertical speed`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        val before = l.yOffset
        l.moveLaser()
        assertEquals(before - l.yOffsetMovementSpeed, l.yOffset, EPS)
        assertTrue("plasma should move slower than 7 (piercing speed)", l.yOffsetMovementSpeed < 7f)
    }

    @Test
    fun `Plasma rotation is zero`() {
        val l = PlasmaShipLaser(id = "pl1", xOffset = 0f, yOffset = 100f, yRange = 1000f)
        assertEquals(0f, l.rotation, EPS)
    }
}
