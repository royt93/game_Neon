package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.tranphuloi.neon.App
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.dlg.difficulty.DialogDifficultyPicker
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Widget test cho [DialogDifficultyPicker] — chốt 3 lựa chọn độ khó (DỄ/VỪA/KHÓ)
 * render đúng, và tap một lựa chọn bắn callback onPicked.
 *
 * Dùng lại [App.settings] (singleton App tạo trong onCreate) qua LocalSettings —
 * KHÔNG new SettingsRepository để tránh xung đột "multiple DataStores cùng file".
 */
class DialogDifficultyPickerWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settings by lazy {
        ApplicationProvider.getApplicationContext<App>().settings
    }

    @Test
    fun three_difficulty_options_render() {
        composeRule.setContent {
            NeonTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalSettings provides settings) {
                    DialogDifficultyPicker(onPicked = {})
                }
            }
        }
        composeRule.waitForText("DỄ")
        composeRule.onNodeWithText("DỄ").assertIsDisplayed()
        composeRule.onNodeWithText("VỪA").assertIsDisplayed()
        composeRule.onNodeWithText("KHÓ").assertIsDisplayed()
    }

    @Test
    fun tapping_a_difficulty_fires_on_picked() {
        var picked = 0
        composeRule.setContent {
            NeonTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalSettings provides settings) {
                    DialogDifficultyPicker(onPicked = { picked++ })
                }
            }
        }
        composeRule.waitForText("VỪA")
        composeRule.onNodeWithText("VỪA").performClick()

        // setDifficulty là suspend chạy trong rememberCoroutineScope → onPicked
        // bắn sau khi write xong; chờ tới khi callback được gọi.
        composeRule.waitUntil(timeoutMillis = 5_000) { picked >= 1 }
        assertTrue("tap độ khó phải bắn onPicked ít nhất 1 lần", picked >= 1)
    }
}
