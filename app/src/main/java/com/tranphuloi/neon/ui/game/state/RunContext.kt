package com.tranphuloi.neon.ui.game.state

import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.ui.game.mode.GameMode
import com.tranphuloi.neon.ui.game.modifier.RunModifier

/**
 * Wave 5 foundation — immutable per-run snapshot. Created once at run start
 * (after mode/modifier/difficulty pickers), passed through nav args (just keys),
 * reconstructed inside `rememberGameState()` by reading repositories.
 *
 * Read ONCE at GameState construction; never re-read mid-run. This prevents
 * "what if user changes settings mid-game" footguns and keeps replay logic
 * deterministic.
 *
 * For 48x permanent progression, [metaUpgrades] carries a snapshot of skill
 * tree node ranks — applied via EffectiveStats.compute() at init.
 */
@Immutable
data class RunContext(
    val mode: GameMode = GameMode.CAMPAIGN,
    val modifier: RunModifier = RunModifier.NONE,
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** 17c — null in non-daily modes; only DAILY uses this. */
    val dailySeed: Long? = null,
    /** 48x — meta upgrade ranks snapshot. Empty map = no upgrades. */
    val metaUpgrades: Map<String, Int> = emptyMap(),
) {
    companion object {
        /** Default context for plain campaign run (back-compat for code paths
         *  that haven't been migrated yet — round 22 incremental rollout). */
        val DEFAULT: RunContext = RunContext()
    }
}
