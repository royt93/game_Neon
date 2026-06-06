package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wave 21 (#4) — pin thưởng điểm danh hằng ngày [dailyRewardFor].
 * Base 50◇, +25◇/ngày liên tiếp, trần ở ngày 7 (200◇).
 */
class DailyRewardTest {

    @Test
    fun `day 1 grants base 50`() {
        assertEquals(50, dailyRewardFor(1))
    }

    @Test
    fun `reward grows 25 per consecutive day`() {
        assertEquals(75, dailyRewardFor(2))
        assertEquals(100, dailyRewardFor(3))
        assertEquals(125, dailyRewardFor(4))
        assertEquals(150, dailyRewardFor(5))
        assertEquals(175, dailyRewardFor(6))
    }

    @Test
    fun `caps at day 7 = 200 and stays there`() {
        assertEquals(200, dailyRewardFor(7))
        assertEquals("trần ở 200 dù streak rất dài", 200, dailyRewardFor(30))
    }

    @Test
    fun `streak below 1 is treated as day 1 (coerced)`() {
        assertEquals(50, dailyRewardFor(0))
        assertEquals(50, dailyRewardFor(-5))
    }

    // ── Wave 22 (#4) — thưởng mốc màn ──

    @Test
    fun `stage milestone grants 30 per 5 stages`() {
        assertEquals(0, stageMilestoneBonus(0))
        assertEquals(0, stageMilestoneBonus(4))
        assertEquals(30, stageMilestoneBonus(5))
        assertEquals(30, stageMilestoneBonus(9))
        assertEquals(60, stageMilestoneBonus(10))
        assertEquals(180, stageMilestoneBonus(33))
    }

    @Test
    fun `stage milestone caps at 300 (stage 50+)`() {
        assertEquals(300, stageMilestoneBonus(50))
        assertEquals(300, stageMilestoneBonus(999))
    }

    @Test
    fun `stage milestone handles negative gracefully`() {
        assertEquals(0, stageMilestoneBonus(-3))
    }
}
