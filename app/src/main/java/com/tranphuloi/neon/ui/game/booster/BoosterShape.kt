package com.tranphuloi.neon.ui.game.booster

/**
 * Round 78 spec follow-up — replaces the prior `shapeKey: String?` magic-string
 * pattern with a type-safe enum. Compiler catches typos, IDE provides
 * autocomplete + refactor support, exhaustive `when` over `BoosterShape` warns
 * if a new value is added but not handled in [BoosterCanvas].
 *
 * Single source of truth for booster visual identity. [BoosterToBoosterUIMapper]
 * maps each [BoosterType] → [BoosterShape]; [BoosterCanvas] dispatches solely on
 * this value (no more two-tier `shapeKey ?: drawableId` lookup).
 *
 * The 6 BASE values (CROSS … HEART) correspond to the original drawable-id
 * dispatch; the rest are Round 78 additions to remove duplicate-shape issues.
 */
enum class BoosterShape {
    // ── Base 6 (Round 66b) ──
    CROSS,           // medical + (HEALTH_BOOSTER)
    OCTAGON,         // shield silhouette (SHIELD_BOOSTER)
    TRIANGLE_UP,     // arrow up (LASER_BOOSTER)
    TRIPLE_BARS,     // 3 vertical bars (TRIPLE_LASER_BOOSTER)
    STAR,            // 5-point star (ULTIMATE_WEAPON_BOOSTER)
    HEART,           // life heart (REVIVE_TOKEN)

    // ── Round 78 additions (#3 spec follow-up) ──
    ATOM,            // electron orbits (ATOMIC_BOOSTER)
    FLAME,           // fire petal (FIRE_BOOSTER)
    MAGNET,          // U-magnet horseshoe (MAGNET_BOOST)
    LIGHTNING,       // zigzag bolt (BERSERK, ZIGZAG_BOOSTER)
    CROSSHAIR,       // target reticle (HOMING_BOOSTER)
    BEAM,            // horizontal beam (KAMEHAMEHA_BOOSTER)
    SPLIT_FORK,      // Y-fork branch (SPLIT_BOOSTER)
    ARROW_RIGHT,     // → chevron (PIERCING_BOOSTER)
    RING_PULSE,      // 3 concentric rings (PLASMA_BOOSTER)
    DOLLAR,          // $ glyph (SCORE_X3)
    PLUS_DOUBLE,     // double + (QUICK_HEAL)
    SHARD,           // crystal shard (MINERAL_SUPERCHARGE)
    AURA_RING,       // soft ring (HEALING_AURA)
    PHASE_DIAMOND,   // ghost diamond (PHASE_SHIELD)
    CLOUD_PUFF,      // smoke cloud (SMOKE_BOOSTER)
    SPREAD_FAN,      // 3-arrow fan (SPREAD_SHOT)
    CRYSTAL_SPARK,   // 4-point spark (CRIT_SURGE)
    DOUBLE_ARROW,    // 2 chevrons (DOUBLE_FIRE)
    ARROW_CYCLE,     // cyclic arrow (BOUNCE_BOOSTER)
    BIG_DOT,         // heavy disc (GIANT_BOOSTER)
}
