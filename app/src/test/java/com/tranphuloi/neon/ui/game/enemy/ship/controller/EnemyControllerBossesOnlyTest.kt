package com.tranphuloi.neon.ui.game.enemy.ship.controller

import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 23 — RunModifier.BOSSES_ONLY ("Chỉ boss"): khi bật, spawn enemy thường
 * phải bị bỏ qua hoàn toàn; spawn boss vẫn hoạt động bình thường.
 */
class EnemyControllerBossesOnlyTest {

    private val regular = RegularEnemyType(
        drawableId = 0,
        width = 40f,
        height = 40f,
        hp = 10f,
        impactPower = 1f,
        formation = Row(rowCount = 1),
        xOffsetSpeed = 0f,
        yOffsetSpeed = 5f,
        enemySpawnRate = Once,
    )

    private fun controller(bossesOnly: Boolean, onUpdate: (List<com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy>) -> Unit) = EnemyController(
        screenWidth = 400f,
        screenHeight = 800f,
        uuidUtils = UuidUtils(),
        getShip = { Ship(xOffset = 200f, yOffset = 700f) },
        initialEnemies = emptyList(),
        setEnemies = onUpdate,
        addMinerals = { _, _, _, _ -> },
        addExplosion = { _, _, _, _ -> },
        bossesOnly = { bossesOnly },
    )

    @Test
    fun `regular enemy spawn skipped when bossesOnly active`() {
        var latest = emptyList<com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy>()
        val ctl = controller(bossesOnly = true) { latest = it }
        ctl.addEnemy(regular)
        assertTrue("không được spawn enemy thường khi bossesOnly bật", latest.isEmpty())
    }

    @Test
    fun `boss spawn still works when bossesOnly active`() {
        var latest = emptyList<com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy>()
        val ctl = controller(bossesOnly = true) { latest = it }
        ctl.addEnemy(LevelOneBossType)
        assertEquals("boss vẫn phải spawn được", 1, latest.size)
    }

    @Test
    fun `regular enemy spawns normally when bossesOnly inactive`() {
        var latest = emptyList<com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy>()
        val ctl = controller(bossesOnly = false) { latest = it }
        ctl.addEnemy(regular)
        assertEquals("enemy thường vẫn spawn bình thường khi modifier tắt", 1, latest.size)
    }
}
