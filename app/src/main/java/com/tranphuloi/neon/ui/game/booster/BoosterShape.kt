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
    // PLUS_DOUBLE removed Round 79 audit — QUICK_HEAL now maps to HEALING_FLASK.
    SHARD,           // crystal shard (MINERAL_SUPERCHARGE)
    AURA_RING,       // soft ring (HEALING_AURA)
    PHASE_DIAMOND,   // ghost diamond (PHASE_SHIELD)
    CLOUD_PUFF,      // smoke cloud (SMOKE_BOOSTER)
    SPREAD_FAN,      // 3-arrow fan (SPREAD_SHOT)
    CRYSTAL_SPARK,   // 4-point spark (CRIT_SURGE)
    DOUBLE_ARROW,    // 2 chevrons (DOUBLE_FIRE)
    ARROW_CYCLE,     // cyclic arrow (BOUNCE_BOOSTER)
    BIG_DOT,         // heavy disc (GIANT_BOOSTER)

    // ── Round 79 audit fix (dup elimination) ──
    RAGE_FANG,       // jagged fang/teeth — BERSERK (was LIGHTNING dup w/ ZIGZAG)
    HEALING_FLASK,   // potion bottle — QUICK_HEAL (was PLUS_DOUBLE dup w/ CROSS)

    // ── Wave 11a — 3 new shapes ──
    REGEN_PULSE,     // pulsing pill — REGEN_BOOSTER (slow passive heal)
    FREEZE_FLAKE,    // 6-arm snowflake — TIME_FREEZE_BOOSTER
    MINI_RING,       // small ring + arrow — MINI_BOOSTER (shrink+speed)

    // ── Wave 11a Phase 2 — 2 more shapes ──
    VAMPIRE_FANG,    // 2 downward fangs + drop — VAMPIRE_BOOSTER (lifesteal)
    GHOST_TRAIL,     // dashed circle + trailing dots — GHOST_BOOSTER (intangible)

    // ── Wave 11a Phase 3 — 3 more shapes ──
    GRAVITY_WELL,    // concentric spiraling arrows pointing in — GRAVITY_BOOSTER
    REFLECT_BUMPER,  // shield arc + bounce arrow — REFLECT_BOOSTER
    CHAIN_BOLT,      // 3-node lightning chain — CHAIN_LIGHTNING_BOOSTER

    // ── Wave 11a Phase 4 ──
    CLONE_PAIR,      // 2 small ship silhouettes side-by-side — CLONE_BOOSTER

    // ── Task 01 (Slice 4) ──
    DRONE_ROTOR,     // diamond + orbit ring — DRONE_BOOSTER (drone companion)
}
