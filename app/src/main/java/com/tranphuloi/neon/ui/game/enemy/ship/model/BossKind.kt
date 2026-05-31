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
    STAR(displayName = "Mặt Trời Đỏ Máu"),
    CROSS(displayName = "Thập Tự Ngọc Lục Bảo"),
    ORB(displayName = "Lính Gác Mắt Sát Thủ"),
    FRACTAL(displayName = "Hộ Vệ Nguyên Tử"),
    SPIDER(displayName = "Bá Vương Thiên Hà"),

    // Round 79 (#1) — 4 new shapes to eliminate visual duplicates across the
    // 9 chapter encounters (Ch3End/Ch3Mid/Ch4Mid/Ch4End previously reused).
    DEATH_MOON(displayName = "Mặt Trăng Tử Thần"),
    HAUNTED_KID(displayName = "Hồn Ma Trẻ Em"),
    HELL_LORD(displayName = "Chúa Tể Địa Ngục"),
    SATAN_GLYPH(displayName = "Quỷ Satan"),

    // ── Round 81 — 12 new boss kinds per user roster ──
    HEN_MOTHER(displayName = "Gà Mái Dầu"),
    BUFFALO_RAGE(displayName = "Trâu Hung Hẵn"),
    DUMB_RAT(displayName = "Chuột Ngu Si"),
    FIERCE_TIGER(displayName = "Cọp Hung Tợn"),
    SEXY_DIVA(displayName = "Cô Gái Sexy"),
    TROLL_TOWER(displayName = "Tháp Tinh Quỷ"),
    TWIN_SUMMITS(displayName = "Đôi Đỉnh Sinh Hoa"),
    VOID_GLOBES(displayName = "Đôi Cầu Hư Vô"),
    WHITE_DRAGON(displayName = "Bạch Long Mắt Lam"),
    HAMMER_SICKLE(displayName = "Cộng Sản Lên Ngôi"),
    MONEY_TYCOON(displayName = "Tư Bản Bóc Lột"),
    GOLDEN_TYCOON(displayName = "Tycoon Vàng"),

    // ── Wave 15 batch 1 — 3 boss user nêu đích danh ──
    SKULL_CROSSBONES(displayName = "Đầu Lâu Xương Chéo"),
    VAMPIRE(displayName = "Ma Cà Rồng"),
    COSMIC_CENTIPEDE(displayName = "Con Rết Vũ Trụ"),

    // ── Wave 16 batch 2 — 3 boss user nêu đích danh (nốt) ──
    GIANT_CONDOM(displayName = "Bao Cao Su Khổng Lồ"),
    VENOM_SPIDER(displayName = "Nhện Venom"),
    CORRUPTION(displayName = "Tham Nhũng"),
}
