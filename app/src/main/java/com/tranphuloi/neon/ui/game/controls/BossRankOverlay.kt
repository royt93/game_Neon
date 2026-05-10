package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import kotlinx.coroutines.delay

private const val DURATION_MILLIS = 1800L

/** S = perfect, A = excellent, B = good, C = OK, D = poor */
enum class BossRank(val letter: String, val color: () -> Color, val label: String) {
    S(letter = "S", color = { NeonGold }, label = "PERFECT KILL"),
    A(letter = "A", color = { NeonCyan }, label = "EXCELLENT"),
    B(letter = "B", color = { NeonMagenta }, label = "GOOD"),
    C(letter = "C", color = { NeonCyan.copy(alpha = 0.7f) }, label = "OK"),
    D(letter = "D", color = { NeonRedAlert }, label = "BARELY");

    companion object {
        /**
         * Compute rank from kill time (ms since boss spawned) + hpRatio
         * (current_hp / hp_at_boss_spawn — 1.0 = no damage taken, 0.0 = all dropped).
         */
        fun compute(timeToKillMs: Long, hpRatio: Float): BossRank {
            val noDamage = hpRatio >= 0.99f
            val mostlyKept = hpRatio >= 0.7f
            return when {
                timeToKillMs < 30_000L && noDamage -> S
                timeToKillMs < 45_000L && mostlyKept -> A
                timeToKillMs < 60_000L -> B
                timeToKillMs < 90_000L -> C
                else -> D
            }
        }
    }
}

/**
 * Boss kill rank overlay — shown 1.8s after a boss is killed. Big letter (S/A/B/C/D)
 * with neon glow + label below + radial halo backdrop.
 */
@Composable
fun BossRankOverlay(
    rank: BossRank?,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (rank == null || shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        repeat((DURATION_MILLIS / 33).toInt()) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > DURATION_MILLIS) return
    val t = elapsed.toFloat() / DURATION_MILLIS
    // Pop in 0..0.15 (0.4 → 1.6), settle 0.15..0.35 (1.6 → 1.0), hold, fade out 0.85..1
    val scale = when {
        t < 0.15f -> 0.4f + (t / 0.15f) * 1.2f
        t < 0.35f -> 1.6f - ((t - 0.15f) / 0.20f) * 0.6f
        else -> 1.0f
    }
    val alpha = if (t < 0.85f) 1f else 1f - ((t - 0.85f) / 0.15f)
    val color = rank.color()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) {
        // Halo backdrop.
        Canvas(modifier = Modifier.size(280.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.6f),
                        color.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        // 3-layer text: outer halo + mid glow + crisp white core.
        Text(
            text = rank.letter,
            color = color.copy(alpha = 0.32f),
            fontSize = 152.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer { scaleX = 1.10f; scaleY = 1.10f },
        )
        Text(
            text = rank.letter,
            color = color.copy(alpha = 0.7f),
            fontSize = 144.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer { scaleX = 1.05f; scaleY = 1.05f },
        )
        Text(
            text = rank.letter,
            color = Color.White,
            fontSize = 138.sp,
            fontWeight = FontWeight.Black,
        )
        // Label below the letter — small descriptive text. Use `offset` (dp-aware)
        // not `graphicsLayer.translationY` (pixels — caused overlap on the 138sp S
        // letter because 90px ≈ 25dp at density 3.5x, well inside the letter's
        // ±69dp vertical extent). 100dp puts the label clearly below the descender.
        Text(
            text = rank.label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 100.dp),
        )
    }
}
