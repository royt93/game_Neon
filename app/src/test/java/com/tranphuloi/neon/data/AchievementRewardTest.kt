package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 22 (#4) — pin thưởng khoáng khi mở mới thành tựu [achievementReward]:
 * Đồng 30◇ · Bạc 60◇ · Vàng 120◇.
 */
class AchievementRewardTest {

    @Test
    fun `reward scales by tier`() {
        assertEquals(30, achievementReward(AchievementTier.BRONZE))
        assertEquals(60, achievementReward(AchievementTier.SILVER))
        assertEquals(120, achievementReward(AchievementTier.GOLD))
    }

    @Test
    fun `every achievement tier maps to a positive reward`() {
        Achievement.entries.forEach {
            assertTrue(
                "${it.id} (${it.tier}) phải có thưởng > 0",
                achievementReward(it.tier) > 0,
            )
        }
    }

    @Test
    fun `higher tier never rewards less`() {
        assertTrue(achievementReward(AchievementTier.SILVER) > achievementReward(AchievementTier.BRONZE))
        assertTrue(achievementReward(AchievementTier.GOLD) > achievementReward(AchievementTier.SILVER))
    }
}
