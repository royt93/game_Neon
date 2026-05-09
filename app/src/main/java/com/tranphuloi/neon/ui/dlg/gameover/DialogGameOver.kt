package com.tranphuloi.neon.ui.dlg.gameover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LeaderboardEntry
import com.tranphuloi.neon.data.LocalLeaderboard
import com.tranphuloi.neon.utils.Logger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialogGameOver(score: String, onRestartGame: () -> Unit) {
    val leaderboard = LocalLeaderboard.current
    val entries by leaderboard.topEntries.collectAsState(initial = emptyList())
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Logger.d("DialogGameOver shown (score=$score)")
        if (!submitted) {
            val parsed = score.toIntOrNull()
            if (parsed != null) {
                Logger.d("DialogGameOver: submitting score=$parsed to leaderboard")
                leaderboard.submit(parsed)
            }
            submitted = true
        }
    }

    val highestSoFar = entries.maxByOrNull { it.score }?.score ?: score.toIntOrNull() ?: 0
    val currentScore = score.toIntOrNull() ?: 0
    val isNewBest = currentScore >= highestSoFar && currentScore > 0
    val playerRank = entries
        .indexOfFirst { it.score == currentScore }
        .let { if (it == -1) null else it + 1 }

    // Pulsing border + glow — drives all animated alpha values uniformly.
    val infiniteTransition = rememberInfiniteTransition(label = "gameOverPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val titleShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titleShimmer",
    )

    // Manual Card replacement: full control over draw layers (gradient bg, two
    // animated border strokes, ambient halo).
    Box(
        modifier = Modifier
            .neonGlow(color = NeonRedAlert, intensity = 0.6f * pulse, radiusFactor = 1.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeonBgDeep,
                        NeonBgMid,
                        NeonBgDeep,
                    ),
                ),
            )
            // Outer pulsing red alarm border.
            .border(
                BorderStroke(2.5.dp, NeonRedAlert.copy(alpha = pulse)),
                RoundedCornerShape(14.dp),
            )
            // Inner subtle cyan accent border for layered neon feel.
            .drawBehind {
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.35f * pulse),
                    topLeft = androidx.compose.ui.geometry.Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 12.dp.toPx(),
                        size.height - 12.dp.toPx(),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            // Animated title — shimmer between red and gold via interpolation.
            val titleColor = lerpColor(NeonRedAlert, NeonGold, titleShimmer * 0.4f)
            Text(
                text = stringResource(id = R.string.game_over_dialog_title),
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                color = titleColor,
                style = TextStyle(letterSpacing = 6.sp),
                modifier = Modifier
                    .neonGlow(color = NeonRedAlert, intensity = 0.7f * pulse, radiusFactor = 1.3f)
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        // Subtle scale pulse to match alpha pulse.
                        scaleX = 0.97f + 0.06f * pulse
                        scaleY = 0.97f + 0.06f * pulse
                    },
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Score row — magenta dramatic mineral count.
            ScoreRow(
                label = "MINERALS",
                value = score,
                accentColor = NeonGold,
                pulse = pulse,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Best — cyan accent.
            ScoreRow(
                label = "BEST",
                value = highestSoFar.toString(),
                accentColor = NeonCyan,
                pulse = pulse,
            )

            // NEW BEST flag — only on first-ever or new high.
            if (isNewBest && currentScore > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGold.copy(alpha = 0.2f + 0.3f * pulse))
                        .border(
                            BorderStroke(1.5.dp, NeonGold.copy(alpha = pulse)),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "★ NEW BEST ★",
                        color = NeonGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        style = TextStyle(letterSpacing = 3.sp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            LeaderboardList(entries = entries, currentScore = currentScore, playerRank = playerRank, pulse = pulse)
            Spacer(modifier = Modifier.height(22.dp))

            // Restart button — pulsing halo ring + neon outlined pill.
            RestartButton(pulse = pulse, onClick = {
                Logger.d("DialogGameOver: Restart pressed")
                onRestartGame()
            })
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String, accentColor: Color, pulse: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(letterSpacing = 3.sp),
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.neonGlow(
                color = accentColor,
                intensity = 0.5f + 0.2f * pulse,
                radiusFactor = 1.3f,
            ),
        )
    }
}

@Composable
private fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    currentScore: Int,
    playerRank: Int?,
    pulse: Float,
) {
    if (entries.isEmpty()) return
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(
                BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "TOP RUNS",
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        entries.take(5).forEachIndexed { idx, e ->
            val isCurrent = playerRank == (idx + 1) && e.score == currentScore
            val rankColor = when {
                isCurrent -> NeonGold
                idx == 0 -> NeonGold
                idx == 1 -> NeonCyan
                idx == 2 -> NeonMagenta
                else -> Color.White.copy(alpha = 0.6f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isCurrent) {
                    Modifier
                        .fillMaxWidth()
                        .background(NeonGold.copy(alpha = 0.12f * pulse))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                },
            ) {
                Text(
                    text = "#${idx + 1}",
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "${e.score}",
                    color = if (isCurrent) NeonGold else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = df.format(Date(e.timestampMillis)),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun RestartButton(pulse: Float, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.15f),
                        NeonCyan.copy(alpha = 0.4f * pulse),
                        NeonCyan.copy(alpha = 0.15f),
                    ),
                ),
            )
            .border(
                BorderStroke(2.dp, NeonCyan.copy(alpha = pulse)),
                RoundedCornerShape(24.dp),
            )
            .neonGlow(color = NeonCyan, intensity = 0.6f * pulse, radiusFactor = 1.5f)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 32.dp, vertical = 12.dp),
    ) {
        Text(
            text = "▶ ${stringResource(id = R.string.restart_game_button).uppercase()}",
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            style = TextStyle(letterSpacing = 4.sp),
        )
    }
}

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * tt,
        green = from.green + (to.green - from.green) * tt,
        blue = from.blue + (to.blue - from.blue) * tt,
        alpha = from.alpha + (to.alpha - from.alpha) * tt,
    )
}
