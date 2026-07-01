package com.tranphuloi.neon.integration

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.tranphuloi.neon.App
import com.tranphuloi.neon.ui.game.mode.GameMode
import kotlinx.coroutines.runBlocking

/**
 * Xoá checkpoint của MỌI game mode. MenuScreen scale-to-fit (KHÔNG scroll ở màn
 * thường); khi tồn tại checkpoint, dòng "▸ Đang ở màn X" làm nội dung tràn → menu
 * bật nhánh mustScroll HOẶC Compose clip nút chơi (ở giữa). Xoá sạch trước mỗi
 * lần launch để Menu ở trạng thái gọn nhất.
 */
fun App.clearAllCheckpoints() = runBlocking {
    GameMode.values().forEach { runPersistence.clearCheckpoint(it.key) }
}

/**
 * Tìm nút chơi ("▶ BẮT ĐẦU" hoặc "▶ TIẾP TỤC") một cách bền bỉ với layout Menu:
 *  - chờ node xuất hiện,
 *  - nếu chưa thấy (menu tràn → mustScroll), vuốt để lộ ra rồi thử lại.
 * Trả về UiObject2 hoặc null.
 */
fun UiDevice.findPlayButton(): UiObject2? {
    fun locate(timeout: Long): UiObject2? =
        wait(Until.findObject(By.textContains("BẮT ĐẦU")), timeout)
            ?: findObject(By.textContains("TIẾP TỤC"))

    var play = locate(8_000)
    if (play != null) return play

    // Menu có thể đang ở nhánh mustScroll → vuốt lên (cuộn xuống) rồi thử lại.
    repeat(2) {
        swipe(displayWidth / 2, displayHeight * 3 / 4, displayWidth / 2, displayHeight / 4, 12)
        play = locate(2_000)
        if (play != null) return play
    }
    // Vuốt ngược lại (về đầu) phòng khi nút ở phía trên.
    repeat(2) {
        swipe(displayWidth / 2, displayHeight / 4, displayWidth / 2, displayHeight * 3 / 4, 12)
        play = locate(2_000)
        if (play != null) return play
    }
    return play
}
