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
    }
}
