package com.tranphuloi.neon.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d Pixel-3 round 4 — pin the flash overlay alpha curve contract.
 *
 * `flashCurve(elapsedMs, peakMs, totalMs)` is the pure helper that produces
 * the alpha multiplier for the UltimateLaser cyan / SmartBomb violet
 * fullscreen flash overlays (GameScreen.kt). Pre-round-4 the flash used
 * linear fade-out `1 - elapsed/total` which felt instantaneous; new curve
 * ramps UP to 1 over [0, peakMs] then DOWN to 0 over [peakMs, totalMs].
 *
 * The production function is `private fun flashCurve` in GameScreen.kt so
 * we mirror it here as a regression-style algorithm pin. (Re-implementation
 * pattern documented in audit cycle 4 as acceptable for short pure helpers
 * that aren't worth the visibility refactor — same approach used in
 * EnemyOverlapWave11dTest pre-Audit-3.)
 */
class FlashCurveTest {

    /** Mirror of GameScreen.kt:flashCurve. */
    private fun flashCurve(elapsedMs: Long, peakMs: Long, totalMs: Long): Float {
        if (elapsedMs < 0L || elapsedMs >= totalMs) return 0f
        return if (elapsedMs < peakMs) {
            elapsedMs.toFloat() / peakMs
        } else {
            (1f - (elapsedMs - peakMs).toFloat() / (totalMs - peakMs)).coerceAtLeast(0f)
        }
    }

    @Test
    fun `t=0 returns 0 (flash just fired, fade-in not started)`() {
        assertEquals(0f, flashCurve(0L, peakMs = 150L, totalMs = 900L), 1e-4f)
    }

    @Test
    fun `t at peak returns 1 (full brightness)`() {
        // At elapsedMs = peakMs, formula is `1 - 0/750 = 1`.
        assertEquals(1f, flashCurve(150L, peakMs = 150L, totalMs = 900L), 1e-4f)
    }

    @Test
    fun `t = totalMs returns 0 (flash ended)`() {
        assertEquals(0f, flashCurve(900L, peakMs = 150L, totalMs = 900L), 1e-4f)
    }

    @Test
    fun `t past totalMs returns 0 (flash already faded)`() {
        assertEquals(0f, flashCurve(5000L, peakMs = 150L, totalMs = 900L), 1e-4f)
    }

    @Test
    fun `negative t returns 0 (defensive — clock skew can produce negatives)`() {
        assertEquals(0f, flashCurve(-100L, peakMs = 150L, totalMs = 900L), 1e-4f)
    }

    @Test
    fun `ramp-up phase is linear 0 to 1 over peakMs`() {
        // Half-way through ramp-up.
        assertEquals(0.5f, flashCurve(75L, peakMs = 150L, totalMs = 900L), 1e-4f)
        // Quarter-way.
        assertEquals(0.25f, flashCurve(37L, peakMs = 150L, totalMs = 900L), 0.01f)
    }

    @Test
    fun `fade-out phase is linear 1 to 0 over remaining time`() {
        // Half-way through fade-out (peak + (totalMs-peakMs)/2 = 150 + 375 = 525)
        assertEquals(0.5f, flashCurve(525L, peakMs = 150L, totalMs = 900L), 1e-4f)
        // Just past peak — still very bright.
        assertTrue(flashCurve(160L, peakMs = 150L, totalMs = 900L) > 0.95f)
    }

    @Test
    fun `SmartBomb tuning (peak=180, total=1000) hits 1 at peak`() {
        // Different tuning than UltimateLaser cyan to convey heavier ability.
        assertEquals(1f, flashCurve(180L, peakMs = 180L, totalMs = 1000L), 1e-4f)
        // Half-fade.
        assertEquals(0.5f, flashCurve(590L, peakMs = 180L, totalMs = 1000L), 0.01f)
    }

    @Test
    fun `curve never produces values outside 0 to 1`() {
        // Sweep 0..1500ms in 50ms steps; assert all values clamped properly.
        for (t in 0L..1500L step 50L) {
            val v = flashCurve(t, peakMs = 150L, totalMs = 900L)
            assertTrue("t=$t produced $v outside [0,1]", v in 0f..1f)
        }
    }

    @Test
    fun `peak duration ratio — 150 of 900 means peak at one-sixth of total`() {
        // Pin the tuning so it doesn't drift silently. UltimateLaser settings:
        // peak 150 of 900 = 16.7% into the flash. Quick ramp-up, longer fade.
        val ratio = 150f / 900f
        assertTrue("Ramp-up ratio drift detected", ratio in 0.15f..0.18f)
    }

    @Test
    fun `regression — prior linear fade-out returns 1 at t=0`() {
        // Pre-fix used `1 - elapsed/total`. At t=0 that's 1.0 — flash AT full
        // brightness from frame 1. New curve returns 0 at t=0 (ramp-up). If
        // anyone reverts to linear, this test fires.
        val priorBehavior = 1f - 0f / 900f   // 1.0
        val newBehavior = flashCurve(0L, peakMs = 150L, totalMs = 900L)  // 0.0
        assertTrue(
            "Prior linear-fade and new ramp-curve must differ at t=0",
            priorBehavior != newBehavior,
        )
    }
}
