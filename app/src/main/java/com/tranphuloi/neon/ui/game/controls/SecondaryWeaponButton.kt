package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon

/**
 * Wave 6 (29x) round 40 — secondary-weapon fire button. Round 67.7 vector
 * upgrade: replaced emoji glyphs (🚀💠💥) with Canvas vector drawings per
 * weapon type. Same footprint + cooldown ring as before.
 */
@Composable
fun SecondaryWeaponButton(
    weapon: SecondaryWeapon,
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
            // Round 67.7 — vector icon per weapon. Replaced emoji rendering.
            Canvas(modifier = Modifier.size(22.dp)) {
                drawWeaponIcon(weapon, accent)
            }
        }
        // Cooldown ring overlay
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

/**
 * Round 67.7 — per-weapon vector icon. All inscribed in a 22dp Canvas.
 *
 *   MISSILE: vertical arrow / tear-drop pointing UP với cánh nhỏ + đuôi flame
 *   MINE:    4-spike caltrop diamond + center pulse dot
 *   BURST:   8-ray asterisk + bright center
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWeaponIcon(
    weapon: SecondaryWeapon,
    accent: Color,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    when (weapon) {
        SecondaryWeapon.MISSILE -> {
            // Body: tear-drop pointing up
            val tipY = h * 0.10f
            val baseY = h * 0.78f
            val halfW = w * 0.18f
            val body = Path().apply {
                moveTo(cx, tipY)
                lineTo(cx + halfW, baseY - h * 0.10f)
                lineTo(cx + halfW * 0.6f, baseY)
                lineTo(cx - halfW * 0.6f, baseY)
                lineTo(cx - halfW, baseY - h * 0.10f)
                close()
            }
            drawPath(body, accent)
            // Side fins
            val finPath = Path().apply {
                moveTo(cx - halfW, baseY - h * 0.20f)
                lineTo(cx - halfW * 1.8f, baseY)
                lineTo(cx - halfW * 0.5f, baseY)
                close()
            }
            drawPath(finPath, accent)
            val finPath2 = Path().apply {
                moveTo(cx + halfW, baseY - h * 0.20f)
                lineTo(cx + halfW * 1.8f, baseY)
                lineTo(cx + halfW * 0.5f, baseY)
                close()
            }
            drawPath(finPath2, accent)
            // Flame trail
            drawCircle(NeonGold, w * 0.06f, Offset(cx, baseY + h * 0.04f))
        }
        SecondaryWeapon.MINE -> {
            // 4-spike diamond shape: vertical + horizontal lines crossing
            val rOuter = w * 0.42f
            val rInner = rOuter * 0.30f
            // Outer 4-pointed star (cardinal directions)
            val star = Path().apply {
                moveTo(cx, cy - rOuter)                       // N
                lineTo(cx + rInner, cy - rInner)              // NE
                lineTo(cx + rOuter, cy)                       // E
                lineTo(cx + rInner, cy + rInner)              // SE
                lineTo(cx, cy + rOuter)                       // S
                lineTo(cx - rInner, cy + rInner)              // SW
                lineTo(cx - rOuter, cy)                       // W
                lineTo(cx - rInner, cy - rInner)              // NW
                close()
            }
            drawPath(star, accent)
            // Center pulse
            drawCircle(Color.White.copy(alpha = 0.85f), w * 0.07f, Offset(cx, cy))
        }
        SecondaryWeapon.BURST -> {
            // 8-ray asterisk
            val rOuter = w * 0.42f
            val rInner = rOuter * 0.25f
            // 16 vertices for 8-pointed star
            val star = Path().apply {
                val step = (Math.PI / 8.0).toFloat()
                for (i in 0 until 16) {
                    val angle = -Math.PI.toFloat() / 2 + i * step
                    val r = if (i % 2 == 0) rOuter else rInner
                    val x = cx + r * kotlin.math.cos(angle)
                    val y = cy + r * kotlin.math.sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(star, accent)
            // Bright center
            drawCircle(Color.White.copy(alpha = 0.85f), rOuter * 0.30f, Offset(cx, cy))
        }
    }
}
