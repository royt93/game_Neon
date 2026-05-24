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
    // since we don't have dedicated icons for these yet. Round 54 added a
    // magenta/cyan tint + glyph badge in BoosterToBoosterUIMapper so player
    // can distinguish from LASER_BOOSTER / ULTIMATE_WEAPON_BOOSTER.
    //
    // Round 55 — bumped 8 → 12 each (combined 19.4%, was 13.8%). Empirically,
    // 93s of gameplay produced 0 PIERCING+PLASMA drops (P=3.4% RNG variance);
    // 19.4% combined means ~5 expected drops in 93s, enough to runtime-validate
    // round 52 rarity scaling and round 54 visual disambiguation. Adjust the
    // BoosterTypeTest weight-sum assertion if you tune further.
    PIERCING_BOOSTER(R.drawable.booster_red_lasers, weight = 12),
    PLASMA_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 12),

    // Round 60 (38x) — +10 support items closing Wave 4 Combat depth. All reuse
    // existing drawables + glyph badges + tints (Round 54 pattern) instead of
    // new art assets. Weight=6 each → +60 total, new total = 184. Each new
    // booster ≈ 3.3% drop rate; existing baseline (LASER/SHIELD/TRIPLE/HEALTH/
    // ULTIMATE) dilute from 15.3% → 10.3%, PIERCING/PLASMA from 9.7% → 6.5%.
    // Weight 6 keeps REVIVE_TOKEN (weight=5) the rarest drop — preserving the
    // existing `REVIVE_TOKEN is the rarest drop` invariant in BoosterTypeTest.
    MAGNET_BOOST(R.drawable.booster_shield, weight = 6),
    CRIT_SURGE(R.drawable.booster_red_lasers, weight = 6),
    SPREAD_SHOT(R.drawable.booster_triple_laser, weight = 6),
    BERSERK(R.drawable.booster_ultimate_weapon, weight = 6),
    PHASE_SHIELD(R.drawable.booster_shield, weight = 6),
    SCORE_X3(R.drawable.booster_health, weight = 6),
    QUICK_HEAL(R.drawable.booster_health, weight = 6),
    MINERAL_SUPERCHARGE(R.drawable.booster_health, weight = 6),
    HEALING_AURA(R.drawable.booster_health, weight = 6),
    DOUBLE_FIRE(R.drawable.booster_red_lasers, weight = 6),

    // Round 67 (Wave 10a) — 3 new bullet-type boosters with full behaviors.
    // Weight=6 each (REVIVE-rarest invariant preserved). +18 total weight,
    // new total 202. Each new booster ~3.0% drop rate.
    FIRE_BOOSTER(R.drawable.booster_red_lasers, weight = 6),
    HOMING_BOOSTER(R.drawable.booster_triple_laser, weight = 6),
    BOUNCE_BOOSTER(R.drawable.booster_red_lasers, weight = 6),
    // Round 67.5 — GIANT bullet booster. Total weight 202 → 208.
    GIANT_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 6),
    // Round 68 (Wave 10 finish) — 5 boosters mới trigger 5 bullets còn lại.
    // Total weight 208 → 238.
    SMOKE_BOOSTER(R.drawable.booster_shield, weight = 6),
    ZIGZAG_BOOSTER(R.drawable.booster_red_lasers, weight = 6),
    KAMEHAMEHA_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 6),
    ATOMIC_BOOSTER(R.drawable.booster_ultimate_weapon, weight = 6),
    SPLIT_BOOSTER(R.drawable.booster_red_lasers, weight = 6),
}
