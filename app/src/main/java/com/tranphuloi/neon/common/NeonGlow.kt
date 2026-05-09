package com.tranphuloi.neon.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.neonGlow(
    color: Color,
    intensity: Float = 0.55f,
    radiusFactor: Float = 1.6f,
): Modifier = this.drawBehind {
    val r = (size.minDimension / 2f) * radiusFactor
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = intensity),
                color.copy(alpha = intensity * 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = r,
        ),
        radius = r,
        center = center,
    )
}
