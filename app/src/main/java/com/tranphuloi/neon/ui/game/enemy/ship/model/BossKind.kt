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
enum class BossKind {
    STAR,
    CROSS,
    ORB,
    FRACTAL,
    SPIDER,

    // Round 79 (#1) — 4 new shapes to eliminate visual duplicates across the
    // 9 chapter encounters (Ch3End/Ch3Mid/Ch4Mid/Ch4End previously reused).
    DEATH_MOON,      // mặt trăng tử thần — full moon + skull cracks
    HAUNTED_KID,     // kid ma ám — ghost child silhouette
    HELL_LORD,       // chúa tể địa ngục — devil head + horns
    SATAN_GLYPH,     // quỷ satan — inverted pentagram + center eye

    // ── Round 81 — 12 new boss kinds per user roster ──
    HEN_MOTHER,      // con gà mái dầu — throws eggs
    BUFFALO_RAGE,    // con trâu hung hẵn — throws horns
    DUMB_RAT,        // con chuột ngu si — laser bullets
    FIERCE_TIGER,    // con cọp hung tợn — roar bullet pattern
    SEXY_DIVA,       // cô gái sexy — throws hair 4 directions (feminine silhouette)
    TROLL_TOWER,     // tháp tinh quỷ (dương vật tinh nghịch → tasteful tower)
    TWIN_SUMMITS,    // đôi đỉnh sinh hoa (nhũ hoa → tasteful twin domes)
    VOID_GLOBES,     // đôi cầu hư vô (cặp mông → tasteful twin spheres)
    WHITE_DRAGON,    // bạch long mắt lam — fire breath
    HAMMER_SICKLE,   // cộng sản — hammer+sickle symbol throws
    MONEY_TYCOON,    // tư bản bóc lột — throws money bags
    GOLDEN_TYCOON,   // (trump caricature, no real name) — orange hair tycoon + dollar throws
}
