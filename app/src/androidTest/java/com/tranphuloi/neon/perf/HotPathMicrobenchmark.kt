package com.tranphuloi.neon.perf

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.explosion.controller.ExplosionController
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.ui.game.laser.LightningChain
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.status.StatusEffect
import com.tranphuloi.neon.ui.game.status.StatusEffectController
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 20 — real-device-CPU counterpart to [com.tranphuloi.neon.perf.HotPathPerfTest]'s
 * JVM iteration-budget proxy. Same target functions, one-to-one, so each has
 * a direct JVM-timing vs real-hardware comparison point. `measureRepeated`
 * handles warmup/iteration itself — no manual `repeat(N)` loop.
 */
@RunWith(AndroidJUnit4::class)
class HotPathMicrobenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun pathPoolAcquireRelease() {
        PathPool.clearForTest()
        benchmarkRule.measureRepeated {
            val p = PathPool.acquire()
            PathPool.release(p)
        }
    }

    @Test
    fun statusEffectControllerApply() {
        val controller = StatusEffectController()
        val enemyIds = (0 until 50).map { "enemy-$it" }
        var i = 0
        benchmarkRule.measureRepeated {
            val enemyId = enemyIds[i % enemyIds.size]
            val type = if (i % 2 == 0) StatusEffect.BURN else StatusEffect.SLOW
            controller.apply(enemyId = enemyId, type = type, nowMillis = i.toLong())
            i++
        }
    }

    @Test
    fun lightningChainComputeChainTargets() {
        val enemies = (0 until 30).map { i ->
            perfEnemy("e$i", x = (i % 6) * 60f, y = (i / 6) * 120f)
        }
        benchmarkRule.measureRepeated {
            LightningChain.computeChainTargets(startX = 180f, startY = 400f, enemies = enemies)
        }
    }

    @Test
    fun enemyControllerProcessEnemies() {
        val type = RegularEnemyType(
            drawableId = 0,
            width = 40f,
            height = 40f,
            hp = 100f,
            impactPower = 1f,
            formation = Row(rowCount = 1),
            xOffsetSpeed = 0f,
            yOffsetSpeed = 5f,
            enemySpawnRate = Once,
        )
        val initialEnemies = (0 until 30).map { i ->
            RegularEnemy(
                screenWidth = 400f,
                screenHeight = 800f,
                xOffset = (i % 6) * 60f,
                type = type,
                initialYOffset = (i / 6) * 120f,
                hp = if (i % 5 < 2) 0f else 100f,
            )
        }
        val controller = EnemyController(
            screenWidth = 400f,
            screenHeight = 800f,
            uuidUtils = UuidUtils(),
            getShip = { Ship(xOffset = 200f, yOffset = 700f) },
            initialEnemies = initialEnemies,
            setEnemies = {},
            addMinerals = { _, _, _, _ -> },
            addExplosion = { _, _, _, _ -> },
        )
        benchmarkRule.measureRepeated {
            controller.processEnemies()
        }
    }

    @Test
    fun explosionControllerProcessExplosions() {
        val initialExplosions = (0 until 8).map { i ->
            Explosion(xOffset = i * 40f, yOffset = i * 30f, size = 80f)
        }
        val controller = ExplosionController(
            initialExplosions = initialExplosions,
            updateExplosions = {},
        )
        benchmarkRule.measureRepeated {
            controller.processExplosions()
        }
    }

    private fun perfEnemy(id: String, x: Float, y: Float): com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy =
        object : com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy {
            override val enemyId = id
            override val width = 40f
            override val height = 40f
            override var xOffset = x
            override var yOffset = y
            override var hp = 100f
            override val initialHp = 100f
            override val impactPower = 10f
            override val drawableId = 0
            override val minerals = 1
            override val destroyed = false
            override val outOfScreen = false
            override var lastImpactMillis = 0L
            override val isBoss = false
            override val displayName = "perf"
            override fun enemyRect() =
                androidx.compose.ui.geometry.Rect(xOffset, yOffset, xOffset + width, yOffset + height)
            override fun process() {}
            override fun generateLasers(): List<com.tranphuloi.neon.ui.game.laser.Laser> = emptyList()
            override fun onObjectImpact(impactPower: Float) {}
        }
}
