package com.tranphuloi.neon.ui.game.ship.ship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 53 — soft decay math for [ShipController.computeChargeStartAfterDamage].
 *
 * Setup convention: nowMillis = 20_000, chargeStartMillis = 0 means charge is
 * fully built (20s elapsed = 100%). After applying damage, the returned new
 * start time represents how much "elapsed" the charge has left.
 *
 * Progress = (nowMillis - returnedStart) / CHARGE_FILL_MS (= 20_000ms).
 */
class ChargeShotSoftDecayTest {

    private val now = 20_000L
    private val fullChargeStart = 0L

    @Test
    fun `base penalty only — zero damage shaves exactly 3s`() {
        val after = ShipController.computeChargeStartAfterDamage(
            chargeStartMillis = fullChargeStart,
            damageAmount = 0,
            nowMillis = now,
        )
        // 0 + 3000 + 0 = 3000 → elapsed = 17000 → 85% progress
        assertEquals(3_000L, after)
    }

    @Test
    fun `light damage 25 shaves 3_5s (base 3s + 25 × 20ms)`() {
        val after = ShipController.computeChargeStartAfterDamage(
            chargeStartMillis = fullChargeStart,
            damageAmount = 25,
            nowMillis = now,
        )
        assertEquals(3_500L, after)
    }

    @Test
    fun `heavy damage 100 caps scaled penalty at 2s (total 5s)`() {
        val after = ShipController.computeChargeStartAfterDamage(
            chargeStartMillis = fullChargeStart,
            damageAmount = 100,
            nowMillis = now,
        )
        assertEquals(5_000L, after)
    }

    @Test
    fun `damage beyond cap (500) yields same penalty as cap-boundary (100)`() {
        val atCap = ShipController.computeChargeStartAfterDamage(0L, 100, now)
        val overCap = ShipController.computeChargeStartAfterDamage(0L, 500, now)
        assertEquals("scaled penalty must clamp", atCap, overCap)
    }

    @Test
    fun `chargeStart never exceeds now — repeated hits drain to zero but not negative`() {
        var current = fullChargeStart
        // 4 heavy hits (100 dmg each, 5s shave) drain 20s fully
        repeat(4) {
            current = ShipController.computeChargeStartAfterDamage(current, 100, now)
        }
        assertEquals("after 4 heavy hits start should equal now (zero charge)", now, current)

        // 5th hit doesn't push start past now
        val fifth = ShipController.computeChargeStartAfterDamage(current, 100, now)
        assertEquals(now, fifth)
    }

    @Test
    fun `near-full charge gets clamped when single hit would push past now`() {
        // chargeStart=18_000 → only 2s elapsed = 10% progress. A 100-dmg hit
        // would push start to 23_000, but it must clamp to nowMillis=20_000.
        val after = ShipController.computeChargeStartAfterDamage(18_000L, 100, now)
        assertEquals(now, after)
    }

    @Test
    fun `scaled penalty scales linearly under cap`() {
        val dmg25 = ShipController.computeChargeStartAfterDamage(0L, 25, now)   // +3500
        val dmg50 = ShipController.computeChargeStartAfterDamage(0L, 50, now)   // +4000
        val dmg75 = ShipController.computeChargeStartAfterDamage(0L, 75, now)   // +4500
        assertEquals(3_500L, dmg25)
        assertEquals(4_000L, dmg50)
        assertEquals(4_500L, dmg75)
    }

    @Test
    fun `light damage in fast succession still drains the meter eventually`() {
        // Simulate 10 light scrapes (25 dmg) — each ~3.5s shave.
        // 20_000 / 3_500 = ~5.7 → 6 hits should fully drain.
        var current = fullChargeStart
        var hitsToDrain = 0
        while (current < now && hitsToDrain < 20) {
            current = ShipController.computeChargeStartAfterDamage(current, 25, now)
            hitsToDrain++
        }
        assertEquals("expected drain in ~6 light hits", 6, hitsToDrain)
        assertEquals(now, current)
    }

    @Test
    fun `negative damage clamps scaled penalty to 0 (defensive — caller already gates)`() {
        // updateHp() only calls resetCharge() when effective < 0, passing -effective
        // (always positive). But if a future caller miswires this, defensive clamp
        // ensures we still apply at least the base 3s penalty rather than crashing
        // or going negative.
        val after = ShipController.computeChargeStartAfterDamage(0L, -50, now)
        assertEquals("base penalty only when scaled goes negative", 3_000L, after)
    }

    @Test
    fun `previously charge was reset to now (regression — soft decay must be slower)`() {
        // Pre-round-53: any damage → chargeStartMillis = now → 0% instantly.
        // Soft decay: 25 dmg should leave ~82.5% progress (start=3_500 of fill 20_000).
        val after = ShipController.computeChargeStartAfterDamage(0L, 25, now)
        val progressAfter = (now - after).toFloat() / 20_000f
        assertTrue("soft decay must leave >50% progress after one light hit, got $progressAfter", progressAfter > 0.5f)
    }
}
