package com.tranphuloi.neon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 11 (đợt 3) — generator baseline profile THẬT (macrobenchmark) cho hot-path
 * cold-start + GameScreen first-composition (bottleneck jank). Chạy trên device qua
 * plugin androidx.baselineprofile 1.5.0-alpha07 (bản đầu hỗ trợ AGP 9.1.1). Thay
 * cho profile curated thủ công trước đó.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.tranphuloi.neon",
        // Sinh thêm startup profile (dex layout tối ưu cold-start — đúng jank mục tiêu).
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        // Splash → Menu (xử lý cả DifficultyPicker của lần chạy sạch đầu).
        device.wait(Until.hasObject(By.text("SKY FORCE")), 12_000)
        device.findObject(By.text("Vừa"))?.click()
        device.wait(Until.hasObject(By.text("SKY FORCE")), 8_000)
        // Vào Game qua testTag menu_play (relaunch-retry để né clip adaptive-scale).
        var play = device.wait(Until.findObject(By.res("menu_play")), 8_000)
        repeat(4) {
            if (play != null) return@repeat
            pressHome(); startActivityAndWait()
            device.wait(Until.hasObject(By.text("SKY FORCE")), 8_000)
            play = device.wait(Until.findObject(By.res("menu_play")), 6_000)
        }
        play?.click()
        // Cho game loop chạy vài giây để phủ đường hot-path gameplay.
        Thread.sleep(6_000)
    }
}
