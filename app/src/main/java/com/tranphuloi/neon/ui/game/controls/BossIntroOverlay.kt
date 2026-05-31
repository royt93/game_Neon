package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow

/** Wave 16 — full-screen cinematic duration (matches HitStopController freeze). */
private const val DURATION_MILLIS: Long = 2400L

/**
 * Wave 16 — Boss intro CINEMATIC (user pick: full-screen 2-3s, freeze + skip).
 *
 * Replaces the old compact top-banner. While this shows, the simulation is
 * frozen via [com.tranphuloi.neon.ui.game.hitstop.HitStopController] (the boss
 * fight only begins when the cinematic ends). Tapping anywhere calls [onSkip],
 * which ends both the cinematic and the freeze early.
 *
 * Stages (t = elapsed / DURATION):
 *   - 0.00–0.15  enter: scrim fades in, boss name zooms 0.6→1.0
 *   - 0.15–0.82  hold:  name + taunt + pulsing alarm border
 *   - 0.82–1.00  exit:  everything fades out → gameplay resumes
 */
@Composable
fun BossIntroOverlay(
    bossName: String,
    bossTaunt: String,
    shownAtMillis: Long,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shownAtMillis == 0L || bossName.isBlank()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        // ~33ms ticks for the cinematic's own animation (independent of the
        // frozen sim, which advances on wall-clock once unfrozen).
        repeat(80) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(30L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > DURATION_MILLIS) return
    val t = elapsed.toFloat() / DURATION_MILLIS

    // Scrim + content alpha envelope.
    val enter = (t / 0.15f).coerceIn(0f, 1f)
    val exit = if (t > 0.82f) 1f - ((t - 0.82f) / 0.18f).coerceIn(0f, 1f) else 1f
    val envelope = enter * exit
    val scrimAlpha = 0.82f * envelope
    // Name zoom-in 0.6 → 1.0 during enter, tiny settle after.
    val nameScale = 0.6f + 0.4f * enter
    // Alarm border pulse.
    val pulse = (kotlin.math.sin(t * Math.PI.toFloat() * 6f) * 0.5f + 0.5f)
    val borderAlpha = pulse * envelope

    val noopInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Dark scrim → reads as "frozen"; captures the tap to skip.
            .background(NeonBgDeep.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = noopInteraction,
                indication = null,
                onClick = onSkip,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Pulsing red alarm border around the whole screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(BorderStroke(10.dp, NeonRedAlert.copy(alpha = borderAlpha)), RectangleShape),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .graphicsLayer { alpha = envelope },
        ) {
            Text(
                text = "⚠ NGUY HIỂM",
                color = NeonRedAlert.copy(alpha = (0.6f + 0.4f * pulse)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                style = TextStyle(letterSpacing = 6.sp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            // Boss name — huge, centered, zoom-in + neon glow.
            Text(
                text = bossName,
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                style = TextStyle(letterSpacing = 2.sp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = nameScale
                        scaleY = nameScale
                    }
                    .neonGlow(NeonRedAlert, intensity = 0.6f, radiusFactor = 1.4f),
            )
            if (bossTaunt.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(BorderStroke(1.dp, NeonRedAlert.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                ) {
                    Text(
                        text = "“$bossTaunt”",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Chạm để bỏ qua",
                color = Color.White.copy(alpha = 0.45f * envelope),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
