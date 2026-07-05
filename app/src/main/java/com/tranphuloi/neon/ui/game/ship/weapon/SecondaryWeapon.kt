package com.tranphuloi.neon.ui.game.ship.weapon

import androidx.compose.runtime.Immutable

/**
 * Wave 6 (29x) — secondary weapon types fired from a second on-screen button
 * (next to smart bomb). One active type at a time per run; defaults to MISSILE.
 * Player chọn ở DialogLoadoutPicker ("Vũ khí phụ").
 *
 * Cả 3 ĐÃ triển khai đầy đủ + chọn được (Round 41+): MISSILE (tên lửa dò),
 * MINE (thả mìn AoE sau ship — [Mine], fire+detonate+render trong GameState/
 * GameWorld), BURST (quét 5 địch gần). (Comment "stub" cũ Round 40 đã lỗi thời.)
 */
@Immutable
enum class SecondaryWeapon(
    val displayName: String,
    val cooldownMs: Long,
    val glyph: String,
) {
    MISSILE(displayName = "Tên lửa", cooldownMs = 5_000L, glyph = "🚀"),
    MINE(displayName = "Mìn", cooldownMs = 7_000L, glyph = "💠"),
    BURST(displayName = "Quét", cooldownMs = 8_000L, glyph = "💥");

    companion object {
        fun fromName(name: String?): SecondaryWeapon =
            entries.firstOrNull { it.name == name } ?: MISSILE
    }
}
