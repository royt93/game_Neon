package com.tranphuloi.neon.ui.game.drone

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Task 01 — Drone companion. Bay quanh tàu người chơi (quỹ đạo tròn), tự bắn địch
 * gần nhất, CÓ HP (trúng đạn/địch thì vỡ). Domain thuần, immutable — controller
 * cập nhật bằng copy. Sống sót qua rememberSaveable nhờ Serializable.
 */
@Keep
@Immutable
data class Drone(
    val id: String,
    val xOffset: Float,
    val yOffset: Float,
    /** Góc hiện tại trên quỹ đạo quanh tàu (radian). */
    val orbitAngle: Float,
    val hp: Int = MAX_HP,
    val lastFireMillis: Long = 0L,
    val size: Float = 44f,
) : Serializable {
    val destroyed: Boolean get() = hp <= 0

    companion object {
        const val MAX_HP = 30
        /** Bán kính quỹ đạo quanh tàu (px). */
        const val ORBIT_RADIUS = 120f
        /** Tốc độ quay quỹ đạo (radian mỗi bước orbit). */
        const val ORBIT_SPEED = 0.06f
        /** Khoảng cách tối thiểu giữa 2 lần bắn của một drone (ms). */
        const val FIRE_INTERVAL_MS = 700L
        /** Sát thương mỗi phát đạn drone (yếu hơn tàu). */
        const val SHOT_DAMAGE = 40f
    }
}

/** Ý định bắn 1 phát từ drone tại (x,y) nhắm tới địch (targetX,targetY). */
data class DroneShot(
    val fromX: Float,
    val fromY: Float,
    val targetX: Float,
    val targetY: Float,
)

/**
 * Task 01 (Slice 6) — 1 cú va chạm drone↔đạn địch tại tâm drone (x,y).
 * [destroyed] = true khi cú này khiến drone vỡ (HP≤0) → GameState nổ + haptic.
 */
data class DroneHit(
    val x: Float,
    val y: Float,
    val destroyed: Boolean,
)
