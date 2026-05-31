package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 16 (Bullet Slice 3) — ZIGZAG bullet. Rises at standard speed while its
 * x weaves left↔right on a sine of distance travelled, so it sweeps a wide
 * column and can clip enemies a straight shot would miss. Destroyed on first
 * hit (collision arm); the signature is purely the weaving path here.
 */
@Keep
data class ZigZagShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = ShipLaser.SHIP_LASER_WIDTH,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 7f
    override var height: Float = 20f
    override var rotation: Float = 0f
    override var impactPower: Float = 22f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false
    override val bulletType: BulletType = BulletType.ZIGZAG

    /** Spawn x is the weave centre; xOffset oscillates ±[AMPLITUDE] around it. */
    private val centerX: Float = xOffset
    private var traveled: Float = 0f

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
        traveled += yOffsetMovementSpeed
        xOffset = centerX + (kotlin.math.sin((traveled / WAVELENGTH).toDouble()) * AMPLITUDE).toFloat()
    }

    companion object {
        private const val AMPLITUDE: Double = 34.0
        private const val WAVELENGTH: Float = 38f
    }
}
