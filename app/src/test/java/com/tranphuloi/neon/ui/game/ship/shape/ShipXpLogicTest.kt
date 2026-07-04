package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 03 — hành vi [ShipXpLevels]: bảng level theo ngưỡng, bonus HP luỹ tiến,
 * XP-còn-thiếu, tiến độ, và XP mỗi run. JUnit4, no-mock (đúng convention).
 */
class ShipXpLogicTest {

    @Test
    fun `levelForXp maps thresholds correctly`() {
        assertEquals("0 XP → L1", 1, ShipXpLevels.levelForXp(0))
        assertEquals("99 → L1", 1, ShipXpLevels.levelForXp(99))
        assertEquals("100 → L2", 2, ShipXpLevels.levelForXp(100))
        assertEquals("299 → L2", 2, ShipXpLevels.levelForXp(299))
        assertEquals("300 → L3", 3, ShipXpLevels.levelForXp(300))
        assertEquals("699 → L3", 3, ShipXpLevels.levelForXp(699))
        assertEquals("700 → L4", 4, ShipXpLevels.levelForXp(700))
        assertEquals("1499 → L4", 4, ShipXpLevels.levelForXp(1499))
        assertEquals("1500 → L5", 5, ShipXpLevels.levelForXp(1500))
        assertEquals("9999 → cap L5", 5, ShipXpLevels.levelForXp(9999))
    }

    @Test
    fun `levelForXp never goes below 1 or above max`() {
        assertEquals("âm → L1", 1, ShipXpLevels.levelForXp(-50))
        assertEquals("cực lớn → L5", ShipXpLevels.MAX_LEVEL, ShipXpLevels.levelForXp(Int.MAX_VALUE))
    }

    @Test
    fun `hpBonusMul increases monotonically and caps at level 5`() {
        assertEquals("L1 = 1.0", 1.0f, ShipXpLevels.hpBonusMulForLevel(1), 0.0001f)
        assertEquals("L5 = 1.08", 1.08f, ShipXpLevels.hpBonusMulForLevel(5), 0.0001f)
        var prev = 0f
        for (lvl in 1..ShipXpLevels.MAX_LEVEL) {
            val m = ShipXpLevels.hpBonusMulForLevel(lvl)
            assertTrue("bonus tăng dần theo level", m > prev)
            prev = m
        }
        assertEquals("level vượt max → kẹp L5", ShipXpLevels.hpBonusMulForLevel(99), ShipXpLevels.hpBonusMulForLevel(5), 0.0001f)
    }

    @Test
    fun `hpBonusMulForXp matches level bonus`() {
        assertEquals(ShipXpLevels.hpBonusMulForLevel(1), ShipXpLevels.hpBonusMulForXp(0), 0.0001f)
        assertEquals(ShipXpLevels.hpBonusMulForLevel(3), ShipXpLevels.hpBonusMulForXp(300), 0.0001f)
        assertEquals(ShipXpLevels.hpBonusMulForLevel(5), ShipXpLevels.hpBonusMulForXp(2000), 0.0001f)
    }

    @Test
    fun `xpToNextLevel counts remaining and is zero at max`() {
        assertEquals("0 XP cần 100 để lên L2", 100, ShipXpLevels.xpToNextLevel(0))
        assertEquals("40 XP cần 60 nữa", 60, ShipXpLevels.xpToNextLevel(40))
        assertEquals("100 (L2) cần 200 để lên L3", 200, ShipXpLevels.xpToNextLevel(100))
        assertEquals("đã max → 0", 0, ShipXpLevels.xpToNextLevel(1500))
        assertEquals("trên max → 0", 0, ShipXpLevels.xpToNextLevel(5000))
    }

    @Test
    fun `progressInLevel stays within 0 to 1 and is full at max`() {
        assertEquals("đầu L1 = 0", 0f, ShipXpLevels.progressInLevel(0), 0.0001f)
        assertEquals("giữa L1 (50/100)", 0.5f, ShipXpLevels.progressInLevel(50), 0.0001f)
        assertEquals("max level → 1", 1f, ShipXpLevels.progressInLevel(1500), 0.0001f)
        for (xp in intArrayOf(0, 1, 99, 100, 500, 1499, 1500, 9999)) {
            val p = ShipXpLevels.progressInLevel(xp)
            assertTrue("progress trong [0,1] cho xp=$xp", p in 0f..1f)
        }
    }

    @Test
    fun `xpForRun sums enemies plus boss bonus`() {
        assertEquals("20 địch + 0 boss", 20, ShipXpLevels.xpForRun(20, 0))
        assertEquals("20 địch + 2 boss = 20+20", 40, ShipXpLevels.xpForRun(20, 2))
        assertEquals("âm → kẹp 0", 0, ShipXpLevels.xpForRun(-5, -1))
    }
}
