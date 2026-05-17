package com.tranphuloi.neon.ui.game.story

import androidx.compose.runtime.Immutable

/**
 * Wave 5 (47x) — single line of in-game dialogue / narration.
 * Speaker is "Narrator" for chapter intros, boss name for boss taunts.
 */
@Immutable
data class StoryLine(
    val speaker: String,
    val text: String,
    val durationMs: Int = 3500,
)
