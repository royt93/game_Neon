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
/**
 * Wave 17j — hình dáng vẽ của đạn (1:1 với hàm draw trong LaserCanvas). Tách
 * enum để model BulletType khai báo shape tường minh, không lẫn vào dispatch.
 */
enum class BulletShape {
    CAPSULE, NEEDLE, ORB, FLAME, HOMING_DART, RICOCHET, GIANT_DISC, PUFF,
    ZIGZAG, BEAM, ATOM, TRIDENT, TICKET, FIREWORK, BRICK, BAGUETTE, DURIAN, HEART,
}

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
        // Wave 17 — CƠ CHẾ RIÊNG: "cày xuyên". Trước GIANT chỉ là đạn to + ×2 dmg
        // (stat thuần, không cơ chế) → trùng cảm giác với mọi đạn thường to. Nay
        // xuyên 4 địch như xe lu cày qua đội hình, khác hẳn PIERCING (xuyên 3,
        // ×1 dmg, thân mảnh) và KAMEHAMEHA (beam xuyên-tất, ×3).
        pierceCount = 4,
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

    // ════════════════════════════════════════════════════════════════════════
    // Wave 17j — MODEL NHẬN DIỆN gom 1 chỗ. Mỗi đạn khai báo ĐẦY ĐỦ: shape /
    // size (bodyWidth) / color / special. Render (LaserCanvas) + spawn
    // (LasersController.buildOneLaser) ĐỌC từ đây → 1 nguồn sự thật, không còn
    // rải rác → bảo đảm khác biệt (có test chốt mọi field duy nhất).
    // ════════════════════════════════════════════════════════════════════════

    /** SHAPE — hình dáng vẽ (1:1 với hàm draw trong LaserCanvas). */
    val shape: BulletShape
        get() = when (this) {
            NORMAL -> BulletShape.CAPSULE
            PIERCING -> BulletShape.NEEDLE
            PLASMA -> BulletShape.ORB
            FIRE -> BulletShape.FLAME
            HOMING -> BulletShape.HOMING_DART
            BOUNCE -> BulletShape.RICOCHET
            GIANT -> BulletShape.GIANT_DISC
            SMOKE -> BulletShape.PUFF
            ZIGZAG -> BulletShape.ZIGZAG
            KAMEHAMEHA -> BulletShape.BEAM
            ATOMIC -> BulletShape.ATOM
            SPLIT -> BulletShape.TRIDENT
            LOTTERY -> BulletShape.TICKET
            FIREWORK -> BulletShape.FIREWORK
            BRICK -> BulletShape.BRICK
            BANH_MI -> BulletShape.BAGUETTE
            DURIAN -> BulletShape.DURIAN
            HEART -> BulletShape.HEART
        }

    /** SIZE — bề rộng thân (px). Dải 3 (kim) → 38 (khói khổng lồ). */
    val bodyWidth: Float
        get() = when (this) {
            ZIGZAG -> 3f
            NORMAL -> 5f
            PIERCING -> 6f
            FIRE -> 7f
            HOMING -> 8f
            BOUNCE -> 9f
            SPLIT -> 11f
            HEART -> 12f
            LOTTERY -> 13f
            BRICK -> 14f
            BANH_MI -> 15f
            PLASMA -> 22f
            FIREWORK -> 24f
            DURIAN -> 26f
            ATOMIC -> 28f
            GIANT -> 34f
            KAMEHAMEHA -> 36f
            SMOKE -> 38f
        }

    /** COLOR — màu nhận diện (signature). Ghép với booster gốc qua [BulletTypeColorMap]. */
    val colorArgb: Long get() = BulletTypeColorMap.argbFor(this)

    /** SPECIAL — kỹ năng đặc biệt (mô tả ngắn, đọc được trên UI/Bách Khoa). */
    val special: String
        get() = when (this) {
            NORMAL -> "Bắn thẳng, 1 hit"
            PIERCING -> "Xuyên 3 địch"
            PLASMA -> "Nổ vùng AoE 80px"
            FIRE -> "Gây cháy DoT 3 giây"
            HOMING -> "Tự đuổi địch gần nhất"
            BOUNCE -> "Nảy 3 lần khỏi mép màn"
            GIANT -> "Cày xuyên 4 địch + ×2 sát thương"
            SMOKE -> "Khói AoE 60px, bay chậm"
            ZIGZAG -> "Bay zigzag lượn né"
            KAMEHAMEHA -> "Beam xuyên-tất ×3 sát thương"
            ATOMIC -> "Nổ AoE 150 + phóng xạ cháy"
            SPLIT -> "Trúng → tách 3 đạn con"
            LOTTERY -> "Sát thương ngẫu nhiên 0.3–3×"
            FIREWORK -> "Nổ AoE 130 + bắn ra 5 đạn con"
            BRICK -> "Nặng ×2.2 + hất văng địch"
            BANH_MI -> "Xuyên 3 + hồi máu mỗi hit"
            DURIAN -> "Nổ AoE 110 + làm chậm địch"
            HEART -> "Tự đuổi + gây choáng (stun)"
        }

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
