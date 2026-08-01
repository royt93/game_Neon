package com.tranphuloi.neon.widget

import androidx.compose.material.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.game.buff.RunBuff
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Task 33 audit fix — curse-buff (GLASS_CANNON/SLOTH/RECKLESS) descriptionRes
 * render qua stringResource() KHÔNG format args, giống hệt DialogBuffPicker.kt
 * dòng "buff.descriptionRes?.let { stringResource(it) }". Render trực tiếp
 * (không qua NeonBottomSheet của DialogBuffPicker, vốn reveal không ổn định
 * dưới createComposeRule — xem CLAUDE.md) để chốt riêng phần escaping.
 */
class CurseBuffDescriptionWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(buff: RunBuff) {
        composeRule.setContent {
            NeonTheme {
                Text(text = buff.descriptionRes?.let { stringResource(it) } ?: buff.description)
            }
        }
    }

    private fun assertNoDoublePercent() {
        assertTrue(
            "buff description must not contain literal '%%'",
            composeRule.onAllNodesWithText("%%", substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun glass_cannon_description_shows_single_percent() {
        render(RunBuff.GLASS_CANNON)
        composeRule.onNodeWithText("80%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }

    @Test
    fun sloth_description_shows_single_percent() {
        render(RunBuff.SLOTH)
        composeRule.onNodeWithText("30%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }

    @Test
    fun reckless_description_shows_single_percent() {
        render(RunBuff.RECKLESS)
        composeRule.onNodeWithText("50%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }
}
