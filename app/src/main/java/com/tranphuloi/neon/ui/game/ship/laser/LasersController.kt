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
    /**
     * Wave 5 (25x / 48x) — damage multiplier applied at hit time. Combines
     * RunModifier (GLASS_CANNON, BERSERKER, DOUBLE_OR_NOTHING) + skill tree
     * FIREPOWER node. Default 1.0 = no boost.
     */
    private val damageMultiplier: () -> Float = { 1f },
) {

    init {
        Logger.d("LasersController init: initialShipLasers=${initialShipLasers.size}, initialUltimateLasers=${initialUltimateLasers.size}")
    }

    private var shipLasers: List<Laser> = initialShipLasers
    private var ultimateLasers: List<Laser> = initialUltimateLasers

    val fireLaserId = uuidUtils.getUuid()
    val fireLaserRepeatTime = Millis(100)
    fun fireLasers(ship: Ship) {
        // Round 35 (35x) — bullet-type override takes priority over normal lasers.
        // Triple-laser fan still applies for spread shot.
        if (ship.activeBulletType != BulletType.NORMAL) {
            val newLasers = fireBulletTypeLasers(ship)
            shipLasers = shipLasers + newLasers
            updateShipLasersUI()
            return
        }

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

    /**
     * Round 35 (35x) — bullet-type variant fire. Centers on ship like NORMAL.
     * Returns the new lasers spawned this tick (typically 1, triple-spread = 3).
     */
    private fun fireBulletTypeLasers(ship: Ship): List<Laser> {
        val centerX = ship.xOffset + ship.width / 2
        val top = ship.yOffset - 22f
        val templates: List<Laser> = when (ship.activeBulletType) {
            BulletType.PIERCING -> listOf(
                PiercingShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = centerX - 3f,
                    yOffset = top,
                    yRange = screenHeight,
                ),
            )
            BulletType.PLASMA -> listOf(
                PlasmaShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = centerX - PlasmaShipLaser.PLASMA_WIDTH / 2,
                    yOffset = top - 12f,
                    yRange = screenHeight,
                ),
            )
            BulletType.NORMAL -> emptyList()                // unreachable; gated at caller
        }
        return templates
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

            // 25x/48x — apply damage multiplier (modifier + meta) per hit.
            val dmgMul = damageMultiplier()
            val effectiveDamage = laser.impactPower * dmgMul
            if (spaceObjectRectList.any { it.overlaps(laserRect) }) {
                val index = spaceObjectRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = spaceObjects[index]
                val hitX = target.xOffset + target.size / 2f
                val hitY = target.yOffset + target.size / 2f
                target.onObjectImpact(effectiveDamage)
                // Trigger same impact feedback as enemy hits — sparks + mini explosion +
                // damage number + hit-stop freeze. Rocks are non-boss so isBoss=false.
                onLaserHit(target.id, effectiveDamage.toInt(), hitX, hitY, false)
                destroyShipLaser(laser)
                updateShipLasersUI()
            }
            if (enemyRectList.any { it.overlaps(laserRect) }) {
                val index = enemyRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = enemies[index]
                target.onObjectImpact(effectiveDamage)
                onLaserHit(
                    target.enemyId,
                    effectiveDamage.toInt(),
                    target.xOffset + target.width / 2f,
                    target.yOffset,
                    target.isBoss,
                )
                // Round 35 (35x) — PIERCING / PLASMA collision behavior.
                when (laser.bulletType) {
                    BulletType.PIERCING -> {
                        // Decrement pierce; destroy only when exhausted.
                        // Round 37 — removed per-hit Logger.d (fired inside Millis(1) tick;
                        // during a PIERCING run through enemy formations this spammed dozens
                        // of lines per second). onLaserHit upstream already records the hit.
                        laser.pierceRemaining = laser.pierceRemaining - 1
                        if (laser.pierceRemaining <= 0) {
                            destroyShipLaser(laser)
                        }
                    }
                    BulletType.PLASMA -> {
                        // AoE damage: enemies within radius take 50% damage.
                        val aoeRadius = laser.bulletType.aoeRadius
                        val hitCenterX = target.xOffset + target.width / 2f
                        val hitCenterY = target.yOffset + target.height / 2f
                        val aoeDmg = effectiveDamage * 0.5f
                        enemies.forEachIndexed { i, other ->
                            if (i == index) return@forEachIndexed
                            val dx = (other.xOffset + other.width / 2f) - hitCenterX
                            val dy = (other.yOffset + other.height / 2f) - hitCenterY
                            if (dx * dx + dy * dy <= aoeRadius * aoeRadius) {
                                other.onObjectImpact(aoeDmg)
                                onLaserHit(
                                    other.enemyId,
                                    aoeDmg.toInt(),
                                    other.xOffset + other.width / 2f,
                                    other.yOffset,
                                    other.isBoss,
                                )
                            }
                        }
                        // Round 37 — removed per-hit Logger.d (fired inside Millis(1) tick).
                        // onLaserHit handles the per-target signal; AoE participants are
                        // logged via their own onLaserHit calls a few lines above.
                        destroyShipLaser(laser)
                    }
                    BulletType.NORMAL -> destroyShipLaser(laser)
                }
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
