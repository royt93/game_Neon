package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Keep
@Immutable
data class EnemyUI(
    val enemyId: String,
    val width: Float,
    val height: Float,
    val xOffset: Float,
    val yOffset: Float,
    val hpBarWidth: Float,
    @DrawableRes val drawableId: Int,
    val lastImpactMillis: Long = 0L,
    val isBoss: Boolean = false,
    val displayName: String = "",
    val currentHp: Float = 0f,
    val initialHp: Float = 0f,
    val isInEntryPhase: Boolean = false,
    /** 34d FinalBoss only: 1..3, else 0. */
    val currentPhase: Int = 0,
    /** 34d Wall-clock of last phase transition; 0 if none. Drives PhaseTransitionBanner. */
    val phaseTransitionMillis: Long = 0L,
    /**
     * Round 35 (42x) — ARGB tint colors for active status effects (BURN / SLOW /
     * STUN). Empty when no effect. GameWorld overlays one tinted Image per
     * value. Stored as `Long` (0xAARRGGBB) instead of `StatusEffect` enum so
     * EnemyUI stays self-contained and Serializable-friendly.
     */
    val activeStatusEffectTints: List<Long> = emptyList(),
) : Serializable
