package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.game.controls.SynergyBanner
import com.tranphuloi.neon.ui.game.ship.ship.SynergyKind
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Task 33 audit fix — chốt SynergyBanner render đúng "%" đơn (không phải "%%")
 * cho cả 3 SynergyKind. Assert bằng substring số + "%" (locale-agnostic, vì
 * values/values-vi/values-en có thể khác câu chữ tuỳ locale thiết bị test).
 */
class SynergyBannerWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(kind: SynergyKind) {
        composeRule.setContent {
            NeonTheme {
                SynergyBanner(kind = kind, shownAtMillis = System.currentTimeMillis())
            }
        }
    }

    private fun assertNoDoublePercent() {
        assertTrue(
            "banner text must not contain literal '%%'",
            composeRule.onAllNodesWithText("%%", substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun shield_weapon_synergy_shows_single_percent() {
        render(SynergyKind.SHIELD_WEAPON)
        composeRule.onNodeWithText("15%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }

    @Test
    fun magnet_supercharge_synergy_shows_single_percent() {
        render(SynergyKind.MAGNET_SUPERCHARGE)
        composeRule.onNodeWithText("25%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }

    @Test
    fun vampire_dmg_synergy_shows_single_percent() {
        render(SynergyKind.VAMPIRE_DMG)
        composeRule.onNodeWithText("10%", substring = true).assertIsDisplayed()
        assertNoDoublePercent()
    }
}
