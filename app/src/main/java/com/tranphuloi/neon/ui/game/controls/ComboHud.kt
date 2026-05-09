package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.combo.ComboTier
import kotlinx.coroutines.delay

private const val COMBO_TIMEOUT_MILLIS: Long = 2000L

/**
 * 7c: Combo HUD always-visible khi count > 0.
 *  - x{count} number + multiplier ({tier.multiplier})
 *  - Decay ring shows time-until-expire
 *  - Color tier-coded
 *  - Bonus chain score = count × tierMultiplier
 */
@Composable
fun ComboHud(
    count: Int,
    tier: ComboTier,
    lastKillMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (count == 0 || lastKillMillis == 0L) return

    // Tick at ~10fps to update decay ring (100ms) — combo decay over 2s doesn't need finer res.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastKillMillis) {
        // Auto-stop after combo timeout (no point ticking once expired).
        val deadline = lastKillMillis + COMBO_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            nowMillis = System.currentTimeMillis()
            delay(100L)
        }
    }
    val elapsed = (nowMillis - lastKillMillis).coerceAtLeast(0L)
    val progress = (1f - elapsed.toFloat() / COMBO_TIMEOUT_MILLIS).coerceIn(0f, 1f)
    if (progress <= 0f) return                                          // expired

    val color = when (tier) {
        ComboTier.GODLIKE -> NeonRedAlert
        ComboTier.UNSTOPPABLE, ComboTier.RAMPAGE -> NeonMagenta
        else -> NeonGold
    }
    val chainBonus = count * tier.multiplier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
            // Decay ring track + foreground.
            Canvas(modifier = Modifier.size(32.dp)) {
                val r = size.minDimension / 2 - 2f
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = r,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = 2.5f),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 2.5f),
                    topLeft = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                )
            }
            Text(
                text = "x$count",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.neonGlow(color, intensity = 0.4f, radiusFactor = 1.2f),
            )
        }
        Column {
            Text(
                text = "${tier.multiplier}× MULT",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "+$chainBonus chain",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
