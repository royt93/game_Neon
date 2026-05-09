package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import kotlinx.coroutines.delay

/**
 * E-c: Power-up duration UI. One badge per active booster, with circular countdown
 * + flash warning when remaining < 2s.
 */
@Composable
fun PowerUpIndicators(
    ship: Ship,
    modifier: Modifier = Modifier,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Only tick when at least 1 booster is active — saves CPU when no powerups.
    val anyActive = ship.shieldEnabled || ship.laserBoosterEnabled || ship.tripleLaserBoosterEnabled
    LaunchedEffect(anyActive) {
        if (!anyActive) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(150L)                              // ~7fps for countdown ring is plenty
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        if (ship.shieldEnabled && ship.shieldEndMillis > 0L) {
            PowerUpBadge(
                label = "S",
                color = NeonCyan,
                endMillis = ship.shieldEndMillis,
                durationMillis = SHIELD_DURATION,
                nowMillis = nowMillis,
            )
        }
        if (ship.laserBoosterEnabled && ship.laserBoosterEndMillis > 0L) {
            PowerUpBadge(
                label = "L",
                color = NeonGold,
                endMillis = ship.laserBoosterEndMillis,
                durationMillis = LASER_DURATION,
                nowMillis = nowMillis,
            )
        }
        if (ship.tripleLaserBoosterEnabled && ship.tripleLaserBoosterEndMillis > 0L) {
            PowerUpBadge(
                label = "T",
                color = NeonMagenta,
                endMillis = ship.tripleLaserBoosterEndMillis,
                durationMillis = TRIPLE_DURATION,
                nowMillis = nowMillis,
            )
        }
    }
}

@Composable
private fun PowerUpBadge(
    label: String,
    color: Color,
    endMillis: Long,
    durationMillis: Long,
    nowMillis: Long,
) {
    val remainingMillis = (endMillis - nowMillis).coerceAtLeast(0L)
    val progress = (remainingMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    val warning = remainingMillis < 2000L && remainingMillis > 0L
    val effectiveColor = if (warning) NeonRedAlert else color

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .neonGlow(color = effectiveColor, intensity = if (warning) 0.7f else 0.4f),
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val r = size.minDimension / 2 - 4f
            // Track.
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = r,
                center = Offset(size.width / 2, size.height / 2),
                style = Stroke(width = 3f),
            )
            // Progress arc — sweepAngle proportional to remaining.
            val sweep = 360f * progress
            drawArc(
                color = effectiveColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 3f),
                topLeft = Offset((size.width - r * 2) / 2, (size.height - r * 2) / 2),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            )
        }
        Text(
            text = label,
            color = effectiveColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// Mirrored from ShipController to compute ratio without coupling.
private const val SHIELD_DURATION: Long = 10_000L
private const val LASER_DURATION: Long = 15_000L
private const val TRIPLE_DURATION: Long = 20_000L
