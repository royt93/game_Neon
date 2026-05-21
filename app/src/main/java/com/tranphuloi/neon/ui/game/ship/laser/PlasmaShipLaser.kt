package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 4 (35x) round 35 — plasma laser. Larger bullet, +60% damage on impact,
 * spawns AoE explosion radius 80dp dealing 50% damage to nearby enemies.
 */
@Keep
data class PlasmaShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = PLASMA_WIDTH,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 5f           // slightly slower than normal (heavier projectile feel)
    override var height: Float = 34f
    override var rotation: Float = 0f
    override var impactPower: Float = 40f                   // 25 × 1.6 = 40 baseline
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false

    override val bulletType: BulletType = BulletType.PLASMA

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
    }

    companion object {
        const val PLASMA_WIDTH: Float = 16f
    }
}
