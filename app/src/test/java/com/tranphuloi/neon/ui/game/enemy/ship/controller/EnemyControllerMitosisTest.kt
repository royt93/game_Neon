package com.tranphuloi.neon.ui.game.enemy.ship.controller

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 27 — Amip Vũ Trụ mitosis-on-death: chết sinh 2 con nhỏ hơn
 * (isMitosisChild=true), con KHÔNG tách tiếp khi chết, enemy khác
 * (splitsOnDeath=false) không bị ảnh hưởng.
 */
class EnemyControllerMitosisTest {

    private val splittingType = RegularEnemyType(
        drawableId = 0,
        width = 40f,
        height = 40f,
        hp = 100f,
        impactPower = 1f,
        formation = Row(rowCount = 1),
        xOffsetSpeed = 0f,
        yOffsetSpeed = 5f,
        enemySpawnRate = Once,
        splitsOnDeath = true,
    )

    private val nonSplittingType = splittingType.copy(splitsOnDeath = false)

    private fun controller(
        initialEnemies: List<Enemy>,
        onUpdate: (List<Enemy>) -> Unit,
    ) = EnemyController(
        screenWidth = 400f,
        screenHeight = 800f,
        uuidUtils = UuidUtils(),
        getShip = { Ship(xOffset = 200f, yOffset = 700f) },
        initialEnemies = initialEnemies,
        setEnemies = onUpdate,
        addMinerals = { _, _, _, _ -> },
        addExplosion = { _, _, _, _ -> },
    )

    @Test
    fun `parent enemy with splitsOnDeath spawns 2 mitosis children on death`() {
        val parent = RegularEnemy(
            screenWidth = 400f,
            screenHeight = 800f,
            xOffset = 100f,
            type = splittingType,
            sizeScale = 1f,
            hp = 0f,
        )
        var lastEnemies: List<Enemy> = emptyList()
        val controller = controller(listOf(parent)) { lastEnemies = it }

        controller.processEnemies()

        assertEquals("chết cha → đúng 2 con", 2, lastEnemies.size)
        val children = lastEnemies.filterIsInstance<RegularEnemy>()
        assertTrue("cả 2 con phải đánh dấu isMitosisChild", children.all { it.isMitosisChild })
        children.forEach {
            assertEquals("con dùng lại type của cha", splittingType, it.type)
            assertEquals("sizeScale con = 40% cha", parent.sizeScale * 0.4f, it.sizeScale, 0.0001f)
            assertTrue("con còn sống (hp > 0)", it.hp > 0f)
        }
    }

    @Test
    fun `mitosis child does not split again on death`() {
        val child = RegularEnemy(
            screenWidth = 400f,
            screenHeight = 800f,
            xOffset = 100f,
            type = splittingType,
            sizeScale = 0.4f,
            hp = 0f,
            isMitosisChild = true,
        )
        var lastEnemies: List<Enemy> = emptyList()
        val controller = controller(listOf(child)) { lastEnemies = it }

        controller.processEnemies()

        assertTrue("con chết không tách tiếp — list rỗng", lastEnemies.isEmpty())
    }

    @Test
    fun `enemy without splitsOnDeath is removed normally without spawning children`() {
        val enemy = RegularEnemy(
            screenWidth = 400f,
            screenHeight = 800f,
            xOffset = 100f,
            type = nonSplittingType,
            sizeScale = 1f,
            hp = 0f,
        )
        var lastEnemies: List<Enemy> = emptyList()
        val controller = controller(listOf(enemy)) { lastEnemies = it }

        controller.processEnemies()

        assertFalse("splitsOnDeath=false không tách", nonSplittingType.splitsOnDeath)
        assertTrue("enemy thường chết bình thường, không sinh con", lastEnemies.isEmpty())
    }
}
