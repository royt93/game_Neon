package com.tranphuloi.neon.widget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.game.controls.PowerUpIndicators
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Task 01 (Slice 7) — widget test cho badge số drone trong [PowerUpIndicators]:
 *  - droneCount > 0 → hiện badge "◈N".
 *  - droneCount == 0 → không có badge drone.
 *
 * Ship dựng không buff (mọi *Enabled = false) → `anyActive` false → LaunchedEffect
 * countdown return ngay, không cần chỉnh mainClock (khác ComboHud).
 */
class PowerUpIndicatorsDroneWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val idleShip = Ship(xOffset = 0f, yOffset = 0f)

    @Test
    fun shows_drone_badge_with_count_when_drones_active() {
        composeRule.setContent {
            NeonTheme {
                PowerUpIndicators(ship = idleShip, droneCount = 2)
            }
        }
        composeRule.onNodeWithText("◈2").assertIsDisplayed()
    }

    @Test
    fun hides_drone_badge_when_no_drones() {
        composeRule.setContent {
            NeonTheme {
                PowerUpIndicators(ship = idleShip, droneCount = 0)
            }
        }
        // Không dùng Kotlin assert() (bị tắt trên Android → no-op) — dùng JUnit.
        assertTrue(
            "droneCount == 0 thì không được render badge drone (◈)",
            composeRule.onAllNodesWithText("◈", substring = true).fetchSemanticsNodes().isEmpty(),
        )
    }
}
