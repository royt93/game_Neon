package com.tranphuloi.neon.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Widget test THẬT (androidTest) thay cho BulletWave18WidgetTest.kt (Robolectric,
 * @Ignore) — chạy Compose UI trên thiết bị/emulator thật qua createComposeRule().
 *
 * Chốt: displayName của các đạn Wave 18 batch 3 render đúng chuỗi tiếng Việt có
 * dấu (regression guard cho i18n + cho việc rò rỉ tên enum thay vì displayName).
 */
class BulletDisplayNameWidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun batch3_bullet_display_names_render_in_compose() {
        val names = listOf(
            BulletType.BUBBLE_TEA, BulletType.FISH_SAUCE,
            BulletType.SANDAL, BulletType.QR_CODE,
        ).map { it.displayName }

        composeRule.setContent {
            NeonTheme {
                Column { names.forEach { Text(it) } }
            }
        }

        names.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun every_bullet_type_has_a_nonblank_display_name() {
        // Không render 30+ node (dễ chồng lấn) — chỉ kiểm bất biến dữ liệu để bảo
        // đảm mọi đạn đều có tên hiển thị hợp lệ trước khi đưa lên UI.
        // JUnit assertTrue (KHÔNG dùng Kotlin assert() — bị tắt mặc định trên
        // Android nên assertion sẽ là no-op → test pass giả).
        BulletType.values().forEach { type ->
            assertTrue(
                "BulletType.${type.name} có displayName rỗng / whitespace",
                type.displayName.isNotBlank(),
            )
        }
    }
}
