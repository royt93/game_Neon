package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.runtime.Immutable

/**
 * Wave 4 (35x) round 35 — bullet type the ship is currently firing.
 *
 * - NORMAL: existing ShipLaser / ShipBoostedLaser flow. Single-hit destroy on impact.
 * - PIERCING: laser passes through enemies, max 3 hits then destroy.
 * - PLASMA: bigger laser, +60% damage, AoE explosion radius 80dp on impact.
 *   Hit enemies in radius take 50% laser damage.
 *
 * Activated via booster pickup (PIERCING_BOOSTER / PLASMA_BOOSTER). Each
 * activation lasts [activeDurationMillis]. NORMAL is the default fallback.
 *
 * HOMING bullet (auto-target nearest enemy) was scoped but deferred — needs
 * per-tick target tracking via x/yVelocity refactor of Laser.
 */
@Immutable
@androidx.annotation.Keep
enum class BulletType(
    val displayName: String,
    val activeDurationMillis: Long,
    val damageMultiplier: Float,
    val pierceCount: Int,
    val aoeRadius: Float,
    val glyph: String,
    /**
     * Wave 12 round 3 — if non-null, this bullet is locked in
     * DialogLoadoutPicker until the matching ShopItem (by id) is purchased.
     * null = always available. Gate evaluated via `ShopItem.isShopUnlocked`.
     */
    val shopUnlockId: String? = null,
) {
    NORMAL(
        displayName = "Đạn thường",
        activeDurationMillis = 0L,                  // not timed; default
        damageMultiplier = 1f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "•",
    ),
    PIERCING(
        displayName = "Xuyên",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1f,
        pierceCount = 3,                            // hits up to 3 enemies before destroy
        aoeRadius = 0f,
        glyph = "→",
    ),
    PLASMA(
        displayName = "Plasma",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.6f,
        pierceCount = 0,
        aoeRadius = 80f,                            // damage radius on impact
        glyph = "◯",
        shopUnlockId = "bullet_plasma",            // Wave 14a — shop-gated (AoE premium)
    ),

    // Round 67 (Wave 10a) — 3 new bullet types with FULL behaviors implemented.
    // KAMEHAMEHA/ATOMIC/SPLIT/ZIGZAG/SMOKE deferred to Round 68+ (each requires
    // dedicated movement/spawn/AoE logic worth its own focused round).
    //
    // Picked the 3 simplest-to-implement behaviors for Round 67:
    //   FIRE   — reuses existing BURN status effect (Round 34) on hit
    //   HOMING — mirrors MissileLaser pattern (already proven Round 40)
    //   BOUNCE — adds bounceCount field + edge-detect in moveLaser
    FIRE(
        displayName = "Lửa",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.2f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "♨",                               // steam/heat symbol (Unicode, not emoji)
    ),
    HOMING(
        displayName = "Đuổi theo",
        activeDurationMillis = 10_000L,
        damageMultiplier = 0.8f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "◎",                               // bullseye target
    ),
    BOUNCE(
        displayName = "Phản xạ",
        activeDurationMillis = 12_000L,
        damageMultiplier = 0.7f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "⇄",                               // double-arrow bounce
    ),
    // Round 67.5 — GIANT bullet (đạn khổng lồ). User-listed in original vision
    // (msg Round 67). ×2 size visual + ×2 damage. Simpler than KAMEHAMEHA
    // (no charge-up mechanic). Reuses ShipLaser body via new GiantShipLaser
    // subclass at 2× dimensions.
    GIANT(
        displayName = "Khổng lồ",
        activeDurationMillis = 10_000L,
        damageMultiplier = 2f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "⬤",                               // large filled circle
        shopUnlockId = "bullet_giant",             // Wave 14a — shop-gated (×2 dmg premium)
    ),
    // Round 68 (Wave 10 finish) — 5 bullet types remaining. Stubs: enum
    // entries + dispatch + damage mul. UNIQUE BEHAVIORS deferred to Round 69+
    // (each behavior 1 dedicated round per bullet). Damage mul + duration
    // metadata real ngay từ Round 68.
    SMOKE(
        displayName = "Khói",
        activeDurationMillis = 10_000L,
        damageMultiplier = 0.8f,
        pierceCount = 0,
        aoeRadius = 60f,
        glyph = "❍",                               // outline circle (cloud-ish)
    ),
    ZIGZAG(
        displayName = "Zigzag",
        activeDurationMillis = 12_000L,
        damageMultiplier = 0.9f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "⌇",                               // wavy diagonal
    ),
    KAMEHAMEHA(
        displayName = "Kamehameha",
        activeDurationMillis = 8_000L,
        damageMultiplier = 3f,
        pierceCount = 99,
        aoeRadius = 0f,
        glyph = "⊛",                               // circled asterisk
        shopUnlockId = "bullet_kamehameha",        // Wave 12 round 3 — shop-gated
    ),
    ATOMIC(
        displayName = "Nguyên tử",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.5f,
        pierceCount = 0,
        aoeRadius = 150f,
        glyph = "⊙",                               // circled dot (nucleus)
        shopUnlockId = "bullet_atomic",            // Wave 12 round 3 — shop-gated
    ),
    SPLIT(
        displayName = "Phân tách",
        activeDurationMillis = 12_000L,
        damageMultiplier = 0.6f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "Ѱ",                               // psi (3-prong)
    ),

    // ── Wave 16 — đạn trào phúng batch 1 ──
    /** Vé Số — sát thương NGẪU NHIÊN mỗi phát (hên xui, từ 0.3× tới 3×). */
    LOTTERY(
        displayName = "Vé Số",
        activeDurationMillis = 12_000L,
        damageMultiplier = 1f,                      // base; mỗi viên random tại spawn
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "?",                                // ô số bí ẩn
    ),
    /** Pháo Hoa — nổ chùm AoE rộng khi trúng. */
    FIREWORK(
        displayName = "Pháo Hoa",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.1f,
        pierceCount = 0,
        aoeRadius = 130f,                           // nổ chùm rộng
        glyph = "✺",
    ),
    /** Cục Gạch (Nokia 1280) — to, chậm, nặng (nồi đồng cối đá). */
    BRICK(
        displayName = "Cục Gạch",
        activeDurationMillis = 12_000L,
        damageMultiplier = 2.2f,                    // nặng
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "▦",
    ),

    // ── Wave 16 — đạn trào phúng batch 2 ──
    /** Bánh Mì — giòn rụm, XUYÊN qua 3 địch (như PIERCING). */
    BANH_MI(
        displayName = "Bánh Mì",
        activeDurationMillis = 12_000L,
        damageMultiplier = 1f,
        pierceCount = 3,
        aoeRadius = 0f,
        glyph = "⊐",                                // ổ bánh mì (vector glyph)
    ),
    /** Sầu Riêng — nổ "mùi" AoE rộng khi trúng (nặng mùi). */
    DURIAN(
        displayName = "Sầu Riêng",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.2f,
        pierceCount = 0,
        aoeRadius = 110f,
        glyph = "✸",
    ),
    /** Like/Tim — thả tim TỰ ĐUỔI theo địch (như HOMING). */
    HEART(
        displayName = "Like/Tim",
        activeDurationMillis = 10_000L,
        damageMultiplier = 0.9f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "♡",
    );

    companion object {
        /**
         * Round 45 (36x) — parse by enum-name (matches DataStore persistence in
         * [com.tranphuloi.neon.data.SettingsKeys.PREFERRED_BULLET_TYPE]).
         * Falls back to [NORMAL] for null/unknown so old saves or corrupt
         * preferences resolve to the inert baseline.
         */
        fun fromName(name: String?): BulletType =
            entries.firstOrNull { it.name == name } ?: NORMAL

        /**
         * Round 52 (40x Item combos) — pierce count scales with the rarity of
         * the booster that activated PIERCING. Common 3 (baseline) → Rare 4
         * → Epic 5. Returns base [PIERCING.pierceCount] for non-PIERCING
         * activations (defensive — caller shouldn't ask).
         */
        fun pierceCountForRarity(rarity: com.tranphuloi.neon.ui.game.booster.BoosterRarity): Int =
            when (rarity) {
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.COMMON -> PIERCING.pierceCount
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.RARE -> PIERCING.pierceCount + 1
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.EPIC -> PIERCING.pierceCount + 2
            }

        /**
         * Round 52 (40x Item combos) — PLASMA AoE radius scales with the
         * rarity of the booster. Common 80px (baseline) → Rare 110px → Epic
         * 140px. Caller multiplies [PLASMA.aoeRadius] by the returned factor.
         */
        fun plasmaAoeMultiplierForRarity(rarity: com.tranphuloi.neon.ui.game.booster.BoosterRarity): Float =
            when (rarity) {
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.COMMON -> 1.0f
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.RARE -> 1.375f      // 80 → 110
                com.tranphuloi.neon.ui.game.booster.BoosterRarity.EPIC -> 1.75f       // 80 → 140
            }
    }
}
