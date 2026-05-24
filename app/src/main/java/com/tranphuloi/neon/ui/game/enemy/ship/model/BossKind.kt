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
}
