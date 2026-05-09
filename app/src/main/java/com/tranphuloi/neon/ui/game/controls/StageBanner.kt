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
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import kotlinx.coroutines.delay

/**
 * 5c: Neon-glow stage banner. Replaces plain Text rendering for `StageMessage`.
 *
 *  - 3-layer text stack (outer halo + mid glow + crisp white core)
 *  - Canvas radial halo behind text
 *  - Color picked by message keyword: "GO!" gold, "boss" red, default cyan
 *  - Countdown variant ("3", "2", "1") gets bigger scale + faster pulse
 */
@Composable
fun StageBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    if (message.isBlank()) return

    // Tick a "shown at" timestamp keyed on message — so changing message resets animation.
    var shownAtMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(message) {
        shownAtMillis = System.currentTimeMillis()
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(message) {
        repeat(60) {                                                    // ~2s @ 33ms
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    val t = (elapsed.toFloat() / 1500f).coerceIn(0f, 1f)
    val color = pickColor(message)
    val isCountdown = message in COUNTDOWN_TOKENS
    // Pop-in 0..0.2: 0.5 → 1.4, settle 0.2..0.5: 1.4 → 1.0, hold, fade out 0.85..1
    val baseScale = when {
        t < 0.15f -> 0.5f + (t / 0.15f) * 0.9f
        t < 0.35f -> 1.4f - ((t - 0.15f) / 0.20f) * 0.4f
        else -> 1.0f
    }
    val scale = if (isCountdown) baseScale * 1.4f else baseScale
    val alpha = when {
        t < 0.85f -> 1f
        else -> 1f - ((t - 0.85f) / 0.15f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) {
        // Halo backdrop — smaller per "to quá" feedback.
        Canvas(modifier = Modifier.size(if (isCountdown) 240.dp else 200.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.55f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }

        // Reduced font sizes: countdown 96→64, regular 44→30 per user feedback.
        val coreFontSize = if (isCountdown) 64.sp else 30.sp
        val midFontSize = if (isCountdown) 68.sp else 32.sp
        val outerFontSize = if (isCountdown) 72.sp else 34.sp

        // Outer halo (largest, soft).
        Text(
            text = message,
            color = color.copy(alpha = 0.30f),
            fontSize = outerFontSize,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = if (isCountdown) 0.sp else 4.sp),
            modifier = Modifier.graphicsLayer { scaleX = 1.10f; scaleY = 1.10f },
        )
        // Mid glow.
        Text(
            text = message,
            color = color.copy(alpha = 0.65f),
            fontSize = midFontSize,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = if (isCountdown) 0.sp else 4.sp),
            modifier = Modifier.graphicsLayer { scaleX = 1.05f; scaleY = 1.05f },
        )
        // Crisp white core.
        Text(
            text = message,
            color = Color.White,
            fontSize = coreFontSize,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = if (isCountdown) 0.sp else 4.sp),
        )
    }
}

private fun pickColor(message: String): Color {
    val lower = message.lowercase()
    return when {
        lower.contains("go") -> NeonGold
        lower.contains("boss") -> NeonMagenta
        lower.contains("rekt") || lower.contains("end") -> NeonMagenta
        else -> NeonCyan
    }
}

private val COUNTDOWN_TOKENS = setOf("3", "2", "1")
