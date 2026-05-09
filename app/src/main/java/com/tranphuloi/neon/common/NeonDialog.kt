package com.tranphuloi.neon.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Common neon dialog container — single source of truth for dialog look-and-feel.
 *
 * Visual structure:
 *  - Vertical gradient bg (NeonBgDeep → NeonBgMid → NeonBgDeep)
 *  - Outer border in [accentColor] (optional pulse)
 *  - Inner cyan accent border for layered depth
 *  - Ambient halo via [neonGlow]
 *  - Animated title with scale pulse + optional shimmer to [titleSecondaryColor]
 *
 * Slots:
 *  - [body]: main content (typically a Column)
 *  - [actions]: button row (use [NeonDialogButton] for consistent buttons)
 */
@Composable
fun NeonDialog(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = true,
    titleSecondaryColor: Color? = null,                   // shimmer target color (null = no shimmer)
    titleSize: androidx.compose.ui.unit.TextUnit = 36.sp,
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonDialogPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (pulse) 0.5f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val titleShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (titleSecondaryColor != null) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titleShimmer",
    )

    Box(
        modifier = modifier
            .neonGlow(color = accentColor, intensity = 0.5f * pulseAlpha, radiusFactor = 1.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(NeonBgDeep, NeonBgMid, NeonBgDeep),
                ),
            )
            .border(
                BorderStroke(2.5.dp, accentColor.copy(alpha = pulseAlpha)),
                RoundedCornerShape(14.dp),
            )
            .drawBehind {
                // Inner subtle accent border for layered neon depth.
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.35f * pulseAlpha),
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(
                        size.width - 12.dp.toPx(),
                        size.height - 12.dp.toPx(),
                    ),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            val effectiveTitleColor = if (titleSecondaryColor != null) {
                lerpColor(accentColor, titleSecondaryColor, titleShimmer * 0.4f)
            } else accentColor
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = titleSize,
                color = effectiveTitleColor,
                style = TextStyle(letterSpacing = 5.sp),
                modifier = Modifier
                    .neonGlow(color = accentColor, intensity = 0.6f * pulseAlpha, radiusFactor = 1.3f)
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        scaleX = 0.97f + 0.06f * pulseAlpha
                        scaleY = 0.97f + 0.06f * pulseAlpha
                    },
            )
            Spacer(modifier = Modifier.height(18.dp))

            body()

            Spacer(modifier = Modifier.height(20.dp))
            // Actions stacked vertically — each button fillMaxWidth. Prevents
            // horizontal overflow that cropped buttons on small screens.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                actions()
            }
        }
    }
}

/**
 * Pill button matching neon dialog aesthetic. Always-on outer ring stroke,
 * gradient horizontal background, ambient halo. Press not animated (TextButton-like
 * behavior — instantaneous tap response).
 */
@Composable
fun NeonDialogButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingGlyph: String = "",                            // e.g. "▶" or "⏸"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "btnPulseAlpha",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.10f),
                        color.copy(alpha = 0.35f * pulse),
                        color.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(
                BorderStroke(2.dp, color.copy(alpha = pulse)),
                RoundedCornerShape(22.dp),
            )
            .neonGlow(color = color, intensity = 0.5f * pulse, radiusFactor = 1.4f)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        val full = if (leadingGlyph.isNotEmpty()) "$leadingGlyph $text" else text
        Text(
            text = full,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            style = TextStyle(letterSpacing = 3.sp),
        )
    }
}

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * tt,
        green = from.green + (to.green - from.green) * tt,
        blue = from.blue + (to.blue - from.blue) * tt,
        alpha = from.alpha + (to.alpha - from.alpha) * tt,
    )
}
