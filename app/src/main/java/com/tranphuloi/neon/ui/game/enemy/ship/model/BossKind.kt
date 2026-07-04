package com.tranphuloi.neon.ui.game.enemy.ship.model

/**
 * Round 71 (Issue 4d) — 5 distinct boss silhouettes:
 *   STAR    : original 8-point star (LevelOneBoss baseline)
 *   CROSS   : 4-arm cross spinner (LevelTwoBoss)
 *   ORB     : large orb + ring satellite (MidBoss variant 1)
 *   FRACTAL : recursive triangle (MidBoss variant 2, 3)
 *   SPIDER  : 8 legs radiating (FinalBoss)
 *
 * Attack patterns + audio cue defer Round 72+ (cần EnemyLasersController
 * refactor + audio asset). Visual silhouette done Round 71.
 */
/**
 * Round 84 audit — added `displayName` field. Used by boss classes' intro
 * banner (LevelOneBoss/LevelTwoBoss/MidBoss) thay generic "LEVEL 1 BOSS" /
 * "TIỂU BOSS TẤN CÔNG" hardcoded. Khi `bossKindOverride` đã set ở factory
 * resolve (Ch4 OFFENSIVE → HELL_LORD), boss banner sẽ hiện đúng tên Vietnamese
 * khớp với InfoScreen entry.
 */
@androidx.annotation.Keep
enum class BossKind(val displayName: String) {
    STAR(displayName = "Mặt trời đỏ máu"),
    CROSS(displayName = "Thập tự ngọc lục bảo"),
    ORB(displayName = "Lính gác mắt sát thủ"),
    FRACTAL(displayName = "Hộ vệ nguyên tử"),
    SPIDER(displayName = "Bá vương thiên hà"),

    // Round 79 (#1) — 4 new shapes to eliminate visual duplicates across the
    // 9 chapter encounters (Ch3End/Ch3Mid/Ch4Mid/Ch4End previously reused).
    DEATH_MOON(displayName = "Mặt trăng tử thần"),
    HAUNTED_KID(displayName = "Hồn ma trẻ em"),
    HELL_LORD(displayName = "Chúa tể địa ngục"),
    SATAN_GLYPH(displayName = "Quỷ satan"),

    // ── Round 81 — 12 new boss kinds per user roster ──
    HEN_MOTHER(displayName = "Gà mái dầu"),
    BUFFALO_RAGE(displayName = "Trâu hung hẵn"),
    DUMB_RAT(displayName = "Chuột ngu si"),
    FIERCE_TIGER(displayName = "Cọp hung tợn"),
    SEXY_DIVA(displayName = "Cô gái sexy"),
    TROLL_TOWER(displayName = "Tháp tinh quỷ"),
    TWIN_SUMMITS(displayName = "Đôi đỉnh sinh hoa"),
    VOID_GLOBES(displayName = "Đôi cầu hư vô"),
    WHITE_DRAGON(displayName = "Bạch long mắt lam"),
    HAMMER_SICKLE(displayName = "Cộng sản lên ngôi"),
    MONEY_TYCOON(displayName = "Tư bản bóc lột"),
    GOLDEN_TYCOON(displayName = "Tycoon vàng"),

    // ── Wave 15 batch 1 — 3 boss user nêu đích danh ──
    SKULL_CROSSBONES(displayName = "Đầu lâu xương chéo"),
    VAMPIRE(displayName = "Ma cà rồng"),
    COSMIC_CENTIPEDE(displayName = "Con rết vũ trụ"),

    // ── Wave 16 batch 2 — 3 boss user nêu đích danh (nốt) ──
    GIANT_CONDOM(displayName = "Bao cao su khổng lồ"),
    VENOM_SPIDER(displayName = "Nhện venom"),
    CORRUPTION(displayName = "Tham nhũng"),

    // ── Wave 18 batch 1 — 3 boss trào phúng (đời sống VN) ──
    TRAFFIC_JAM(displayName = "Trùm kẹt xe"),
    KPI_BOSS(displayName = "Sếp KPI"),
    TIKTOKER(displayName = "Hot TikToker"),

    // ── Wave 19 batch 2 — 3 boss trào phúng (nốt) ──
    ATM_BANKRUPT(displayName = "ATM hết tiền"),
    NOKIA_BRICK(displayName = "Cục gạch nokia 1280"),
    INFLATION_STORM(displayName = "Bão giá lạm phát"),

    // ── Wave 20 batch 3 — 4 boss trào phúng (hết batch 1) ──
    SOCIAL_DRAMA(displayName = "Drama mạng xã hội"),
    PYRAMID_SCHEME(displayName = "Trùm đa cấp"),
    FORTUNE_TELLER(displayName = "Thầy bói online"),
    KITCHEN_GOD(displayName = "Ông táo cưỡi cá chép"),

    // ── Wave 21 batch 4 — 2 boss trào phúng (batch 2 mở màn) ──
    CRYPTO_BRO(displayName = "Ông chú crypto"),
    TOXIC_KID(displayName = "Trẻ trâu toxic"),

    // ── Wave 22 batch 5 — 2 boss trào phúng ──
    KARAOKE_BOSS(displayName = "Trùm karaoke lạc tông"),
    FLASHY_TYCOON(displayName = "Đại gia phông bạt"),

    // ── Wave 23 batch 6 — 2 boss trào phúng ──
    DR_GOOGLE(displayName = "Bác sĩ google"),
    CAT_EMPEROR(displayName = "Hoàng thượng mèo"),

    // ── Wave 24 batch 7 — 2 boss trào phúng (HẾT batch 2 = đủ 18) ──
    SALE_FANATIC(displayName = "Thánh cuồng sale"),
    GHOST_MONTH(displayName = "Cô hồn tháng 7"),
}
