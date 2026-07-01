package com.tranphuloi.neon.ui.game.combo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [ComboController] — kill counting, tier-advance
 * callback firing exactly once per new tier, reset-window expiry.
 *
 * Uses a short [ComboController.resetWindowMillis] with a generous
 * `Thread.sleep` margin for expiry tests to avoid flaking near the real
 * 2000ms default boundary (see plan Tier 1A note).
 */
class ComboControllerBehaviorTest {

    // ── Kill counting + tier mapping ──

    @Test
    fun `first kill has no tier (below DOUBLE threshold)`() {
        val ctrl = ComboController()
        val tier = ctrl.onEnemyKilled()
        assertEquals(1, ctrl.count)
        assertEquals(ComboTier.NONE, tier)
        assertEquals(1, ctrl.multiplier())
    }

    @Test
    fun `consecutive kills advance tier at each threshold`() {
        val ctrl = ComboController()
        val tiers = (1..12).map { ctrl.onEnemyKilled() }
        assertEquals(
            listOf(
                ComboTier.NONE, ComboTier.DOUBLE, ComboTier.TRIPLE, ComboTier.TRIPLE,
                ComboTier.RAMPAGE, ComboTier.RAMPAGE, ComboTier.RAMPAGE,
                ComboTier.UNSTOPPABLE, ComboTier.UNSTOPPABLE, ComboTier.UNSTOPPABLE, ComboTier.UNSTOPPABLE,
                ComboTier.GODLIKE,
            ),
            tiers,
        )
        assertEquals(12, ctrl.count)
        assertEquals(ComboTier.GODLIKE, ctrl.currentTier())
        assertEquals(8, ctrl.multiplier())
    }

    @Test
    fun `onTierAdvance fires exactly once per new tier, not on repeat kills within the same tier`() {
        val advanced = mutableListOf<ComboTier>()
        val ctrl = ComboController(onTierAdvance = { advanced.add(it) })
        repeat(7) { ctrl.onEnemyKilled() } // counts 1..7 → NONE,DOUBLE,TRIPLE,TRIPLE,RAMPAGE,RAMPAGE,RAMPAGE
        assertEquals(listOf(ComboTier.DOUBLE, ComboTier.TRIPLE, ComboTier.RAMPAGE), advanced)
    }

    // ── Reset-window expiry ──

    @Test
    fun `onEnemyKilled resets combo when gap since last kill exceeds resetWindowMillis`() {
        val ctrl = ComboController(resetWindowMillis = 30L)
        ctrl.onEnemyKilled()
        ctrl.onEnemyKilled()
        assertEquals(2, ctrl.count)

        Thread.sleep(150L) // well past the 30ms window — not flaky.
        ctrl.onEnemyKilled()

        assertEquals("gap quá dài phải reset về 1 (kill hiện tại), không cộng dồn", 1, ctrl.count)
    }

    @Test
    fun `checkExpiry resets count and tier when idle past resetWindowMillis`() {
        val ctrl = ComboController(resetWindowMillis = 30L)
        repeat(3) { ctrl.onEnemyKilled() }
        assertEquals(ComboTier.TRIPLE, ctrl.currentTier())

        Thread.sleep(150L)
        ctrl.checkExpiry()

        assertEquals(0, ctrl.count)
        assertEquals(ComboTier.NONE, ctrl.currentTier())
    }

    @Test
    fun `checkExpiry is a no-op when count is already zero`() {
        val ctrl = ComboController(resetWindowMillis = 30L)
        ctrl.checkExpiry()
        assertEquals(0, ctrl.count)
    }

    @Test
    fun `checkExpiry does nothing while still within the reset window`() {
        val ctrl = ComboController(resetWindowMillis = 5_000L)
        ctrl.onEnemyKilled()
        ctrl.onEnemyKilled()
        ctrl.checkExpiry()
        assertTrue("chưa hết window thì không được reset", ctrl.count == 2)
    }
}
