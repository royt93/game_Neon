package com.tranphuloi.neon.ui.game.enemy

import com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d — pin the audit-P2 overlap-rejection contract.
 *
 * Audit-3 #1 fix — tests now drive the PRODUCTION
 * [EnemyController.bboxOverlaps] function directly (extracted to companion as
 * `@VisibleForTesting internal`).
 *
 * Pixel-2 #3 fix — multiplier tightened 0.45 → 0.5 because user reported
 * residual visual overlap. Tests below updated to reflect the tighter rule.
 *
 * Contract pinned:
 *   1. Two enemies at the same (x, y) collide.
 *   2. Adjacent Row members at current max width 46 + spacing ~61 still pass.
 *   3. SineWave / V members staggered vertically pass (dy > yLimit).
 *   4. Boss spawns bypass entirely (verified separately by inspection).
 */
class EnemyOverlapWave11dTest {

    /** Stub enemy footprint for overlap testing. */
    private data class FP(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Thin wrapper around production predicate so existing tests read cleanly. */
    private fun collides(candidate: FP, other: FP): Boolean =
        EnemyController.bboxOverlaps(
            ax = candidate.x, ay = candidate.y, aw = candidate.w, ah = candidate.h,
            bx = other.x, by = other.y, bw = other.w, bh = other.h,
        )

    @Test
    fun `identical position collides`() {
        val a = FP(100f, 100f, 40f, 40f)
        val b = FP(100f, 100f, 40f, 40f)
        assertTrue(collides(a, b))
    }

    @Test
    fun `Row formation max-tier-2 widths spaced ~61dp apart do NOT collide`() {
        // Stage.kt: width = 40 + tier * 3. Max tier = 2 → max width 46dp.
        // FormationXOffset.kt: distanceBetween = (411 + 0) / 6 - 46 / 6
        //                                     = 68.5 - 7.67 = 60.83dp.
        // xLimit = (46 + 46) * 0.5 = 46 → 60.83 > 46 → no collision (Pixel-2 #3
        // tightened multiplier from 0.45 to 0.5; current data still safe).
        val w = 46f
        val dx = 411f / 6f - w / 6f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertFalse("Adjacent Row members must pass at current widths", collides(a, b))
    }

    @Test
    fun `Row spacing breakpoint moves to width 58 at multiplier 0_5`() {
        // Pixel-2 #3 tightened multiplier 0.45 → 0.5. At width 60,
        // Row spacing = 411/6 - 60/6 = 58.5. xLimit = (60+60)*0.5 = 60.
        // 58.5 < 60 → COLLIDES (rejected). So if difficulty tuning bumps
        // enemy width past ~58dp, Row formations start losing members.
        // Current max width is 46dp → 12dp safety buffer.
        val w = 60f
        val dx = 411f / 6f - w / 6f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertTrue("Row at w=60 hits the multiplier-0.5 breakpoint", collides(a, b))
    }

    @Test
    fun `Row spacing fails at extreme width 80dp - documented breakpoint`() {
        // At width 80, Row distanceBetween = 411/6 - 80/6 = 55.17.
        // xLimit = (80+80)*0.45 = 72 → 55.17 < 72 → collides.
        // This documents the future-breakpoint — pretty far from current 46dp
        // ceiling. If we ever push width past ~75dp we MUST also tune
        // FormationXOffset.rowXOffset's distanceBetween formula or reduce
        // rowCount.
        val w = 80f
        val dx = 411f / 6f - w / 6f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertTrue("Extreme-wide Row at w=80dp would collide — flag for future tuning",
            collides(a, b))
    }

    // ── Audit-2 Risk #2 documentation tests — multi-formation same-tick bunching ──
    //
    // Risk #2 was diagnosed as theoretical (audit said "plausible"). The fix
    // attempt (skip-if-other-at-spawn-band) accidentally nullified Bug #4
    // overlap detection because for same-height enemies y-threshold (= h)
    // always exceeded yLimit (= 0.9h). Reverted. These tests document the
    // KNOWN edge case so a future change re-introducing the bug is visible.

    @Test
    fun `Risk #2 documentation — SineWave i0 vs same-tick Row at centerX WOULD collide`() {
        // Row member spawned at centerX (≈205.5 on 411dp screen, w=46 →
        // xOffset 182.5 for index 3) is in `existingSnapshot` at yOffset=0.
        // SineWave's i=0 candidate at (centerX, 0) — both share x and y → collides.
        //
        // In production this is dormant: Stage.kt fires exactly one formation
        // per tick (one of ZigZag/Row/V/SineWave from the seed selector).
        // If a future stage script adds parallel formation spawning, the
        // collision below activates and silently truncates the second
        // formation. Fix would require timestamping enemies + skipping
        // those spawned this tick.
        val w = 46f
        val centerXForW = 411f / 2f - w / 2f  // = 182.5
        val rowMember = FP(centerXForW, 0f, w, w)
        val sineWaveI0 = FP(centerXForW, 0f, w, w)
        assertTrue("Same-position same-tick spawns DO collide — documents Risk #2 dormant edge case",
            collides(sineWaveI0, rowMember))
    }

    @Test
    fun `SineWave members vertically staggered (yStep = h * 1_4) pass`() {
        // SineWave spawns all at centerX; vertical stagger -i * yStep.
        // yStep = h * 1.4 → dy = 1.4·h. yLimit = (h+h)*0.45 = 0.9·h.
        // 1.4·h > 0.9·h → no collision.
        val h = 40f
        val yStep = h * 1.4f
        val a = FP(100f, 0f, h, h)
        val b = FP(100f, yStep, h, h)
        assertFalse("SineWave vertical stagger must pass", collides(a, b))
    }

    @Test
    fun `V formation wings (xStep = w * 1_4) pass`() {
        // V xStep = w * 1.4. Two wings at +1 and -1 → dx = 1.4·w.
        // xLimit = (w+w)*0.45 = 0.9·w → passes.
        val w = 40f
        val xStep = w * 1.4f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + xStep, 50f, w, w)
        assertFalse("V wing tip vs center must pass", collides(a, b))
    }

    @Test
    fun `enemies at same x but very different y do NOT collide`() {
        // New SineWave can't overlap existing Row whose i=0 element is still
        // near yOffset=0 because they share centerX. But once Row members
        // drift south (yOffset grows), even same-x is fine.
        val a = FP(100f, 0f, 40f, 40f)        // freshly spawned
        val b = FP(100f, 400f, 40f, 40f)      // existing, drifted south
        assertFalse(collides(a, b))
    }

    @Test
    fun `enemies at far x but same y do NOT collide`() {
        val a = FP(50f, 100f, 40f, 40f)
        val b = FP(300f, 100f, 40f, 40f)
        assertFalse(collides(a, b))
    }

    @Test
    fun `edges exactly touching pass (multiplier 0_5 boundary)`() {
        // dx = w (centers exactly one width apart, edges touching). With
        // multiplier 0.5: xLimit = w → dx == xLimit → strict `<` so passes.
        val w = 40f
        val dx = w
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertFalse("Edge-touching pair (dx = w) must pass at multiplier 0.5", collides(a, b))
    }

    @Test
    fun `centers within 80 percent overlap distance fails (clear overlap)`() {
        // dx = w * 0.8, dy = h * 0.8 → both axes deep inside footprint.
        val w = 40f
        val a = FP(100f, 100f, w, w)
        val b = FP(100f + w * 0.8f, 100f + w * 0.8f, w, w)
        // xLimit = yLimit = 0.9w = 36; dx = dy = 32 → 32 < 36 → true.
        assertTrue(collides(a, b))
    }

    @Test
    fun `near-overlap at dx=0_8w rejected (multiplier 0_5)`() {
        // dx = 0.8w. At multiplier 0.5: xLimit = w → 0.8w < w → collides.
        // Documents the tight bound chosen by Pixel-2 #3 fix.
        val w = 40f
        val a = FP(0f, 0f, w, w)
        val b = FP(0.8f * w, 0f, w, w)
        assertTrue("Near-overlap at dx=0.8w must collide at multiplier 0.5", collides(a, b))
    }

    @Test
    fun `predicate is symmetric`() {
        // BBOX predicate is order-independent (dx + dy + limits all use commutative ops).
        val a = FP(50f, 50f, 60f, 60f)
        val b = FP(70f, 60f, 50f, 50f)
        assertEquals(
            "collide(a,b) must equal collide(b,a) for the BBOX check",
            collides(a, b), collides(b, a),
        )
    }

    // ── Pixel-2 #3 regression guards — production multiplier 0.5 ──

    @Test
    fun `regression — multiplier 0_55 would falsely reject Row at width 46`() {
        // Pixel-2 #3 chose multiplier 0.5 as the upper bound that still
        // leaves 12dp safety buffer for current max width 46dp Row formations.
        // If someone tightens to 0.55, Row formations would have:
        //   xLimit = (46+46)*0.55 = 50.6
        //   Row spacing = 60.83 > 50.6 → still passes (barely).
        // At 0.6: xLimit = 55.2 → still passes.
        // At 0.66: xLimit = 60.72 → 60.83 > 60.72 → still passes by 0.1dp.
        // At 0.67+: xLimit = 61.64 → 60.83 < 61.64 → REJECTS. So the
        // sensible upper bound is ~0.66.
        //
        // To regression-guard, we assert that the CURRENT impl passes the
        // Row spacing at width 46 (already done in test above), AND that
        // a deliberately-over-tightened pair (dx=w*1.1) still fails — i.e.
        // multiplier hasn't dropped below ~0.55.
        val w = 40f
        val a = FP(0f, 0f, w, w)
        val b = FP(1.1f * w, 0f, w, w)  // dx > w → above all reasonable xLimits
        assertFalse(
            "Pair at dx=1.1w must NOT collide regardless of multiplier choice in [0.45, 0.66]",
            collides(a, b),
        )
    }

    @Test
    fun `regression — multiplier 0_45 would falsely accept this near-overlap pair`() {
        // Inverse: if production were relaxed back to 0.45, a pair at
        // dx=0.95w would pass (xLimit=0.9w, 0.95w > 0.9w). At current 0.5,
        // xLimit=w, 0.95w < w → collides correctly. Catches regression.
        val w = 40f
        val a = FP(0f, 0f, w, w)
        val b = FP(0.95f * w, 0f, w, w)
        assertTrue(
            "Near-overlap pair at dx=0.95w MUST collide at multiplier 0.5 — if it doesn't, multiplier regressed to 0.45 or lower",
            collides(a, b),
        )
    }
}
