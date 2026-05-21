package com.tranphuloi.neon.ui.game.booster

import androidx.annotation.DrawableRes
import com.tranphuloi.neon.R

enum class BoosterType(
    @DrawableRes val drawableId: Int,
    val weight: Int,
) {
    ULTIMATE_WEAPON_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 19),
    SHIELD_BOOSTER(R.drawable.booster_shield, weight = 19),
    HEALTH_BOOSTER(R.drawable.booster_health, weight = 19),
    LASER_BOOSTER(R.drawable.booster_red_lasers, weight = 19),
    TRIPLE_LASER_BOOSTER(R.drawable.booster_triple_laser, weight = 19),
    // Auto-revive: rare 5% drop. Picking it up while alive stores the token; when ship
    // hp hits 0 the token is consumed automatically (1× per game).
    REVIVE_TOKEN(R.drawable.booster_revive, weight = 5),
    // Round 35 (35x) — bullet-type boosters. Activate ship.activeBulletType for
    // 10s. Reuse existing drawable assets (booster_red_lasers / booster_ultimate_weapon)
    // since we don't have dedicated icons for these yet.
    PIERCING_BOOSTER(R.drawable.booster_red_lasers, weight = 8),
    PLASMA_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 8),
}
