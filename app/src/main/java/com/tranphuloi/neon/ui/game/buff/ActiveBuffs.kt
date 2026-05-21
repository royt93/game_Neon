package com.tranphuloi.neon.ui.game.buff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/**
 * Wave 4 (42x) round 34 — list of buffs picked during current run.
 * GameState owns the MutableState; DialogBuffPicker writes to it via
 * LocalActiveBuffs CompositionLocal.
 *
 * Cleared on Game route entry (each Game composition starts fresh list).
 * Persisted across boss-kill cycles within same run.
 */
val LocalActiveBuffs =
    compositionLocalOf<MutableState<List<RunBuff>>> {
        error("LocalActiveBuffs not provided")
    }

/**
 * Aggregate buff multipliers. Each buff's effects multiply together
 * (e.g. two HP buffs +25% each → final hp ×1.5625).
 */
data class BuffMultipliers(
    val hpMul: Float = 1f,
    val damageMul: Float = 1f,
    val speedMul: Float = 1f,
    val magnetMul: Float = 1f,
    val scoreMul: Float = 1f,
) {
    companion object {
        fun from(buffs: List<RunBuff>): BuffMultipliers {
            var hp = 1f; var dmg = 1f; var spd = 1f; var mag = 1f; var sco = 1f
            buffs.forEach {
                hp *= it.hpMul
                dmg *= it.damageMul
                spd *= it.speedMul
                mag *= it.magnetMul
                sco *= it.scoreMul
            }
            return BuffMultipliers(hp, dmg, spd, mag, sco)
        }
    }
}
