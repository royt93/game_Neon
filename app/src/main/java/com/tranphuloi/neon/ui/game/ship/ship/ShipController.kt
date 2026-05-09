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
import kotlin.math.PI
import kotlin.math.sin

// init log placed in init {} block below

class ShipController(
    private val screenWidth: Float,
    screenHeight: Float,
    private var ship: Ship,
    private val setShip: (Ship) -> Unit,
    private val onShipDestroyed: () -> Unit = {},
    private val onShipDamaged: () -> Unit = {},
    private val onBoosterPickedUp: () -> Unit = {},
    private val damageMultiplier: () -> Float = { 1f },
) {

    init {
        Logger.d("ShipController init: screen=${screenWidth}x${screenHeight}, ship hp=${ship.hp}")
    }

    private val spaceShipCollidePower: Float = 100f
    private val movementSpeed: Float = 2f
    private val maxYOffset: Float = screenHeight - 140

    var movingLeft = false
    var movingRight = false

    // Cinematic spawn animation: bottom → fly up to center → sway → fly down to play.
    // Total 3s. During spawn: damage absorbed (see updateHp), player input ignored,
    // ship position fully driven by the choreographed path below.
    private val spawnStartMillis: Long = System.currentTimeMillis()
    private val spawnFlyUpMillis: Long = 800L
    private val spawnSwayMillis: Long = 1400L
    private val spawnFlyDownMillis: Long = 800L
    private val spawnTotalMillis: Long =
        spawnFlyUpMillis + spawnSwayMillis + spawnFlyDownMillis
    private val spawnStartY: Float = screenHeight + 240f       // off-screen below
    private val spawnCenterY: Float = screenHeight * 0.35f
    private val spawnCenterX: Float = screenWidth / 2f - 85f / 2f

    val moveShipId = UUID.randomUUID().toString()
    val moveShipRepeatTime = Millis(3)

    fun moveShip() {
        val elapsed = System.currentTimeMillis() - spawnStartMillis
        if (elapsed < spawnTotalMillis) {
            applySpawnPath(elapsed)
            return
        }
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

    private fun applySpawnPath(elapsed: Long) {
        val phase1End = spawnFlyUpMillis
        val phase2End = phase1End + spawnSwayMillis
        when {
            elapsed < phase1End -> {
                // Phase 1: fly up bottom → center, cubic ease-out.
                val t = elapsed / phase1End.toFloat()
                val tEase = 1f - (1f - t) * (1f - t) * (1f - t)
                updateXOffset(spawnCenterX)
                updateYOffset(spawnStartY + (spawnCenterY - spawnStartY) * tEase)
            }
            elapsed < phase2End -> {
                // Phase 2: sway side-to-side at center, damped sine wave.
                val swayElapsed = elapsed - phase1End
                val swayT = swayElapsed / spawnSwayMillis.toFloat()
                val swayPhase = swayT * (PI.toFloat() * 2f) * 1.5f   // 1.5 oscillations
                val damp = 1f - swayT * 0.4f
                val swayX = sin(swayPhase) * 90f * damp
                updateXOffset(spawnCenterX + swayX)
                updateYOffset(spawnCenterY)
            }
            else -> {
                // Phase 3: fly down center → play position, smoothstep ease-in-out.
                val downElapsed = elapsed - phase2End
                val downT = (downElapsed / spawnFlyDownMillis.toFloat()).coerceIn(0f, 1f)
                val tEase = downT * downT * (3f - 2f * downT)
                updateXOffset(spawnCenterX)
                updateYOffset(spawnCenterY + (maxYOffset - spawnCenterY) * tEase)
            }
        }
    }

    fun isSpawning(): Boolean =
        System.currentTimeMillis() - spawnStartMillis < spawnTotalMillis

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
                Logger.d("Collision: ship ↔ spaceObject (shield=${ship.shieldEnabled}, impactPower=${spaceObject.impactPower})")
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
                Logger.d("Collision: ship ↔ booster type=${booster.type} (shield=${ship.shieldEnabled})")
                boosters[boosterIndex].collect()
                onBoosterPickedUp()
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
                Logger.d("Collision: ship ↔ enemy id=${enemy.enemyId.take(6)} (shield=${ship.shieldEnabled})")
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
                Logger.d("Collision: ship ↔ enemyLaser (shield=${ship.shieldEnabled}, impactPower=${enemyLaser.impactPower.toInt()})")
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
        ship = ship.copy(
            shieldEnabled = enable,
            shieldEndMillis = if (enable) shieldEndDurationMillis else 0L,
        )
        setShip(ship)
    }

    private fun resolveShipDrawable(ship: Ship): Int =
        if (ship.laserBoosterEnabled) R.drawable.ship_boosted_laser
        else R.drawable.ship_regular_laser

    private fun updateLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(
            laserBoosterEnabled = enable,
            laserBoosterEndMillis = if (enable) laserBoosterEndDurationMillis else 0L,
        )
        ship = updated.copy(drawableId = resolveShipDrawable(updated))
        setShip(ship)
    }

    private fun updateTripleLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(
            tripleLaserBoosterEnabled = enable,
            tripleLaserBoosterEndMillis = if (enable) tripleLaserBoosterEndDurationMillis else 0L,
        )
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

    private var iframesEndMillis: Long = 0L

    private fun updateHp(hpChange: Int) {
        if (ship.hp <= 0) return
        // Damage absorption: i-frames OR spawn animation. Healing (hpChange > 0) always applies.
        if (hpChange < 0 && (System.currentTimeMillis() < iframesEndMillis || isSpawning())) {
            Logger.d("Ship hp: damage Δ=$hpChange ABSORBED (iframes or spawn)")
            return
        }
        val multiplier = damageMultiplier()
        val effective = if (hpChange < 0) -((-hpChange) * multiplier).toInt() else hpChange
        val before = ship.hp
        val newHp = (ship.hp + effective).coerceAtLeast(0)
        ship = ship.copy(hp = newHp)
        if (effective != 0) {
            Logger.d("Ship hp: $before → ${ship.hp} (Δ=$effective, raw=$hpChange, multiplier=$multiplier)")
        }
        setShip(ship)
        if (effective < 0) {
            iframesEndMillis = System.currentTimeMillis() + IFRAMES_DURATION_MILLIS
            Logger.d("Ship i-frames: ON until $iframesEndMillis (+${IFRAMES_DURATION_MILLIS}ms)")
            onShipDamaged()
        }
        if (before > 0 && newHp == 0) {
            Logger.w("Ship destroyed (hp=0) → onShipDestroyed()")
            onShipDestroyed()
        }
    }

    companion object {
        const val TRIPLE_LASER_SIDE_OFFSET: Float = 20f
        const val IFRAMES_DURATION_MILLIS: Long = 600L
    }
}
