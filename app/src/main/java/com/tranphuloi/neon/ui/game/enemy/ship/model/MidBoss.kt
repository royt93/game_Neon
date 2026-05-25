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
    // Round 82 — data-driven via variant.defaultBossKind. 12 new variants tự
    // mang BossKind riêng → KHÔNG cần hardcode dispatch. bossKindOverride vẫn
    // ưu tiên cao nhất cho chapter-aware reuse (Ch4 OFFENSIVE→HELL_LORD, etc.).
    override val bossKind: BossKind = bossKindOverride ?: variant.defaultBossKind
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

        // Movement pattern by variant. Round 82 — 12 new variants tái sử dụng
        // movement pattern của variant gốc gần nhất theo behavior:
        // - Aggressive (HEN/BUFFALO/TIGER/TROLL/DIVA/SATAN/SICKLE/TYCOON) → sine
        // - Defensive (RAT/DRAGON/MONEY/SUMMITS/GLOBES) → slow patrol
        movementTime += 1f
        val patternForVariant: Int = when (variant) {
            MidBossType.OFFENSIVE -> 0           // sine
            MidBossType.DEFENSIVE -> 1           // patrol
            MidBossType.SWARM -> 2               // figure-8
            MidBossType.HEN_MOTHER -> 2          // erratic
            MidBossType.BUFFALO_RAGE -> 0        // sine
            MidBossType.DUMB_RAT -> 1            // patrol
            MidBossType.FIERCE_TIGER -> 0        // sine
            MidBossType.SEXY_DIVA -> 2           // erratic
            MidBossType.TROLL_TOWER -> 1         // patrol slow
            MidBossType.TWIN_SUMMITS -> 1        // patrol
            MidBossType.VOID_GLOBES -> 1         // patrol slow heavy
            MidBossType.WHITE_DRAGON -> 0        // sine sinuous
            MidBossType.HAMMER_SICKLE -> 0       // sine
            MidBossType.MONEY_TYCOON -> 1        // patrol
            MidBossType.GOLDEN_TYCOON -> 2       // erratic
        }
        when (patternForVariant) {
            0 -> {
                // Sine wave horizontal: ±100px around center, 3s cycle.
                val t = movementTime / 600f
                val targetX = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 100f
                xOffset += (targetX - xOffset) * 0.05f
            }
            1 -> {
                // Slow horizontal patrol — left ↔ right.
                val cycle = (movementTime / 1200f) % 2f
                val phase = if (cycle < 1f) cycle else 2f - cycle
                xOffset = phase * (screenWidth - width)
            }
            2 -> {
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
        // Round 82 — 12 new variants reuse existing laser patterns (triple-spread,
        // barrage, aimed). Bullet customization (egg/horn/laser/roar/hair-projectile
        // etc per user spec) defer R83+.
        val firePatternId: Int = when (variant) {
            MidBossType.OFFENSIVE -> 0
            MidBossType.DEFENSIVE -> 1
            MidBossType.SWARM -> 2
            MidBossType.HEN_MOTHER -> 2          // spread (egg cluster proxy)
            MidBossType.BUFFALO_RAGE -> 0        // aimed (horn throw proxy)
            MidBossType.DUMB_RAT -> 0            // aimed laser
            MidBossType.FIERCE_TIGER -> 1        // barrage roar
            MidBossType.SEXY_DIVA -> 2           // spread hair
            MidBossType.TROLL_TOWER -> 0         // aimed projectile
            MidBossType.TWIN_SUMMITS -> 0        // dual high-dmg aimed
            MidBossType.VOID_GLOBES -> 1         // cluster barrage
            MidBossType.WHITE_DRAGON -> 1        // fire breath barrage
            MidBossType.HAMMER_SICKLE -> 2       // hammer+sickle spread
            MidBossType.MONEY_TYCOON -> 2        // money bill spread
            MidBossType.GOLDEN_TYCOON -> 2       // dollar bill spread
        }
        return when (firePatternId) {
            0 -> if (phase2) tripleSpreadLasers(ship) else listOf(aimedLaser(ship))
            1 -> if (phase2) barrageLasers() else listOf(aimedLaser(ship))
            2 -> if (phase2) tripleSpreadLasers(ship) else listOf(aimedLaser(ship))
            else -> listOf(aimedLaser(ship))
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
