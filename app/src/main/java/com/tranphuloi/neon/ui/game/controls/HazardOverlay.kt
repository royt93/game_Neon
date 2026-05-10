package com.tranphuloi.neon.ui.game.controls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tranphuloi.neon.ui.game.stage.HazardType

/**
 * 32d Wave 4 — semi-transparent visibility overlay per active hazard.
 * - ASTEROID_STORM: no overlay (storm visible via increased space rocks).
 * - NEBULA_FOG: 30% darkening + slow alpha pulse 0.20..0.40, simulates clouds passing.
 * - ICE_PATCHES: cold blue tint at top + bottom edges (visual cue only, no mechanic yet).
 *
 * Place at zIndex between background (1f) and gameplay layers (10f+).
 */
@Composable
fun HazardOverlay(
    hazard: HazardType?,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hazard == null) return
    when (hazard) {
        HazardType.ASTEROID_STORM -> Unit                                  // no visual overlay
        HazardType.NEBULA_FOG -> NebulaFogOverlay(reduceMotion, modifier)
        HazardType.ICE_PATCHES -> IcePatchesOverlay(modifier)
    }
}

@Composable
private fun NebulaFogOverlay(reduceMotion: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "nebula-fog")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-pulse",
    )
    val effectiveAlpha = if (reduceMotion) 0.30f else pulseAlpha
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0530).copy(alpha = effectiveAlpha),    // dark purple top
                        Color(0xFF0A0220).copy(alpha = effectiveAlpha * 0.5f),
                        Color(0xFF1A0530).copy(alpha = effectiveAlpha),
                    ),
                ),
            ),
    )
}

@Composable
private fun IcePatchesOverlay(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF80E0FF).copy(alpha = 0.18f),              // cyan top
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFF80E0FF).copy(alpha = 0.18f),              // cyan bottom
                    ),
                ),
            ),
    )
}
