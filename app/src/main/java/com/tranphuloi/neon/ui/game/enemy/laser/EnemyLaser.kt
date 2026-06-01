package com.tranphuloi.neon.ui.game.enemy.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser
import java.util.*
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Wave 17 — quỹ đạo đạn địch. Trước chỉ TUYẾN TÍNH (xSpeed/ySpeed cố định) nên
 * mọi skill boss chung 1 cảm giác bay. Nay thêm 3 quỹ đạo phi tuyến để boss có
 * chiêu khác biệt thật:
 *  - [LaserMotion.LINEAR] bay thẳng theo vận tốc spawn (mặc định, giữ hành vi cũ).
 *  - [LaserMotion.HOMING] tự lái BÁM theo tàu (controller cập nhật target mỗi tick).
 *  - [LaserMotion.ACCEL]  GIA TỐC — nhanh dần (đòn "húc"/lao tới càng lúc càng gắt).
 *  - [LaserMotion.CURVE]  bay CONG — dao động ngang hình sin quanh hướng xuống.
 */
enum class LaserMotion { LINEAR, HOMING, ACCEL, CURVE }

@Keep
data class EnemyLaser(
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float,
    override var height: Float = 30f,
    override val xOffsetMovementSpeed: Float = 0f,
    override val yOffsetMovementSpeed: Float = 3.5f,
    override val drawableId: Int = R.drawable.ic_laser_red_14,
    override var impactPower: Float = 100f,
    /** Quỹ đạo. Mặc định LINEAR = hệt hành vi cũ. */
    val motion: LaserMotion = LaserMotion.LINEAR,
    /** HOMING — tốc độ lerp hướng về target (0..1; cao = bám gắt). */
    val homingTurn: Float = 0.05f,
    /** ACCEL — hệ số nhân tốc độ mỗi tick (>1). */
    val accelRate: Float = 1.035f,
    /** CURVE — biên độ dao động ngang (px, lệch 2 bên quanh tâm) + tần số sin. */
    val curveAmp: Float = 16f,
    val curveFreq: Float = 0.16f,
) : Laser {

    override val id: String = UUID.randomUUID().toString()
    override var rotation: Float = 0f
    override var destroyed: Boolean = false

    // ── Kinematics động (không thuộc data-identity; đạn không bị copy) ──
    private var vx: Float = xOffsetMovementSpeed
    private var vy: Float = yOffsetMovementSpeed
    private var age: Float = 0f

    /** HOMING — vị trí tàu; controller ghi mỗi tick TRƯỚC moveLaser. */
    var targetX: Float = xOffset
    var targetY: Float = yOffset + 600f

    private val baseSpeed: Float =
        hypot(xOffsetMovementSpeed, yOffsetMovementSpeed).coerceAtLeast(0.6f)

    /** CURVE — tâm trôi ngang; vị trí weave quanh tâm này (≠ tích phân vận tốc). */
    private var centerX: Float = xOffset

    override fun moveLaser() {
        age += 1f
        when (motion) {
            LaserMotion.LINEAR -> {
                xOffset += vx
                yOffset += vy
            }
            LaserMotion.HOMING -> {
                val dx = targetX - xOffset
                val dy = targetY - yOffset
                val len = hypot(dx, dy).coerceAtLeast(1f)
                val desiredVx = dx / len * baseSpeed
                val desiredVy = dy / len * baseSpeed
                vx += (desiredVx - vx) * homingTurn
                vy += (desiredVy - vy) * homingTurn
                xOffset += vx
                yOffset += vy
            }
            LaserMotion.ACCEL -> {
                vx = (vx * accelRate).coerceIn(-MAX_SPEED, MAX_SPEED)
                vy = (vy * accelRate).coerceIn(-MAX_SPEED, MAX_SPEED)
                xOffset += vx
                yOffset += vy
            }
            LaserMotion.CURVE -> {
                // Vị trí ngang = tâm (trôi theo xSpeed) ± biên độ sin → lệch ĐỀU
                // hai bên quanh tâm (tích phân vận tốc chỉ lệch 1 phía nên không
                // dùng). vy giữ nguyên → vẫn rơi xuống.
                centerX += xOffsetMovementSpeed
                yOffset += vy
                xOffset = centerX + sin(age * curveFreq) * curveAmp
            }
        }
    }

    companion object {
        /** Trần tốc độ để HOMING/ACCEL không vọt quá nhanh thành bất khả né. */
        private const val MAX_SPEED: Float = 4f
    }
}
