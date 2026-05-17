package com.tranphuloi.neon.ui.game.meta

import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.state.EffectiveStats

/**
 * Wave 5 (48x) — skill tree node definition.
 *
 * Layout philosophy: 5 base nodes (HP / Damage / Magnet / Shield / Speed) form
 * the trunk; 15 advanced nodes branch off the base nodes. Branching nodes
 * require their parent to be at minRequiredParentRank before being unlockable.
 *
 * Costs scale linearly per rank: rank 1 = baseCost, rank 2 = baseCost × 2, etc.
 * UI shows nextCost() based on current rank.
 *
 * `key` is the persistence key in DataStore (must be stable).
 */
@Immutable
enum class SkillNode(
    val key: String,
    val displayName: String,
    val description: String,
    val baseCost: Int,
    val maxRank: Int,
    val parentKey: String? = null,
    val minRequiredParentRank: Int = 0,
    val tierIndex: Int = 0,                          // visual layout tier (0=root, 1=mid, 2=leaf)
) {
    // --- Tier 0: 5 nốt nền tảng ---
    BASE_HP(
        key = EffectiveStats.META_KEY_HP,
        displayName = "GIÁP CỨNG",
        description = "+10% HP tối đa mỗi cấp",
        baseCost = 100,
        maxRank = 5,
    ),
    BASE_DAMAGE(
        key = EffectiveStats.META_KEY_DAMAGE,
        displayName = "HỎA LỰC",
        description = "+8% sát thương mỗi cấp",
        baseCost = 120,
        maxRank = 5,
    ),
    BASE_MAGNET(
        key = EffectiveStats.META_KEY_MAGNET,
        displayName = "HỐ HẤP DẪN",
        description = "+15% bán kính nam châm mỗi cấp",
        baseCost = 80,
        maxRank = 4,
    ),
    BASE_SHIELD(
        key = "meta_shield",
        displayName = "KHIÊN AEGIS",
        description = "+1.5s thời lượng khiên mỗi cấp",
        baseCost = 100,
        maxRank = 4,
    ),
    BASE_SPEED(
        key = EffectiveStats.META_KEY_SPEED,
        displayName = "NHANH NHẸN",
        description = "+6% tốc độ di chuyển mỗi cấp",
        baseCost = 90,
        maxRank = 4,
    ),

    // --- Tier 1: 8 nốt nhánh (yêu cầu cấp nốt cha >= 2 hoặc 3) ---
    REGEN(
        key = "meta_regen",
        displayName = "TỰ HỒI",
        description = "Tự hồi +5 HP/giây khi không bị bắn",
        baseCost = 250,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    CRIT(
        key = "meta_crit",
        displayName = "CHÍ MẠNG",
        description = "+10% tỷ lệ chí mạng mỗi cấp (×2 sát thương)",
        baseCost = 280,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    LIFETIME_BONUS(
        key = "meta_lifetime",
        displayName = "NHÌN SAO",
        description = "+5% điểm thưởng mỗi cấp",
        baseCost = 220,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_MAGNET,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    SHIELD_BURST(
        key = "meta_shield_burst",
        displayName = "PHẢN ỨNG",
        description = "Khiên hết hạn → nổ mini-bom",
        baseCost = 300,
        maxRank = 2,
        parentKey = "meta_shield",
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    DASH(
        key = "meta_dash",
        displayName = "HẬU TĂNG LỰC",
        description = "I-frame ngắn sau khi bị bắn",
        baseCost = 260,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SPEED,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    EXTRA_BOMB(
        key = "meta_extra_bomb",
        displayName = "KHO ĐẠN",
        description = "+1 smart bomb khởi đầu mỗi cấp",
        baseCost = 200,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    COMBO_KEEP(
        key = "meta_combo_keep",
        displayName = "ĐÀ COMBO",
        description = "Combo kéo dài thêm 0.5s mỗi cấp",
        baseCost = 230,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    REVIVE_DROP(
        key = "meta_revive_drop",
        displayName = "TIM PHƯỢNG",
        description = "+2% tỷ lệ rơi revive token mỗi cấp",
        baseCost = 350,
        maxRank = 2,
        parentKey = "meta_shield",
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),

    // --- Tier 2: 2 nốt endgame (yêu cầu cấp nốt tier-1 >= 2) ---
    LEGENDARY_HP(
        key = "meta_legendary_hp",
        displayName = "LÒ RÈN SAO",
        description = "+50 HP cố định + 1 smart bomb",
        baseCost = 800,
        maxRank = 1,
        parentKey = "meta_regen",
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    LEGENDARY_DAMAGE(
        key = "meta_legendary_dmg",
        displayName = "PHÁO HƯ KHÔNG",
        description = "+25% sát thương khi HP > 75%",
        baseCost = 800,
        maxRank = 1,
        parentKey = "meta_crit",
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    ;

    /** Cost for the next rank purchase. */
    fun costForNextRank(currentRank: Int): Int = baseCost * (currentRank + 1)
}
