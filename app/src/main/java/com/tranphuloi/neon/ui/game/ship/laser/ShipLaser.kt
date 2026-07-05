package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

@Keep
data class ShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = SHIP_LASER_WIDTH,
    // Round 71 (Issue 4a) — explicit bulletType to surface unique vector shape
    // trong LaserCanvas. Default NORMAL = capsule. Stub bullets (FIRE/HOMING/
    // SMOKE/ZIGZAG/KAMEHAMEHA/ATOMIC/SPLIT) pass actual type cho visual diff.
    override val bulletType: BulletType = BulletType.NORMAL,
    // Task 15 — đưa vào ctor (giữ default 0/7) để Airburst đặt vận tốc per-shard.
    override val xOffsetMovementSpeed: Float = 0f,
    override val yOffsetMovementSpeed: Float = 7f,
) : Laser {

    override var height: Float = 20f
    override var rotation: Float = 0f
    override var impactPower: Float = 25f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
        xOffset += xOffsetMovementSpeed   // Task 15 — Airburst con bay ngang (đạn khác xSpeed=0)
    }

    companion object {
        const val SHIP_LASER_WIDTH: Float = 5f
    }
}
