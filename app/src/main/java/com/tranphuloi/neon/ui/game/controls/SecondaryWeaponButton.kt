package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.neonGlow

/**
 * Wave 6 (29x) round 40 — secondary-weapon fire button. Same footprint as
 * [SmartBombButton] but the badge is a cooldown ring instead of a count:
 *
 * - `cooldownProgress = 0f` → ring full (button on cooldown, just fired)
 * - `cooldownProgress = 1f` → ring empty (button ready, tap to fire)
 *
 * Disabled (no glow, dim border) when [cooldownProgress] < 1f.
 */
@Composable
fun SecondaryWeaponButton(
    glyph: String,
    cooldownProgress: Float,                  // 0f..1f
    onFire: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = cooldownProgress >= 1f
    val accent = if (ready) NeonCyan else Color.White.copy(alpha = 0.25f)
    Box(
        modifier = modifier
            .size(42.dp)
            .pointerInput(ready) {
                if (ready) {
                    detectTapGestures(onTap = { onFire() })
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(BorderStroke(1.5.dp, accent), CircleShape)
                .neonGlow(
                    color = accent,
                    intensity = if (ready) 0.55f else 0.0f,
                    radiusFactor = 1.5f,
                ),
        ) {
            Text(text = glyph, fontSize = 16.sp)
        }
        // Cooldown ring overlay — draws around the button face. Filled clockwise
        // from 12 o'clock based on (1f - cooldownProgress). When ready, ring is
        // invisible (sweep=0).
        if (!ready) {
            Canvas(
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center),
            ) {
                val sweep = (1f - cooldownProgress).coerceIn(0f, 1f) * 360f
                drawArc(
                    color = NeonCyan.copy(alpha = 0.85f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    size = Size(size.width, size.height),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}
