package com.tranphuloi.neon.integration

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.tranphuloi.neon.App
import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test — game loop chạy nhiều tick THẬT trên thiết bị, lái bằng
 * UiAutomator (xem NavigationFlowTest để hiểu vì sao không dùng compose rule).
 *
 * Game loop sống trong DisposableEffect trên Dispatchers.IO với delay(8) (~125Hz).
 * Test vào màn Game, để loop chạy vài giây (hàng trăm iteration), rồi chốt bất
 * biến: Activity còn RESUMED, không crash. Đây cũng là nơi HapticController/
 * SfxController (không JVM-testable) chạy thật trong loop — lỗi sẽ làm test đỏ.
 */
@RunWith(AndroidJUnit4::class)
class GameLoopMultiTickTest {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val app: App get() = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        runBlocking { app.settings.setDifficulty(Difficulty.NORMAL) }
        // Menu scale-to-fit sẽ clip nút chơi nếu có checkpoint → xoá sạch mọi mode.
        app.clearAllCheckpoints()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        // Loop đã chết sau close() → xoá checkpoint run vừa tạo, tránh rò sang
        // class test kế (race với setUp của nó).
        app.clearAllCheckpoints()
    }

    private fun launchFresh() {
        if (::scenario.isInitialized) scenario.close()
        app.clearAllCheckpoints()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /**
     * Đưa Menu về trạng thái có nút chơi bấm được. MenuScreen thi thoảng clip nút
     * chơi (adaptive-scale + pulse animation race) → relaunch để có layout pass
     * mới; launch tươi luôn render nút chơi (bằng chứng: chạy đơn lẻ ổn định).
     */
    private fun acquirePlay(): UiObject2 {
        repeat(3) { attempt ->
            device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000)
            if (device.hasObject(By.text("CHỌN ĐỘ KHÓ"))) {
                device.wait(Until.findObject(By.text("VỪA")), 5_000)?.click()
                device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000)
            }
            // Settle a11y tree của Menu sau khi process đã chơi game (test trước).
            Thread.sleep(1_200)
            device.findPlayButton()?.let { return it }
            if (attempt < 2) launchFresh()
        }
        error("Không tìm được nút chơi (BẮT ĐẦU/TIẾP TỤC) sau 3 lần relaunch")
    }

    private fun enterGame() {
        acquirePlay().click()
        assertTrue("Bấm nút chơi phải vào Game (rời Menu)", device.wait(Until.gone(By.text("SKY FORCE")), 10_000))
    }

    @Test
    fun game_loop_survives_many_ticks_without_crashing() {
        enterGame()

        // Để loop chạy ~5s (≈ 600+ iteration @125Hz) theo wall-clock.
        Thread.sleep(5_000)

        // Bất biến: Activity vẫn RESUMED (không crash/không tự finish).
        val state = scenario.state
        assertTrue(
            "MainActivity phải còn RESUMED sau nhiều tick game loop, thực tế=$state",
            state.isAtLeast(Lifecycle.State.RESUMED),
        )
    }

    @Test
    fun back_press_while_running_opens_pause_dialog() {
        enterGame()

        // Cho loop chạy ngắn để chắc chắn gameStatus == RUNNING (BackHandler bật).
        Thread.sleep(1_500)

        // GameScreen chặn back khi RUNNING → toggle pause + điều hướng GamePause.
        device.pressBack()

        assertTrue(
            "Back khi đang chơi phải mở dialog TẠM DỪNG (GamePause)",
            device.wait(Until.hasObject(By.text("TẠM DỪNG")), 8_000) != null,
        )
    }
}
