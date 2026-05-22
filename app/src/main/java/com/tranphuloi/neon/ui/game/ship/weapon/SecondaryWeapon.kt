package com.tranphuloi.neon.ui.game.ship.weapon

import androidx.compose.runtime.Immutable

/**
 * Wave 6 (29x) round 40 — secondary weapon types fired from a second on-screen
 * button (next to smart bomb). One active type at a time per run; defaults to
 * MISSILE. A pre-game picker (round 41+ via 36x Loadout) will let the player
 * choose between types.
 *
 * Round 40 ships only MISSILE — MINE and BURST are stubs reserved here so the
 * enum surface is stable for the upcoming Loadout work.
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
