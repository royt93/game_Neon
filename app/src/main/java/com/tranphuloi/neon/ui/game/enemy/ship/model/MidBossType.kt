package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.common.Once

/**
 * 33c+d Wave 4 — Mid-boss variants spawned every ~6 stages within a chapter.
 * Each variant has distinct stats + behavior. Phase transition triggers at HP < 50%
 * (handled inside `MidBoss` class — increases fire rate / enables secondary attack).
 */
sealed class MidBossType(val drawableId: Int, val baseHp: Float, val displayName: String) :
    EnemyType(spawnRate = Once) {

    /** Aggressive shooter — moderate HP, fast lasers. Phase 2: triple-shot spread. */
    object OFFENSIVE : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1500f,
        displayName = "OFFENSIVE MID-BOSS",
    )

    /** Tanky — high HP, slow movement. Phase 2: laser barrage. */
    object DEFENSIVE : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2500f,
        displayName = "DEFENSIVE MID-BOSS",
    )

    /**
     * Swarm — low HP, fast erratic figure-8 movement. Phase 2: aggressive triple-spread.
     * Reuses green_boss drawable (only 2 boss webps available); distinguished from
     * DEFENSIVE by behavior (figure-8 motion vs slow patrol) and lower HP.
     */
    object SWARM : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1200f,
        displayName = "SWARM MID-BOSS",
    )
}
