package com.tranphuloi.neon.ui.game.enemy.laser

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.utils.Logger
import java.util.*

class EnemyLasersController(
    private val screenHeight: Float,
    initialEnemyLasers: List<Laser>,
    private val setEnemyLasers: (List<Laser>) -> Unit,
) {

    init {
        Logger.d("EnemyLasersController init: initialLasers=${initialEnemyLasers.size}")
    }

    var enemyLasers: List<Laser> = initialEnemyLasers
        private set

    val fireEnemyLaserId = UUID.randomUUID().toString()
    val fireEnemyLaserRepeatTime = Millis(1000)
    fun fireEnemyLasers(enemies: List<Enemy>) {
        if (enemies.isEmpty()) return
        val enemy = enemies.random()
        val generatedLasers = enemy.generateLasers()
        enemyLasers = enemyLasers + generatedLasers
        Logger.d("EnemyLasersController.fireEnemyLasers: enemy=${enemy.enemyId.take(6)} fired ${generatedLasers.size} laser(s) (active=${enemyLasers.size})")
        updateShipLasers()
    }

    val processLasersId = UUID.randomUUID().toString()
    val processLasersRepeatTime = Millis(5)
    fun processLasers() {
        val before = enemyLasers.size
        enemyLasers.forEach {
            it.moveLaser()
            if (it.yOffset > screenHeight || it.destroyed) destroyEnemyLaser(it)
        }
        val removed = before - enemyLasers.size
        if (removed > 0) {
            Logger.d("EnemyLasersController.processLasers: removed $removed (off-screen/destroyed), active=${enemyLasers.size}")
        }
        updateShipLasers()
    }

    fun hasEnemyLasers() = enemyLasers.isNotEmpty()

    private fun destroyEnemyLaser(laser: Laser) {
        enemyLasers = enemyLasers - laser
    }

    private fun updateShipLasers() {
        setEnemyLasers(enemyLasers)
    }
}
