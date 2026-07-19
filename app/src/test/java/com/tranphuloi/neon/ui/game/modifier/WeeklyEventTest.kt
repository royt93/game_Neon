package com.tranphuloi.neon.ui.game.modifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 29 — [WeeklyEvent.modifierFor] deterministic theo tuần, không bao giờ
 * NONE, và quay vòng qua pool. JUnit4, no-mock.
 */
class WeeklyEventTest {

    @Test
    fun `same week key yields same modifier`() {
        assertEquals(WeeklyEvent.modifierFor(2948L), WeeklyEvent.modifierFor(2948L))
    }

    @Test
    fun `never returns NONE`() {
        for (week in 0L..100L) {
            assertNotEquals("week=$week không được ra NONE", RunModifier.NONE, WeeklyEvent.modifierFor(week))
        }
    }

    @Test
    fun `cycles through the whole pool across consecutive weeks`() {
        val poolSize = RunModifier.entries.count { it != RunModifier.NONE }
        val seen = (0 until poolSize).map { WeeklyEvent.modifierFor(it.toLong()) }.toSet()
        assertEquals("mỗi tuần trong 1 chu kỳ ra 1 modifier khác nhau", poolSize, seen.size)
    }

    @Test
    fun `wraps around after a full cycle`() {
        val poolSize = RunModifier.entries.count { it != RunModifier.NONE }
        assertEquals(
            "week 0 và week poolSize cùng modifier (quay vòng)",
            WeeklyEvent.modifierFor(0L), WeeklyEvent.modifierFor(poolSize.toLong()),
        )
    }

    @Test
    fun `handles negative week key safely`() {
        // floorMod → không crash, ra modifier hợp lệ (không NONE).
        val m = WeeklyEvent.modifierFor(-3L)
        assertTrue("âm vẫn ra modifier hợp lệ", m != RunModifier.NONE)
    }
}
