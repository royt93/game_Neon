package com.tranphuloi.neon.ui.game.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tranphuloi.neon.utils.Logger

/**
 * Centralized haptic patterns for the game. Defined as enum so a single
 * `Reduce motion` toggle (future) can short-circuit all calls.
 */
enum class HapticPattern(val durationMs: Long, val amplitude: Int) {
    LIGHT_TICK(durationMs = 25, amplitude = 60),     // booster pickup, button press
    MEDIUM(durationMs = 90, amplitude = 140),         // ship damage
    HEAVY(durationMs = 220, amplitude = 220),         // boss appear
    LONG(durationMs = 450, amplitude = 255),          // game over
}

class HapticController(private val appContext: Context) {

    private val vibrator: Vibrator? = resolveVibrator()
    private var enabled: Boolean = vibrator?.hasVibrator() == true

    init {
        Logger.d("HapticController init: hasVibrator=$enabled, sdkInt=${Build.VERSION.SDK_INT}")
    }

    fun setEnabled(enabled: Boolean) {
        Logger.d("HapticController.setEnabled $enabled")
        this.enabled = enabled
    }

    fun vibrate(pattern: HapticPattern) {
        if (!enabled) return
        val v = vibrator ?: return
        Logger.d("HapticController.vibrate $pattern (${pattern.durationMs}ms amp=${pattern.amplitude})")
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(pattern.durationMs, pattern.amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern.durationMs)
            }
        }.onFailure {
            Logger.w("HapticController.vibrate failed for $pattern", it)
        }
    }

    private fun resolveVibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appContext.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.onFailure {
        Logger.w("HapticController: failed to resolve Vibrator", it)
    }.getOrNull()
}

val LocalHaptic = staticCompositionLocalOf<HapticController> {
    error("HapticController not provided. Wrap in CompositionLocalProvider(LocalHaptic provides ...).")
}
