package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 16 (Bullet Slice 3) — SMOKE bullet. A slow, fat drifting puff that, on
 * impact, deals a small AoE ([BulletType.SMOKE].aoeRadius = 60) — routed
 * through the PLASMA/ATOMIC splash arm in [LasersController.monitorLaserCollision].
 * Trades rate/speed for area: good against clustered weak enemies.
 */
@Keep
data class SmokeShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = SMOKE_WIDTH,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 3.4f          // slow drift
    override var height: Float = SMOKE_WIDTH
    override var rotation: Float = 0f
    override var impactPower: Float = 20f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false
    override val bulletType: BulletType = BulletType.SMOKE

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
        rotation += 3f                                       // lazy tumbling puff
    }

    companion object {
        const val SMOKE_WIDTH: Float = 16f
    }
}
