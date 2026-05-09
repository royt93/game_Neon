package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.damage.DamageNumber
import kotlinx.coroutines.delay

/**
 * 2c+10b: Floating damage numbers overlay.
 *  - Per-frame compute progress for each number.
 *  - Crit (≥100) renders larger + red.
 *  - Float upward 40dp + alpha fade over 600ms lifetime.
 */
@Composable
fun DamageNumbersOverlay(
    numbers: List<DamageNumber>,
    bossIntroShownAtMillis: Long = 0L,
) {
    if (numbers.isEmpty()) return
    // Skip rendering during 1.5s boss intro — banner at top should not be cluttered
    // with damage numbers from upper-row enemies (per user feedback).
    if (bossIntroShownAtMillis > 0L &&
        System.currentTimeMillis() - bossIntroShownAtMillis < 1500L) {
        return
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasNumbers = numbers.isNotEmpty()
    LaunchedEffect(hasNumbers) {
        // BUG fix: short-circuit when list empty so we don't keep ticking forever.
        if (!hasNumbers) return@LaunchedEffect
        // 50ms tick (was 33ms) — 20fps is plenty for a number floating up 40dp in 600ms.
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(50L)
        }
    }

    numbers.forEach { dn ->
        val t = dn.progress(nowMillis)
        if (t >= 1f) return@forEach
        val alpha = 1f - t
        val yOffset = dn.initialY - DamageNumber.FLOAT_DISTANCE * t   // float up
        val scale = if (dn.isCrit) {
            // Pop in 0..0.2: 0.6 → 1.4, then settle 1.4 → 1.0
            when {
                t < 0.2f -> 0.6f + (t / 0.2f) * 0.8f
                t < 0.4f -> 1.4f - ((t - 0.2f) / 0.2f) * 0.4f
                else -> 1.0f
            }
        } else {
            0.9f
        }
        Text(
            text = "-${dn.damage}",
            color = if (dn.isCrit) NeonRedAlert else NeonGold,
            fontSize = if (dn.isCrit) 22.sp else 14.sp,
            fontWeight = if (dn.isCrit) FontWeight.Black else FontWeight.Bold,
            modifier = Modifier
                .offset(x = dn.xOffset.dp, y = yOffset.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}
