package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType

/**
 * 31d Wave 4 — 5 chapters × ~20 stages = 100-stage campaign script.
 * Each chapter exposes its visual palette, drawable palette, hazard mechanic,
 * mid-boss variant, and final-stage boss type.
 *
 * Chapter ARGB colors are alpha-blended onto stageTint in GameScreen — the
 * 0xRRGGBB component is the color, alpha is applied at render time.
 */
enum class Chapter(
    val id: Int,
    val displayName: String,
    val tintArgb: Long,
    val regularEnemyDrawables: List<Int>,
    val hazard: HazardType?,
    val midBossType: MidBossType?,
    val finalBossType: EnemyType,
) {
    ASTEROID_BELT(
        id = 1,
        displayName = "VÀNH ĐAI TIỂU HÀNH TINH",
        tintArgb = 0xFFFFB048,                        // warm gold-orange
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_1,
            R.drawable.enemy_red_2,
            R.drawable.enemy_red_3,
        ),
        hazard = HazardType.ASTEROID_STORM,
        midBossType = MidBossType.OFFENSIVE,
        finalBossType = LevelOneBossType,
    ),
    NEBULA_CLOUD(
        id = 2,
        displayName = "MÂY TINH VÂN",
        tintArgb = 0xFFB14CFF,                        // violet
        regularEnemyDrawables = listOf(
            R.drawable.enemy_green_1,
            R.drawable.enemy_green_2,
            R.drawable.enemy_green_3,
            R.drawable.enemy_green_4,
        ),
        hazard = HazardType.NEBULA_FOG,
        midBossType = MidBossType.DEFENSIVE,
        finalBossType = LevelTwoBossType,
    ),
    ICE_PLANET(
        id = 3,
        displayName = "HÀNH TINH BĂNG",
        tintArgb = 0xFF00F0FF,                        // cyan ice
        regularEnemyDrawables = listOf(
            R.drawable.enemy_light_blue_1,
            R.drawable.enemy_light_blue_2,
            R.drawable.enemy_light_blue_3,
            R.drawable.enemy_light_blue_4,
            R.drawable.enemy_light_blue_5,
        ),
        hazard = HazardType.ICE_PATCHES,
        midBossType = MidBossType.SWARM,
        finalBossType = LevelOneBossType,             // reuse — palette change handled visually
    ),
    HOSTILE_STATION(
        id = 4,
        displayName = "TRẠM THÙ ĐỊCH",
        tintArgb = 0xFFFF2D55,                        // red alert
        // Round 74 (R73d) — Chapter 4 introduces ELITE family (cross/orb).
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_2,
            R.drawable.enemy_red_3,
            R.drawable.enemy_green_3,
            R.drawable.enemy_light_blue_4,
            R.drawable.enemy_cross_1,
            R.drawable.enemy_cross_2,
            R.drawable.enemy_orb_1,
            R.drawable.enemy_orb_2,
        ),
        hazard = null,                                // station = open zone
        midBossType = MidBossType.OFFENSIVE,
        finalBossType = LevelTwoBossType,
    ),
    GALAXY_CORE(
        id = 5,
        displayName = "LÕI THIÊN HÀ",
        tintArgb = 0xFFFF2DE0,                        // deep magenta
        // Round 74 (R73d) — Chapter 5 introduces BERSERKER family (chevron/spike).
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_3,
            R.drawable.enemy_green_4,
            R.drawable.enemy_light_blue_5,
            R.drawable.enemy_chevron_1,
            R.drawable.enemy_chevron_2,
            R.drawable.enemy_spike_1,
            R.drawable.enemy_spike_2,
        ),
        hazard = null,
        midBossType = null,                           // no mid-boss before final
        finalBossType = FinalBossType,                // 34d 3-phase final boss
    );
}

/**
 * 32d Stage hazards — per-chapter environmental modifier baked into StageGame.
 * Mechanic implemented in GameState loop tinker callbacks; visual cues in GameScreen.
 */
enum class HazardType {
    /** Frequent space rock spawns + larger rocks. */
    ASTEROID_STORM,
    /** Reduced star/dust visibility (background dimmed) + occasional fog cloud overlay. */
    NEBULA_FOG,
    /** Ship slip — movement keeps gliding briefly after release (inertia). */
    ICE_PATCHES,
}
