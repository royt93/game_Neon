package com.tranphuloi.neon.ui.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.tranphuloi.neon.R
import com.tranphuloi.neon.utils.Logger

/**
 * Activity-scoped SFX player. SoundPool keeps small audio clips decoded in memory
 * for low-latency playback. Released alongside [AudioPlayerHolder].
 *
 * Placeholder samples copied from external project; swap by replacing res/raw/sfx_*.mp3.
 *  - sfx_laser.mp3       (was n01)
 *  - sfx_explosion.mp3   (was n12)
 *  - sfx_damage.mp3      (was n08)
 *  - sfx_pickup.mp3      (was n05)
 *  - sfx_gameover.mp3    (was n20)
 */
enum class SfxEvent(val resId: Int) {
    LASER(R.raw.sfx_laser),
    EXPLOSION(R.raw.sfx_explosion),
    DAMAGE(R.raw.sfx_damage),
    PICKUP(R.raw.sfx_pickup),
    GAME_OVER(R.raw.sfx_gameover),
}

class SfxController(private val appContext: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: MutableMap<SfxEvent, Int> = mutableMapOf()
    private var volume: Float = 0.9f

    init {
        Logger.d("SfxController init: pre-loading ${SfxEvent.values().size} SFX into SoundPool")
        SfxEvent.values().forEach { event ->
            val id = pool.load(appContext, event.resId, /* priority */ 1)
            soundIds[event] = id
            Logger.d("  SFX loaded: ${event.name} → soundId=$id (resId=${event.resId})")
        }
    }

    fun setVolume(percent: Int) {
        volume = (percent.coerceIn(0, 100)) / 100f
        Logger.d("SfxController.setVolume volume=$volume")
    }

    fun play(event: SfxEvent) {
        play(event, rate = 1f)
    }

    /**
     * Round 74 (R73e) — pitch-shifted variant. Cùng asset file nhưng playback
     * rate khác → tạo cảm giác sound khác. Per-boss audio cue dùng cùng
     * sfx_explosion với rate khác nhau (0.7=trầm SPIDER, 1.3=cao STAR, etc).
     */
    fun play(event: SfxEvent, rate: Float) {
        val id = soundIds[event] ?: run {
            Logger.w("SfxController.play: no soundId for $event")
            return
        }
        val safeRate = rate.coerceIn(0.5f, 2.0f)
        val streamId = pool.play(id, volume, volume, /* priority */ 1, /* loop */ 0, safeRate)
        if (streamId == 0) {
            Logger.w("SfxController.play: SoundPool returned 0 (busy/not loaded yet) for $event")
        }
    }

    fun release() {
        Logger.d("SfxController.release: SoundPool.release()")
        pool.release()
        soundIds.clear()
    }
}

val LocalSfx = staticCompositionLocalOf<SfxController> {
    error("SfxController not provided. Wrap in CompositionLocalProvider(LocalSfx provides ...).")
}
