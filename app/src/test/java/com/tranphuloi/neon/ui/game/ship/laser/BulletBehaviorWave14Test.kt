package com.tranphuloi.neon.ui.game.ship.laser

import com.tranphuloi.neon.ui.game.ship.ship.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 14 — pins the bullet-behavior wiring fixed this round:
 *  - KAMEHAMEHA reuses PiercingShipLaser body but keeps its own bulletType +
 *    pierce-99 (so it pierces, not destroyed on first hit).
 *  - ATOMIC reuses PlasmaShipLaser body but keeps its own bulletType (AoE uses
 *    ATOMIC.aoeRadius = 150, bigger than PLASMA's 80).
 *  - Ship gains `baseBulletType` (whole-run weapon) — default NORMAL.
 */
class BulletBehaviorWave14Test {

    @Test
    fun `KAMEHAMEHA laser pierces (pierceRemaining = 99) and keeps its bulletType`() {
        val l = PiercingShipLaser(
            id = "k", xOffset = 0f, yOffset = 0f, yRange = 800f,
            bulletType = BulletType.KAMEHAMEHA,
        )
        assertEquals(BulletType.KAMEHAMEHA, l.bulletType)
        assertEquals(BulletType.KAMEHAMEHA.pierceCount, l.pierceRemaining)
        assertTrue("KAMEHAMEHA must pierce many", l.pierceRemaining >= 99)
    }

    @Test
    fun `default PiercingShipLaser stays PIERCING with pierce 3`() {
        val l = PiercingShipLaser(id = "p", xOffset = 0f, yOffset = 0f, yRange = 800f)
        assertEquals(BulletType.PIERCING, l.bulletType)
        assertEquals(BulletType.PIERCING.pierceCount, l.pierceRemaining)
    }

    @Test
    fun `ATOMIC laser keeps its bulletType and ATOMIC AoE is wider than PLASMA`() {
        val l = PlasmaShipLaser(id = "a", xOffset = 0f, yOffset = 0f, yRange = 800f, bulletType = BulletType.ATOMIC)
        assertEquals(BulletType.ATOMIC, l.bulletType)
        assertTrue(
            "ATOMIC AoE (${BulletType.ATOMIC.aoeRadius}) must exceed PLASMA (${BulletType.PLASMA.aoeRadius})",
            BulletType.ATOMIC.aoeRadius > BulletType.PLASMA.aoeRadius,
        )
        assertEquals(150f, BulletType.ATOMIC.aoeRadius, 0.001f)
    }

    @Test
    fun `default PlasmaShipLaser stays PLASMA`() {
        val l = PlasmaShipLaser(id = "pl", xOffset = 0f, yOffset = 0f, yRange = 800f)
        assertEquals(BulletType.PLASMA, l.bulletType)
    }

    @Test
    fun `Ship baseBulletType defaults to NORMAL (whole-run weapon base)`() {
        assertEquals(BulletType.NORMAL, Ship(xOffset = 0f, yOffset = 0f).baseBulletType)
    }

    @Test
    fun `Ship baseBulletType is preserved on copy (revert target)`() {
        val s = Ship(xOffset = 0f, yOffset = 0f).copy(baseBulletType = BulletType.HOMING, activeBulletType = BulletType.PLASMA)
        // simulate booster expiry revert → activeBulletType should become baseBulletType
        val reverted = s.copy(activeBulletType = s.baseBulletType, bulletTypeEndMillis = 0L)
        assertEquals(BulletType.HOMING, reverted.activeBulletType)
    }
}
