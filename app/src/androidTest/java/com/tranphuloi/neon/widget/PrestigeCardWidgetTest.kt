package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.shop.PrestigeCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Task 10 (đợt 3, audit→9.5) — widget test cho [PrestigeCard]: render đúng theo
 * state (cấp/buff/cost/gate) + click chỉ kích hoạt khi đủ điều kiện.
 */
class PrestigeCardWidgetTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun card_enabled_shows_level_buff_cost_and_ready() {
        rule.setContent {
            NeonTheme {
                PrestigeCard(level = 2, cost = 6000, skillRanks = 10, minRanks = 8,
                    canPrestige = true, onPrestige = {})
            }
        }
        rule.onNodeWithText("Prestige · cấp 2", substring = true).assertIsDisplayed()
        rule.onNodeWithText("+8% mọi chỉ số", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Sẵn sàng", substring = true).assertIsDisplayed()
        rule.onNodeWithText("6000◇", substring = true).assertIsDisplayed()
    }

    @Test
    fun card_disabled_by_ranks_shows_gate_and_current_ranks() {
        rule.setContent {
            NeonTheme {
                PrestigeCard(level = 0, cost = 1500, skillRanks = 3, minRanks = 8,
                    canPrestige = false, onPrestige = {})
            }
        }
        rule.onNodeWithText("Cần ≥ 8 rank", substring = true).assertIsDisplayed()
        rule.onNodeWithText("đang 3", substring = true).assertIsDisplayed()
    }

    @Test
    fun click_when_enabled_fires_onPrestige() {
        var clicked = false
        rule.setContent {
            NeonTheme {
                PrestigeCard(level = 1, cost = 3000, skillRanks = 8, minRanks = 8,
                    canPrestige = true, onPrestige = { clicked = true })
            }
        }
        rule.onNodeWithText("Prestige · cấp 1", substring = true).performClick()
        assertTrue("card đủ điều kiện → click phải gọi onPrestige", clicked)
    }

    @Test
    fun click_when_disabled_does_not_fire_onPrestige() {
        var clicked = false
        rule.setContent {
            NeonTheme {
                PrestigeCard(level = 0, cost = 1500, skillRanks = 2, minRanks = 8,
                    canPrestige = false, onPrestige = { clicked = true })
            }
        }
        rule.onNodeWithText("Prestige · cấp 0", substring = true).performClick()
        assertFalse("card disabled → click KHÔNG gọi onPrestige", clicked)
    }
}
