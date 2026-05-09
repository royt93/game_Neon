package com.tranphuloi.neon.ui.game.controls

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
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.combo.ComboTier
import kotlinx.coroutines.delay

/**
 * Kc combo popup: shows tier label briefly when [tier] advances. Uses [shownAtMillis]
 * as a key so successive tier advances retrigger the animation.
 */
@Composable
fun ComboPopup(
    tier: ComboTier,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (tier == ComboTier.NONE || shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        // Repaint at 30fps for the popup duration.
        repeat(POPUP_DURATION_MILLIS / 33) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }

    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > POPUP_DURATION_MILLIS) return
    val t = elapsed.toFloat() / POPUP_DURATION_MILLIS // 0..1
    val scale = 0.7f + (1f - kotlin.math.abs(0.5f - t) * 2f) * 0.6f
    val alpha = 1f - t

    val color = when (tier) {
        ComboTier.GODLIKE -> NeonRedAlert
        ComboTier.UNSTOPPABLE, ComboTier.RAMPAGE -> NeonMagenta
        else -> NeonGold
    }

    Text(
        text = tier.displayLabel,
        color = color,
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    )
}

private const val POPUP_DURATION_MILLIS: Int = 1200
