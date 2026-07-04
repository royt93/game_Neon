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
 * UiAutomator định vị node qua **testTag** (`testTagsAsResourceId=true` ở root →
 * `By.res("menu_play")` v.v.), KHÔNG phụ thuộc chuỗi hiển thị/animation.
 *
 * Vì sao UiAutomator thay vì createAndroidComposeRule: SplashScreen điều hướng
 * NGAY trong LaunchedEffect; compose-ui-test chạy effect off-main → navController
 * chạm setCurrentState off-main → crash trên Android 17. UiAutomator để app chạy
 * nguyên bản (effect trên main như production).
 *
 * Chạy qua Test Orchestrator (`clearPackageData=true`) → mỗi test 1 process +
 * data pristine → Menu tối giản, nút không bị clip.
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val app: App get() = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        runBlocking { app.settings.setDifficulty(Difficulty.NORMAL) } // seed difficultyPicked
        app.clearAllCheckpoints()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    /** Chờ Splash → Menu (đã seed đi thẳng Menu; xử lý cả nhánh DifficultyPicker). */
    private fun awaitMenu() {
        val ok = device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000) != null ||
            device.hasObject(By.text("Chọn độ khó"))
        assertTrue("Splash phải dẫn tới Menu hoặc DifficultyPicker", ok)
        if (device.hasObject(By.text("Chọn độ khó"))) {
            device.wait(Until.findObject(By.text("Vừa")), 5_000)?.click()
            assertTrue("Chọn độ khó xong phải vào Menu", device.wait(Until.hasObject(By.text("SKY FORCE")), 10_000) != null)
        }
    }

    /**
     * Tới Menu rồi định vị node theo testTag ([By.res], nhờ `testTagsAsResourceId`).
     * MenuScreen sau fix clip: nội dung hoặc vừa màn (nút on-screen) hoặc cuộn được
     * (không còn clip) → [findByTag] cuộn để lộ nút nếu cần. Không còn relaunch-retry.
     */
    private fun acquire(tag: String): UiObject2 {
        awaitMenu()
        return device.findByTag(tag)
            ?: error("Không tìm được node testTag='$tag' trên Menu")
    }

    @Test
    fun splash_lands_on_menu() {
        awaitMenu()
        assertTrue("Sau Splash phải tới Menu", device.hasObject(By.text("SKY FORCE")))
    }

    @Test
    fun menu_opens_settings_dialog() {
        acquire("menu_settings").click()
        // "ÂM THANH" chỉ có trong DialogSettings → chốt dialog mở thật.
        assertTrue(
            "Mở Settings phải thấy section ÂM THANH của dialog",
            device.wait(Until.hasObject(By.textContains("ÂM THANH")), 8_000) != null,
        )
    }

    @Test
    fun menu_starts_game_and_leaves_menu() {
        acquire("menu_play").click()
        // Vào Game: nút chơi của Menu biến mất, app không crash.
        assertTrue("Bấm nút chơi phải rời Menu để vào Game", device.wait(Until.gone(By.res("menu_play")), 10_000))
    }
}
