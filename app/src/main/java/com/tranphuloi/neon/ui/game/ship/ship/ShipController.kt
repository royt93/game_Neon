package com.tranphuloi.neon.ui.game.ship.ship

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.booster.Booster
import com.tranphuloi.neon.ui.game.booster.BoosterType
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObject
import com.tranphuloi.neon.utils.Logger
import java.util.*

class ShipController(
    private val screenWidth: Float,
    screenHeight: Float,
    private var ship: Ship,
    private val setShip: (Ship) -> Unit,
    private val onShipDestroyed: () -> Unit = {},
    private val onShipDamaged: () -> Unit = {},
) {

    private val spaceShipCollidePower: Float = 100f
    private val movementSpeed: Float = 2f
    private val maxYOffset: Float = screenHeight - 140

    var movingLeft = false
    var movingRight = false

    val moveShipId = UUID.randomUUID().toString()
    val moveShipRepeatTime = Millis(3)

    fun moveShip() {
        if (ship.yOffset > maxYOffset) {
            updateYOffset(ship.yOffset - movementSpeed)
        }
        if (movingLeft && ship.xOffset >= 0 - ship.width / 4) {
            updateXOffset(ship.xOffset - movementSpeed)
        } else movingLeft = false
        if (movingRight && ship.xOffset <= screenWidth - ship.width / 1.5) {
            updateXOffset(ship.xOffset + movementSpeed)
        } else movingRight = false
    }

    private var shieldBoosterStartMillis: Long = 0
    private val shieldBoosterTimeMillis: Long = 10000
    private var shieldEndDurationMillis: Long = 0
    private fun enableShield(enable: Boolean) {
        if (ship.shieldEnabled != enable) {
            Logger.d("Booster: shield ${if (enable) "ON (+${shieldBoosterTimeMillis}ms)" else "OFF"}")
        }
        updateShieldEnabled(enable)
        if (enable) {
            shieldBoosterStartMillis = System.currentTimeMillis()
            shieldEndDurationMillis = shieldBoosterStartMillis + shieldBoosterTimeMillis
        }
    }

    private var laserBoosterStartMillis: Long = 0
    private val laserBoosterTimeMillis: Long = 15000
    private var laserBoosterEndDurationMillis: Long = 0
    private fun enableLaserBooster(enable: Boolean) {
        if (ship.laserBoosterEnabled != enable) {
            Logger.d("Booster: laser ${if (enable) "ON (+${laserBoosterTimeMillis}ms)" else "OFF"}")
        }
        updateLaserBoosterEnabled(enable)
        if (enable) {
            laserBoosterStartMillis = System.currentTimeMillis()
            laserBoosterEndDurationMillis = laserBoosterStartMillis + laserBoosterTimeMillis
        }
    }

    private var tripleLaserBoosterStartMillis: Long = 0
    private val tripleLaserBoosterTimeMillis: Long = 20000
    private var tripleLaserBoosterEndDurationMillis: Long = 0
    private fun enableTripleLaserBooster(enable: Boolean) {
        if (ship.tripleLaserBoosterEnabled != enable) {
            Logger.d("Booster: triple-laser ${if (enable) "ON (+${tripleLaserBoosterTimeMillis}ms)" else "OFF"}")
        }
        updateTripleLaserBoosterEnabled(enable)
        if (enable) {
            tripleLaserBoosterStartMillis = System.currentTimeMillis()
            tripleLaserBoosterEndDurationMillis =
                tripleLaserBoosterStartMillis + tripleLaserBoosterTimeMillis
        }
    }

    val monitorShipCollisionsId = UUID.randomUUID().toString()
    val monitorShipCollisionsRepeatTime = Millis(100)
    fun monitorShipCollisions(
        spaceObjects: List<SpaceObject>,
        boosters: List<Booster>,
        enemies: List<Enemy>,
        enemyLasers: List<Laser>,
        fileUltimateLaser: () -> Unit,
    ) {
        val shipRect by lazy {
            Rect(
                offset = Offset(x = ship.xOffset, y = ship.yOffset),
                size = Size(width = ship.width, height = ship.height)
            )
        }
        val shipShieldRect by lazy {
            Rect(
                center = Offset(
                    x = ship.xOffset + ship.width / 2,
                    y = ship.yOffset + ship.height / 2
                ),
                radius = ship.shieldRadius
            )
        }

        spaceObjects.forEachIndexed { spaceObjectIndex, spaceObject ->
            val spaceRect by lazy {
                Rect(
                    offset = Offset(x = spaceObject.xOffset, y = spaceObject.yOffset),
                    size = Size(width = spaceObject.size, height = spaceObject.size)
                )
            }
            if (spaceRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                spaceObjects[spaceObjectIndex].onObjectImpact(spaceShipCollidePower)

                val hpImpact: Int = when (ship.shieldEnabled && spaceObject.impactPower > 0) {
                    true -> 0
                    false -> spaceObject.impactPower
                }
                updateHp(-hpImpact)

                when (spaceObject.drawableId) {
                    BoosterType.ULTIMATE_WEAPON_BOOSTER.drawableId -> fileUltimateLaser()
                    BoosterType.SHIELD_BOOSTER.drawableId -> enableShield(enable = true)
                    BoosterType.LASER_BOOSTER.drawableId -> enableLaserBooster(enable = true)
                    BoosterType.TRIPLE_LASER_BOOSTER.drawableId -> enableTripleLaserBooster(enable = true)
                }
            }
        }

        boosters.forEachIndexed { boosterIndex, booster ->
            val boosterRect by lazy {
                Rect(
                    offset = Offset(x = booster.xOffset, y = booster.yOffset),
                    size = Size(width = booster.size, height = booster.size)
                )
            }
            if (boosterRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                boosters[boosterIndex].collect()
                when (booster.type) {
                    BoosterType.ULTIMATE_WEAPON_BOOSTER -> fileUltimateLaser()
                    BoosterType.SHIELD_BOOSTER -> enableShield(enable = true)
                    BoosterType.LASER_BOOSTER -> enableLaserBooster(enable = true)
                    BoosterType.TRIPLE_LASER_BOOSTER -> enableTripleLaserBooster(enable = true)
                    BoosterType.HEALTH_BOOSTER -> updateHp(100)
                }
            }
        }
        enemies.forEachIndexed { enemyIndex, enemy ->
            val enemyRect by lazy {
                Rect(
                    offset = Offset(x = enemy.xOffset, y = enemy.yOffset),
                    size = Size(width = enemy.width, height = enemy.height)
                )
            }
            if (enemyRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                enemies[enemyIndex].onObjectImpact(spaceShipCollidePower)

                val hpImpact: Int = when (ship.shieldEnabled && enemy.impactPower > 0) {
                    true -> 0
                    false -> enemy.impactPower.toInt()
                }
                updateHp(-hpImpact)
            }
        }
        enemyLasers.forEachIndexed { enemyIndex, enemyLaser ->
            val enemyLaserRect by lazy {
                Rect(
                    offset = Offset(x = enemyLaser.xOffset, y = enemyLaser.yOffset),
                    size = Size(width = enemyLaser.width, height = enemyLaser.height)
                )
            }
            if (enemyLaserRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                enemyLasers[enemyIndex].destroyed = true

                val hpImpact: Float = when (ship.shieldEnabled && enemyLaser.impactPower > 0) {
                    true -> 0f
                    false -> enemyLaser.impactPower
                }
                updateHp(-hpImpact.toInt())
            }
        }

        val currentTime = System.currentTimeMillis()
        if (shieldEndDurationMillis < currentTime) enableShield(enable = false)
        if (laserBoosterEndDurationMillis < currentTime) enableLaserBooster(enable = false)
        if (tripleLaserBoosterEndDurationMillis < currentTime) enableTripleLaserBooster(enable = false)
    }

    private fun updateShieldEnabled(enable: Boolean) {
        ship = ship.copy(shieldEnabled = enable)
        setShip(ship)
    }

    private fun resolveShipDrawable(ship: Ship): Int =
        if (ship.laserBoosterEnabled) R.drawable.ship_boosted_laser
        else R.drawable.ship_regular_laser

    private fun updateLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(laserBoosterEnabled = enable)
        ship = updated.copy(drawableId = resolveShipDrawable(updated))
        setShip(ship)
    }

    private fun updateTripleLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(tripleLaserBoosterEnabled = enable)
        ship = updated.copy(drawableId = resolveShipDrawable(updated))
        setShip(ship)
    }

    private fun updateXOffset(xOffset: Float) {
        ship = ship.copy(xOffset = xOffset)
        setShip(ship)
    }

    private fun updateYOffset(yOffset: Float) {
        ship = ship.copy(yOffset = yOffset)
        setShip(ship)
    }

    private fun updateHp(hpChange: Int) {
        if (ship.hp <= 0) return
        val before = ship.hp
        val newHp = (ship.hp + hpChange).coerceAtLeast(0)
        ship = ship.copy(hp = newHp)
        if (hpChange != 0) {
            Logger.d("Ship hp: $before → ${ship.hp} (Δ=$hpChange)")
        }
        setShip(ship)
        if (hpChange < 0) {
            onShipDamaged()
        }
        if (before > 0 && newHp == 0) {
            Logger.w("Ship destroyed (hp=0) → onShipDestroyed()")
            onShipDestroyed()
        }
    }

    companion object {
        const val TRIPLE_LASER_SIDE_OFFSET: Float = 20f
    }
}
