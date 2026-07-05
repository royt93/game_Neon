package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Task 12 (đợt 4) — PASSIVE thành thạo (mastery): mỗi tàu mở 1 passive độc nhất
 * khi đạt max level ([ShipXpLevels.MAX_LEVEL]). Luôn bật (khác kỹ năng chủ động
 * [ShipAbility] của Task 05). User chốt Hybrid: 5 primitive stat (fold vào
 * EffectiveStats) + hook runtime đơn giản.
 */
enum class PassiveEffect {
    // ── Stat (fold vào EffectiveStats.compute — pure testable) ──
    DAMAGE_UP,   // +magnitude × damageMul
    HP_UP,       // +magnitude × hpMul
    SPEED_UP,    // +magnitude × speedMul
    MAGNET_UP,   // +magnitude × magnetMul
    SCORE_UP,    // +magnitude × scoreMul
    // ── Hook runtime (GameState — Slice 2) ──
    START_SHIELD, // vào trận +magnitude khiên (làm tròn)
    REGEN,        // +magnitude hp/giây
    LIFESTEAL,    // diệt địch hồi magnitude hp
    COMBO_KEEP,   // cửa sổ combo × (1+magnitude)
    PIERCE_UP ;   // RESERVED — chưa map tào nào (pierce nằm sâu ở LasersController;
                  // 2 tàu Xuyên phá/Song pháo tạm map DAMAGE_UP). Giữ cho tương lai.

    /** Passive dạng stat → áp thẳng vào EffectiveStats (không cần hook runtime). */
    val isStat: Boolean
        get() = this == DAMAGE_UP || this == HP_UP || this == SPEED_UP ||
            this == MAGNET_UP || this == SCORE_UP
}

/**
 * 22 passive, map 1-1 với [ShipShape] qua [forShip]. Tên/mô tả VN (hardcode như
 * ShipShape/ShipAbility). [magnitude] = độ mạnh (stat = tỉ lệ +%, hook = giá trị thô).
 */
enum class ShipPassive(
    val displayName: String,
    val description: String,
    val effect: PassiveEffect,
    val magnitude: Float,
    val glyph: String,
) {
    THIEN_XA("Thiện xạ", "+12% sát thương vĩnh viễn", PassiveEffect.DAMAGE_UP, 0.12f, "🎯"),
    DAU_DAN_NANG("Đầu đạn nặng", "+18% sát thương", PassiveEffect.DAMAGE_UP, 0.18f, "☄"),
    LINH_HOAT("Linh hoạt", "+18% tốc độ di chuyển", PassiveEffect.SPEED_UP, 0.18f, "💨"),
    GIAP_DAY("Giáp dày", "+25% máu tối đa", PassiveEffect.HP_UP, 0.25f, "🛡"),
    TANG_AP_P("Tăng áp", "+15% tốc độ di chuyển", PassiveEffect.SPEED_UP, 0.15f, "⚡"),
    NGOI_SAO_SANG("Ngôi sao sáng", "+20% điểm", PassiveEffect.SCORE_UP, 0.20f, "★"),
    HAO_QUANG_HUT("Hào quang hút", "+30% bán kính nam châm", PassiveEffect.MAGNET_UP, 0.30f, "◎"),
    HUT_LINH_HON("Hút linh hồn", "Diệt địch hồi 2 máu", PassiveEffect.LIFESTEAL, 2f, "✷"),
    HAO_QUANG_HOI("Hào quang hồi", "Hồi 1 máu mỗi giây", PassiveEffect.REGEN, 1f, "✚"),
    XUYEN_PHA("Xuyên phá", "+15% sát thương xuyên", PassiveEffect.DAMAGE_UP, 0.15f, "➤"),
    TRU_THAN_P("Trụ thần", "+20% máu tối đa", PassiveEffect.HP_UP, 0.20f, "⯃"),
    SAO_VANG_P("Sao vàng", "+25% điểm", PassiveEffect.SCORE_UP, 0.25f, "✶"),
    QUYEN_RU_P("Quyến rũ", "+35% bán kính nam châm", PassiveEffect.MAGNET_UP, 0.35f, "❥"),
    TU_KHI_P("Tử khí", "+20% sát thương", PassiveEffect.DAMAGE_UP, 0.20f, "☠"),
    LUOI_HAI_P("Lưỡi hái", "Diệt địch hồi 3 máu", PassiveEffect.LIFESTEAL, 3f, "⚰"),
    TO_NHEN_P("Tơ nhện", "Giữ combo lâu gấp đôi", PassiveEffect.COMBO_KEEP, 1f, "✳"),
    GIAP_THEP("Giáp thép", "Vào trận có sẵn 2 khiên", PassiveEffect.START_SHIELD, 2f, "⛊"),
    SONG_PHAO_P("Song pháo", "+15% sát thương kép", PassiveEffect.DAMAGE_UP, 0.15f, "⁋"),
    KIEM_DAO_P("Kiếm đạo", "+20% tốc độ di chuyển", PassiveEffect.SPEED_UP, 0.20f, "刀"),
    THAN_TOC_P("Thần tốc", "+15% sát thương", PassiveEffect.DAMAGE_UP, 0.15f, "⚡"),
    DAI_BANG_P("Đại bàng", "+20% máu tối đa", PassiveEffect.HP_UP, 0.20f, "🦅"),
    KY_SI_P("Kỵ sĩ", "+30% bán kính nam châm", PassiveEffect.MAGNET_UP, 0.30f, "♞"),
    ;

    companion object {
        /** Map 1-1 ShipShape → passive. Exhaustive để thêm tàu mới là compile-error. */
        fun forShip(shape: ShipShape): ShipPassive = when (shape) {
            ShipShape.FIGHTER -> THIEN_XA
            ShipShape.BOMBER -> DAU_DAN_NANG
            ShipShape.STEALTH -> LINH_HOAT
            ShipShape.TANK -> GIAP_DAY
            ShipShape.INTERCEPTOR -> TANG_AP_P
            ShipShape.NGOI_SAO -> NGOI_SAO_SANG
            ShipShape.CAU_VONG -> HAO_QUANG_HUT
            ShipShape.PHU_THUY -> HUT_LINH_HON
            ShipShape.AURA_GLOW -> HAO_QUANG_HOI
            ShipShape.SUNG_3_NONG -> XUYEN_PHA
            ShipShape.OBELISK_SPIRE -> TRU_THAN_P
            ShipShape.VIETNAM -> SAO_VANG_P
            ShipShape.DIVA -> QUYEN_RU_P
            ShipShape.CHET_CHOC -> TU_KHI_P
            ShipShape.TU_THAN -> LUOI_HAI_P
            ShipShape.MANG_NHEN_ACE -> TO_NHEN_P
            ShipShape.AO_GIAP_THIET -> GIAP_THEP
            ShipShape.TWIN_DOMES -> SONG_PHAO_P
            ShipShape.NHAT_BAN -> KIEM_DAO_P
            ShipShape.HAN_QUOC -> THAN_TOC_P
            ShipShape.MY -> DAI_BANG_P
            ShipShape.PHAP -> KY_SI_P
        }

        /**
         * Passive ĐANG HIỆU LỰC: chỉ mở khi tàu đạt max level. Trả null nếu chưa
         * đủ level (passive còn khoá). Dùng ở EffectiveStats + GameState.
         */
        fun activeFor(shape: ShipShape, shipLevel: Int): ShipPassive? =
            if (shipLevel >= ShipXpLevels.MAX_LEVEL) forShip(shape) else null
    }
}
