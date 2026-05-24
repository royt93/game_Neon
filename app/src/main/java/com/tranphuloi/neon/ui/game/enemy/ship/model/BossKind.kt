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
}
