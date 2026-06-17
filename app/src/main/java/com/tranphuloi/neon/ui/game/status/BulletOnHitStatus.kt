package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.ui.game.ship.laser.BulletType

/**
 * Wave 18 — SSOT thuần cho các [StatusEffect] mà một đạn GÂY RA KHI TRÚNG trực
 * tiếp (100%, khác với 10% random ở onLaserHit).
 *
 * Tách khỏi `GameState.onLaserHit` (lambda trong Composable, khó test) để logic
 * "đạn nào → status nào" có thể unit-test độc lập, và để thêm đạn mới chỉ sửa
 * 1 chỗ. KHÔNG bao gồm các hiệu ứng KHÔNG-phải-status (BANH_MI hồi máu, BRICK
 * hất văng) — những cái đó cần shipController/knockback nên giữ ở GameState.
 *
 * FIRE / ATOMIC cũng gây BURN nhưng được áp qua nhánh `if` riêng ở GameState
 * (lịch sử Round 67 / Wave 17) nên cố ý KHÔNG nằm ở đây để tránh áp 2 lần.
 */
object BulletOnHitStatus {

    fun statusEffectsFor(bulletType: BulletType): List<StatusEffect> = when (bulletType) {
        BulletType.DURIAN -> listOf(StatusEffect.SLOW)              // Sầu Riêng — làm chậm
        BulletType.HEART -> listOf(StatusEffect.STUN)               // Like/Tim — choáng
        BulletType.FISH_SAUCE -> listOf(StatusEffect.CORROSION)     // Nước Mắm — ăn mòn DoT
        BulletType.QR_CODE -> listOf(StatusEffect.SLOW, StatusEffect.STUN) // Mã QR — "đơ máy"
        else -> emptyList()
    }
}
