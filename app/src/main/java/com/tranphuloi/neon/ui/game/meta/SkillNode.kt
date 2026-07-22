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
        displayName = "Giáp cứng",
        description = "+10% HP tối đa mỗi cấp",
        baseCost = 100,
        maxRank = 5,
    ),
    BASE_DAMAGE(
        key = EffectiveStats.META_KEY_DAMAGE,
        displayName = "Hỏa lực",
        description = "+8% sát thương mỗi cấp",
        baseCost = 120,
        maxRank = 5,
    ),
    BASE_MAGNET(
        key = EffectiveStats.META_KEY_MAGNET,
        displayName = "Hố hấp dẫn",
        description = "+15% bán kính nam châm mỗi cấp",
        baseCost = 80,
        maxRank = 4,
    ),
    BASE_SHIELD(
        key = EffectiveStats.META_KEY_SHIELD,
        displayName = "Khiên aegis",
        description = "+1.5s thời lượng khiên mỗi cấp",
        baseCost = 100,
        maxRank = 4,
    ),
    BASE_SPEED(
        key = EffectiveStats.META_KEY_SPEED,
        displayName = "Nhanh nhẹn",
        description = "+6% tốc độ di chuyển mỗi cấp",
        baseCost = 90,
        maxRank = 4,
    ),

    // --- Tier 1: 8 nốt nhánh (yêu cầu cấp nốt cha >= 2 hoặc 3) ---
    REGEN(
        key = EffectiveStats.META_KEY_REGEN,
        displayName = "Tự hồi",
        description = "Tự hồi +5 HP/giây khi không bị bắn",
        baseCost = 250,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    CRIT(
        key = EffectiveStats.META_KEY_CRIT,
        displayName = "Chí mạng",
        description = "+10% tỷ lệ chí mạng mỗi cấp (×2 sát thương)",
        baseCost = 280,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    LIFETIME_BONUS(
        key = EffectiveStats.META_KEY_LIFETIME,
        displayName = "Nhìn sao",
        description = "+5% điểm thưởng mỗi cấp",
        baseCost = 220,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_MAGNET,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    SHIELD_BURST(
        key = EffectiveStats.META_KEY_SHIELD_BURST,
        displayName = "Phản ứng",
        description = "Khiên hết hạn → nổ mini-bom",
        baseCost = 300,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SHIELD,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    DASH(
        key = EffectiveStats.META_KEY_DASH,
        displayName = "Hậu tăng lực",
        description = "I-frame ngắn sau khi bị bắn",
        baseCost = 260,
        maxRank = 2,
        parentKey = EffectiveStats.META_KEY_SPEED,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    EXTRA_BOMB(
        key = EffectiveStats.META_KEY_EXTRA_BOMB,
        displayName = "Kho đạn",
        description = "+1 smart bomb khởi đầu mỗi cấp",
        baseCost = 200,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    COMBO_KEEP(
        key = EffectiveStats.META_KEY_COMBO_KEEP,
        displayName = "Đà combo",
        description = "Combo kéo dài thêm 0.5s mỗi cấp",
        baseCost = 230,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 3,
        tierIndex = 1,
    ),
    REVIVE_DROP(
        key = EffectiveStats.META_KEY_REVIVE_DROP,
        displayName = "Tim phượng",
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
        displayName = "Lò rèn sao",
        description = "+50 HP cố định + 1 smart bomb",
        baseCost = 800,
        maxRank = 1,
        parentKey = EffectiveStats.META_KEY_REGEN,
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    LEGENDARY_DAMAGE(
        key = EffectiveStats.META_KEY_LEGENDARY_DMG,
        displayName = "Pháo hư không",
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
        displayName = "Tổ hợp hàng không",
        description = "-10% chi phí mở khoá loại tàu mỗi cấp",
        baseCost = 200,
        maxRank = 5,                                       // 50% off max
        parentKey = EffectiveStats.META_KEY_HP,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    BULLET_DURATION(
        key = EffectiveStats.META_KEY_BULLET_DURATION,
        displayName = "Tia sao bền bỉ",
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
        displayName = "Phi đội drone",
        description = "Mở khoá drone hộ tống tự bắn · +1 drone tối đa mỗi cấp",
        baseCost = 320,
        maxRank = 2,                                       // rank 1→1 drone, rank 2→2 drone
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),

    // Task 24 (Wave 5) — mở rộng skill-tree, 6 node mới. Cùng convention:
    // unlock theo rank cha, không gate prestige, cost tuyến tính cũ.
    MINERAL_BOOST(
        key = EffectiveStats.META_KEY_MINERAL_BOOST,
        displayName = "Máy hút khoáng",
        description = "+10% khoáng chất nhận được mỗi cấp",
        baseCost = 150,
        maxRank = 4,
        parentKey = EffectiveStats.META_KEY_MAGNET,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    FIRE_RATE(
        key = EffectiveStats.META_KEY_FIRE_RATE,
        displayName = "Nòng súng nóng",
        description = "-5% nhịp bắn mỗi cấp (bắn nhanh hơn)",
        baseCost = 250,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_DAMAGE,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    BOOSTER_DURATION(
        key = EffectiveStats.META_KEY_BOOSTER_DURATION,
        displayName = "Lõi năng lượng",
        description = "+8% thời lượng mọi buff booster mỗi cấp",
        baseCost = 150,
        maxRank = 4,
        parentKey = EffectiveStats.META_KEY_SPEED,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    MAGNET_PULL_SPEED(
        key = EffectiveStats.META_KEY_MAGNET_PULL_SPEED,
        displayName = "Từ trường mạnh",
        description = "+20% tốc độ hút khoáng trong bán kính mỗi cấp",
        baseCost = 120,
        maxRank = 4,
        parentKey = EffectiveStats.META_KEY_MAGNET,
        minRequiredParentRank = 2,
        tierIndex = 1,
    ),
    PIERCE_CHANCE(
        key = EffectiveStats.META_KEY_PIERCE_CHANCE,
        displayName = "Đầu đạn xuyên",
        description = "+10% cơ hội đạn thường xuyên thủng thêm 1 địch mỗi cấp",
        baseCost = 700,
        maxRank = 3,
        parentKey = EffectiveStats.META_KEY_CRIT,
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    SECOND_WIND(
        key = EffectiveStats.META_KEY_SECOND_WIND,
        displayName = "Hơi thở thứ hai",
        description = "Hồi sinh 1 lần mỗi lượt chơi thay vì gục ngã",
        baseCost = 800,
        maxRank = 1,
        parentKey = EffectiveStats.META_KEY_REGEN,
        minRequiredParentRank = 2,
        tierIndex = 2,
    ),
    ;

    /** Cost for the next rank purchase. */
    fun costForNextRank(currentRank: Int): Int = baseCost * (currentRank + 1)
}
