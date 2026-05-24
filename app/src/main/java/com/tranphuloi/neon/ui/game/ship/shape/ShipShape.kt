package com.tranphuloi.neon.ui.game.ship.shape

import androidx.compose.runtime.Immutable

/**
 * Round 68 (Wave 8) — Ship shape variants. Each shape có stat profile riêng
 * + vector recipe khác (deferred to Round 69+). Behaviors stub trong Round 68:
 * tất cả render về Fighter shape (drawShipVector hiện tại) but stats múliplier
 * áp dụng đúng qua effectiveStats.
 *
 * Unlocking: lifetime minerals threshold per shape. FIGHTER là default
 * unlocked. Round 69+ implement unlock check + ShipPickerScreen UI.
 */
@Immutable
enum class ShipShape(
    val key: String,
    val displayName: String,
    val unlockMinerals: Int,
    val hpMul: Float,
    val speedMul: Float,
    val damageMul: Float,
) {
    FIGHTER(key = "fighter", displayName = "Tiêm Kích", unlockMinerals = 0,
        hpMul = 1f, speedMul = 1f, damageMul = 1f),
    BOMBER(key = "bomber", displayName = "Oanh Tạc", unlockMinerals = 1000,
        hpMul = 1.25f, speedMul = 0.90f, damageMul = 1.1f),
    STEALTH(key = "stealth", displayName = "Tàng Hình", unlockMinerals = 2500,
        hpMul = 0.85f, speedMul = 1.20f, damageMul = 1f),
    TANK(key = "tank", displayName = "Tăng", unlockMinerals = 5000,
        hpMul = 1.50f, speedMul = 0.75f, damageMul = 0.95f),
    INTERCEPTOR(key = "interceptor", displayName = "Đánh Chặn", unlockMinerals = 10000,
        hpMul = 0.80f, speedMul = 1.30f, damageMul = 1.10f);

    companion object {
        fun fromKey(key: String?): ShipShape =
            entries.firstOrNull { it.key == key } ?: FIGHTER
    }
}
