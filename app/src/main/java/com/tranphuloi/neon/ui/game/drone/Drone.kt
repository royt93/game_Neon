package com.tranphuloi.neon.ui.game.drone

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Task 01 — Drone companion. Bay quanh tàu người chơi (quỹ đạo tròn), tự bắn địch
 * gần nhất, CÓ HP (trúng đạn/địch thì vỡ). Domain thuần, immutable — controller
 * cập nhật bằng copy. Sống sót qua rememberSaveable nhờ Serializable.
 */
/**
 * Task 06 — biến thể drone. ATTACK bắn địch; SHIELD không bắn nhưng HP gấp đôi +
 * chặn đạn địch (đệm); HEAL không bắn mà hồi máu tàu chậm.
 */
enum class DroneVariant {
    ATTACK, SHIELD, HEAL;

    companion object {
        fun fromKey(key: String?): DroneVariant =
            entries.firstOrNull { it.name == key } ?: ATTACK
    }
}

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
    /** Task 06 — loại drone (quyết định bắn/chặn/hồi + màu render). */
    val variant: DroneVariant = DroneVariant.ATTACK,
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

        // Task 06 — SHIELD trâu gấp đôi; HEAL hồi máu tàu chậm.
        const val SHIELD_HP_MULT = 2
        const val HEAL_INTERVAL_MS = 700L
        const val HEAL_AMOUNT = 1

        /** HP tối đa theo variant (SHIELD ×2). */
        fun maxHpFor(variant: DroneVariant): Int =
            if (variant == DroneVariant.SHIELD) MAX_HP * SHIELD_HP_MULT else MAX_HP
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
