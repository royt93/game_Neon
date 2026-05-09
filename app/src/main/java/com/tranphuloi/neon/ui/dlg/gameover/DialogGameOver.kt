package com.tranphuloi.neon.ui.dlg.gameover

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonDialog
import com.tranphuloi.neon.common.NeonDialogButton
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

    NeonDialog(
        title = stringResource(id = R.string.game_over_dialog_title),
        accentColor = NeonRedAlert,
        titleSecondaryColor = NeonGold,
        titleSize = 36.sp,
        body = {
            ScoreRow(
                label = "MINERALS",
                value = score,
                accentColor = NeonGold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ScoreRow(
                label = "BEST",
                value = highestSoFar.toString(),
                accentColor = NeonCyan,
            )
            if (isNewBest && currentScore > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGold.copy(alpha = 0.25f))
                        .border(
                            BorderStroke(1.5.dp, NeonGold),
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
            LeaderboardList(
                entries = entries,
                currentScore = currentScore,
                playerRank = playerRank,
            )
        },
        actions = {
            NeonDialogButton(
                text = stringResource(id = R.string.restart_game_button).uppercase(),
                color = NeonCyan,
                leadingGlyph = "▶",
                onClick = {
                    Logger.d("DialogGameOver: Restart pressed")
                    onRestartGame()
                },
            )
        },
    )
}

@Composable
private fun ScoreRow(label: String, value: String, accentColor: Color) {
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
                intensity = 0.55f,
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
                        .background(NeonGold.copy(alpha = 0.18f))
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
