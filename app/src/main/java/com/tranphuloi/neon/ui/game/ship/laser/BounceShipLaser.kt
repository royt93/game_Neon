package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Round 67 (Wave 10a) — BOUNCE bullet laser. Ricochets off left/right screen
 * edges, can hit up to 3 enemies before being destroyed.
 *
 * Movement: yOffset rises at standard speed, xOffset shifts each tick by
 * xVelocity. When xOffset hits left edge (≤0) or right edge (≥screenWidth),
 * xVelocity flips sign — classic billiard bounce.
 *
 * Initial xVelocity = ±3 (random sign at spawn) so the very first frame
 * already shows visible angular motion.
 */
@Keep
data class BounceShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    private val screenWidth: Float,
    override var width: Float = ShipLaser.SHIP_LASER_WIDTH,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 7f
    override var height: Float = 20f
    override var rotation: Float = 0f
    override var impactPower: Float = 25f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false

    /** Lateral velocity for ricochet — flipped on edge hit. */
    var xVelocity: Float = if (kotlin.random.Random.nextBoolean()) 3f else -3f

    /** Hits remaining before destroy. Starts at 3 (Round 67 baseline). */
    var hitsRemaining: Int = 3

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
        xOffset += xVelocity
        // Bounce off screen edges. Inset by width so the body stays fully
        // visible at the bounce moment.
        if (xOffset <= 0f) {
            xOffset = 0f
            xVelocity = kotlin.math.abs(xVelocity)
        } else if (xOffset + width >= screenWidth) {
            xOffset = screenWidth - width
            xVelocity = -kotlin.math.abs(xVelocity)
        }
    }
}
