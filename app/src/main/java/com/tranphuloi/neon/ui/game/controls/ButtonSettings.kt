package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.neonGlow

/**
 * Round 67.6 — Vector gear icon replacing `button_settings.webp` bitmap.
 * 8-tooth gear with center circle; matches neon vector aesthetic.
 */
@Composable
fun ButtonSettings(modifier: Modifier = Modifier, onSettings: () -> Unit) {
    val buttonPaddingEnd = dimensionResource(id = R.dimen.button_padding)
    val buttonPaddingTop = 16.dp
    val buttonSize = 60.dp

    Canvas(
        modifier = modifier
            .padding(top = buttonPaddingTop, end = buttonPaddingEnd)
            .size(buttonSize)
            .neonGlow(color = NeonCyan, intensity = 0.5f, radiusFactor = 1.3f)
            .clickable { onSettings() },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = minOf(size.width, size.height) * 0.42f
        val innerR = outerR * 0.55f
        val toothCount = 8
        // Gear outline: alternating outer/inner radius at every 22.5°.
        val path = Path().apply {
            val step = (Math.PI / toothCount).toFloat()
            // Use a wider "tooth" by combining 2 step segments per tooth.
            val verts = toothCount * 2
            for (i in 0 until verts) {
                val angle = -Math.PI.toFloat() / 2 + i * step
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + r * kotlin.math.cos(angle)
                val y = cy + r * kotlin.math.sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path, NeonCyan)
        // Center hole
        drawCircle(Color.Black, innerR * 0.5f, Offset(cx, cy))
        drawCircle(NeonCyan, innerR * 0.5f, Offset(cx, cy), style = Stroke(width = size.width * 0.04f))
    }
}
