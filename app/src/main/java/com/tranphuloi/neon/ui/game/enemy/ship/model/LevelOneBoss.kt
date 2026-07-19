package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import java.util.*

@Keep
data class LevelOneBoss(
    private val screenWidth: Float,
    private val screenHeight: Float,
    // Crash fix — lambda capture không Serializable; Bundle instance-state
    // save (dumpStats khi Activity bị che khuất giữa combat boss) từng
    // NotSerializableException → BadParcelableException crash toàn app.
    @Transient
    private val getShip: () -> Ship,
    /** Round 79 (#1) — optional override để Ch3End có thể render là DEATH_MOON
     * thay vì duplicate STAR/SUN của Ch1End. Null = giữ default STAR. */
    private val bossKindOverride: BossKind? = null,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = 150f
    override val height: Float = 100f
    override var hp: Float = 3000f
    override val initialHp: Float = hp
    override val impactPower: Float = 10f
    override val minerals: Int = 10
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override val drawableId: Int = R.drawable.enemy_red_boss
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = true
    // Round 71 (Issue 4d) — STAR silhouette (8-point baseline, red). Round 79
    // (#1): cho phép override (Ch3End → DEATH_MOON).
    override val bossKind: BossKind = bossKindOverride ?: BossKind.STAR
    // Round 84 audit — read displayName from bossKind enum (Vietnamese name)
    // thay hardcoded "LEVEL 1 BOSS" dev-jargon.
    override val displayName: String = bossKind.displayName
    private val bossMovementSpeed = 0.5f

    private val minXOffset = width
    private val maxXOffset = screenWidth - width
    private val minYOffset = height / 2
    private val maxYOffset = screenHeight / 2

    private var movement: Movement = Movement.TOP_LEFT_TOP_RIGHT

    override var xOffset: Float = minXOffset
    // Dramatic entry: start off-screen above, slide down through entry phase
    // before normal patrol movement begins.
    override var yOffset: Float = -height
    private val entrySpeed: Float = 2.0f                     // ~400 px/sec at 5ms tick
    private var knockbackVel: Float = 0f
    override val isInEntryPhase: Boolean get() = yOffset < minYOffset

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
        // Entry phase: slide down from off-screen-above to patrol position.
        if (yOffset < minYOffset) {
            yOffset = (yOffset + entrySpeed).coerceAtMost(minYOffset)
            if (hp <= 0) destroyed = true
            return
        }

        if (xOffset <= 0 && yOffset <= minYOffset) {
            movement = Movement.TOP_LEFT_TOP_RIGHT
        } else if (yOffset <= minYOffset && xOffset >= maxXOffset) {
            movement = Movement.TOP_RIGHT_BOTTOM_RIGHT
        } else if (xOffset >= maxXOffset && yOffset >= maxYOffset) {
            movement = Movement.BOTTOM_RIGHT_BOTTOM_LEFT
        } else if (yOffset >= maxYOffset && xOffset <= 0) {
            movement = Movement.BOTTOM_LEFT_TOP_RIGHT
        }

        when (movement) {
            Movement.TOP_LEFT_TOP_RIGHT -> xOffset += bossMovementSpeed
            Movement.TOP_RIGHT_BOTTOM_RIGHT -> yOffset += bossMovementSpeed
            Movement.BOTTOM_RIGHT_BOTTOM_LEFT -> xOffset -= bossMovementSpeed
            Movement.BOTTOM_LEFT_TOP_RIGHT -> yOffset -= bossMovementSpeed
        }

        // Smooth knockback decay 0.92 (~200ms recovery).
        if (knockbackVel != 0f) {
            yOffset += knockbackVel
            knockbackVel *= 0.92f
            if (kotlin.math.abs(knockbackVel) < 0.05f) knockbackVel = 0f
        }

        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    override fun generateLasers(): List<Laser> {
        // Round 74 (R73e) — STAR boss attack pattern: 8-laser RING BARRAGE
        // radial từ center. Mỗi laser bay theo hướng riêng 45° spacing. Player
        // phải né như giữa "starburst" của một quả pháo hoa.
        val width = 24f
        val centerX = xOffset + this.width / 2 - width / 2
        val centerY = yOffset + height / 2
        val speed = 4f
        return (0 until 8).map { i ->
            val angle = i * 45.0 * Math.PI / 180.0
            // Negative Y because lasers move "down" in game = positive y is down,
            // but enemy laser yOffsetMovementSpeed sign convention varies. Match
            // existing yOffMovementSpeed > 0 = downward by using sin(angle) magnitude.
            val dx = kotlin.math.cos(angle).toFloat() * speed
            val dy = kotlin.math.sin(angle).toFloat() * speed
            EnemyLaser(
                xOffset = centerX,
                yOffset = centerY,
                yRange = screenHeight,
                width = width,
                height = width,
                xOffsetMovementSpeed = dx,
                yOffsetMovementSpeed = dy,
                drawableId = R.drawable.ic_laser_red_8,
            )
        }
    }

    override fun onObjectImpact(impactPower: Float) {
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        // Boss smooth knockback. Stronger so player sees push (was -0.7/-1.5).
        if (!isInEntryPhase) {
            knockbackVel = (knockbackVel - 1.5f).coerceAtLeast(-3f)
        }
    }

    private enum class Movement {
        TOP_LEFT_TOP_RIGHT,
        TOP_RIGHT_BOTTOM_RIGHT,
        BOTTOM_RIGHT_BOTTOM_LEFT,
        BOTTOM_LEFT_TOP_RIGHT
    }
}
