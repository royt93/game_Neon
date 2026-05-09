package com.tranphuloi.neon.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tranphuloi.neon.common.Blue
import com.tranphuloi.neon.common.Pink
import com.tranphuloi.neon.ui.game.audio.AudioPlayer
import com.tranphuloi.neon.ui.game.controls.ButtonsMovement
import com.tranphuloi.neon.ui.game.controls.ButtonSettings
import com.tranphuloi.neon.ui.game.controls.IndicatorStatus
import com.tranphuloi.neon.ui.game.state.rememberGameState
import com.tranphuloi.neon.ui.game.world.GameWorld

@Composable
fun GameScreen(onGamePause: () -> Unit) {
    val gameState = rememberGameState()

    AudioPlayer(gameStatus = gameState.gameStatus)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Blue, Pink)))
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
                modifier = Modifier.weight(1f)
            )
            ButtonsMovement(
                onMoveLeft = { gameState.moveShipLeft(it) },
                onMoveRight = { gameState.moveShipRight(it) },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
