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
        key = EffectiveStats.META_KEY_SHIELD,
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
        key = EffectiveStats.META_KEY_REGEN,
        displayName = "TỰ HỒI",
        description = "Tự hồi +5 HP/giây khi không bị bắn",
        baseCost = 250,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    CRIT(
        key = EffectiveStats.META_KEY_CRIT,
        displayName = "CHÍ MẠNG",
        description = "+10% tỷ lệ chí mạng mỗi cấp (×2 sát thương)",
        baseCost = 280,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    LIFETIME_BONUS(
        key = EffectiveStats.META_KEY_LIFETIME,
        displayName = "NHÌN SAO",
        description = "+5% điểm thưởng mỗi cấp",
        baseCost = 220,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_MAGNET,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    SHIELD_BURST(
        key = EffectiveStats.META_KEY_SHIELD_BURST,
        displayName = "PHẢN ỨNG",
        description = "Khiên hết hạn → nổ mini-bom",
        baseCost = 300,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SHIELD,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    DASH(
        key = EffectiveStats.META_KEY_DASH,
        displayName = "HẬU TĂNG LỰC",
        description = "I-frame ngắn sau khi bị bắn",
        baseCost = 260,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SPEED,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    EXTRA_BOMB(
        key = EffectiveStats.META_KEY_EXTRA_BOMB,
        displayName = "KHO ĐẠN",
        description = "+1 smart bomb khởi đầu mỗi cấp",
        baseCost = 200,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    COMBO_KEEP(
        key = EffectiveStats.META_KEY_COMBO_KEEP,
        displayName = "ĐÀ COMBO",
        description = "Combo kéo dài thêm 0.5s mỗi cấp",
        baseCost = 230,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    REVIVE_DROP(
        key = EffectiveStats.META_KEY_REVIVE_DROP,
        displayName = "TIM PHƯỢNG",
        description = "+2% tỷ lệ rơi revive token mỗi cấp",
        baseCost = 350,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SHIELD,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),

    // --- Tier 2: 2 nốt endgame (yêu cầu cấp nốt tier-1 >= 2) ---
    LEGENDARY_HP(
        key = EffectiveStats.META_KEY_LEGENDARY_HP,
        displayName = "LÒ RÈN SAO",
        description = "+50 HP cố định + 1 smart bomb",
        baseCost = 800,
        maxRank = 1,
        parentKey = EffectiveStats.META_KEY_REGEN,
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    LEGENDARY_DAMAGE(
        key = EffectiveStats.META_KEY_LEGENDARY_DMG,
        displayName = "PHÁO HƯ KHÔNG",
        description = "+25% sát thương khi HP > 75%",
        baseCost = 800,
        maxRank = 1,
        parentKey = EffectiveStats.META_KEY_CRIT,
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),

    // Round 74 (R73f) — Wave 8 / Wave 10 expansion. Wire vào EffectiveStats
    // via metaUpgrades map. Hai node tier-1 mới, parentKey = BASE_DAMAGE/HP
    // tương ứng nature (ship unlock = HP investment, bullet = damage investment).
    SHIP_UNLOCK_DISCOUNT(
        key = EffectiveStats.META_KEY_SHIP_UNLOCK_DISCOUNT,
        displayName = "TỔ HỢP HÀNG KHÔNG",
        description = "-10% chi phí mở khoá loại tàu mỗi cấp",
        baseCost = 200,
        maxRank = 5,                                       // 50% off max
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    BULLET_DURATION(
        key = EffectiveStats.META_KEY_BULLET_DURATION,
        displayName = "TIA SAO BỀN BỈ",
        description = "+10% thời lượng buff đạn mỗi cấp",
        baseCost = 240,
        maxRank = 5,                                       // 50% longer max
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),

    // Task 01 (Slice 5) — mở khoá drone hộ tống. Rank = số drone tối đa (1→2).
    // Chưa mua (rank 0) → DRONE_BOOSTER không rơi. Nhánh HỎA LỰC (hoả lực phụ).
    DRONE_FLEET(
        key = EffectiveStats.META_KEY_DRONE,
        displayName = "PHI ĐỘI DRONE",
        description = "Mở khoá drone hộ tống tự bắn · +1 drone tối đa mỗi cấp",
        baseCost = 320,
        maxRank = 2,                                       // rank 1→1 drone, rank 2→2 drone
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    ;

    /** Cost for the next rank purchase. */
    fun costForNextRank(currentRank: Int): Int = baseCost * (currentRank + 1)
}
