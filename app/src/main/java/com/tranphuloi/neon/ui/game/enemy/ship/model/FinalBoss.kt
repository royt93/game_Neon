package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 34d Final boss type marker. */
object FinalBossType : EnemyType(spawnRate = Once)

/**
 * 34d Final boss — 3 phases on a single HP pool of 22500.
 *
 *   Phase 1 (HP 22500..15000) — basic patrol + aimed laser, 1 laser/sec
 *   Phase 2 (HP 15000..7500)  — figure-8 patrol + 3-spread laser, 1.5 laser/sec
 *   Phase 3 (HP 7500..0)      — fast patrol + ring barrage (8 directions), 2 laser/sec
 *
 * Phase transitions trigger a brief invulnerability window (300ms) + flash via
 * `phaseTransitionMillis` that GameScreen reads to show "PHASE 2/3" cinematic banner.
 */
@Keep
data class FinalBoss(
    private val screenWidth: Float,
    private val screenHeight: Float,
    // Crash fix — lambda capture không Serializable; Bundle instance-state
    // save (dumpStats khi Activity bị che khuất giữa combat boss) từng
    // NotSerializableException → BadParcelableException crash toàn app.
    @Transient
    private val getShip: () -> Ship,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    override val width: Float = 240f
    override val height: Float = 140f
    override var hp: Float = TOTAL_HP
    override val initialHp: Float = TOTAL_HP
    override val impactPower: Float = 15f
    override val minerals: Int = 25
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override val drawableId: Int = R.drawable.enemy_green_boss   // reuse — palette tinted
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = true
    // Round 71 (Issue 4d) — SPIDER silhouette (8 legs, FinalBoss).
    override val bossKind: BossKind = BossKind.SPIDER
    // Round 84 audit — Vietnamese name via BossKind.displayName.
    override val displayName: String = bossKind.displayName

    override var xOffset: Float = (screenWidth - width) / 2f
    override var yOffset: Float = -height
    private val entryTargetY: Float = 110f
    private val entrySpeed: Float = 2.0f
    override val isInEntryPhase: Boolean get() = yOffset < entryTargetY

    private var knockbackVel: Float = 0f
    private var movementTime: Float = 0f

    private var phase1Engaged: Boolean = true                    // initial state
    private var phase2Engaged: Boolean = false
    private var phase3Engaged: Boolean = false

    /** Latest phase-transition wall-clock for cinematic banner trigger. */
    override var phaseTransitionMillis: Long = 0L
        private set

    /** Brief i-frames after a phase change (300ms grace so player sees cinematic). */
    private var iframeUntilMillis: Long = 0L

    /** Phase index 1..3 derived from hp. */
    override val currentPhase: Int
        get() = when {
            hp <= TOTAL_HP - 15000f -> 3
            hp <= TOTAL_HP - 7500f -> 2
            else -> 1
        }

    /** Internal alias for readability inside this class. */
    private val phase: Int get() = currentPhase

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

        movementTime += 1f
        when (phase) {
            1 -> {
                // Slow horizontal patrol.
                val cycle = (movementTime / 1500f) % 2f
                val phaseT = if (cycle < 1f) cycle else 2f - cycle
                xOffset = phaseT * (screenWidth - width)
            }

            2 -> {
                // Figure-8 motion across mid-area.
                val t = movementTime / 350f
                val cx = (screenWidth - width) / 2f
                xOffset = cx + sin(t.toDouble()).toFloat() * 130f
                yOffset = entryTargetY + cos((t * 2.0)).toFloat() * 40f
            }

            3 -> {
                // Aggressive: bouncing horizontal at high speed + slight vertical bob.
                val t = movementTime / 200f
                val cx = (screenWidth - width) / 2f
                xOffset = cx + sin(t.toDouble()).toFloat() * 150f
                yOffset = entryTargetY + sin((t * 3.0)).toFloat() * 25f
            }
        }

        xOffset = xOffset.coerceIn(0f, screenWidth - width)

        if (knockbackVel != 0f) {
            yOffset += knockbackVel
            knockbackVel *= 0.94f                                // less knockback than smaller bosses
            if (kotlin.math.abs(knockbackVel) < 0.05f) knockbackVel = 0f
        }

        // Phase transition gates — fire once each.
        if (!phase2Engaged && hp <= TOTAL_HP - 7500f) {
            phase2Engaged = true
            phaseTransitionMillis = System.currentTimeMillis()
            iframeUntilMillis = phaseTransitionMillis + 300L
        }
        if (!phase3Engaged && hp <= TOTAL_HP - 15000f) {
            phase3Engaged = true
            phaseTransitionMillis = System.currentTimeMillis()
            iframeUntilMillis = phaseTransitionMillis + 300L
        }

        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    override fun generateLasers(): List<Laser> {
        val ship = getShip()
        return when (phase) {
            1 -> listOf(aimedLaser(ship))
            2 -> tripleSpread(ship)
            3 -> ringBarrage()
            else -> emptyList()
        }
    }

    private fun aimedLaser(ship: Ship): Laser {
        val w = 32f
        val dx = ship.xOffset - xOffset
        val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        return EnemyLaser(
            xOffset = xOffset + width / 2 - w / 2,
            yOffset = yOffset + height,
            yRange = screenHeight,
            width = w,
            height = w,
            xOffsetMovementSpeed = (dx / dy) * 0.85f,
            yOffsetMovementSpeed = 0.85f,
            drawableId = R.drawable.ic_laser_red_8,
        )
    }

    private fun tripleSpread(ship: Ship): List<Laser> {
        val w = 28f
        val dx = ship.xOffset - xOffset
        val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        return listOf(-0.45f, 0f, 0.45f).map { off ->
            EnemyLaser(
                xOffset = xOffset + width / 2 - w / 2,
                yOffset = yOffset + height,
                yRange = screenHeight,
                width = w,
                height = w,
                xOffsetMovementSpeed = (dx / dy + off) * 0.75f,
                yOffsetMovementSpeed = 0.85f,
                drawableId = R.drawable.ic_laser_red_8,
            )
        }
    }

    /**
     * Phase 3 ring barrage — lasers fanning outward.
     * IMPORTANT: filter out upward-traveling lasers. EnemyLasersController.processLasers
     * only destroys lasers when `yOffset > screenHeight`, so a laser with negative
     * y-velocity never exits and would leak forever (yOffset goes to -∞). We bias
     * downward (+0.25) and skip any laser with effective ySpeed < 0.15 (fully horizontal
     * + slightly upward arcs are dropped). Result: 5-6 lasers per ring instead of 8,
     * all guaranteed to leave the screen bottom.
     */
    private fun ringBarrage(): List<Laser> {
        val w = 26f
        val cx = xOffset + width / 2
        val cy = yOffset + height / 2
        return (0 until 8).mapNotNull { i ->
            val angle = (i * (2.0 * PI / 8.0)).toFloat()
            val ySpeed = cos(angle) * 0.9f + 0.25f
            if (ySpeed < 0.15f) return@mapNotNull null
            EnemyLaser(
                xOffset = cx - w / 2,
                yOffset = cy - w / 2,
                yRange = screenHeight,
                width = w,
                height = w,
                xOffsetMovementSpeed = sin(angle) * 0.9f,
                yOffsetMovementSpeed = ySpeed,
                drawableId = R.drawable.ic_laser_red_8,
            )
        }
    }

    override fun onObjectImpact(impactPower: Float) {
        // Honour i-frames during phase transitions.
        if (System.currentTimeMillis() < iframeUntilMillis) return
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        if (!isInEntryPhase) {
            knockbackVel = (knockbackVel - 0.8f).coerceAtLeast(-1.8f)
        }
    }

    companion object {
        const val TOTAL_HP: Float = 22500f
    }
}
