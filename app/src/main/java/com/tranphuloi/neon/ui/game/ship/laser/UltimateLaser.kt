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
    // Pixel-3 round 4 — slow beam from 7 → 5 px/tick to dwell longer at
    // bottom band (~1.2s visible instead of ~0.7s) at FAR camera zoom.
    // Total sweep ~10s instead of 7.2s. User reported "splash xanh không
    // phủ full screen" partly because beam moved through bottom too fast.
    override val yOffsetMovementSpeed: Float = 5f
    override var width: Float = 30f
    // Pixel-3 round 4 — taller beam (30 → 60 dp) so visual "vùng effect"
    // feels heavier + covers more vertical area per frame. Combined with
    // slower speed, bottom band sees beam-coverage for ~1.5s post-fire.
    override var height: Float = 60f
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
