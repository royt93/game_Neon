package com.tranphuloi.neon.integration

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.tranphuloi.neon.App
import com.tranphuloi.neon.ui.game.mode.GameMode
import kotlinx.coroutines.runBlocking

/**
 * Xoá checkpoint của MỌI game mode — phòng thủ để test chạy sạch cả khi KHÔNG qua
 * Test Orchestrator (vd chạy từ IDE, không có clearPackageData). Menu gọn thì ít
 * khi phải cuộn.
 */
fun App.clearAllCheckpoints() = runBlocking {
    GameMode.values().forEach { runPersistence.clearCheckpoint(it.key) }
}

/**
 * Định vị node theo **testTag** ([By.res] nhờ `testTagsAsResourceId=true` ở root).
 * Nếu chưa thấy, cuộn để lộ node: MenuScreen khi nội dung tràn (màn thấp / có
 * nhiều dòng) bật nhánh `mustScroll` → nút nằm ngoài vùng nhìn thấy nên UiAutomator
 * (chỉ đọc node visible) không thấy; vuốt để đưa vào khung nhìn. testTag giữ cho
 * locator KHÔNG phụ thuộc chuỗi hiển thị.
 */
fun UiDevice.findByTag(tag: String, timeout: Long = 6_000): UiObject2? {
    wait(Until.findObject(By.res(tag)), timeout)?.let { return it }
    // Vuốt lên (cuộn xuống) để lộ node ở dưới fold.
    repeat(2) {
        swipe(displayWidth / 2, displayHeight * 3 / 4, displayWidth / 2, displayHeight / 4, 12)
        wait(Until.findObject(By.res(tag)), 1_500)?.let { return it }
    }
    // Vuốt xuống (cuộn lên) phòng khi node ở trên.
    repeat(2) {
        swipe(displayWidth / 2, displayHeight / 4, displayWidth / 2, displayHeight * 3 / 4, 12)
        wait(Until.findObject(By.res(tag)), 1_500)?.let { return it }
    }
    return null
}
