package com.tranphuloi.neon.ui.game.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.settings.GameStatus
import com.tranphuloi.neon.utils.Logger

@Composable
fun AudioPlayer(gameStatus: GameStatus) {

    val context = LocalContext.current
    val playlist by rememberSaveable { mutableStateOf(Song.values().apply { shuffle() }) }
    var currentPosition by rememberSaveable { mutableLongStateOf(-1L) }
    val player = remember {
        Logger.d("AudioPlayer: building ExoPlayer with ${playlist.size} tracks: ${playlist.joinToString { it.name }}")
        ExoPlayer.Builder(context).build().apply {
            val packageName = context.packageName
            playlist.forEach {
                val uri = "android.resource://$packageName/${it.resId}"
                addMediaItem(MediaItem.fromUri(uri))
            }
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                // Sources may be unreachable (e.g. dead bit.ly link returning HTTP 402).
                // Skip the failing item; once the whole playlist has failed, stop instead
                // of looping forever and spamming the log.
                override fun onPlayerError(error: PlaybackException) {
                    Logger.e("AudioPlayer error code=${error.errorCodeName} msg=${error.message}", error)
                    if (hasNextMediaItem()) {
                        Logger.d("AudioPlayer: skipping to next media item")
                        seekToNextMediaItem()
                        prepare()
                    } else {
                        Logger.w("AudioPlayer: playlist exhausted, stopping")
                        stop()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    Logger.d("AudioPlayer: media item transition (reason=$reason)")
                }
            })
            prepare()
        }
    }

    fun playAudio() {
        Logger.d("AudioPlayer.playAudio (resume from $currentPosition ms)")
        player.playWhenReady = true
        if (currentPosition >= 0L) player.seekTo(currentPosition)
    }

    fun stopAudio() {
        currentPosition = player.currentPosition
        Logger.d("AudioPlayer.stopAudio (saved position=$currentPosition ms)")
        player.playWhenReady = false
    }

    DisposableEffect(Unit) {
        Logger.d("AudioPlayer DisposableEffect: starting playback")
        playAudio()
        onDispose {
            Logger.d("AudioPlayer onDispose: stop + release ExoPlayer")
            stopAudio()
            player.stop()
            player.release()
        }
    }
    LaunchedEffect(gameStatus) {
        Logger.d("AudioPlayer reacting to gameStatus=$gameStatus")
        when (gameStatus) {
            GameStatus.RUNNING -> playAudio()
            else -> stopAudio()
        }
    }
}

enum class Song(val resId: Int) {
    BKG(R.raw.bkg),
    BKG1(R.raw.bkg1),
    BKG2(R.raw.bkg2)
}
