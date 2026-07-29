package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.TestEnemy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusChainTest {

    @Test
    fun `enemiesInChainRadius includes enemies within radius`() {
        val origin = TestEnemy("origin").apply { xOffset = 0f; yOffset = 0f }
        val near = TestEnemy("near").apply { xOffset = 50f; yOffset = 0f }
        val enemies = listOf(origin, near)
        val result = enemiesInChainRadius(enemies, originX = 25f, originY = 25f, excludeEnemyId = "origin")
        assertEquals(
            "enemies within radius are included / địch trong bán kính được tính",
            listOf("near"),
            result.map { it.enemyId },
        )
    }

    @Test
    fun `enemiesInChainRadius excludes enemies outside radius`() {
        val far = TestEnemy("far").apply { xOffset = 1000f; yOffset = 1000f }
        val result = enemiesInChainRadius(listOf(far), originX = 0f, originY = 0f, excludeEnemyId = "none")
        assertTrue(
            "enemies outside radius are excluded / địch ngoài bán kính bị loại",
            result.isEmpty(),
        )
    }

    @Test
    fun `enemiesInChainRadius always excludes the origin enemy itself`() {
        val origin = TestEnemy("origin").apply { xOffset = 0f; yOffset = 0f }
        val result = enemiesInChainRadius(listOf(origin), originX = 0f, originY = 0f, excludeEnemyId = "origin")
        assertTrue(
            "origin enemy is always excluded / luôn loại trừ chính địch gốc",
            result.isEmpty(),
        )
    }

    @Test
    fun `enemiesInChainRadius respects custom radius`() {
        val edge = TestEnemy("edge").apply { xOffset = 100f; yOffset = 0f }
        val enemies = listOf(edge)
        assertTrue(
            "custom radius is respected (large radius includes) / tôn trọng bán kính tuỳ chỉnh (bán kính lớn thì bao gồm)",
            enemiesInChainRadius(enemies, 0f, 0f, "none", radius = 200f).isNotEmpty(),
        )
        assertTrue(
            "custom radius is respected (small radius excludes) / tôn trọng bán kính tuỳ chỉnh (bán kính nhỏ thì loại trừ)",
            enemiesInChainRadius(enemies, 0f, 0f, "none", radius = 10f).isEmpty(),
        )
    }
}
