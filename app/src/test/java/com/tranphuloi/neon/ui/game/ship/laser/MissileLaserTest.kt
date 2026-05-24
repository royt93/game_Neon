package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissileLaserTest {

    private val EPS = 0.001f

    private fun missileAt(x: Float, y: Float) =
        MissileLaser(id = "m1", xOffset = x, yOffset = y, yRange = 1000f)

    @Test
    fun `bulletType is HOMING for visual dispatch + collision treated single-hit`() {
        // Round 71 fix (Issue 4a audit) — MissileLaser now reports HOMING so
        // LaserCanvas can dispatch unique vector shape (capsule + targeting
        // ring) instead of plain capsule. Collision handler still treats it as
        // single-hit because pierceRemaining stays 0 (separate invariant).
        assertEquals(BulletType.HOMING, missileAt(0f, 0f).bulletType)
    }

    @Test
    fun `pierceRemaining is 0 (single-hit)`() {
        assertEquals(0, missileAt(0f, 0f).pierceRemaining)
    }

    @Test
    fun `starts not destroyed`() {
        assertFalse(missileAt(0f, 0f).destroyed)
    }

    @Test
    fun `impactPower is higher than a normal ship laser`() {
        // Round 40 sets MissileLaser to 60f, vs ShipLaser 25f. The test asserts
        // the relative invariant (don't pin the exact number — keeps room for
        // tuning).
        assertTrue(
            "missile impactPower (${missileAt(0f, 0f).impactPower}) should be > 25",
            missileAt(0f, 0f).impactPower > 25f,
        )
    }

    @Test
    fun `moveLaser without target moves straight up`() {
        val m = missileAt(100f, 500f)
        m.targetX = null
        m.moveLaser()
        assertEquals(100f, m.xOffset, EPS)               // x unchanged
        assertEquals(500f - m.yOffsetMovementSpeed, m.yOffset, EPS)
        assertEquals(0f, m.rotation, EPS)                // no rotation
    }

    @Test
    fun `moveLaser with target to the right nudges xOffset right`() {
        val m = missileAt(100f, 500f)
        m.targetX = 200f                                  // far right
        m.moveLaser()
        assertTrue("xOffset must move right toward target", m.xOffset > 100f)
        assertTrue("rotation must be positive (lean right)", m.rotation > 0f)
    }

    @Test
    fun `moveLaser with target to the left nudges xOffset left`() {
        val m = missileAt(100f, 500f)
        m.targetX = 20f                                   // left
        m.moveLaser()
        assertTrue("xOffset must move left toward target", m.xOffset < 100f)
        assertTrue("rotation must be negative (lean left)", m.rotation < 0f)
    }

    @Test
    fun `lateral nudge is capped at HOMING_X_STEP`() {
        val m = missileAt(100f, 500f)
        m.targetX = 99999f                                // very far right
        m.moveLaser()
        // xOffset should advance by exactly HOMING_X_STEP (not the full delta).
        assertEquals(100f + MissileLaser.HOMING_X_STEP, m.xOffset, EPS)
    }

    @Test
    fun `rotation is capped at MAX_ROTATION_DEG at full lateral nudge`() {
        val m = missileAt(100f, 500f)
        m.targetX = 99999f                                // forces step = +HOMING_X_STEP
        m.moveLaser()
        assertEquals(MissileLaser.MAX_ROTATION_DEG, m.rotation, EPS)
    }

    @Test
    fun `target landing at exact missile x produces zero rotation`() {
        val m = missileAt(100f, 500f)
        m.targetX = 100f                                   // already aligned
        m.moveLaser()
        assertEquals(100f, m.xOffset, EPS)
        assertEquals(0f, m.rotation, EPS)
    }

    @Test
    fun `successive moveLaser ticks accumulate vertical distance`() {
        val m = missileAt(100f, 500f)
        m.targetX = null
        val startY = m.yOffset
        repeat(10) { m.moveLaser() }
        assertEquals(startY - 10 * m.yOffsetMovementSpeed, m.yOffset, EPS)
    }
}
