package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.neonGlow

/**
 * Task 31 — Overdrive bullet-time meter. Ring-progress pattern copied from
 * [ComboHud]: fills as [killCount] approaches [threshold], flips to a bright
 * "OVERDRIVE!" label while [active].
 */
@Composable
fun OverdriveMeterHud(
    killCount: Int,
    threshold: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (killCount == 0 && !active) return
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val progress = if (active) 1f else (killCount.toFloat() / threshold).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
            Canvas(modifier = Modifier.size(32.dp)) {
                val r = size.minDimension / 2 - 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = r,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 2.5f),
                )
                drawArc(
                    color = palette.cyan,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 2.5f),
                    topLeft = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                )
            }
            Text(
                text = if (active) "⚡" else "$killCount",
                color = palette.cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.neonGlow(palette.cyan, intensity = 0.4f, radiusFactor = 1.2f),
            )
        }
        if (active) {
            Text(
                text = stringResource(R.string.overdrive_active_label),
                color = palette.cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.neonGlow(palette.cyan, intensity = 0.5f, radiusFactor = 1.2f),
            )
        }
    }
}
