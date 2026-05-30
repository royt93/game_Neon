package com.tranphuloi.neon.ui.game.stage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d audit-5 P2 — pin the per-stage xAnchorShift formula contract.
 *
 * Formula: `((chapter.id * 12 + gameStage) * 37 % 121 - 60).toFloat()`
 *
 * Contract:
 *   1. Range bounded [-60, +60] dp.
 *   2. Deterministic per (chapter, stage) so save/load preserves the pattern.
 *   3. Well-distributed (no narrow cycle / clustering) across realistic ranges.
 *   4. Coprime constants (37 and 121) ensure no degenerate period inside the
 *      expected gameStage range 1..12 across chapters 1..5.
 */
class StageAnchorShiftTest {

    private fun anchorShift(chapter: Int, gameStage: Int): Float =
        ((chapter * 12 + gameStage) * 37 % 121 - 60).toFloat()

    @Test
    fun `shift values stay in -60 to +60 range`() {
        for (chapter in 1..5) {
            for (stage in 1..12) {
                val shift = anchorShift(chapter, stage)
                assertTrue(
                    "chapter=$chapter stage=$stage shift=$shift out of range [-60, +60]",
                    shift in -60f..60f,
                )
            }
        }
    }

    @Test
    fun `shift is deterministic for a given chapter+stage`() {
        // Same input → same output. Critical for save/load round-trip
        // because Stage.kt re-runs buildGameStage on checkpoint restore.
        for (chapter in 1..3) {
            for (stage in 1..12) {
                val a = anchorShift(chapter, stage)
                val b = anchorShift(chapter, stage)
                assertEquals("Non-deterministic at ch=$chapter st=$stage", a, b, 0f)
            }
        }
    }

    @Test
    fun `chapters 1-5 stages 1-20 produce well-distributed shifts`() {
        // 100 inputs → expect at least 60 unique values out of 121-possible.
        // (Audit verified empirically: 68/100 unique.)
        val shifts = mutableSetOf<Float>()
        for (chapter in 1..5) {
            for (stage in 1..20) {
                shifts.add(anchorShift(chapter, stage))
            }
        }
        assertTrue(
            "Only ${shifts.size}/100 unique shifts — formula may have degenerate cycle",
            shifts.size >= 60,
        )
    }

    @Test
    fun `adjacent stages produce different shifts (no consecutive duplicates)`() {
        // If `(stage1 - stage2)` lands on a multiple of 121/gcd(37, 121) = 121,
        // shifts could collide. We expect adjacent stages within a chapter to
        // always differ — 37 is coprime with 121 so this holds.
        for (chapter in 1..3) {
            for (stage in 1..11) {
                val a = anchorShift(chapter, stage)
                val b = anchorShift(chapter, stage + 1)
                assertTrue(
                    "ch=$chapter st=$stage and st=${stage + 1} both → $a (consecutive dup)",
                    a != b,
                )
            }
        }
    }

    @Test
    fun `chapter advance reseeds shift (chapter 1 vs chapter 2 differ)`() {
        // Same stage across chapters should produce different shifts —
        // otherwise endless mode looping chapter 1 forever would feel
        // identical formation-by-formation.
        for (stage in 1..12) {
            val ch1 = anchorShift(1, stage)
            val ch2 = anchorShift(2, stage)
            assertTrue(
                "chapter 1 vs 2 stage=$stage both → $ch1 (chapter not influencing seed)",
                ch1 != ch2,
            )
        }
    }
}
