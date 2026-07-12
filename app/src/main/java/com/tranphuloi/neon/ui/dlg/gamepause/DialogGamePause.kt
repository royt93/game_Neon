package com.tranphuloi.neon.ui.dlg.gamepause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonDialogButton
import com.tranphuloi.neon.utils.Logger

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogGamePause(
    onResumeGame: () -> Unit,
    onRestartGame: () -> Unit,
    onSettings: () -> Unit = {},
    onBackToMenu: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    LaunchedEffect(Unit) { Logger.d("DialogGamePause shown") }
    // Task 21 (QoL) — restart giữa chừng cần confirm bước 2 để tránh bấm nhầm
    // mất tiến trình đang chơi (khác GameOver: đã chết, không mất gì thêm).
    var confirmingRestart by remember { mutableStateOf(false) }
    // Round 28 — migrated NeonDialog → NeonBottomSheet. dismissible = false so
    // user can't accidentally swipe away mid-fight. ✕ acts as Resume (most
    // natural "dismiss" for pause).
    NeonBottomSheet(
        title = stringResource(id = R.string.game_pause_dialog_title),
        accentColor = palette.cyan,
        titleSize = 28.sp,
        dismissible = false,
        onDismiss = {
            Logger.d("DialogGamePause: ✕ tapped → resume game")
            onResumeGame()
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // testTagsAsResourceId: dialog là window riêng, KHÔNG thừa hưởng cờ này
            // từ NavHost root → khai báo tại đây để androidTest (UiAutomator) định vị
            // từng nút bằng By.res("pause_*").
            modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        ) {
            if (confirmingRestart) {
                Text(
                    text = stringResource(id = R.string.restart_confirm_message),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("pause_restart_confirm_message"),
                )
                NeonDialogButton(
                    text = stringResource(id = R.string.restart_confirm_yes_button).uppercase(),
                    color = palette.redAlert,
                    leadingGlyph = "↻",
                    modifier = Modifier.testTag("pause_restart_confirm_yes"),
                    onClick = {
                        Logger.d("DialogGamePause: Restart confirmed")
                        onRestartGame()
                    },
                )
                NeonDialogButton(
                    text = stringResource(id = R.string.restart_confirm_cancel_button).uppercase(),
                    color = palette.cyan,
                    leadingGlyph = "✕",
                    modifier = Modifier.testTag("pause_restart_cancel"),
                    onClick = {
                        Logger.d("DialogGamePause: Restart cancelled")
                        confirmingRestart = false
                    },
                )
                return@Column
            }
            NeonDialogButton(
                text = stringResource(id = R.string.resume_game_button).uppercase(),
                color = palette.cyan,
                leadingGlyph = "▶",
                modifier = Modifier.testTag("pause_resume"),
                onClick = {
                    Logger.d("DialogGamePause: Resume pressed")
                    onResumeGame()
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.restart_game_button).uppercase(),
                color = palette.magenta,
                leadingGlyph = "↻",
                modifier = Modifier.testTag("pause_restart"),
                onClick = {
                    Logger.d("DialogGamePause: Restart pressed → asking confirm")
                    confirmingRestart = true
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.settings_button).uppercase(),
                color = palette.cyan,
                leadingGlyph = "⚙",
                modifier = Modifier.testTag("pause_settings"),
                onClick = {
                    Logger.d("DialogGamePause: Settings pressed")
                    onSettings()
                },
            )
            // Round 51 (26x Photo mode) — capture screenshot of frozen game
            // world (HUD hidden via gameState.photoModeActive flag).
            NeonDialogButton(
                text = "Chụp ảnh",
                color = palette.gold,
                leadingGlyph = "📸",
                modifier = Modifier.testTag("pause_capture"),
                onClick = {
                    Logger.d("DialogGamePause: Capture photo pressed")
                    onCapturePhoto()
                },
            )
            // Round 27 — exit to menu. Checkpoint preserved so user can resume
            // via "TIẾP TỤC" from MenuScreen later.
            NeonDialogButton(
                text = "Về menu",
                color = palette.gold,
                leadingGlyph = "◀",
                modifier = Modifier.testTag("pause_menu"),
                onClick = {
                    Logger.d("DialogGamePause: Back to Menu pressed (checkpoint preserved)")
                    onBackToMenu()
                },
            )
        }
    }
}
