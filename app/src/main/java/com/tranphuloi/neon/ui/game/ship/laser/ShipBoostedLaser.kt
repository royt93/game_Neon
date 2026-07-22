package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

@Keep
data class ShipBoostedLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = SHIP_BOOSTED_LASER_WIDTH,
    // Round 71 (Issue 4a) — explicit bulletType cho stub bullets dùng
    // ShipBoostedLaser fallback khi `laserBoosterEnabled`.
    override val bulletType: BulletType = BulletType.NORMAL,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 5f
    override var height: Float = 25f
    override var rotation: Float = 0f
    override var impactPower: Float = 100f
    override val drawableId: Int = R.drawable.ic_laser_red_16
    override var destroyed: Boolean = false
    // Task 24 — real backing field so PIERCE_CHANCE skill node can grant a
    // NORMAL bullet 1 extra pierce (interface default setter is a no-op).
    override var pierceRemaining: Int = 0

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
    }

    companion object {
        const val SHIP_BOOSTED_LASER_WIDTH: Float = 8f
    }
}
