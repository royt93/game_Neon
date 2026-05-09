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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tranphuloi.neon.ui.game.settings.GameStatus

@Composable
fun AudioPlayer(gameStatus: GameStatus) {

    val context = LocalContext.current
    val playlist by rememberSaveable { mutableStateOf(Song.values().apply { shuffle() }) }
    var currentPosition by rememberSaveable { mutableLongStateOf(-1L) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playlist.forEach {
                val mediaItem = MediaItem.fromUri(it.uri)
                addMediaItem(mediaItem)
            }
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
        }
    }

    fun playAudio() {
        player.playWhenReady = true
        if (currentPosition >= 0L) player.seekTo(currentPosition)
    }

    fun stopAudio() {
        currentPosition = player.currentPosition
        player.playWhenReady = false
    }

    DisposableEffect(Unit) {
        playAudio()
        onDispose {
            stopAudio()
            player.stop()
            player.release()
        }
    }
    LaunchedEffect(gameStatus) {
        when (gameStatus) {
            GameStatus.RUNNING -> playAudio()
            else -> stopAudio()
        }
    }
}

enum class Song(val uri: String) {
    THIS_MEANS_WAR("https://bit.ly/3JxYIHF"),
    THE_BOMB("https://bit.ly/3qzTzGo")
}
