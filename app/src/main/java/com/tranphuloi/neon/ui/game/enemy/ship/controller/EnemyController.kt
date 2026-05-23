package com.tranphuloi.neon.ui.game.enemy.ship.controller

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.factory.EnemyFactory
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils

class EnemyController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    uuidUtils: UuidUtils,
    private val enemyFactory: EnemyFactory = EnemyFactory(screenWidth, screenHeight),
    private val getShip: () -> Ship,
    initialEnemies: List<Enemy> = emptyList(),
    private val setEnemies: (List<Enemy>) -> Unit,
    private val addMinerals: (xOffset: Float, yOffset: Float, width: Float, mineralAmount: Int) -> Unit,
    private val addExplosion: (xOffset: Float, yOffset: Float, width: Float, height: Float) -> Unit,
    private val onEnemyKilled: (Enemy) -> Unit = {},
) {

    init {
        Logger.d("EnemyController init: initialEnemies=${initialEnemies.size}, screen=${screenWidth}x${screenHeight}")
    }

    private var enemies: List<Enemy> = initialEnemies

    val addEnemyId = uuidUtils.getUuid()
    fun addEnemy(type: EnemyType) {
        // Round 47 — entity cap (perf fix). At peak (chapter 2 NEBULA_FOG) the
        // enemies list hit 53 in user repro logs → 53 EnemyUI allocations per
        // tick from the mapper @ ~125Hz → GC pressure. Bosses bypass the cap
        // (always spawn) so boss waves can never be skipped due to swarm
        // overflow.
        val isBossSpawn = type is com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
        if (!isBossSpawn && enemies.size >= MAX_REGULAR_ENEMIES) {
            // Round 47 + audit fix — must be Logger.v: stage script attempts a
            // spawn every 200-1000ms, so when the cap holds we'd otherwise
            // pump 2-5 Logger.d lines/sec right back into logcat.
            Logger.v { "EnemyController.addEnemy: SKIPPED (cap=$MAX_REGULAR_ENEMIES reached, current=${enemies.size})" }
            return
        }
        val newEnemies = enemyFactory(type = type, getShip = getShip)
        // Round 55 — formation cap leak fix. EnemyFactory returns 1..N enemies
        // (Row=rowCount, VFormation=count, ZigZag=1). Previous check only
        // gated when ALREADY at cap; a Row of 5 spawning at enemies.size=29
        // produced 34 active, breaking the cap by up to formation size. User
        // log showed 32 enemies in chapter 1 with no boss → SmartBomb cleared
        // 32. Trim formation tail to remaining slots so cap is strictly
        // enforced. Bosses still bypass entirely.
        val toAdd = if (isBossSpawn) {
            newEnemies
        } else {
            val remainingSlots = (MAX_REGULAR_ENEMIES - enemies.size).coerceAtLeast(0)
            val clamped = newEnemies.take(remainingSlots)
            if (clamped.size < newEnemies.size) {
                Logger.v {
                    "EnemyController.addEnemy: TRIMMED formation ${newEnemies.size}→${clamped.size} " +
                        "(cap=$MAX_REGULAR_ENEMIES, remaining=$remainingSlots)"
                }
            }
            clamped
        }
        this.enemies += toAdd
        Logger.v { "EnemyController.addEnemy: type=${type::class.simpleName} spawned ${toAdd.size} (active=${this.enemies.size})" }
        updateEnemies()
    }

    companion object {
        /** Round 47 — soft cap for non-boss enemies. Bosses bypass this gate. */
        const val MAX_REGULAR_ENEMIES = 30
    }

    val processEnemiesId = uuidUtils.getUuid()
    val processEnemiesRepeatTime = Millis(5)
    fun processEnemies() {
        var leftScreen = 0
        enemies.forEach {
            it.process()
            if (it.destroyed) {
                enemies -= it
                addMinerals(
                    it.xOffset,
                    it.yOffset + it.height / 2,
                    it.width,
                    it.minerals
                )
                addExplosion(
                    it.xOffset + it.width / 2,
                    it.yOffset + it.height / 2,
                    it.width,
                    it.height
                )
                onEnemyKilled(it)
            } else if (it.outOfScreen) {
                enemies -= it
                leftScreen++
            }
        }
        // Round 37 — was logging "$leftScreen enemies left screen" per 5ms tick.
        // Mid-wave that fired multiple times per second. Active enemy count is
        // available via UI; left-screen events aren't actionable signal.
        updateEnemies()
    }

    fun hasEnemies() = enemies.isNotEmpty()

    private fun updateEnemies() {
        setEnemies(enemies)
    }
}
