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
 * `@VisibleForTesting internal`). Prior version mirrored the algorithm in
 * a test-side helper which was a tautology — production regression to the
 * old multiplier (0.5) would have passed all tests because the test copy
 * was independent.
 *
 * Contract pinned:
 *   1. Two enemies at the same (x, y) collide.
 *   2. Adjacent enemies in a Row formation (current max width 46 + spacing 60+) pass.
 *   3. Wide-Row regression: even at width 60 (future bump), Row spacing
 *      remains > 0.45 × (w + w) = 54 < 60.83 → passes.
 *   4. SineWave / V members staggered vertically pass (dy > yLimit).
 *   5. Within-batch overlap is NOT checked (formations design their own spacing).
 *   6. Boss spawns bypass entirely (verified separately by inspection).
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
        // xLimit = (46 + 46) * 0.45 = 41.4 → 60.83 > 41.4 → no collision.
        val w = 46f
        val dx = 411f / 6f - w / 6f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertFalse("Adjacent Row members must pass at current widths", collides(a, b))
    }

    @Test
    fun `Row spacing survives wide-future width 60dp`() {
        // Future-proofing: if width climbs to 60 via difficulty tuning,
        // Row spacing = 411/6 - 60/6 = 58.5. xLimit = (60+60)*0.45 = 54.
        // 58.5 > 54 → passes. The relaxed 0.45 multiplier buys this margin
        // that the original 0.5 (xLimit=60) didn't.
        val w = 60f
        val dx = 411f / 6f - w / 6f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        assertFalse("Wide-Row spacing must still pass", collides(a, b))
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
    fun `partial overlap below 45 percent passes (edges touching)`() {
        // dx = w * 0.9 → just outside 0.45 * 2w = 0.9w. Edges roughly touch.
        val w = 40f
        val dx = w * 0.9f
        val a = FP(100f, 0f, w, w)
        val b = FP(100f + dx, 0f, w, w)
        // dx = 36, xLimit = 36 → strict `<` so 36 < 36 is FALSE → passes.
        assertFalse(collides(a, b))
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
    fun `relaxed multiplier 0_45 vs prior 0_5 documents the buffer change`() {
        // Demonstrate the audit-P2 fix changed behavior at the boundary.
        // dx = w (centers exactly one width apart). At 0.5 multiplier:
        //   xLimit = w → dx == xLimit → FALSE (boundary). Originally tight.
        // At 0.45 multiplier:
        //   xLimit = 0.9w → dx > xLimit → FALSE with 0.1w buffer.
        val w = 40f
        val dx = w  // centers exactly one width apart
        val a = FP(0f, 0f, w, w)
        val b = FP(dx, 0f, w, w)
        // Current impl with 0.45: 40 < 36 → FALSE.
        val curr = collides(a, b)
        assertFalse("Multiplier 0.45 leaves 0.1w buffer for adjacent enemies", curr)
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

    // ── Audit-3 #1 regression guards — production multiplier 0.45 ──

    @Test
    fun `regression — multiplier 0_5 would falsely reject this adjacent pair`() {
        // Adjacent Row members at distance dx = w (centers exactly one width
        // apart). With multiplier 0.45 → xLimit = 0.9w → 0.9w < w → NO collide.
        // With BIASED multiplier 0.5 → xLimit = w → w < w is FALSE so still
        // no collide here at exact boundary. Need a slightly tighter dx
        // to expose the difference.
        //
        // Construction: dx = 0.95w. With multiplier 0.45 → xLimit = 0.9w →
        // 0.95w > 0.9w → no collide (correct). With multiplier 0.5 →
        // xLimit = w → 0.95w < w → collide (false reject). If production
        // formula regresses to 0.5, this test fires.
        val w = 40f
        val a = FP(0f, 0f, w, w)
        val b = FP(0.95f * w, 0f, w, w)
        assertFalse(
            "Adjacent pair at dx=0.95w must NOT collide — if it does, multiplier may have regressed to 0.5",
            collides(a, b),
        )
    }

    @Test
    fun `regression — multiplier 0_4 would falsely accept this near-overlap pair`() {
        // Defensive upper bound: if multiplier were ever DROPPED to 0.4 (over-
        // relaxation), enemies overlapping by ~20% would be accepted. We want
        // them rejected. Construction: dx = 0.85w. With multiplier 0.45 →
        // xLimit = 0.9w → 0.85w < 0.9w → collides (correct reject). With
        // multiplier 0.4 → xLimit = 0.8w → 0.85w > 0.8w → no collide (regression).
        val w = 40f
        val a = FP(0f, 0f, w, w)
        val b = FP(0.85f * w, 0f, w, w)
        assertTrue(
            "Near-overlap pair at dx=0.85w MUST collide — if it doesn't, multiplier may have under-relaxed to 0.4",
            collides(a, b),
        )
    }
}
