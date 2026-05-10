package com.tranphuloi.neon.ui.game.haptic

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tranphuloi.neon.utils.Logger

/**
 * Centralized haptic patterns for the game. Defined as enum so a single
 * `Reduce motion` toggle (future) can short-circuit all calls.
 *
 * `minIntervalMs` throttles repeat fires of the same pattern. Crucial for
 * GODLIKE-tier combos that previously fired HEAVY (220ms) ~10×/sec, saturating
 * the vibrator service binder.
 */
enum class HapticPattern(val durationMs: Long, val amplitude: Int, val minIntervalMs: Long) {
    LIGHT_TICK(durationMs = 25, amplitude = 60, minIntervalMs = 0L),     // pickup
    MEDIUM(durationMs = 90, amplitude = 140, minIntervalMs = 60L),       // ship damage
    HEAVY(durationMs = 220, amplitude = 220, minIntervalMs = 200L),      // boss / GODLIKE kill
    LONG(durationMs = 450, amplitude = 255, minIntervalMs = 0L),         // game over
}

class HapticController(private val appContext: Context) {

    private val vibrator: Vibrator? = resolveVibrator()
    private var enabled: Boolean = vibrator?.hasVibrator() == true
    private val lastFireUptimeMs = LongArray(HapticPattern.values().size)

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
        if (pattern.minIntervalMs > 0L) {
            val now = SystemClock.uptimeMillis()
            val last = lastFireUptimeMs[pattern.ordinal]
            if (now - last < pattern.minIntervalMs) return
            lastFireUptimeMs[pattern.ordinal] = now
        }
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
