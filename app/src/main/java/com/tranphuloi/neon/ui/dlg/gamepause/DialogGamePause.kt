package com.tranphuloi.neon.ui.dlg.gamepause

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonDialogButton
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
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
            // testTagsAsResourceId: dialog là window riêng, KHÔNG thừa hưởng cờ này
            // từ NavHost root → khai báo tại đây để androidTest (UiAutomator) định vị
            // từng nút bằng By.res("pause_*").
            modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        ) {
            NeonDialogButton(
                text = stringResource(id = R.string.resume_game_button).uppercase(),
                color = NeonCyan,
                leadingGlyph = "▶",
                modifier = Modifier.testTag("pause_resume"),
                onClick = {
                    Logger.d("DialogGamePause: Resume pressed")
                    onResumeGame()
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.restart_game_button).uppercase(),
                color = NeonMagenta,
                leadingGlyph = "↻",
                modifier = Modifier.testTag("pause_restart"),
                onClick = {
                    Logger.d("DialogGamePause: Restart pressed")
                    onRestartGame()
                },
            )
            NeonDialogButton(
                text = stringResource(id = R.string.settings_button).uppercase(),
                color = NeonCyan,
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
                text = "CHỤP ẢNH",
                color = NeonGold,
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
                text = "VỀ MENU",
                color = NeonGold,
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
