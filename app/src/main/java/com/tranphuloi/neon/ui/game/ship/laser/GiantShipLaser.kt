package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Round 67.5 — GIANT bullet laser. User-listed "đạn khổng lồ" in the Wave 10
 * vision (missed from initial Round 67). ×2 size + ×2 damage compared to
 * NORMAL ShipLaser. No charge-up — simpler than KAMEHAMEHA (Round 68 plan).
 *
 * Movement: identical to ShipLaser (vertical up, 7 px/tick).
 * Damage mul applied via BulletType.damageMultiplier in GameState's
 * damageMultiplier lambda — `impactPower` here stays at NORMAL baseline so
 * future ship.activeBulletType.damageMultiplier × 2 stacks correctly.
 */
@Keep
data class GiantShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = ShipLaser.SHIP_LASER_WIDTH * 2f,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = 7f
    override var height: Float = 40f                                // ×2 the 20 of ShipLaser
    override var rotation: Float = 0f
    override var impactPower: Float = 25f                            // base; ×2 applied via BulletType mul
    override val drawableId: Int = R.drawable.ic_laser_blue_7        // legacy, unused (Round 66 vector)
    override var destroyed: Boolean = false
    // Round 71 fix (Issue 4a audit) — surface bulletType cho LaserCanvas dispatch.
    override val bulletType: BulletType = BulletType.GIANT
    // Wave 17 — "cày xuyên": GIANT giờ xuyên nhiều địch. Cần field THẬT (interface
    // mặc định pierceRemaining là no-op) để collision decrement được.
    override var pierceRemaining: Int = BulletType.GIANT.pierceCount

    override fun moveLaser() {
        yOffset -= yOffsetMovementSpeed
    }
}
