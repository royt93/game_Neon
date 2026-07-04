package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 10 (đợt 3) — Prestige Reset: hàm thuần (cost exponential, buff +4%/cấp,
 * gate đủ tiền + đủ rank).
 */
class PrestigeLogicTest {

    @Test
    fun `prestigeCost tăng exponential 1500 x 2 mũ level`() {
        assertEquals("level 0 → 1500◇", 1500, prestigeCost(0))
        assertEquals("level 1 → 3000◇", 3000, prestigeCost(1))
        assertEquals("level 2 → 6000◇", 6000, prestigeCost(2))
        assertEquals("level 3 → 12000◇", 12000, prestigeCost(3))
    }

    @Test
    fun `prestigeCost không tràn Int ở level cao`() {
        assertTrue("cost dương ở cap level", prestigeCost(50) > 0)
    }

    @Test
    fun `prestigeMultiplier cộng dồn 4 phần trăm mỗi cấp`() {
        assertEquals("chưa prestige = 1.0", 1.0f, prestigeMultiplier(0), 0.0001f)
        assertEquals("cấp 1 = 1.04", 1.04f, prestigeMultiplier(1), 0.0001f)
        assertEquals("cấp 5 = 1.20", 1.20f, prestigeMultiplier(5), 0.0001f)
    }

    @Test
    fun `prestigeMultiplier level âm coerce về 1_0`() {
        assertEquals(1.0f, prestigeMultiplier(-3), 0.0001f)
    }

    @Test
    fun `canPrestige cần đủ tiền VÀ đủ rank`() {
        assertTrue("đủ cả 2", canPrestige(balance = 1500, cost = 1500, totalSkillRanks = 8, minRanks = 8))
        assertFalse("thiếu tiền", canPrestige(balance = 1499, cost = 1500, totalSkillRanks = 8, minRanks = 8))
        assertFalse("thiếu rank", canPrestige(balance = 5000, cost = 1500, totalSkillRanks = 7, minRanks = 8))
    }
}
