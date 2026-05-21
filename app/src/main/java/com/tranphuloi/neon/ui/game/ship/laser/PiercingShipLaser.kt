package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 4 (35x) round 35 — piercing laser. Passes through enemies, max 3 hits
 * before destroying. Uses red laser drawable to distinguish from normal cyan.
 */
@Keep
data class PiercingShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = 6f,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 7f
    override var height: Float = 22f
    override var rotation: Float = 0f
    override var impactPower: Float = 28f                   // slightly more than normal (compensates for being rarer)
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false

    override val bulletType: BulletType = BulletType.PIERCING

    /** Mutable hit counter — decremented in LasersController on each hit. */
    override var pierceRemaining: Int = BulletType.PIERCING.pierceCount

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
    }
}
