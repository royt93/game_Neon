package com.tranphuloi.neon.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.audio.AudioPlayer
import com.tranphuloi.neon.ui.game.audio.LocalSfx
import com.tranphuloi.neon.ui.game.audio.SfxEvent
import com.tranphuloi.neon.ui.game.background.SpaceBackground
import com.tranphuloi.neon.ui.game.combo.ComboTier
import com.tranphuloi.neon.ui.game.controls.AchievementBanner
import com.tranphuloi.neon.ui.game.controls.BossHpBar
import com.tranphuloi.neon.ui.game.controls.BossIntroOverlay
import com.tranphuloi.neon.ui.game.controls.StageBanner
import com.tranphuloi.neon.ui.game.controls.WaveClearBanner
import com.tranphuloi.neon.ui.game.controls.ButtonsMovement
import com.tranphuloi.neon.ui.game.controls.ButtonSettings
import com.tranphuloi.neon.ui.game.controls.ComboPopup
import com.tranphuloi.neon.ui.game.controls.IndicatorStatus
import com.tranphuloi.neon.ui.game.controls.PowerUpIndicators
import com.tranphuloi.neon.ui.game.controls.TutorialOverlay
import com.tranphuloi.neon.ui.game.controls.Vignette
import com.tranphuloi.neon.ui.game.haptic.HapticPattern
import com.tranphuloi.neon.ui.game.haptic.LocalHaptic
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

/**
 * F-b: Subtle ambient tint per stage cluster. Stage map matches enemy color palette:
 *   stages 0-9   → cyan/blue tint   (red enemies on default deep gradient)
 *   stages 10-15 → violet           (boss 1 + green enemies)
 *   stages 16-21 → magenta tint     (light blue enemies, mid game)
 *   stages 22+   → red tint         (boss 2 + final stages, danger)
 */
private fun stageTintColor(stageIndex: Int): Color {
    val tintAlpha = 0.06f
    return when {
        stageIndex >= 22 -> NeonRedAlert.copy(alpha = tintAlpha)
        stageIndex >= 16 -> NeonMagenta.copy(alpha = tintAlpha)
        stageIndex >= 10 -> NeonViolet.copy(alpha = tintAlpha)
        else -> NeonCyan.copy(alpha = tintAlpha * 0.6f)
    }
}

@Composable
fun GameScreen(
    onGamePause: () -> Unit,
    onGameOver: (score: String) -> Unit,
) {
    LaunchedEffect(Unit) { Logger.d("GameScreen entered") }
    val haptic = LocalHaptic.current
    val sfx = LocalSfx.current
    val settings = LocalSettings.current
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val vibrationEnabled by settings.vibrationEnabled.collectAsState(initial = true)
    val tutorialShown by settings.tutorialShown.collectAsState(initial = true) // optimistic to avoid flash on first compose

    val gameState = rememberGameState()
    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.GAME_OVER) {
            Logger.d("GameScreen detected GAME_OVER → kill-cam 1500ms then onGameOver(score=${gameState.mineralsEarnedTotal})")
            if (vibrationEnabled) haptic.vibrate(HapticPattern.LONG)
            sfx.play(SfxEvent.GAME_OVER)
            // 11c: delay navigation to play kill-cam slow-mo replay.
            kotlinx.coroutines.delay(1500L)
            onGameOver(gameState.mineralsEarnedTotal)
        }
    }
    LaunchedEffect(gameState.lastShipDamageMillis) {
        // Skip MEDIUM damage haptic if ship just died — the GAME_OVER LaunchedEffect already
        // fires HapticPattern.LONG; firing both was causing double-haptic on death.
        if (gameState.lastShipDamageMillis > 0L &&
            gameState.gameStatus != GameStatus.GAME_OVER) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.MEDIUM)
            sfx.play(SfxEvent.DAMAGE)
        }
    }
    LaunchedEffect(gameState.lastBoosterPickupMillis) {
        if (gameState.lastBoosterPickupMillis > 0L) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.LIGHT_TICK)
            sfx.play(SfxEvent.PICKUP)
        }
    }
    LaunchedEffect(gameState.lastEnemyKillMillis) {
        if (gameState.lastEnemyKillMillis > 0L) {
            sfx.play(SfxEvent.EXPLOSION)
            // Per-tier haptic on combo escalations.
            if (vibrationEnabled) {
                when (gameState.comboTier) {
                    ComboTier.RAMPAGE, ComboTier.UNSTOPPABLE -> haptic.vibrate(HapticPattern.MEDIUM)
                    ComboTier.GODLIKE -> haptic.vibrate(HapticPattern.HEAVY)
                    else -> Unit
                }
            }
        }
    }
    LaunchedEffect(gameState.lastStageAdvanceMillis) {
        if (gameState.lastStageAdvanceMillis > 0L && vibrationEnabled) {
            haptic.vibrate(HapticPattern.LIGHT_TICK)
        }
    }
    // 21c: Boss intro alarm — heavy haptic + explosion SFX as alarm sting.
    LaunchedEffect(gameState.bossIntroShownAtMillis) {
        if (gameState.bossIntroShownAtMillis > 0L) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.HEAVY)
            sfx.play(SfxEvent.EXPLOSION)
        }
    }

    AudioPlayer(gameStatus = gameState.gameStatus)

    val now = System.currentTimeMillis()
    val damageElapsed = (now - gameState.lastShipDamageMillis).coerceAtLeast(0L)

    // 30c: cinematic camera zoom — Animatable drives display-refresh-rate interpolation
    // (replaces previous manual sin-from-elapsed-millis which only sampled on
    // recomposition and produced visible stutter).
    val zoomEvents = longArrayOf(
        gameState.lastShipDamageMillis,
        gameState.bossIntroShownAtMillis,
        gameState.bossKillEventMillis,
    )
    val nearestZoomEvent = zoomEvents.maxOrNull() ?: 0L
    val gameZoomScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(nearestZoomEvent) {
        if (nearestZoomEvent > 0L && !reduceMotion) {
            gameZoomScaleAnim.animateTo(
                targetValue = 1.10f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            )
            gameZoomScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            )
        }
    }

    // 11c: kill-cam zoom — Animatable chain: snap → 1.5× → hold → settle.
    val killCamScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(gameState.killCamStartedAtMillis) {
        if (gameState.killCamStartedAtMillis > 0L && !reduceMotion) {
            killCamScaleAnim.snapTo(1f)
            killCamScaleAnim.animateTo(
                targetValue = 1.5f,
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing),
            )
            delay(1100L)
            killCamScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
            )
        }
    }
    val finalScale = maxOf(gameZoomScaleAnim.value, killCamScaleAnim.value)
    val shakeProgress =
        if (reduceMotion) 0f
        else (1f - (damageElapsed.toFloat() / SHAKE_DURATION_MILLIS)).coerceIn(0f, 1f)
    val flashProgress =
        if (reduceMotion) (1f - (damageElapsed.toFloat() / FLASH_DURATION_MILLIS)).coerceIn(0f, 1f) * 0.5f
        else (1f - (damageElapsed.toFloat() / FLASH_DURATION_MILLIS)).coerceIn(0f, 1f)
    val shakeAmplitude = SHAKE_MAX_PX * shakeProgress
    val shakeX =
        if (shakeAmplitude > 0f) (sin(damageElapsed / 18.0) * shakeAmplitude).toFloat() else 0f
    val shakeY =
        if (shakeAmplitude > 0f) (cos(damageElapsed / 21.0) * shakeAmplitude).toFloat() else 0f

    // F-b: stage-based ambient tint. derivedStateOf prevents cascade recomposition
    // when other parts of gameState change (only fires when stageIndex actually shifts).
    val stageTint by remember {
        derivedStateOf { stageTintColor(gameState.stageIndex) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonBgDeep, NeonBgMid, NeonBgEdge)
                )
            )
    ) {
        // Layer 0: Full-screen space ambience (5-layer parallax stars + dust + nebula
        // + galaxy + comet). Lifted out of GameWorld so it covers the bottom strip
        // where movement buttons sit, not just the entity area.
        SpaceBackground(
            state = gameState.background,
            running = gameState.gameStatus == GameStatus.RUNNING,
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )
        // Layer 1: stage tint — subtle color bias matching enemy palette of current stage.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(stageTint)
                .zIndex(1f)
        )

        IndicatorStatus(
            gameTime = gameState.gameTimeIndicator,
            hp = gameState.ship.hp,
            mineralsEarnedTotal = gameState.mineralsEarnedTotal,
            comboCount = gameState.comboCount,
            comboTier = gameState.comboTier,
            lastEnemyKillMillis = gameState.lastEnemyKillMillis,
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
        // Power-up duration indicators (Ec).
        PowerUpIndicators(
            ship = gameState.ship,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .zIndex(300f)
        )
        // 1c: Compact boss HP bar (200dp wide). Pinned 16dp BELOW the Settings icon
        // (top-right). Settings ends ~y=92dp (top padding 32 + size 60), so 16dp gap
        // gives top=108dp.
        BossHpBar(
            enemies = gameState.enemies,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 108.dp, end = 12.dp)
                .zIndex(300f)
        )

        // 5c: Neon glow stage banner replaces plain Text for stage messages.
        StageBanner(
            message = gameState.gameMessage,
            modifier = Modifier.align(Alignment.Center),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                }
        ) {
            GameWorld(
                ship = gameState.ship,
                shipLasers = gameState.shipLasers,
                ultimateLasers = gameState.ultimateLasers,
                spaceObjects = gameState.spaceObjects,
                boosters = gameState.boosters,
                enemies = gameState.enemies,
                enemyLasers = gameState.enemyLasers,
                minerals = gameState.minerals,
                explosions = gameState.explosions,
                magnetRadius = gameState.magnetRadius,
                damageNumbers = gameState.damageNumbers,
                pickupPopups = gameState.pickupPopups,
                bossIntroShownAtMillis = gameState.bossIntroShownAtMillis,
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

        // Vignette dark frame (1c cinematic).
        Vignette(
            hp = gameState.ship.hp,
            modifier = Modifier.fillMaxSize().zIndex(150f)
        )

        // Combo popup (Kc).
        ComboPopup(
            tier = gameState.comboPopupTier,
            shownAtMillis = gameState.comboPopupShownMillis,
            modifier = Modifier.align(Alignment.Center).zIndex(400f)
        )

        // Tutorial overlay (Nb): show on first ever game session.
        if (!tutorialShown) {
            TutorialOverlay(
                modifier = Modifier.fillMaxSize().zIndex(500f)
            )
        }

        // Damage flash overlay (gated by reduceMotion).
        if (flashProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeonRedAlert.copy(alpha = FLASH_MAX_ALPHA * flashProgress))
                    .zIndex(250f)
            )
        }
        // 12c: Wave clear bonus banner.
        WaveClearBanner(
            shownAtMillis = gameState.waveClearBannerShownMillis,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(420f)
        )
        // 21c: Boss intro overlay — pulsing red border + boss name + alarm SFX.
        BossIntroOverlay(
            bossName = gameState.bossIntroName,
            shownAtMillis = gameState.bossIntroShownAtMillis,
            modifier = Modifier.zIndex(440f),
        )
        // 9b: Achievement banner (slide-in 3s).
        AchievementBanner(
            achievement = gameState.achievementUnlocked,
            shownAtMillis = gameState.achievementShownAtMillis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(450f)
        )
        // 18c: Boss kill flash overlay (200ms white flash).
        val bossFlashElapsed = (now - gameState.bossKillFlashMillis).coerceAtLeast(0L)
        val bossFlashProgress = (1f - bossFlashElapsed.toFloat() / 200f).coerceIn(0f, 1f)
        if (bossFlashProgress > 0f && !reduceMotion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f * bossFlashProgress))
                    .zIndex(260f)
            )
        }
    }
}
