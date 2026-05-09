package com.tranphuloi.neon.ui.game.audio

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
        Logger.d("AudioPlayerHolder.setVolume percent=$percent → vol=$v")
        player?.volume = v
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
        Logger.d("AudioPlayerHolder.onPause(lifecycle)")
        pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Logger.d("AudioPlayerHolder.onDestroy(lifecycle): releasing")
        release()
    }
}
