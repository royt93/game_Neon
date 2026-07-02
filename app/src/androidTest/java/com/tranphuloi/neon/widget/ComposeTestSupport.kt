package com.tranphuloi.neon.widget

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText

/**
 * Chờ tới khi ít nhất một node mang [text] xuất hiện (dialog slide-in sau delay).
 * Tránh flake do NeonBottomSheet chỉ set visible=true sau delay(16ms).
 */
fun ComposeContentTestRule.waitForText(
    text: String,
    substring: Boolean = false,
    // 10s để dung sai máy budget/cold-start (orchestrator restart process mỗi test):
    // NeonBottomSheet slide-in + frame đầu có thể > 5s trên thiết bị chậm.
    timeoutMillis: Long = 10_000,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }
}
