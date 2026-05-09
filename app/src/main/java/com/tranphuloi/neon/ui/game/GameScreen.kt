package com.tranphuloi.neon.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.audio.AudioPlayer
import com.tranphuloi.neon.ui.game.controls.ButtonsMovement
import com.tranphuloi.neon.ui.game.controls.ButtonSettings
import com.tranphuloi.neon.ui.game.controls.IndicatorStatus
import com.tranphuloi.neon.ui.game.settings.GameStatus
import com.tranphuloi.neon.ui.game.state.rememberGameState
import com.tranphuloi.neon.ui.game.world.GameWorld
import com.tranphuloi.neon.utils.Logger
import kotlin.math.cos
import kotlin.math.sin

private const val SHAKE_DURATION_MILLIS = 280L
private const val FLASH_DURATION_MILLIS = 360L
private const val SHAKE_MAX_PX = 14f
private const val FLASH_MAX_ALPHA = 0.45f

@Composable
fun GameScreen(
    onGamePause: () -> Unit,
    onGameOver: (score: String) -> Unit,
) {
    LaunchedEffect(Unit) { Logger.d("GameScreen entered") }
    val gameState = rememberGameState()
    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.GAME_OVER) {
            Logger.d("GameScreen detected GAME_OVER → onGameOver(score=${gameState.mineralsEarnedTotal})")
            onGameOver(gameState.mineralsEarnedTotal)
        }
    }

    AudioPlayer(gameStatus = gameState.gameStatus)

    val now = System.currentTimeMillis()
    val damageElapsed = (now - gameState.lastShipDamageMillis).coerceAtLeast(0L)
    val shakeProgress =
        (1f - (damageElapsed.toFloat() / SHAKE_DURATION_MILLIS)).coerceIn(0f, 1f)
    val flashProgress =
        (1f - (damageElapsed.toFloat() / FLASH_DURATION_MILLIS)).coerceIn(0f, 1f)
    val shakeAmplitude = SHAKE_MAX_PX * shakeProgress
    val shakeX =
        if (shakeAmplitude > 0f) (sin(damageElapsed / 18.0) * shakeAmplitude).toFloat() else 0f
    val shakeY =
        if (shakeAmplitude > 0f) (cos(damageElapsed / 21.0) * shakeAmplitude).toFloat() else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonBgDeep, NeonBgMid, NeonBgEdge)
                )
            )
    ) {
        IndicatorStatus(
            gameTime = gameState.gameTimeIndicator,
            hp = gameState.ship.hp,
            mineralsEarnedTotal = gameState.mineralsEarnedTotal,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(300f)
        )
        ButtonSettings(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(300f)
        ) {
            Logger.d("Settings button pressed → toggleGameStatus + open pause")
            gameState.toggleGameStatus()
            onGamePause()
        }
        Text(
            text = gameState.gameMessage,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.h3
        )
        Column(modifier = Modifier.fillMaxSize()) {
            GameWorld(
                ship = gameState.ship,
                shipLasers = gameState.shipLasers,
                ultimateLasers = gameState.ultimateLasers,
                stars = gameState.stars,
                spaceObjects = gameState.spaceObjects,
                boosters = gameState.boosters,
                enemies = gameState.enemies,
                enemyLasers = gameState.enemyLasers,
                minerals = gameState.minerals,
                explosions = gameState.explosions,
                modifier = Modifier
                    .weight(1f)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(
                                x = shakeX.toInt(),
                                y = shakeY.toInt()
                            )
                        }
                    }
            )
            ButtonsMovement(
                onMoveLeft = { gameState.moveShipLeft(it) },
                onMoveRight = { gameState.moveShipRight(it) },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        if (flashProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeonRedAlert.copy(alpha = FLASH_MAX_ALPHA * flashProgress))
                    .zIndex(250f)
            )
        }
    }
}
