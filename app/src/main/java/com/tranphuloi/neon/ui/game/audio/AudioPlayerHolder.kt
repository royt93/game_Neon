package com.tranphuloi.neon.ui.game.audio

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tranphuloi.neon.utils.Logger

/**
 * Activity-scoped holder for ExoPlayer. Built once on Activity onCreate, released on onDestroy.
 * Survives Compose recomposition / NavController transitions, eliminating the prior 700ms
 * "two players alive at once" window observed in logs during restart.
 *
 * Lifecycle: Activity hosts → CompositionLocal exposes to Composables → callers invoke play/pause.
 */
class AudioPlayerHolder(private val appContext: Context) : DefaultLifecycleObserver {

    private var player: ExoPlayer? = null
    private var savedPositionMillis: Long = -1L
    private var built: Boolean = false

    // Round 78 issue #7 — track playWhenReady at the moment lifecycle ON_PAUSE
    // fires so ON_RESUME can restore it. Previously onPause() forced
    // playWhenReady=false and no onResume() callback existed → music stayed
    // silent after foregrounding from MenuScreen (where the AudioPlayer
    // composable's LaunchedEffect(RUNNING) does NOT re-fire on lifecycle alone).
    // GameScreen worked by coincidence because GameState's own lifecycle
    // observer flipped gameStatus PAUSE↔RUNNING, re-keying LaunchedEffect.
    private var wasPlayingBeforeLifecyclePause: Boolean = false

    fun build(playlist: List<Song>) {
        if (built) {
            Logger.d("AudioPlayerHolder.build: already built — skip")
            return
        }
        built = true
        Logger.d("AudioPlayerHolder.build: ${playlist.size} tracks: ${playlist.joinToString { it.name }}")
        player = ExoPlayer.Builder(appContext).build().apply {
            val packageName = appContext.packageName
            playlist.forEach {
                val uri = "android.resource://$packageName/${it.resId}"
                addMediaItem(MediaItem.fromUri(uri))
            }
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Logger.e(
                        "AudioPlayerHolder error code=${error.errorCodeName} msg=${error.message}",
                        error
                    )
                    val p = player ?: return
                    if (p.hasNextMediaItem()) {
                        Logger.d("AudioPlayerHolder: skipping to next track")
                        p.seekToNextMediaItem()
                        p.prepare()
                    } else {
                        Logger.w("AudioPlayerHolder: playlist exhausted, stopping")
                        p.stop()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    Logger.d("AudioPlayerHolder: media transition reason=$reason")
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Logger.d("AudioPlayerHolder: playback state=$playbackState")
                }
            })
            prepare()
        }
    }

    fun play() {
        val p = player ?: run {
            Logger.w("AudioPlayerHolder.play: player not built")
            return
        }
        Logger.d("AudioPlayerHolder.play (resume from $savedPositionMillis ms)")
        p.playWhenReady = true
        if (savedPositionMillis >= 0L) p.seekTo(savedPositionMillis)
    }

    fun pause() {
        val p = player ?: return
        savedPositionMillis = p.currentPosition
        Logger.d("AudioPlayerHolder.pause (saved position=$savedPositionMillis ms)")
        p.playWhenReady = false
    }

    fun setVolume(percent: Int) {
        val v = (percent.coerceIn(0, 100)) / 100f
        // Round 44 — music intensity transitions step through volume 15-30× over
        // ~1s each. At Logger.d that pumped logcat with dozens of lines per
        // transition. Moved to verbose-only gate.
        Logger.v { "AudioPlayerHolder.setVolume percent=$percent → vol=$v" }
        player?.volume = v
    }

    /**
     * Round 62 — set music pitch (1.0 = baseline). Used alongside the existing
     * volume-based intensity system (Round 19 / 8c) to give boss fights a
     * subtle "tension" feel (+5% pitch) and low-HP a "weary" feel (-8% pitch).
     * Speed stays at 1.0 — only pitch shifts. ExoPlayer handles via Sonic
     * algorithm; no audible artifacts in the ±10% range.
     *
     * Animatable smoothing happens at the caller (GameScreen) so transitions
     * are gradual (~500ms tween) — sudden pitch jumps would be jarring.
     */
    fun setPitch(pitch: Float) {
        val p = pitch.coerceIn(0.7f, 1.3f)
        Logger.v { "AudioPlayerHolder.setPitch pitch=$p" }
        // Reuse existing speed (1.0) to keep playback tempo stable.
        player?.playbackParameters = PlaybackParameters(1.0f, p)
    }

    private fun release() {
        val p = player ?: run {
            Logger.d("AudioPlayerHolder.release: already released")
            return
        }
        Logger.d("AudioPlayerHolder.release: stopping + releasing ExoPlayer")
        p.stop()
        p.release()
        player = null
        built = false
    }

    override fun onPause(owner: LifecycleOwner) {
        val p = player
        if (p == null) {
            Logger.d("AudioPlayerHolder.onPause(lifecycle): player not built")
            return
        }
        wasPlayingBeforeLifecyclePause = p.playWhenReady
        Logger.d("AudioPlayerHolder.onPause(lifecycle) wasPlaying=$wasPlayingBeforeLifecyclePause")
        if (p.playWhenReady) {
            savedPositionMillis = p.currentPosition
            p.playWhenReady = false
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        val p = player
        if (p == null) {
            Logger.d("AudioPlayerHolder.onResume(lifecycle): player not built")
            return
        }
        Logger.d("AudioPlayerHolder.onResume(lifecycle) wasPlaying=$wasPlayingBeforeLifecyclePause")
        if (wasPlayingBeforeLifecyclePause) {
            // ExoPlayer preserves position when only playWhenReady was toggled,
            // so no explicit seekTo is needed here.
            p.playWhenReady = true
        }
        wasPlayingBeforeLifecyclePause = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Logger.d("AudioPlayerHolder.onDestroy(lifecycle): releasing")
        release()
    }
}
