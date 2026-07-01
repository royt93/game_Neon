package com.tranphuloi.neon.integration

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
 * Integration test — điều hướng THẬT qua NavHost của [MainActivity], lái bằng
 * UiAutomator (không dùng compose test rule).
 *
 * Vì sao UiAutomator thay vì createAndroidComposeRule: SplashScreen điều hướng
 * NGAY trong LaunchedEffect (navController.popBackStack/navigate). compose-ui-test
 * chạy LaunchedEffect trên dispatcher nền (frame-deferring) → navController chạm
 * setCurrentState off-main → IllegalStateException trên Android 17. UiAutomator
 * để app chạy nguyên bản (effect trên main như production) nên hết crash, và đọc
 * được text Compose qua cầu accessibility.
 *
 * Seed difficultyPicked=true trước launch để Splash đi thẳng Menu.
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val app: App get() = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        runBlocking { app.settings.setDifficulty(Difficulty.NORMAL) }
        // Menu gọn (nút "BẮT ĐẦU" hiển thị) — xem FlowSupport.clearAllCheckpoints.
        app.clearAllCheckpoints()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        app.clearAllCheckpoints()
    }

    private fun waitText(text: String, timeout: Long = 10_000): Boolean =
        device.wait(Until.hasObject(By.text(text)), timeout) != null

    private fun waitTextContains(text: String, timeout: Long = 8_000): Boolean =
        device.wait(Until.hasObject(By.textContains(text)), timeout) != null

    private fun advanceToMenu() {
        // Chờ Splash quyết định: Menu (đã seed) hoặc DifficultyPicker (an toàn).
        val reached = device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000) != null ||
            device.hasObject(By.text("CHỌN ĐỘ KHÓ"))
        assertTrue("Splash phải dẫn tới Menu hoặc DifficultyPicker", reached)
        if (device.hasObject(By.text("CHỌN ĐỘ KHÓ"))) {
            device.wait(Until.findObject(By.text("VỪA")), 5_000)?.click()
            assertTrue("Sau khi chọn độ khó phải vào Menu", waitText("SKY FORCE"))
        }
        // Sau khi process đã chơi game (class test trước), a11y tree của Menu cần
        // thời gian để lộ các nút (pulse/adaptive-scale). Settle trước khi tìm nút.
        Thread.sleep(1_200)
    }

    /**
     * Tới Menu và tìm node theo [finder]; nếu chưa thấy (MenuScreen thi thoảng
     * clip nút do adaptive-scale race sau khi chơi game), relaunch để có layout
     * pass mới. Launch tươi luôn render đủ nút (bằng chứng: chạy đơn lẻ ổn định).
     */
    private fun acquire(what: String, finder: () -> UiObject2?): UiObject2 {
        repeat(3) { attempt ->
            advanceToMenu()
            finder()?.let { return it }
            if (attempt < 2) {
                scenario.close()
                app.clearAllCheckpoints()
                scenario = ActivityScenario.launch(MainActivity::class.java)
            }
        }
        error("Không tìm được '$what' trên Menu sau 3 lần relaunch")
    }

    @Test
    fun splash_lands_on_menu() {
        advanceToMenu()
        assertTrue("Sau Splash phải tới Menu (thấy 'SKY FORCE')", waitText("SKY FORCE"))
    }

    @Test
    fun menu_opens_settings_dialog() {
        val settings = acquire("nút Cài đặt (⚙/CÀI ĐẶT)") {
            // Chờ có node (không dùng findObject tức thời) — nút ở cuối menu, xuất
            // hiện muộn hơn trên cold start (orchestrator restart process mỗi test).
            device.wait(Until.findObject(By.text("⚙")), 6_000)
                ?: device.wait(Until.findObject(By.textContains("CÀI ĐẶT")), 2_000)
        }
        settings.click()
        // DialogSettings có section "ÂM THANH" (chỉ có trong dialog, không ở Menu)
        // → chốt dialog đã mở thật, không phải vẫn ở Menu.
        assertTrue(
            "Mở Settings phải thấy section ÂM THANH của dialog",
            waitTextContains("ÂM THANH"),
        )
    }

    @Test
    fun menu_starts_game_and_leaves_menu() {
        acquire("nút chơi (BẮT ĐẦU/TIẾP TỤC)") { device.findPlayButton() }.click()

        // Vào Game: tiêu đề Menu ("SKY FORCE") phải biến mất, app không crash.
        val leftMenu = device.wait(Until.gone(By.text("SKY FORCE")), 10_000)
        assertTrue("Bấm nút chơi phải rời Menu để vào Game", leftMenu)
    }
}
