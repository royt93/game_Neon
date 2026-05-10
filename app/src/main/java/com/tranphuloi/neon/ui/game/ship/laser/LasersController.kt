package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.laser.ShipBoostedLaser.Companion.SHIP_BOOSTED_LASER_WIDTH
import com.tranphuloi.neon.ui.game.ship.laser.ShipLaser.Companion.SHIP_LASER_WIDTH
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.ship.ship.ShipController.Companion.TRIPLE_LASER_SIDE_OFFSET
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObject
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils

class LasersController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val uuidUtils: UuidUtils,
    initialShipLasers: List<Laser> = listOf(),
    initialUltimateLasers: List<Laser> = listOf(),
    private val setShipLasers: (List<Laser>) -> Unit,
    private val setUltimateLasers: (List<Laser>) -> Unit,
    private val onLaserHit: (targetId: String, damage: Int, x: Float, y: Float, isBoss: Boolean) -> Unit = { _, _, _, _, _ -> },
) {

    init {
        Logger.d("LasersController init: initialShipLasers=${initialShipLasers.size}, initialUltimateLasers=${initialUltimateLasers.size}")
    }

    private var shipLasers: List<Laser> = initialShipLasers
    private var ultimateLasers: List<Laser> = initialUltimateLasers

    val fireLaserId = uuidUtils.getUuid()
    val fireLaserRepeatTime = Millis(100)
    fun fireLasers(ship: Ship) {

        val lasers = if (ship.laserBoosterEnabled) {
            // Laser bottom flush with ship top (`ship.yOffset`). Since laser height
            // = 25, top y = ship.yOffset - 25 places laser edge-to-edge with ship.
            val laser = ShipBoostedLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - SHIP_BOOSTED_LASER_WIDTH / 2,
                yOffset = ship.yOffset - 25f,
                yRange = screenHeight
            )
            if (ship.tripleLaserBoosterEnabled) {
                listOf(
                    laser.copy(xOffset = laser.xOffset - TRIPLE_LASER_SIDE_OFFSET),
                    laser.copy(xOffset = laser.xOffset),
                    laser.copy(xOffset = laser.xOffset + TRIPLE_LASER_SIDE_OFFSET)
                )
            } else {
                listOf(laser)
            }
        } else {
            // Laser bottom flush with ship top — height = 20.
            val laser = ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - SHIP_LASER_WIDTH / 2,
                yOffset = ship.yOffset - 20f,
                yRange = screenHeight
            )
            if (ship.tripleLaserBoosterEnabled) {
                listOf(
                    laser.copy(xOffset = laser.xOffset - TRIPLE_LASER_SIDE_OFFSET),
                    laser.copy(xOffset = laser.xOffset),
                    laser.copy(xOffset = laser.xOffset + TRIPLE_LASER_SIDE_OFFSET)
                )
            } else {
                listOf(laser)
            }
        }

        shipLasers = shipLasers + lasers
        updateShipLasersUI()
    }

    val processShipLasersId = uuidUtils.getUuid()
    val processShipLasersRepeatTime = Millis(5)
    fun processShipLasers() {
        shipLasers.forEach {
            it.moveLaser()
            // Cleanup once laser scrolls fully off the top of the screen.
            // (Coord system is now TopStart; laser leaves top when yOffset < -height.)
            if (it.yOffset < -100f || it.destroyed) destroyShipLaser(it)
        }
        updateShipLasersUI()
    }

    fun hasShipLasers() = shipLasers.isNotEmpty()

    private fun destroyShipLaser(laser: Laser) {
        shipLasers = shipLasers - laser
        updateShipLasersUI()
    }

    fun fireUltimateLaser() {
        Logger.d("LasersController.fireUltimateLaser: spawning $ULTIMATE_LASERS_COUNT vertical beams (sweep bottom→top, existing=${ultimateLasers.size})")
        val ultimateLaserList = mutableListOf<UltimateLaser>()
        val horizontalLaserDistance = screenWidth / ULTIMATE_LASERS_COUNT
        for (i in 0..ULTIMATE_LASERS_COUNT) {
            val ultimateLaser = UltimateLaser(
                id = uuidUtils.getUuid(),
                xOffset = horizontalLaserDistance * i,
                yOffset = screenHeight,        // start at bottom edge, sweep up via yOffset -= 7
                yRange = screenHeight
            )
            ultimateLaserList.add(ultimateLaser)
        }
        // Append instead of replace — was `ultimateLasers = ultimateLaserList`, which
        // wiped the previous in-flight batch when a new fire (ChargeShot auto + booster
        // pickup) triggered within ~10s of each other. Caused beams to "disappear at
        // halfway height" visually. Now both batches coexist until they fly off-screen.
        ultimateLasers = ultimateLasers + ultimateLaserList
        updateUltimateLasers()
    }

    val processLasersId = uuidUtils.getUuid()
    val processLasersRepeatTime = Millis(40)
    fun processLasers() {
        ultimateLasers.forEach {
            it.moveLaser()
            if (it.yOffset < -screenHeight || it.destroyed) destroyUltimateShipLaser(it)
        }
        updateUltimateLasers()
    }

    fun hasUltimateLasers() = ultimateLasers.isNotEmpty()

    private fun destroyUltimateShipLaser(laser: Laser) {
        ultimateLasers = ultimateLasers - laser
        updateUltimateLasers()
    }

    val monitorLaserCollisionId = uuidUtils.getUuid()
    val monitorLaserCollisionRepeatTime = Millis(1)
    fun monitorLaserCollision(spaceObjects: List<SpaceObject>, enemies: List<Enemy>) {
        val lasers = shipLasers + ultimateLasers
        val spaceObjectRectList = spaceObjects.map { it.spaceObjectRect() }
        val enemyRectList = enemies.map { it.enemyRect() }
        lasers.forEach { laser ->

            // Round 13 regression FIX: was `y = yOffset + screenHeight - height`
            // — that was an OLD BottomStart→TopStart convert. After round 13 made
            // lasers natively TopStart, this convert pushed rect off-screen → no
            // collisions for ship laser. Use raw yOffset directly now.
            val laserRect = Rect(
                offset = Offset(x = laser.xOffset, y = laser.yOffset),
                size = Size(width = laser.width, height = laser.height)
            )

            if (spaceObjectRectList.any { it.overlaps(laserRect) }) {
                val index = spaceObjectRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = spaceObjects[index]
                val hitX = target.xOffset + target.size / 2f
                val hitY = target.yOffset + target.size / 2f
                Logger.d("Collision: laser id=${laser.id.take(6)} → spaceObject hp=${target.hp.toInt()} (-${laser.impactPower.toInt()})")
                target.onObjectImpact(laser.impactPower)
                // Trigger same impact feedback as enemy hits — sparks + mini explosion +
                // damage number + hit-stop freeze. Rocks are non-boss so isBoss=false.
                onLaserHit(target.id, laser.impactPower.toInt(), hitX, hitY, false)
                destroyShipLaser(laser)
                updateShipLasersUI()
            }
            if (enemyRectList.any { it.overlaps(laserRect) }) {
                val index = enemyRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = enemies[index]
                Logger.d("Collision: laser id=${laser.id.take(6)} → enemy id=${target.enemyId.take(6)} hp=${target.hp.toInt()} (-${laser.impactPower.toInt()})")
                target.onObjectImpact(laser.impactPower)
                onLaserHit(
                    target.enemyId,
                    laser.impactPower.toInt(),
                    target.xOffset + target.width / 2f,
                    target.yOffset,
                    target.isBoss,
                )
                destroyShipLaser(laser)
                updateShipLasersUI()
            }
        }
    }

    private fun updateShipLasersUI() {
        setShipLasers(shipLasers)
    }

    private fun updateUltimateLasers() {
        setUltimateLasers(ultimateLasers)
    }

    companion object {
        const val ULTIMATE_LASERS_COUNT = 9
    }
}
