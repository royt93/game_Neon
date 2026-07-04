package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Task 05 — hiệu ứng kỹ năng chủ động (primitive MỚI, không tái dùng booster buff).
 * Mỗi tàu (ShipShape) gắn 1 [ShipAbility] riêng, ability dùng 1 effect này +
 * tham số/tên riêng để 22 skill cảm giác khác nhau. Hiện thực effect ở slice sau
 * (GameState wiring); Slice 1 chỉ khung + cooldown.
 */
enum class AbilityEffect {
    NOVA,          // nổ lan toàn màn (sát thương tức thời)
    OVERDRIVE,     // tăng nhịp bắn + sát thương tạm thời
    BULWARK,       // khiên/bất tử tạm thời
    PHASE_DASH,    // lướt né + bất tử ngắn
    REPAIR,        // hồi máu tức thời
    TIME_DILATION, // làm chậm toàn địch tạm thời
    LASER_STORM,   // bắn loạt đạn thêm
    MAGNET_PULSE,  // hút toàn bộ khoáng
    DECOY,         // thả mồi nhử hút đạn
    CRIT_FRENZY,   // chí mạng đảm bảo tạm thời
}

/**
 * Task 05 — 22 kỹ năng chủ động, map 1-1 với [ShipShape]. Mỗi skill: tên/mô tả
 * riêng (VN, hardcode như ShipShape), [effect] primitive, cooldown gốc + thời
 * lượng. Cooldown thực = base × [ShipXpLevels.cooldownMulForLevel] (giảm theo level).
 */
enum class ShipAbility(
    val displayName: String,
    val description: String,
    val effect: AbilityEffect,
    val baseCooldownMs: Long,
    val durationMs: Long,
    val glyph: String,
) {
    // ── Core 5 ──
    BUNG_NO("Bùng Nổ", "Nổ lan toàn màn hình", AbilityEffect.NOVA, 16_000, 0, "✸"),
    RAI_BOM("Rải Bom", "Bom lan diện rộng, sát thương lớn", AbilityEffect.NOVA, 18_000, 0, "✷"),
    TANG_HINH("Tàng Hình", "Lướt né + bất tử ngắn", AbilityEffect.PHASE_DASH, 14_000, 2_500, "✦"),
    LA_CHAN_THEP("Lá Chắn Thép", "Khiên bất khả xâm phạm", AbilityEffect.BULWARK, 18_000, 4_000, "⛨"),
    TANG_AP("Tăng Áp", "Bùng hoả lực (bắn nhanh + mạnh)", AbilityEffect.OVERDRIVE, 15_000, 5_000, "⯀"),

    // ── 12 (Round 79 batch) ──
    SAO_BANG("Sao Băng", "Trút loạt sao băng laser", AbilityEffect.LASER_STORM, 15_000, 3_000, "★"),
    CAU_VONG_SKILL("Cầu Vồng", "Bẻ cong thời gian, làm chậm địch", AbilityEffect.TIME_DILATION, 16_000, 4_000, "◗"),
    PHEP_THUAT("Phép Thuật", "Chí mạng đảm bảo", AbilityEffect.CRIT_FRENZY, 15_000, 5_000, "✺"),
    HAO_QUANG("Hào Quang", "Hồi phục sinh lực", AbilityEffect.REPAIR, 20_000, 0, "✚"),
    DAI_LIEN("Đại Liên", "Bão đạn 3 nòng", AbilityEffect.LASER_STORM, 16_000, 3_500, "⁂"),
    TRU_THAN("Trụ Thần", "Khiên thần thánh", AbilityEffect.BULWARK, 19_000, 4_000, "⯃"),
    SAO_VANG("Sao Vàng", "Nổ ánh sao vàng toàn màn", AbilityEffect.NOVA, 16_000, 0, "✶"),
    QUYEN_RU("Quyến Rũ", "Thả mồi nhử hút đạn địch", AbilityEffect.DECOY, 17_000, 4_000, "❥"),
    TU_KHI("Tử Khí", "Bùng sát thương tử thần", AbilityEffect.OVERDRIVE, 15_000, 5_000, "☠"),
    LUOI_HAI("Lưỡi Hái", "Chí mạng thu hoạch linh hồn", AbilityEffect.CRIT_FRENZY, 16_000, 5_000, "⚰"),
    TO_NHEN("Tơ Nhện", "Giăng tơ làm chậm địch", AbilityEffect.TIME_DILATION, 15_000, 4_000, "✳"),
    GIAP_THIET("Giáp Thiết", "Khiên thép dày", AbilityEffect.BULWARK, 18_000, 4_500, "⛊"),

    // ── 5 (Twin Domes + 4 quốc gia) ──
    SONG_PHAO("Song Pháo", "Bắn kép loạt đạn", AbilityEffect.LASER_STORM, 15_000, 3_000, "⁋"),
    KIEM_DAO("Kiếm Đạo", "Lướt kiếm né đòn", AbilityEffect.PHASE_DASH, 14_000, 2_500, "刀"),
    THAN_TOC("Thần Tốc", "Bùng tốc độ + hoả lực", AbilityEffect.OVERDRIVE, 15_000, 5_000, "⚡"),
    DAI_BANG("Đại Bàng", "Sà xuống nổ lớn", AbilityEffect.NOVA, 17_000, 0, "🦅"),
    KY_SI("Kỵ Sĩ", "Hút khoáng + xung kích", AbilityEffect.MAGNET_PULSE, 14_000, 0, "♞"),
    ;

    /** Cooldown thực (ms) theo level tàu (giảm dần). */
    fun effectiveCooldownMs(shipLevel: Int): Long =
        (baseCooldownMs * ShipXpLevels.cooldownMulForLevel(shipLevel)).toLong()

    companion object {
        /** Map 1-1 ShipShape → ability. Exhaustive để thêm tàu mới là compile-error. */
        fun forShip(shape: ShipShape): ShipAbility = when (shape) {
            ShipShape.FIGHTER -> BUNG_NO
            ShipShape.BOMBER -> RAI_BOM
            ShipShape.STEALTH -> TANG_HINH
            ShipShape.TANK -> LA_CHAN_THEP
            ShipShape.INTERCEPTOR -> TANG_AP
            ShipShape.NGOI_SAO -> SAO_BANG
            ShipShape.CAU_VONG -> CAU_VONG_SKILL
            ShipShape.PHU_THUY -> PHEP_THUAT
            ShipShape.AURA_GLOW -> HAO_QUANG
            ShipShape.SUNG_3_NONG -> DAI_LIEN
            ShipShape.OBELISK_SPIRE -> TRU_THAN
            ShipShape.VIETNAM -> SAO_VANG
            ShipShape.DIVA -> QUYEN_RU
            ShipShape.CHET_CHOC -> TU_KHI
            ShipShape.TU_THAN -> LUOI_HAI
            ShipShape.MANG_NHEN_ACE -> TO_NHEN
            ShipShape.AO_GIAP_THIET -> GIAP_THIET
            ShipShape.TWIN_DOMES -> SONG_PHAO
            ShipShape.NHAT_BAN -> KIEM_DAO
            ShipShape.HAN_QUOC -> THAN_TOC
            ShipShape.MY -> DAI_BANG
            ShipShape.PHAP -> KY_SI
        }
    }
}
