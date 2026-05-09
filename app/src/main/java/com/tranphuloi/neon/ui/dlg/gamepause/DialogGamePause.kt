package com.tranphuloi.neon.ui.dlg.gamepause

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.utils.Logger

@Composable
fun DialogGamePause(
    onResumeGame: () -> Unit,
    onRestartGame: () -> Unit,
    onSettings: () -> Unit = {},
) {
    LaunchedEffect(Unit) { Logger.d("DialogGamePause shown") }
    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonCyan),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonCyan, intensity = 0.35f, radiusFactor = 1.2f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = stringResource(id = R.string.game_pause_dialog_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h4,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    Logger.d("DialogGamePause: Resume pressed")
                    onResumeGame()
                }) {
                    Text(
                        text = stringResource(id = R.string.resume_game_button),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.h6,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    Logger.d("DialogGamePause: Restart pressed")
                    onRestartGame()
                }) {
                    Text(
                        text = stringResource(id = R.string.restart_game_button),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.h6,
                        color = NeonMagenta
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    Logger.d("DialogGamePause: Settings pressed")
                    onSettings()
                }) {
                    Text(
                        text = stringResource(id = R.string.settings_button),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.h6,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}
