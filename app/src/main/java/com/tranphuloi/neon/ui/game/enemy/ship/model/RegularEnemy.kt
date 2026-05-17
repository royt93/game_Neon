package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.laser.Laser
import java.util.*

@Keep
data class RegularEnemy(
    private val screenWidth: Float,
    private val screenHeight: Float,
    override var xOffset: Float,
    private val type: RegularEnemyType,
    override var hp: Float = type.hp,
    /** Wave 5 (28x) — V/SineWave formations stagger spawn y position. Default 0 = top of screen. */
    private val initialYOffset: Float = 0f,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = type.width
    override val height: Float = type.height
    override val initialHp: Float = hp
    override val impactPower: Float = type.impactPower
    override val minerals: Int = 1
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override var yOffset: Float = initialYOffset
    override val drawableId: Int = type.drawableId
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = false
    override val displayName: String = "Regular"
    private var moveRight = true
    private val xOffsetMovementSpeed = type.xOffsetSpeed
    private val yOffsetMovementSpeed = type.yOffsetSpeed
    // Smooth knockback: velocity accumulates from each hit, decays 15% per tick.
    private var knockbackVel: Float = 0f
    /** Wave 5 (28x) — SineWave anchor x captured at spawn. xOffset oscillates around this. */
    private val sineAnchorX: Float = xOffset

    override fun enemyRect(): Rect {
        return Rect(
            center = Offset(
                x = xOffset + width / 2,
                y = yOffset + height / 2
            ),
            radius = width / 2
        )
    }

    override fun process() {
        when (type.formation) {
            is ZigZag -> moveZigZagFormation()
            is Row -> moveRectangleFormation()
            is VFormation -> moveRectangleFormation()              // V uses same straight-down motion as Row
            is SineWave -> moveSineWaveFormation()
        }
        // Smooth knockback: apply current velocity then decay.
        // Decay 0.92 (was 0.85) → ~200ms recovery (more visible push than 100ms).
        if (knockbackVel != 0f) {
            yOffset += knockbackVel
            knockbackVel *= 0.92f
            if (kotlin.math.abs(knockbackVel) < 0.05f) knockbackVel = 0f
        }
        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    private fun moveZigZagFormation() {
        if (moveRight) {
            xOffset += xOffsetMovementSpeed
            if (xOffset + width > screenWidth) moveRight = false
        } else {
            xOffset -= xOffsetMovementSpeed
            if (xOffset < 0f) moveRight = true
        }
        yOffset += yOffsetMovementSpeed
    }

    private fun moveRectangleFormation() {
        yOffset += yOffsetMovementSpeed
    }

    private fun moveSineWaveFormation() {
        yOffset += yOffsetMovementSpeed
        val sw = type.formation as SineWave
        // x = anchor + sin(y / period) × amplitude. Period in px-of-y; full
        // oscillation every 2π·period vertical pixels. Default 110px → wavelength
        // ~691px (one full S-wave fits in the visible play area).
        val phase = yOffset / sw.period
        val newX = sineAnchorX + kotlin.math.sin(phase) * sw.amplitude
        // Coerce so the enemy doesn't slide entirely off-screen on extreme amplitude.
        xOffset = newX.coerceIn(0f, screenWidth - width)
    }

    override fun generateLasers(): List<Laser> {
        val laserWidth = 18f
        return listOf(
            EnemyLaser(
                xOffset = xOffset + width / 2 - laserWidth / 2,
                yOffset = yOffset + height,
                yRange = screenHeight,
                width = laserWidth
            )
        )
    }

    override fun onObjectImpact(impactPower: Float) {
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        // Smooth knockback: stronger impulse (-3 per hit, cap -6) so push-back
        // is visually unmistakable. Was -1.5 / -3 which read as "subtle wobble".
        knockbackVel = (knockbackVel - 3f).coerceAtLeast(-6f)
    }
}
