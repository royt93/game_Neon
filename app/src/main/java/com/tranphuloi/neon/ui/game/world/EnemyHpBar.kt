package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import kotlinx.coroutines.delay

/**
 * 3-layer enemy HP bar with damage trail (Diablo/MOBA style).
 *
 * Layers (bottom → top):
 *   1. Black background + colored border (full bar width)
 *   2. White damage trail — lerps from previous HP down to currentHp over 350ms.
 *      When new damage lands while trail is animating, the trail re-targets to
 *      the new lower currentHp from its current position (no jarring snap).
 *   3. Foreground fill (color-tiered): cyan >66%, gold >33%, red ≤33%.
 *
 * The trail being above the foreground fill is intentional — it visually extends
 * the bar past the current fill, showing how much damage was just absorbed.
 */
@Composable
fun EnemyHpBar(
    enemyId: String,
    currentHp: Float,
    initialHp: Float,
    enemyWidth: Float,
    modifier: Modifier = Modifier,
) {
    val safeInitial = initialHp.coerceAtLeast(1f)
    val hpRatio = (currentHp / safeInitial).coerceIn(0f, 1f)
    val barWidth = enemyWidth * 0.85f
    val hpPx = barWidth * hpRatio
    val hpColor = when {
        hpRatio > 0.66f -> NeonCyan
        hpRatio > 0.33f -> NeonGold
        else -> NeonRedAlert
    }

    // Trail HP — lerps toward currentHp over 350ms whenever currentHp drops.
    // Keyed on enemyId so when slot is reused for a new enemy, state resets and
    // doesn't inherit a dead enemy's trail position.
    var trailHp by remember(enemyId) { mutableFloatStateOf(currentHp) }
    LaunchedEffect(currentHp) {
        if (trailHp <= currentHp) {
            // No drop (or trail already below — e.g., heal) — sync immediately.
            trailHp = currentHp
            return@LaunchedEffect
        }
        val startTrail = trailHp
        val target = currentHp
        val durationMs = 350L
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= durationMs) {
                trailHp = target
                break
            }
            val t = elapsed.toFloat() / durationMs
            val ease = 1f - (1f - t) * (1f - t)        // ease-out quadratic
            trailHp = startTrail + (target - startTrail) * ease
            delay(16L)
        }
    }
    val trailRatio = (trailHp / safeInitial).coerceIn(0f, 1f)
    val trailPx = barWidth * trailRatio

    Box(
        modifier = modifier
            .padding(start = (enemyWidth * 0.075f).dp, bottom = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .size(width = barWidth.dp, height = 6.dp)
            .background(Color.Black.copy(alpha = 0.75f))
            .border(
                BorderStroke(1.dp, hpColor.copy(alpha = 0.9f)),
                MaterialTheme.shapes.small,
            )
    ) {
        // Layer 2: white damage trail — extends past fill, fades trail end.
        if (trailPx > hpPx) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .size(width = trailPx.dp, height = 6.dp)
                    .background(Color.White.copy(alpha = 0.75f))
            )
        }
        // Layer 3: actual HP fill — color-tiered.
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .size(width = hpPx.dp, height = 6.dp)
                .background(hpColor)
        )
    }
}
