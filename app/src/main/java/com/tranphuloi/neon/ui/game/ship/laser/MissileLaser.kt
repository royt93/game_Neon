package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 6 (29x) round 40 — homing missile fired by the secondary-weapon button.
 *
 * Behavior:
 *  - Flies upward (yOffset decreases) at [yOffsetMovementSpeed].
 *  - Each tick, [LasersController.processShipLasers] writes the nearest enemy's
 *    x-position into [targetX] (or null if no enemy is alive). [moveLaser]
 *    nudges [xOffset] toward [targetX] by at most [HOMING_X_STEP] per tick — a
 *    soft homing rather than a hard lock so it still misses distant evasive
 *    enemies (good game feel).
 *  - Single-hit (NORMAL bullet type) but higher impactPower than a ship laser.
 *  - Mild rotation toward target so the sprite visibly tilts in flight.
 */
@Keep
data class MissileLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = 8f,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 9f                  // faster than normal laser (7) — feels like a rocket
    override var height: Float = 28f
    override var rotation: Float = 0f
    override var impactPower: Float = 60f                          // 2.4× a normal 25-power shot
    override val drawableId: Int = R.drawable.ic_laser_blue_7      // reuse — distinct from cyan via tint at render-site (round 41)
    override var destroyed: Boolean = false
    // Round 71 fix (Issue 4a audit) — surface bulletType cho LaserCanvas
    // dispatch unique vector shape (HOMING = capsule + targeting ring).
    // Secondary weapon MISSILE cũng dùng class này → render giống HOMING (OK
    // vì cả 2 đều là "homing missile" visually).
    override val bulletType: BulletType = BulletType.HOMING

    /**
     * X-position of the missile's current target. `null` when no enemy is alive
     * (missile flies straight up). Set externally each tick from LasersController.
     */
    var targetX: Float? = null

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
        // Homing nudge: clamp dx so the missile turns smoothly rather than snapping.
        targetX?.let { tx ->
            val dx = tx - xOffset
            val step = dx.coerceIn(-HOMING_X_STEP, HOMING_X_STEP)
            xOffset += step
            // Rotate to face direction of travel (bounded so sprite stays readable).
            rotation = (step / HOMING_X_STEP) * MAX_ROTATION_DEG
        }
    }

    companion object {
        /** Max pixels per tick the missile can adjust horizontally. */
        const val HOMING_X_STEP: Float = 4f

        /** Max rotation degrees applied when at full lateral nudge. */
        const val MAX_ROTATION_DEG: Float = 25f
    }
}
