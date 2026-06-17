package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Wave 18 — WIDGET TEST (Compose + Robolectric) cho tên 4 đạn batch 3.
 *
 * ⚠️ @Ignore — BLOCKER TOOLCHAIN (đã xác minh, không phải lỗi code):
 *   `createComposeRule()` cần host `androidx.activity.ComponentActivity`, nhưng
 *   AGP 9.1.1 KHÔNG merge `androidx.compose.ui:ui-test-manifest` vào manifest
 *   unit-test → Robolectric ném:
 *     "RuntimeException: Unable to resolve activity for Intent { ...
 *      cmp=org.robolectric.default/androidx.activity.ComponentActivity }"
 *   (Robolectric PR #4736). Trùng đúng ghi chú đã có ở feature.md / CLAUDE.md.
 *
 * Khi nâng hạ tầng test-manifest (hoặc lên Paparazzi sau khi hỗ trợ AGP 9.x) →
 * bỏ @Ignore là chạy được ngay. TRONG LÚC ĐÓ độ phủ UI-pipeline của 4 đạn được
 * bảo đảm bởi:
 *   • BulletWave18Test (integration: fireLasers + monitorLaserCollision thật)
 *   • BulletTypeTest / BulletColorIdentityTest / MetaProgressionKeysTest
 *     (shape/color/width/name là SSOT mà mọi Composable UI đọc qua dispatch)
 *   • verify on-device (ảnh chụp 4 tab UI).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Ignore("AGP 9.1.1: ui-test-manifest không merge vào unit-test manifest → " +
    "Robolectric không resolve được ComponentActivity. Bỏ @Ignore khi hạ tầng fixed.")
class BulletWave18WidgetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `batch3 bullet display names render in Compose`() {
        val names = listOf(
            BulletType.BUBBLE_TEA, BulletType.FISH_SAUCE,
            BulletType.SANDAL, BulletType.QR_CODE,
        ).map { it.displayName }
        composeRule.setContent {
            Column { names.forEach { Text(it) } }
        }
        names.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }
}
