package com.tranphuloi.neon.microbenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 20 — combat-peak frame-timing baseline. Forward-looking regression
 * guard (7-round perf-chain 44-49/57/59 already merged, not independently
 * reproducible pre/post) — this is where future jank regressions get caught.
 * Drives Menu → Play → chapter-1 first mid-boss (game-stage 6, Stage.kt —
 * dense enemy + laser load) via UiAutomator, mirroring
 * BaselineProfileGenerator.kt's navigation and GameLoopMultiTickTest's
 * testTag locators (full-activity nav can't use compose-ui-test — see
 * project CLAUDE.md's "Android 17 gotchas").
 */
@RunWith(AndroidJUnit4::class)
class CombatPeakBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun combatPeakFrameTiming() = benchmarkRule.measureRepeated(
        packageName = "com.tranphuloi.neon",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.text("SKY FORCE")), 12_000)
        device.findObject(By.text("Vừa"))?.click()
        device.wait(Until.hasObject(By.text("SKY FORCE")), 8_000)

        val play = device.findByTag("menu_play", timeout = 8_000)
        play?.click()
        device.wait(Until.gone(By.res("menu_play")), 10_000)

        // First mid-boss warning ("Nguy hiểm", Stage.kt line 86) fires at
        // chapter-1 game-stage 6 — wait for it, let the intro messages
        // clear, then measure the boss fight itself.
        device.wait(Until.hasObject(By.text("Nguy hiểm")), 90_000)
        device.wait(Until.gone(By.text("Nguy hiểm")), 5_000)

        Thread.sleep(15_000)
    }

    private fun UiDevice.findByTag(tag: String, timeout: Long = 6_000) =
        wait(Until.findObject(By.res(tag)), timeout)
}
