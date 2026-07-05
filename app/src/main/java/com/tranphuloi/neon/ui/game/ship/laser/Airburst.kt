package com.tranphuloi.neon.ui.game.ship.laser

import kotlin.math.cos
import kotlin.math.sin

/**
 * Task 15 (đợt 4) — vận tốc đạn con AIRBURST: quạt RỘNG hướng LÊN (25°..155°).
 * Tất cả đều có vy>0 (thành phần bay lên) để cull off-top hoạt động (đạn ship chỉ
 * cull khi yOffset < top; ngang/xuống sẽ leak). Pure (test JVM).
 * Trả (xOffsetMovementSpeed, yOffsetMovementSpeed) khớp ShipLaser.moveLaser
 * (yOffset -= ySpeed nên dương = lên; xOffset += xSpeed).
 */
object Airburst {
    const val CHILD_COUNT = 8
    const val CHILD_SPEED = 6f
    private const val START_DEG = 25.0
    private const val END_DEG = 155.0

    fun velocities(count: Int = CHILD_COUNT, speed: Float = CHILD_SPEED): List<Pair<Float, Float>> {
        val a0 = Math.toRadians(START_DEG)
        val a1 = Math.toRadians(END_DEG)
        return (0 until count).map { i ->
            val t = if (count <= 1) 0.5 else i.toDouble() / (count - 1)
            val a = a0 + (a1 - a0) * t
            (cos(a).toFloat() * speed) to (sin(a).toFloat() * speed)
        }
    }
}
