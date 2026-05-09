package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.combo.ComboTier
import kotlinx.coroutines.delay

/**
 * Neon combo popup — tier announcement with multi-layer glow:
 *   1. Radial halo behind text (large soft glow)
 *   2. Text rendered 3 times stacked: outer halo (blur emulation), mid glow, crisp core
 *   3. Scale + alpha lifecycle animation
 */
@Composable
fun ComboPopup(
    tier: ComboTier,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (tier == ComboTier.NONE || shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        repeat(POPUP_DURATION_MILLIS / 33) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }

    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > POPUP_DURATION_MILLIS) return
    val t = elapsed.toFloat() / POPUP_DURATION_MILLIS                       // 0..1
    // Pop-in then settle: scale spikes at t=0.3, settles to 1.0
    val scale = when {
        t < 0.3f -> 0.5f + (t / 0.3f) * 0.8f                                // 0.5 → 1.3
        t < 0.5f -> 1.3f - ((t - 0.3f) / 0.2f) * 0.3f                       // 1.3 → 1.0
        else -> 1.0f
    }
    val alpha = if (t < 0.7f) 1.0f else 1f - ((t - 0.7f) / 0.3f)            // hold then fade

    val color = when (tier) {
        ComboTier.GODLIKE -> NeonRedAlert
        ComboTier.UNSTOPPABLE, ComboTier.RAMPAGE -> NeonMagenta
        else -> NeonGold
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        // Halo behind text — large radial gradient pulses with the popup.
        Canvas(modifier = Modifier.size(300.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.55f),
                        color.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }

        // Layer 1: outermost soft halo (largest, most blurred via graphicsLayer alpha).
        Text(
            text = tier.displayLabel,
            color = color.copy(alpha = 0.35f),
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 4.sp),
            modifier = Modifier.graphicsLayer { scaleX = 1.10f; scaleY = 1.10f },
        )
        // Layer 2: mid glow.
        Text(
            text = tier.displayLabel,
            color = color.copy(alpha = 0.65f),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 4.sp),
            modifier = Modifier.graphicsLayer { scaleX = 1.05f; scaleY = 1.05f },
        )
        // Layer 3: crisp inner core (white-tinted for max neon "burn").
        Text(
            text = tier.displayLabel,
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 4.sp),
        )
    }
}

private const val POPUP_DURATION_MILLIS: Int = 1400
