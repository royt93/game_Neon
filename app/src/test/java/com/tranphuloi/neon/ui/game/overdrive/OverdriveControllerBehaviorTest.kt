package com.tranphuloi.neon.ui.game.overdrive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [OverdriveController] — Task 31 (Overdrive bullet-time).
 * Uses explicit `now` timestamps (no `Thread.sleep`) since the controller
 * takes time as a parameter rather than reading the system clock itself.
 */
class OverdriveControllerBehaviorTest {

    private fun newController(threshold: Int = 20, durationMillis: Long = 4000L) =
        OverdriveController(threshold = threshold, durationMillis = durationMillis)

    @Test
    fun `kills below threshold accumulate without triggering`() {
        val ctrl = newController(threshold = 20)
        repeat(19) { ctrl.onEnemyKilled(now = 0L) }
        assertEquals(19, ctrl.killCount)
        assertFalse(ctrl.isActive(now = 0L))
    }

    @Test
    fun `reaching threshold triggers exactly once and resets kill count to zero`() {
        val ctrl = newController(threshold = 20, durationMillis = 4000L)
        repeat(19) { ctrl.onEnemyKilled(now = 0L) }
        val triggered = ctrl.onEnemyKilled(now = 0L)

        assertTrue("kill thứ 20 phải trigger Overdrive", triggered)
        assertEquals(0, ctrl.killCount)
        assertEquals(4000L, ctrl.activeUntilMillis)
    }

    @Test
    fun `isActive is true throughout the active window and false after it ends`() {
        val ctrl = newController(threshold = 1, durationMillis = 4000L)
        ctrl.onEnemyKilled(now = 1_000L)

        assertTrue(ctrl.isActive(now = 1_000L))
        assertTrue(ctrl.isActive(now = 4_999L))
        assertFalse(ctrl.isActive(now = 5_000L))
        assertFalse(ctrl.isActive(now = 6_000L))
    }

    @Test
    fun `checkExpiry fires exactly once on the active-to-inactive transition`() {
        val ctrl = newController(threshold = 1, durationMillis = 4000L)
        ctrl.onEnemyKilled(now = 1_000L)

        assertFalse("vẫn đang active, chưa được coi là expired", ctrl.checkExpiry(now = 2_000L))
        assertFalse("vẫn đang active, chưa được coi là expired", ctrl.checkExpiry(now = 4_999L))
        assertTrue("đúng lúc active→inactive phải trả về true", ctrl.checkExpiry(now = 5_000L))
        assertFalse("đã báo expired 1 lần rồi thì các lần sau không báo lại", ctrl.checkExpiry(now = 6_000L))
    }

    @Test
    fun `checkExpiry never fires before Overdrive has ever triggered`() {
        val ctrl = newController()
        assertFalse(ctrl.checkExpiry(now = 0L))
        assertFalse(ctrl.checkExpiry(now = 100_000L))
    }

    @Test
    fun `a second kill streak after expiry retriggers Overdrive`() {
        val ctrl = newController(threshold = 2, durationMillis = 1000L)
        ctrl.onEnemyKilled(now = 0L)
        val firstTrigger = ctrl.onEnemyKilled(now = 0L)
        assertTrue(firstTrigger)
        ctrl.checkExpiry(now = 1_000L)

        ctrl.onEnemyKilled(now = 2_000L)
        val secondTrigger = ctrl.onEnemyKilled(now = 2_000L)
        assertTrue("chu kỳ Overdrive thứ 2 phải trigger lại được", secondTrigger)
        assertEquals(3_000L, ctrl.activeUntilMillis)
    }
}
