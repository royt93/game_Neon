package com.tranphuloi.neon.ui.game.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 07 — điều kiện mở thành tựu (AchievementUnlocks). Pure → test biên chính xác.
 */
class AchievementUnlocksTest {

    @Test
    fun `droneDuo needs at least two drones`() {
        assertFalse(AchievementUnlocks.droneDuo(0))
        assertFalse(AchievementUnlocks.droneDuo(1))
        assertTrue(AchievementUnlocks.droneDuo(2))
    }

    @Test
    fun `chainTriple needs at least three hops`() {
        assertFalse(AchievementUnlocks.chainTriple(2))
        assertTrue(AchievementUnlocks.chainTriple(3))
        assertTrue(AchievementUnlocks.chainTriple(4))
    }

    @Test
    fun `lightningMaster needs 100 kills`() {
        assertFalse(AchievementUnlocks.lightningMaster(99))
        assertTrue(AchievementUnlocks.lightningMaster(100))
        assertTrue(AchievementUnlocks.lightningMaster(500))
    }

    @Test
    fun `shipMaxLevel true only when some ship hits max level`() {
        assertFalse("rỗng → false", AchievementUnlocks.shipMaxLevel(emptyMap()))
        assertFalse("chưa max → false", AchievementUnlocks.shipMaxLevel(mapOf("fighter" to 1499, "bomber" to 100)))
        assertTrue("1 tàu ≥1500 (Lv5) → true", AchievementUnlocks.shipMaxLevel(mapOf("fighter" to 300, "tank" to 1500)))
    }

    @Test
    fun `shipCollector needs ten owned ships`() {
        assertFalse(AchievementUnlocks.shipCollector(9))
        assertTrue(AchievementUnlocks.shipCollector(10))
        assertTrue(AchievementUnlocks.shipCollector(21))
    }
}
