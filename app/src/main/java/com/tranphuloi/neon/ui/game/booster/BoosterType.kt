package com.tranphuloi.neon.ui.game.booster

import androidx.annotation.DrawableRes
import com.tranphuloi.neon.R

enum class BoosterType(@DrawableRes val drawableId: Int) {
    ULTIMATE_WEAPON_BOOSTER(R.drawable.booster_ultimate_weapon),
    SHIELD_BOOSTER(R.drawable.booster_shield),
    HEALTH_BOOSTER(R.drawable.booster_health),
    LASER_BOOSTER(R.drawable.booster_red_lasers),
    TRIPLE_LASER_BOOSTER(R.drawable.booster_triple_laser)
}
