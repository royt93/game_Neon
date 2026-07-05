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
    CAMPAIGN("campaign", "Chiến dịch"),
    SURVIVAL("survival", "Sinh tồn"),
    TIME_ATTACK("time_attack", "Đua thời gian"),
    BOSS_RUSH("boss_rush", "Chiến boss"),
    ENDLESS("endless", "Vô tận"),
    DAILY("daily", "Thử thách ngày"),
    // Task: QoL Practice — luyện tập 1 chương đã mở (checkpoint riêng, dùng chung stages).
    PRACTICE("practice", "Luyện tập");

    companion object {
        fun fromKey(key: String?): GameMode =
            values().firstOrNull { it.key == key } ?: CAMPAIGN
    }
}
