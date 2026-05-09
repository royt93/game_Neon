package com.tranphuloi.neon.ui.dlg.gameover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
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

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonRedAlert),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonRedAlert, intensity = 0.4f, radiusFactor = 1.2f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_over_dialog_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h4,
                color = NeonRedAlert
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.game_over_dialog_score, score),
                style = MaterialTheme.typography.h6,
                color = NeonGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BEST: $highestSoFar",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.h6.fontSize,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LeaderboardList(entries = entries)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                Logger.d("DialogGameOver: Restart pressed")
                onRestartGame()
            }) {
                Text(
                    text = stringResource(id = R.string.restart_game_button),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.h6,
                    color = NeonCyan
                )
            }
        }
    }
}

@Composable
private fun LeaderboardList(entries: List<LeaderboardEntry>) {
    if (entries.isEmpty()) return
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        entries.take(5).forEachIndexed { idx, e ->
            Row {
                Text(
                    text = "#${idx + 1}",
                    color = NeonGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.body2.fontSize,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "${e.score}",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.body2.fontSize,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = df.format(Date(e.timestampMillis)),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                    fontSize = MaterialTheme.typography.caption.fontSize,
                )
            }
        }
    }
}
