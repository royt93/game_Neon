package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * 33c+d Mid-boss with phase transition at HP < 50%.
 * - Phase 1 (HP >= 50%): standard attack pattern per variant
 * - Phase 2 (HP < 50%): boosted attack — variant-specific behavior
 *
 * Variants (see MidBossType):
 *   OFFENSIVE — moderate HP, sine-wave horizontal patrol, fast aimed lasers; Phase 2 = triple spread
 *   DEFENSIVE — high HP, slow horizontal patrol, single laser; Phase 2 = laser barrage (5-wave)
 *   SWARM     — low HP, fast erratic movement, frequent lasers; Phase 2 = doubled fire rate
 */
@Keep
data class MidBoss(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val variant: MidBossType,
    private val getShip: () -> Ship,
    /** Round 79 (#1) — chapter-aware bossKind override. */
    private val bossKindOverride: BossKind? = null,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = 130f
    override val height: Float = 90f
    override var hp: Float = variant.baseHp
    override val initialHp: Float = hp
    override val impactPower: Float = 10f
    override val minerals: Int = 5
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override val drawableId: Int = variant.drawableId
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = true               // reuses Boss HP bar + rank overlay
    // Round 71 (Issue 4d) — MidBoss variants map to 2 unique kinds (ORB, FRACTAL).
    // Round 79 (#1) — bossKindOverride (set theo chapter trong EnemyFactory) cho
    // phép Ch4Mid OFFENSIVE reuse render HELL_LORD, Ch3Mid SWARM render
    // HAUNTED_KID — eliminate visual duplicate giữa các chapter.
    override val bossKind: BossKind = bossKindOverride ?: when (variant) {
        MidBossType.OFFENSIVE -> BossKind.ORB
        MidBossType.DEFENSIVE -> BossKind.FRACTAL
        MidBossType.SWARM -> BossKind.FRACTAL
    }
    override val displayName: String = variant.displayName

    override var xOffset: Float = (screenWidth - width) / 2f
    override var yOffset: Float = -height
    private val entryTargetY: Float = 80f
    private val entrySpeed: Float = 2.5f
    override val isInEntryPhase: Boolean get() = yOffset < entryTargetY

    private var knockbackVel: Float = 0f
    private var movementTime: Float = 0f              // accumulates per process() call

    /** True once the phase transition flash has been triggered (one-shot gate). */
    private var phase2Engaged: Boolean = false

    /** External readers (banner): expose phase index for cinematic feedback. */
    val phase: Int get() = if (hp < initialHp * 0.5f) 2 else 1

    override fun enemyRect(): Rect = Rect(
        center = Offset(x = xOffset + width / 2, y = yOffset + height / 2),
        radius = width / 2,
    )

    override fun process() {
        if (yOffset < entryTargetY) {
            yOffset = (yOffset + entrySpeed).coerceAtMost(entryTargetY)
            if (hp <= 0) destroyed = true
            return
        }

        // Movement pattern by variant
        movementTime += 1f
        when (variant) {
            MidBossType.OFFENSIVE -> {
                // Sine wave horizontal: ±100px around center, 3s cycle.
                val t = movementTime / 600f                  // ~600 ticks per cycle
                val targetX = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 100f
                xOffset += (targetX - xOffset) * 0.05f       // smooth chase
            }

            MidBossType.DEFENSIVE -> {
                // Slow horizontal patrol — left ↔ right.
                val cycle = (movementTime / 1200f) % 2f      // 0..2 → 0..1 left→right, 1..2 right→left
                val phase = if (cycle < 1f) cycle else 2f - cycle
                xOffset = phase * (screenWidth - width)
            }

            MidBossType.SWARM -> {
                // Erratic figure-8 motion.
                val t = movementTime / 400f
                xOffset = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 120f
                yOffset = entryTargetY + cos((t * 2.0)).toFloat() * 40f
            }
        }

        // Coerce horizontal bounds.
        xOffset = xOffset.coerceIn(0f, screenWidth - width)

        if (knockbackVel != 0f) {
            yOffset += knockbackVel
            knockbackVel *= 0.92f
            if (kotlin.math.abs(knockbackVel) < 0.05f) knockbackVel = 0f
        }

        // Engage phase 2 transition once.
        if (!phase2Engaged && hp < initialHp * 0.5f) {
            phase2Engaged = true
            lastImpactMillis = System.currentTimeMillis()    // re-uses flash trigger for visual cue
        }

        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    override fun generateLasers(): List<Laser> {
        val ship: Ship = getShip()
        val phase2 = phase == 2
        return when (variant) {
            MidBossType.OFFENSIVE -> {
                if (phase2) tripleSpreadLasers(ship) else listOf(aimedLaser(ship))
            }

            MidBossType.DEFENSIVE -> {
                if (phase2) barrageLasers() else listOf(aimedLaser(ship))
            }

            MidBossType.SWARM -> {
                // Phase 1: single aimed laser. Phase 2: triple-spread (same primitive as
                // OFFENSIVE phase 2) — emulates the "doubled fire rate" intent without
                // touching EnemyLasersController's fixed 1000ms cadence.
                if (phase2) tripleSpreadLasers(ship) else listOf(aimedLaser(ship))
            }
        }
    }

    private fun aimedLaser(ship: Ship): Laser {
        val laserW = 28f
        val dx = ship.xOffset - xOffset
        val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        return EnemyLaser(
            xOffset = xOffset + width / 2 - laserW / 2,
            yOffset = yOffset + height,
            yRange = screenHeight,
            width = laserW,
            height = laserW,
            xOffsetMovementSpeed = (dx / dy) * 0.8f,
            yOffsetMovementSpeed = 0.8f,
            drawableId = R.drawable.ic_laser_red_8,
        )
    }

    private fun tripleSpreadLasers(ship: Ship): List<Laser> {
        // Three lasers at -30°, 0°, +30° spread.
        val laserW = 26f
        val baseDx = ship.xOffset - xOffset
        val baseDy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        return listOf(-0.4f, 0f, 0.4f).map { spreadOffset ->
            EnemyLaser(
                xOffset = xOffset + width / 2 - laserW / 2,
                yOffset = yOffset + height,
                yRange = screenHeight,
                width = laserW,
                height = laserW,
                xOffsetMovementSpeed = (baseDx / baseDy + spreadOffset) * 0.7f,
                yOffsetMovementSpeed = 0.8f,
                drawableId = R.drawable.ic_laser_red_8,
            )
        }
    }

    private fun barrageLasers(): List<Laser> {
        // 5-wide laser barrage covering 80% of screen width, with 1 random gap.
        val laserW = 30f
        val laserCount = 5
        val gap = (Math.random() * laserCount).toInt()
        val spacing = (screenWidth * 0.8f) / laserCount
        val startX = screenWidth * 0.1f
        return (0 until laserCount).filter { it != gap }.map { i ->
            EnemyLaser(
                xOffset = startX + i * spacing,
                yOffset = yOffset + height,
                yRange = screenHeight,
                width = laserW,
                height = laserW,
                xOffsetMovementSpeed = 0f,
                yOffsetMovementSpeed = 0.9f,
                drawableId = R.drawable.ic_laser_red_8,
            )
        }
    }

    override fun onObjectImpact(impactPower: Float) {
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        if (!isInEntryPhase) {
            knockbackVel = (knockbackVel - 1.2f).coerceAtLeast(-2.5f)
        }
    }
}
