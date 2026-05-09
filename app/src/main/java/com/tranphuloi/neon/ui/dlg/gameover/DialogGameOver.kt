package com.tranphuloi.neon.ui.dlg.gameover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.tranphuloi.neon.common.Blue
import com.tranphuloi.neon.common.Pink
import com.tranphuloi.neon.utils.Logger

@Composable
fun DialogGameOver(score: String, onRestartGame: () -> Unit) {
    LaunchedEffect(Unit) { Logger.d("DialogGameOver shown (score=$score)") }
    Card(
        backgroundColor = Blue,
        border = BorderStroke(2.dp, Pink)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_over_dialog_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h4
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.game_over_dialog_score, score),
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                Logger.d("DialogGameOver: Restart pressed")
                onRestartGame()
            }) {
                Text(
                    text = stringResource(id = R.string.restart_game_button),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}
