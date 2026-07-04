package com.tranphuloi.neon.ui.game.modifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 08 — [DailyChallenge.modifierFor] deterministic theo ngày, không bao giờ
 * NONE, và quay vòng qua pool. JUnit4, no-mock.
 */
class DailyChallengeTest {

    @Test
    fun `same day key yields same modifier`() {
        assertEquals(DailyChallenge.modifierFor(20638L), DailyChallenge.modifierFor(20638L))
    }

    @Test
    fun `never returns NONE`() {
        for (day in 0L..100L) {
            assertNotEquals("day=$day không được ra NONE", RunModifier.NONE, DailyChallenge.modifierFor(day))
        }
    }

    @Test
    fun `cycles through the whole pool across consecutive days`() {
        val poolSize = RunModifier.entries.count { it != RunModifier.NONE }
        val seen = (0 until poolSize).map { DailyChallenge.modifierFor(it.toLong()) }.toSet()
        assertEquals("mỗi ngày trong 1 chu kỳ ra 1 modifier khác nhau", poolSize, seen.size)
    }

    @Test
    fun `wraps around after a full cycle`() {
        val poolSize = RunModifier.entries.count { it != RunModifier.NONE }
        assertEquals(
            "day 0 và day poolSize cùng modifier (quay vòng)",
            DailyChallenge.modifierFor(0L), DailyChallenge.modifierFor(poolSize.toLong()),
        )
    }

    @Test
    fun `handles negative day key safely`() {
        // floorMod → không crash, ra modifier hợp lệ (không NONE).
        val m = DailyChallenge.modifierFor(-3L)
        assertTrue("âm vẫn ra modifier hợp lệ", m != RunModifier.NONE)
    }
}
