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
    /**
     * Round 34 (41x) — STUN status effect check. If true, skip fire-laser for
     * the picked enemy this tick. Default = false → no stun gating.
     */
    private val isEnemyStunned: (enemyId: String) -> Boolean = { false },
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
        // Round 47 — cap in-flight enemy lasers. Bosses can spawn 3 lasers per
        // fire (multi-shot) so 30 is ~10 boss volleys' worth still on-screen
        // before throttle kicks in. Off-screen scroll clears the list naturally.
        if (enemyLasers.size >= MAX_ENEMY_LASERS) return
        val enemy = enemies.random()
        // Round 34 (41x) — skip fire if enemy STUNNED.
        if (isEnemyStunned(enemy.enemyId)) {
            Logger.v { "EnemyLasersController.fireEnemyLasers: enemy=${enemy.enemyId.take(6)} STUNNED — skip fire" }
            return
        }
        val generatedLasers = enemy.generateLasers()
        enemyLasers = enemyLasers + generatedLasers
        Logger.v { "EnemyLasersController.fireEnemyLasers: enemy=${enemy.enemyId.take(6)} fired ${generatedLasers.size} laser(s) (active=${enemyLasers.size})" }
        updateShipLasers()
    }

    companion object {
        /** Round 47 — max in-flight enemy lasers. Above this, new fire is dropped. */
        const val MAX_ENEMY_LASERS = 30
    }

    val processLasersId = UUID.randomUUID().toString()
    val processLasersRepeatTime = Millis(5)
    fun processLasers() {
        // Round 37 — was logging "removed N off-screen/destroyed" every 5ms tick.
        // Enemy lasers fall off the bottom edge constantly; this fired 5-15×/sec
        // during normal play. Per-collision events log impacts elsewhere.
        enemyLasers.forEach {
            it.moveLaser()
            if (it.yOffset > screenHeight || it.destroyed) destroyEnemyLaser(it)
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
