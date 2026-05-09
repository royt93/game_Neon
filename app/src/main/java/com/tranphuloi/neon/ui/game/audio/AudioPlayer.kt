package com.tranphuloi.neon.ui.game.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.settings.GameStatus
import com.tranphuloi.neon.utils.Logger

/**
 * CompositionLocal exposing the Activity-scoped [AudioPlayerHolder] to Composables.
 * Throws if read outside a provider — every screen using audio must be inside MainActivity.
 */
val LocalAudioPlayer = staticCompositionLocalOf<AudioPlayerHolder> {
    error("AudioPlayerHolder not provided. Wrap composables in CompositionLocalProvider(LocalAudioPlayer provides ...).")
}

/**
 * Drives the Activity-scoped [AudioPlayerHolder] from a Composable's [GameStatus] state.
 * Replaces the previous DisposableEffect-based AudioPlayer that built/released ExoPlayer
 * on every navigation transition.
 */
@Composable
fun AudioPlayer(gameStatus: GameStatus) {
    val holder = LocalAudioPlayer.current
    LaunchedEffect(gameStatus) {
        Logger.d("AudioPlayer composable reacting to gameStatus=$gameStatus")
        when (gameStatus) {
            GameStatus.RUNNING -> holder.play()
            else -> holder.pause()
        }
    }
}

enum class Song(val resId: Int) {
    BKG(R.raw.bkg),
    BKG1(R.raw.bkg1),
    BKG2(R.raw.bkg2)
}
