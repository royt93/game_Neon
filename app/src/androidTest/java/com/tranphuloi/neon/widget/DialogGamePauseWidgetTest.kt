package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.dlg.gamepause.DialogGamePause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Widget test cho [DialogGamePause] — chốt (1) các nút hành động render đúng nhãn,
 * (2) tap từng nút bắn đúng callback tương ứng.
 *
 * NeonDialogButton render text dạng "$leadingGlyph $text" (vd "▶ TIẾP TỤC") nên
 * dùng substring = true để khớp phần chữ, độc lập với glyph dẫn đầu.
 */
class DialogGamePauseWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun all_action_buttons_render() {
        composeRule.setContent {
            NeonTheme {
                DialogGamePause(onResumeGame = {}, onRestartGame = {})
            }
        }
        composeRule.waitForText("TIẾP TỤC", substring = true)
        composeRule.onNodeWithText("TIẾP TỤC", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CHƠI LẠI", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CÀI ĐẶT", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("CHỤP ẢNH", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("VỀ MENU", substring = true).assertIsDisplayed()
    }

    @Test
    fun resume_button_fires_only_resume_callback() {
        var resume = 0
        var restart = 0
        composeRule.setContent {
            NeonTheme {
                DialogGamePause(
                    onResumeGame = { resume++ },
                    onRestartGame = { restart++ },
                )
            }
        }
        composeRule.waitForText("TIẾP TỤC", substring = true)
        composeRule.onNodeWithText("TIẾP TỤC", substring = true).performClick()

        assertEquals("Resume phải bắn đúng 1 lần", 1, resume)
        assertEquals("Restart không được bắn khi bấm Resume", 0, restart)
    }

    @Test
    fun restart_button_fires_only_restart_callback() {
        var resume = 0
        var restart = 0
        composeRule.setContent {
            NeonTheme {
                DialogGamePause(
                    onResumeGame = { resume++ },
                    onRestartGame = { restart++ },
                )
            }
        }
        composeRule.waitForText("CHƠI LẠI", substring = true)
        composeRule.onNodeWithText("CHƠI LẠI", substring = true).performClick()

        assertEquals("Restart phải bắn đúng 1 lần", 1, restart)
        assertTrue("Resume không được bắn khi bấm Restart", resume == 0)
    }
}
