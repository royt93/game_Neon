package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.game.combo.ComboTier
import com.tranphuloi.neon.ui.game.controls.ComboHud
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Widget test cho [ComboHud] — HUD combo hiển thị đúng theo state:
 *  - count == 0 → không render gì (early return).
 *  - count > 0 & vừa kill → render "x<count>" và "<mult>× Mult".
 *
 * autoAdvance = false: ComboHud có LaunchedEffect vòng lặp `while (currentTimeMillis
 * < deadline) { delay(100) }`. deadline tính theo wall-clock nhưng delay theo test
 * clock → auto-advance sẽ quay test clock vô hạn mà wall-clock chưa tới hạn → treo
 * waitForIdle. Tắt autoAdvance + advance thủ công một nhịp là đủ để compose.
 */
class ComboHudWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_count_and_multiplier_when_combo_active() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NeonTheme {
                ComboHud(
                    count = 5,
                    tier = ComboTier.RAMPAGE,
                    lastKillMillis = System.currentTimeMillis(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)

        composeRule.onNodeWithText("x5").assertIsDisplayed()
        // RAMPAGE.multiplier == 4 (xem ComboTier).
        composeRule.onNodeWithText("4× Mult").assertIsDisplayed()
    }

    @Test
    fun renders_nothing_when_count_is_zero() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NeonTheme {
                ComboHud(
                    count = 0,
                    tier = ComboTier.NONE,
                    lastKillMillis = System.currentTimeMillis(),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(50L)

        // Không có bất kỳ node text combo nào. Dùng JUnit assertTrue (KHÔNG dùng
        // Kotlin assert() — nó bị tắt mặc định trên Android → assertion no-op).
        assertTrue(
            "count == 0 thì ComboHud phải early-return, không render Mult",
            composeRule.onAllNodesWithText("Mult", substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }
}
