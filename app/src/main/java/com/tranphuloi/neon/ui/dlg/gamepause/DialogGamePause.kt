package com.tranphuloi.neon.ui.dlg.gamepause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonDialogButton
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.utils.Logger

@Composable
fun DialogGamePause(
    onResumeGame: () -> Unit,
    onRestartGame: () -> Unit,
    onSettings: () -> Unit = {},
    onBackToMenu: () -> Unit = {},
) {
    LaunchedEffect(Unit) { Logger.d("DialogGamePause shown") }
    // Round 28 — migrated NeonDialog → NeonBottomSheet. dismissible = false so
    // user can't accidentally swipe away mid-fight. ✕ acts as Resume (most
    // natural "dismiss" for pause).
    NeonBottomSheet(
        title = stringResource(id = R.string.game_pause_dialog_title),
        accentColor = NeonCyan,
        titleSize = 28.sp,
        dismissible = false,
        onDismiss = {
            Logger.d("DialogGamePause: ✕ tapped → resume game")
            onResumeGame()
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            NeonDialogButton(
                text = stringResource(id = R.string.resume_game_button).uppercase(),
                color = NeonCyan,
                leadingGlyph = "▶",
                onClick = {
                    Logger.d("DialogGamePause: Resume pressed")
                    onResumeGame()
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.restart_game_button).uppercase(),
                color = NeonMagenta,
                leadingGlyph = "↻",
                onClick = {
                    Logger.d("DialogGamePause: Restart pressed")
                    onRestartGame()
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.settings_button).uppercase(),
                color = NeonCyan,
                leadingGlyph = "⚙",
                onClick = {
                    Logger.d("DialogGamePause: Settings pressed")
                    onSettings()
                },
            )
            // Round 27 — exit to menu. Checkpoint preserved so user can resume
            // via "TIẾP TỤC" from MenuScreen later.
            NeonDialogButton(
                text = "VỀ MENU",
                color = NeonGold,
                leadingGlyph = "◀",
                onClick = {
                    Logger.d("DialogGamePause: Back to Menu pressed (checkpoint preserved)")
                    onBackToMenu()
                },
            )
        }
    }
}
