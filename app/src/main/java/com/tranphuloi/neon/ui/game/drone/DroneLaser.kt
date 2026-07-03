package com.tranphuloi.neon.ui.game.drone

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.laser.LaserSource
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import kotlin.math.sqrt

/**
 * Task 01 (Slice 3) — đạn do drone bắn, chạy trong HỆ LASER CHUNG (được bơm vào
 * `shipLasers` của [com.tranphuloi.neon.ui.game.ship.laser.LasersController] nên
 * tái dùng collision + render + cleanup).
 *
 * Khác đạn tàu (bay thẳng lên): drone laser bay THEO HƯỚNG tới địch gần nhất
 * (vector vận tốc cố định lúc sinh). Tự đánh dấu [destroyed] khi ra ngoài màn để
 * không kẹt list nếu bay ngang/xuống (cull top-only của processShipLasers bỏ sót).
 *
 * [source] = DRONE để phân biệt nguồn; [bulletType] = NORMAL (huỷ khi trúng 1 lần).
 */
@Keep
data class DroneLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val vx: Float,
    private val vy: Float,
    private val screenWidth: Float,
    private val screenHeight: Float,
    override var impactPower: Float = Drone.SHOT_DAMAGE,
) : Laser {

    override var width: Float = WIDTH
    override var height: Float = HEIGHT
    override var rotation: Float = 0f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override val xOffsetMovementSpeed: Float = vx
    override val yOffsetMovementSpeed: Float = vy
    override var destroyed: Boolean = false
    override val source: LaserSource = LaserSource.DRONE
    override val bulletType: BulletType = BulletType.NORMAL

    override fun moveLaser() {
        xOffset += vx
        yOffset += vy
        if (xOffset < -OFF_SCREEN_MARGIN || xOffset > screenWidth + OFF_SCREEN_MARGIN ||
            yOffset < -OFF_SCREEN_MARGIN || yOffset > screenHeight + OFF_SCREEN_MARGIN
        ) {
            destroyed = true
        }
    }

    companion object {
        const val WIDTH: Float = 6f
        const val HEIGHT: Float = 16f

        /** Tốc độ bay (px/tick) — nhanh hơn đạn tàu chút để dễ trúng địch di chuyển. */
        const val SPEED: Float = 9f

        /** Ngưỡng ra-màn để tự huỷ (rộng để không cull sớm khi vừa bắn). */
        const val OFF_SCREEN_MARGIN: Float = 120f

        /**
         * Dựng 1 [DroneLaser] bay từ (fromX,fromY) hướng tới (targetX,targetY),
         * chuẩn hoá vận tốc về [SPEED]. Nếu trùng điểm (len≈0) → bắn thẳng lên.
         * Toạ độ spawn căn tâm đạn về điểm phát.
         */
        fun aimedAt(
            id: String,
            fromX: Float,
            fromY: Float,
            targetX: Float,
            targetY: Float,
            screenWidth: Float,
            screenHeight: Float,
        ): DroneLaser {
            val dx = targetX - fromX
            val dy = targetY - fromY
            val len = sqrt(dx * dx + dy * dy)
            val vx: Float
            val vy: Float
            if (len < 1e-3f) {
                vx = 0f
                vy = -SPEED
            } else {
                vx = dx / len * SPEED
                vy = dy / len * SPEED
            }
            return DroneLaser(
                id = id,
                xOffset = fromX - WIDTH / 2f,
                yOffset = fromY - HEIGHT / 2f,
                vx = vx,
                vy = vy,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
            )
        }
    }
}
