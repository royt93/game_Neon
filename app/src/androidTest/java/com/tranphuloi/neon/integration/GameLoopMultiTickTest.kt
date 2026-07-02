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
 * UiAutomator + testTag (xem NavigationFlowTest để hiểu lựa chọn kỹ thuật).
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
        app.clearAllCheckpoints()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    private fun awaitMenu() {
        val ok = device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000) != null ||
            device.hasObject(By.text("CHỌN ĐỘ KHÓ"))
        assertTrue("Splash phải dẫn tới Menu/DifficultyPicker", ok)
        if (device.hasObject(By.text("CHỌN ĐỘ KHÓ"))) {
            device.wait(Until.findObject(By.text("VỪA")), 5_000)?.click()
            device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000)
        }
    }

    private fun enterGame() {
        awaitMenu()
        val play = device.findByTag("menu_play")
            ?: error("Không tìm được nút chơi (testTag menu_play) trên Menu")
        play.click()
        assertTrue("Bấm nút chơi phải vào Game (rời Menu)", device.wait(Until.gone(By.res("menu_play")), 10_000))
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

    /**
     * Phủ nút + callback của DialogGamePause TRÊN THIẾT BỊ THẬT. Định vị nút bằng
     * testTag (`By.res("pause_resume")` — testTagsAsResourceId khai trong dialog) +
     * cuộn nếu nút ngoài fold ([findByTag]); bấm "Resume" → dialog đóng, resume Game.
     */
    @Test
    fun pause_dialog_resume_button_works() {
        enterGame()
        Thread.sleep(1_500)
        device.pressBack()
        assertTrue(
            "Phải mở dialog TẠM DỪNG",
            device.wait(Until.hasObject(By.text("TẠM DỪNG")), 8_000) != null,
        )
        device.waitForIdle()
        val resume = device.findByTag("pause_resume")
        assertTrue("Pause phải có nút Resume (testTag pause_resume)", resume != null)

        resume!!.click()
        assertTrue(
            "Bấm Resume phải đóng dialog (resume game)",
            device.wait(Until.gone(By.text("TẠM DỪNG")), 8_000),
        )
        assertTrue("Sau resume vẫn ở Game, không quay về Menu", !device.hasObject(By.res("menu_play")))
    }
}
