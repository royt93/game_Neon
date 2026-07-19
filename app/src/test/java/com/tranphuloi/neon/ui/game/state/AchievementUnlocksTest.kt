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

    @Test
    fun `prestigeFirst true from level 1`() {
        assertFalse(AchievementUnlocks.prestigeFirst(0))
        assertTrue(AchievementUnlocks.prestigeFirst(1))
        assertTrue(AchievementUnlocks.prestigeFirst(5))
    }

    @Test
    fun `prestigeMaster needs level 5`() {
        assertFalse(AchievementUnlocks.prestigeMaster(4))
        assertTrue(AchievementUnlocks.prestigeMaster(5))
    }

    @Test
    fun `loadoutTinkerer needs five customized ships`() {
        assertFalse(AchievementUnlocks.loadoutTinkerer(4))
        assertTrue(AchievementUnlocks.loadoutTinkerer(5))
    }

    @Test
    fun `shipAllMaxLevel requires every owned ship maxed`() {
        assertFalse("rỗng → false", AchievementUnlocks.shipAllMaxLevel(emptyList(), emptyMap()))
        assertFalse(
            "1 tàu chưa max → false",
            AchievementUnlocks.shipAllMaxLevel(listOf("fighter", "tank"), mapOf("fighter" to 1500, "tank" to 300)),
        )
        assertTrue(
            "cả 2 tàu đều max → true",
            AchievementUnlocks.shipAllMaxLevel(listOf("fighter", "tank"), mapOf("fighter" to 1500, "tank" to 1500)),
        )
    }

    @Test
    fun `oneHandBossKill true only for left or right handed`() {
        assertFalse(AchievementUnlocks.oneHandBossKill(com.tranphuloi.neon.data.ControlHandMode.TWO_HANDED))
        assertTrue(AchievementUnlocks.oneHandBossKill(com.tranphuloi.neon.data.ControlHandMode.LEFT_HANDED))
        assertTrue(AchievementUnlocks.oneHandBossKill(com.tranphuloi.neon.data.ControlHandMode.RIGHT_HANDED))
    }

    @Test
    fun `dailyStreak7 needs seven day streak`() {
        assertFalse(AchievementUnlocks.dailyStreak7(6))
        assertTrue(AchievementUnlocks.dailyStreak7(7))
    }

    @Test
    fun `dailyStreak30 needs thirty day streak`() {
        assertFalse(AchievementUnlocks.dailyStreak30(29))
        assertTrue(AchievementUnlocks.dailyStreak30(30))
    }
}
