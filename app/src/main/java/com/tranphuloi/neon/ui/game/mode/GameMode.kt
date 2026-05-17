package com.tranphuloi.neon.ui.game.mode

import androidx.compose.runtime.Immutable

/**
 * Wave 5 (43x) — top-level game mode. Controls stage script, leaderboard target,
 * and which mid-run features are gated.
 *
 * - CAMPAIGN: default, plays through 5-chapter buildStageScript.
 * - SURVIVAL: chapter-1 stages looped indefinitely with mild scaling.
 * - TIME_ATTACK: chapter-1 stages with a strict 60s clock; score = enemies killed.
 * - BOSS_RUSH: only StageBoss entries, back-to-back, healing between bosses.
 * - ENDLESS: kicks in after CAMPAIGN ending; spawn × 1.05^wave, hp × 1.1^wave.
 * - DAILY: chapter-1 with daily seed, separate leaderboard (already wired in round 19).
 */
@Immutable
enum class GameMode(val key: String, val displayName: String) {
    CAMPAIGN("campaign", "CHIẾN DỊCH"),
    SURVIVAL("survival", "SINH TỒN"),
    TIME_ATTACK("time_attack", "ĐUA THỜI GIAN"),
    BOSS_RUSH("boss_rush", "CHIẾN BOSS"),
    ENDLESS("endless", "VÔ TẬN"),
    DAILY("daily", "THỬ THÁCH NGÀY");

    companion object {
        fun fromKey(key: String?): GameMode =
            values().firstOrNull { it.key == key } ?: CAMPAIGN
    }
}
