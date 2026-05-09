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
        val newEnemies = enemyFactory(type = type, getShip = getShip)
        this.enemies += newEnemies
        Logger.d("EnemyController.addEnemy: type=${type::class.simpleName} spawned ${newEnemies.size} (active=${this.enemies.size})")
        updateEnemies()
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
        if (leftScreen > 0) {
            Logger.d("EnemyController.processEnemies: $leftScreen enemies left screen (active=${enemies.size})")
        }
        updateEnemies()
    }

    fun hasEnemies() = enemies.isNotEmpty()

    private fun updateEnemies() {
        setEnemies(enemies)
    }
}
