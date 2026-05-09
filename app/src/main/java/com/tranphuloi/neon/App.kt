package com.tranphuloi.neon

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.tranphuloi.neon.data.AchievementsRepository
import com.tranphuloi.neon.data.LeaderboardRepository
import com.tranphuloi.neon.data.SettingsRepository
import com.tranphuloi.neon.utils.Logger

class App : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var leaderboard: LeaderboardRepository
        private set
    lateinit var achievements: AchievementsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Logger.d("App.onCreate (pid=${android.os.Process.myPid()})")
        installUncaughtExceptionHandler()
        settings = SettingsRepository(applicationContext)
        leaderboard = LeaderboardRepository(applicationContext)
        achievements = AchievementsRepository(applicationContext)
    }

    private fun installUncaughtExceptionHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e("UNCAUGHT on thread=${thread.name}: ${throwable.message}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
        Logger.d("App: installed default UncaughtExceptionHandler chain")
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val label = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMb = rt.maxMemory() / (1024 * 1024)
        Logger.w("App.onTrimMemory level=$label heap=${usedMb}MB/${maxMb}MB")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMb = rt.maxMemory() / (1024 * 1024)
        Logger.w("App.onLowMemory: heap=${usedMb}MB/${maxMb}MB")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Logger.d("App.onConfigurationChanged: locale=${newConfig.locales.get(0)} fontScale=${newConfig.fontScale} darkMode=${(newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES}")
    }
}
