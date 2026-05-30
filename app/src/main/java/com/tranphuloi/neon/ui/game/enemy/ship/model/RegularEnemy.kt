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
    /**
     * Wave 13c — per-enemy size scale (see [EnemySize]). Scales hitbox (width/
     * height → enemyRect), HP, movement speed (inverse), and mineral reward.
     * 1f = baseline. Picked once per spawn group in EnemyFactory.
     */
    val sizeScale: Float = 1f,
    override var hp: Float = type.hp * EnemySize.hpFactor(sizeScale),
    /** Wave 5 (28x) — V/SineWave formations stagger spawn y position. Default 0 = top of screen. */
    private val initialYOffset: Float = 0f,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = type.width * sizeScale
    override val height: Float = type.height * sizeScale
    override val initialHp: Float = hp
    override val impactPower: Float = type.impactPower
    override val minerals: Int = EnemySize.mineralReward(sizeScale)
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
    // Wave 13c — bigger = slower, smaller = faster (inverse of sizeScale).
    private val xOffsetMovementSpeed = type.xOffsetSpeed * EnemySize.speedFactor(sizeScale)
    private val yOffsetMovementSpeed = type.yOffsetSpeed * EnemySize.speedFactor(sizeScale)
    // Smooth knockback: velocity accumulates from each hit, decays 15% per tick.
    private var knockbackVel: Float = 0f
    /** Wave 5 (28x) — SineWave anchor x captured at spawn. xOffset oscillates around this. */
    private val sineAnchorX: Float = xOffset

    /**
     * Wave 11d Pixel-2 audit P1 fix — horizontal separation velocity accumulated
     * by EnemyController.applySeparationForces() each tick when this enemy
     * overlaps another. Consumed AFTER formation-specific movement so that
     * SineWave's `xOffset = sineAnchorX + sin(phase) * amplitude` (which
     * overwrites xOffset every tick) doesn't clobber the push. Decays 15%
     * per tick so a single overlap nudge propagates over ~6 frames instead
     * of teleporting.
     *
     * Visible publicly so the controller can accumulate from outside —
     * Kotlin Float read/write is atomic on JVM (no `long`/`double` torn-read
     * issue), so accessing from IO + Compose Main is safe for this primitive.
     */
    var separationVel: Float = 0f

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
        // Pixel-2 audit P1 fix — consume horizontal separation velocity AFTER
        // formation movement. Pre-fix EnemyController.applySeparationForces
        // mutated xOffset directly, which SineWave's "xOffset = sineAnchorX +
        // sin(phase) * amp" overwrote each tick. Now the controller adds to
        // separationVel; we add it to xOffset post-formation so the push
        // survives. Decay matches knockbackVel pattern for consistent feel.
        if (separationVel != 0f) {
            xOffset += separationVel
            separationVel *= 0.85f
            if (kotlin.math.abs(separationVel) < 0.05f) separationVel = 0f
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
