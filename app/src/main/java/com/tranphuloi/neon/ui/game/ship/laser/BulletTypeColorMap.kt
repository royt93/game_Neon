package com.tranphuloi.neon.ui.game.ship.laser

import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper

/**
 * Single source of truth for BulletType → ARGB color identity. Pairs each
 * BulletType with the matching BoosterType's tint constant so player sees
 * one consistent color whether they look at the bullet preview, pick up
 * the booster, or read the activation popup.
 *
 * `NORMAL` has no booster origin — uses a neutral cyan from the theme.
 *
 * Callers should prefer this over inline `when` tables to avoid the hex
 * literal drift class of bug (where hardcoded hex shifts independently
 * from the mapper constant).
 */
object BulletTypeColorMap {

    /** Theme cyan reused for NORMAL bullet (no booster origin). */
    const val NORMAL_ARGB: Long = 0xFF00F0FFL

    fun argbFor(b: BulletType): Long = when (b) {
        BulletType.NORMAL -> NORMAL_ARGB
        BulletType.PIERCING -> BoosterToBoosterUIMapper.PIERCING_TINT_ARGB
        BulletType.PLASMA -> BoosterToBoosterUIMapper.PLASMA_TINT_ARGB
        BulletType.FIRE -> BoosterToBoosterUIMapper.FIRE_TINT_ARGB
        BulletType.HOMING -> BoosterToBoosterUIMapper.HOMING_TINT_ARGB
        BulletType.BOUNCE -> BoosterToBoosterUIMapper.BOUNCE_TINT_ARGB
        BulletType.GIANT -> BoosterToBoosterUIMapper.GIANT_TINT_ARGB
        BulletType.SMOKE -> BoosterToBoosterUIMapper.SMOKE_TINT_ARGB
        BulletType.ZIGZAG -> BoosterToBoosterUIMapper.ZIGZAG_TINT_ARGB
        BulletType.KAMEHAMEHA -> BoosterToBoosterUIMapper.KAMEHAMEHA_TINT_ARGB
        BulletType.ATOMIC -> BoosterToBoosterUIMapper.ATOMIC_TINT_ARGB
        BulletType.SPLIT -> BoosterToBoosterUIMapper.SPLIT_TINT_ARGB
        // Wave 16 — đạn trào phúng (không có booster origin → literal riêng).
        BulletType.LOTTERY -> 0xFFFFC400L            // gold-festive (vé số)
        BulletType.FIREWORK -> 0xFFFF4FA3L           // hot pink (pháo hoa)
        BulletType.BRICK -> 0xFFC8714AL              // gạch nung (terracotta)
        // Wave 16 batch 2 — đạn trào phúng (literal, không booster origin).
        BulletType.BANH_MI -> 0xFFE0A33CL            // vàng nâu bánh mì
        BulletType.DURIAN -> 0xFFB6D43AL             // vàng-xanh sầu riêng
        BulletType.HEART -> 0xFFFF5C8AL              // hồng tim
        // Wave 18 batch 3 — đạn trào phúng (literal, không booster origin).
        BulletType.BUBBLE_TEA -> 0xFFE8C9A0L         // be trà sữa
        BulletType.FISH_SAUCE -> 0xFF9C5A1EL         // nâu cánh gián nước mắm
        BulletType.SANDAL -> 0xFF3E7BFFL             // xanh dương dép tổ ong
        BulletType.QR_CODE -> 0xFF2CE66BL            // xanh lá máy quét
    }
}
