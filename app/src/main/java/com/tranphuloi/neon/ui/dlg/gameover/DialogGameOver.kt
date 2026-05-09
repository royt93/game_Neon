package com.tranphuloi.neon.ui.dlg.gameover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
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
import com.tranphuloi.neon.utils.Logger

@Composable
fun DialogGameOver(score: String, onRestartGame: () -> Unit) {
    LaunchedEffect(Unit) { Logger.d("DialogGameOver shown (score=$score)") }
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
