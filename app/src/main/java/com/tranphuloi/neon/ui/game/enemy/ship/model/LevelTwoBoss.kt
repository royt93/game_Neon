package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.laser.Laser
import java.util.*
import kotlin.random.Random

@Keep
data class LevelTwoBoss(
    private val screenWidth: Float,
    private val screenHeight: Float,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = 230f
    override val height: Float = 130f
    override var hp: Float = 4000f
    override val initialHp: Float = hp
    override val impactPower: Float = 10f
    override val minerals: Int = 10
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override val drawableId: Int = R.drawable.enemy_green_boss
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = true
    override val displayName: String = "LEVEL 2 BOSS"
    private val bossMovementSpeed = 0.5f

    private val maxXOffset = screenWidth - width
    private var movement: Movement = Movement.RIGHT

    override var xOffset: Float = 100f
    // Dramatic entry: start off-screen above, slide down to patrol y=100.
    override var yOffset: Float = -height
    private val entryTargetY: Float = 100f
    private val entrySpeed: Float = 2.0f
    override val isInEntryPhase: Boolean get() = yOffset < entryTargetY

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
        // Entry phase: slide down before LEFT/RIGHT patrol begins.
        if (yOffset < entryTargetY) {
            yOffset = (yOffset + entrySpeed).coerceAtMost(entryTargetY)
            if (hp <= 0) destroyed = true
            return
        }

        if (xOffset <= 0) {
            movement = Movement.RIGHT
        } else if (xOffset >= maxXOffset) {
            movement = Movement.LEFT
        }

        when (movement) {
            Movement.RIGHT -> xOffset += bossMovementSpeed
            Movement.LEFT -> xOffset -= bossMovementSpeed
        }

        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    override fun generateLasers(): List<Laser> {
        val laserWidth = 30f
        val yOffMovementSpeed = 0.8f
        return buildList {
            val laserCount = (screenWidth / laserWidth).toInt()
            val gapSize = 5
            val randomLaserGap = Random.nextInt(1, laserCount - gapSize)
            val gapRange = randomLaserGap..randomLaserGap + gapSize
            for (i in 1 until laserCount) {
                if (!gapRange.contains(i)) {
                    val xOffset = laserWidth * i - laserWidth / 2
                    val laser = EnemyLaser(
                        xOffset = xOffset,
                        yOffset = yOffset + height,
                        yRange = screenHeight,
                        width = laserWidth,
                        height = laserWidth,
                        xOffsetMovementSpeed = 0f,
                        yOffsetMovementSpeed = yOffMovementSpeed,
                        drawableId = R.drawable.ic_laser_red_8
                    )
                    add(laser)
                }
            }
        }
    }

    override fun onObjectImpact(impactPower: Float) {
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        // Boss knockback — smaller, skip entry phase.
        if (!isInEntryPhase) {
            yOffset -= 2.5f
        }
    }

    private enum class Movement {
        LEFT,
        RIGHT
    }
}
