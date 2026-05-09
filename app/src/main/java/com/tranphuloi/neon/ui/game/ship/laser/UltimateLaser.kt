package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

@Keep
data class UltimateLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float = -10f,
    private val yRange: Float,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 7f
    override var width: Float = 30f
    override var height: Float = 30f
    override var rotation: Float = 0f
    override var impactPower: Float = 1000f
    override val drawableId: Int = R.drawable.ic_laser_blue_11
    override var destroyed: Boolean = false

    override fun moveLaser() {
        rotation += 7f
        if (rotation > 360f) rotation = 0f

        yOffset -= yOffsetMovementSpeed
    }
}
