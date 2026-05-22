package com.tranphuloi.neon

import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Minimal Laser stub for JVM unit tests. Mirrors [TestEnemy] in shape.
 */
class TestLaser(
    override val id: String,
    override var xOffset: Float = 0f,
    override var yOffset: Float = 0f,
    override var width: Float = 4f,
    override var height: Float = 20f,
    override var rotation: Float = 0f,
    override var impactPower: Float = 25f,
    override val drawableId: Int = 0,
    override val xOffsetMovementSpeed: Float = 0f,
    override val yOffsetMovementSpeed: Float = 7f,
    override var destroyed: Boolean = false,
) : Laser {
    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
    }
}
