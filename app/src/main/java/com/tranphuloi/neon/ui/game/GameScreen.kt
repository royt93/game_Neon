package com.tranphuloi.neon.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// Note: `derivedStateOf` is still used for `stageTint` below; keep the import.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.tranphuloi.neon.ui.game.audio.LocalAudioPlayer
import com.tranphuloi.neon.ui.game.audio.LocalSfx
import com.tranphuloi.neon.ui.game.audio.SfxEvent
import com.tranphuloi.neon.ui.game.background.SpaceBackground
import com.tranphuloi.neon.ui.game.combo.ComboTier
import com.tranphuloi.neon.ui.game.controls.AchievementBanner
import com.tranphuloi.neon.ui.game.controls.BossHpBar
import com.tranphuloi.neon.ui.game.controls.BossIntroOverlay
import com.tranphuloi.neon.ui.game.controls.BossRankOverlay
import com.tranphuloi.neon.ui.game.controls.HazardOverlay
import com.tranphuloi.neon.ui.game.controls.PhaseTransitionBanner
import com.tranphuloi.neon.ui.game.controls.SmartBombButton
import com.tranphuloi.neon.ui.game.controls.StageBanner
import com.tranphuloi.neon.ui.game.controls.WaveClearBanner
import com.tranphuloi.neon.ui.game.controls.ButtonsMovement
import com.tranphuloi.neon.ui.game.controls.ButtonSettings
import com.tranphuloi.neon.ui.game.controls.ComboPopup
import com.tranphuloi.neon.ui.game.controls.IndicatorStatus
import com.tranphuloi.neon.ui.game.controls.PowerUpIndicators
import com.tranphuloi.neon.ui.game.controls.RevivedBanner
import com.tranphuloi.neon.ui.game.controls.StoryOverlay
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
 * 32d Wave 4 — Per-chapter ambient tint. Maps chapter id (1..5) to its theme color.
 *   Chapter 1 ASTEROID_BELT  → warm gold-orange
 *   Chapter 2 NEBULA_CLOUD   → violet
 *   Chapter 3 ICE_PLANET     → cyan
 *   Chapter 4 HOSTILE_STATION → red
 *   Chapter 5 GALAXY_CORE    → magenta
 */
private fun stageTintColor(chapterId: Int): Color {
    val tintAlpha = 0.07f
    return when (chapterId) {
        1 -> Color(0xFFFFB048).copy(alpha = tintAlpha)               // gold-orange
        2 -> NeonViolet.copy(alpha = tintAlpha)
        3 -> NeonCyan.copy(alpha = tintAlpha * 0.7f)                 // cyan slightly subdued (matches ice)
        4 -> NeonRedAlert.copy(alpha = tintAlpha)
        5 -> NeonMagenta.copy(alpha = tintAlpha)
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

    // Round 27 — intercept system back press while game is RUNNING.
    // Previously back popped the Game route → user lost run + state. Now back
    // pauses + shows DialogGamePause; user can choose Resume / Restart / Menu
    // explicitly. Only enabled when RUNNING — if pause dialog already showing
    // (gameStatus = PAUSE), back falls through to dismiss the dialog naturally.
    // GAME_OVER also leaves back disabled — DialogGameOver has dismissOnBackPress=false.
    androidx.activity.compose.BackHandler(enabled = gameState.gameStatus == GameStatus.RUNNING) {
        Logger.d("Back press intercepted in Game (RUNNING) → opening pause dialog")
        gameState.toggleGameStatus()
        onGamePause()
    }

    val runStats = com.tranphuloi.neon.data.LocalRunStats.current
    val runPersistence = com.tranphuloi.neon.data.LocalRunPersistence.current
    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.GAME_OVER) {
            // Round 23 — TIME_ATTACK timer expiry is also a "victory" (ship alive,
            // no kill-cam). Branch alongside FinalBoss defeat.
            val isVictory = gameState.finalBossDefeated || gameState.timeAttackEnded
            Logger.d("GameScreen detected GAME_OVER → ${if (isVictory) "VICTORY path" else "death kill-cam path"} (score=${gameState.mineralsEarnedTotal}, finalBoss=${gameState.finalBossDefeated}, timeAttack=${gameState.timeAttackEnded})")
            // 34d: differentiate victory feedback from death. Victory = no kill-cam delay,
            // celebratory PICKUP sfx + HEAVY haptic. Death = original LONG haptic + sad sfx.
            if (isVictory) {
                if (vibrationEnabled) haptic.vibrate(HapticPattern.HEAVY)
                sfx.play(SfxEvent.PICKUP)
            } else {
                if (vibrationEnabled) haptic.vibrate(HapticPattern.LONG)
                sfx.play(SfxEvent.GAME_OVER)
            }
            // 15c: snapshot end-of-run stats for DialogGameOver to read.
            runStats.value = com.tranphuloi.neon.data.RunStats(
                score = gameState.mineralsEarnedTotal.toIntOrNull() ?: 0,
                timeSec = gameState.gameTimeSec,
                enemiesKilled = gameState.enemiesKilledTotal,
                bossesDefeated = gameState.bossesDefeatedTotal,
                maxCombo = gameState.maxComboReached,
                stagesReached = gameState.stagesReached,
                victoryAchieved = isVictory,
                gameModeKey = gameState.gameMode.key,
            )
            // Round 26 — only CLEAR checkpoint on VICTORY (run truly complete).
            // On death: keep checkpoint so user can retry from last stage via
            // MenuScreen's "TIẾP TỤC" button. This matches checkpoint-style
            // progression: death = lose this attempt, but stage progress preserved.
            if (isVictory) {
                runPersistence.clearCheckpoint(gameState.gameMode.key)
                Logger.d("VICTORY: cleared checkpoint for ${gameState.gameMode.key} (run completed)")
            } else {
                Logger.d("DEATH: checkpoint preserved for ${gameState.gameMode.key} (user can retry)")
            }
            // 11c: kill-cam slow-mo replay = 1500ms. Victory = 500ms breathing room only
            // (no kill-cam to play, ship still alive).
            kotlinx.coroutines.delay(if (isVictory) 500L else 1500L)
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
    // 14c: Auto-revive feedback — heavy haptic + pickup sting on resurrection.
    LaunchedEffect(gameState.revivedShownAtMillis) {
        if (gameState.revivedShownAtMillis > 0L) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.HEAVY)
            sfx.play(SfxEvent.PICKUP)
        }
    }
    // 6c: Slow-motion critical sting — medium haptic on trigger.
    LaunchedEffect(gameState.bossSlowMotionStartedAtMillis) {
        if (gameState.bossSlowMotionStartedAtMillis > 0L) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.MEDIUM)
        }
    }

    AudioPlayer(gameStatus = gameState.gameStatus)

    // 8c Dynamic music intensity — modulate music volume based on combat pressure.
    // Three tiers blended via Animatable for smooth 500ms transitions:
    //   low (0.70x): few enemies, no boss, hp > 50%
    //   mid (0.85x): 4+ enemies on screen OR hp 30-50%
    //   high (1.00x): boss active OR hp < 30%
    // Final volume = userMusicVolume × intensity. Restored to baseline on dispose
    // so non-game screens (splash, dialogs) play at the user's chosen level.
    val audioHolder = LocalAudioPlayer.current
    val musicVolumePref by settings.musicVolume.collectAsState(initial = 80)
    // Compute intensity inline (NOT derivedStateOf): the closure would capture
    // the first composition's `gameState` and never see updates — `gameState` is
    // a plain Kotlin object, not a Compose State, so derivedStateOf can't track
    // it. Inline recomputes every recomposition (~125Hz) which is cheap.
    val hpRatio = (gameState.ship.hp / 1000f).coerceIn(0f, 1f)
    val hasBoss = gameState.enemies.any { it.isBoss }
    val enemyCount = gameState.enemies.size
    val intensityTarget = when {
        hasBoss -> 1.00f
        hpRatio < 0.30f -> 1.00f
        enemyCount >= 4 || hpRatio < 0.50f -> 0.85f
        else -> 0.70f
    }
    val animatedIntensity = remember { Animatable(0.85f) }
    LaunchedEffect(intensityTarget) {
        Logger.d("Music intensity → $intensityTarget (animating from ${animatedIntensity.value})")
        animatedIntensity.animateTo(intensityTarget, animationSpec = tween(500))
    }
    val effectiveMusicVolume = (musicVolumePref * animatedIntensity.value).toInt().coerceIn(0, 100)
    LaunchedEffect(effectiveMusicVolume) {
        audioHolder.setVolume(effectiveMusicVolume)
    }
    DisposableEffect(audioHolder) {
        onDispose {
            // Restore baseline so splash/menus aren't stuck at attenuated volume.
            audioHolder.setVolume(musicVolumePref)
            Logger.d("GameScreen disposed → music volume restored to $musicVolumePref")
        }
    }

    val now = System.currentTimeMillis()
    val damageElapsed = (now - gameState.lastShipDamageMillis).coerceAtLeast(0L)
    // Camera zoom removed per user feedback — kill-cam slow-mo is still in
    // GameState (frame-skip during kill-cam) but no visual zoom is applied.
    val shakeProgress =
        if (reduceMotion) 0f
        else (1f - (damageElapsed.toFloat() / SHAKE_DURATION_MILLIS)).coerceIn(0f, 1f)
    val flashProgress =
        if (reduceMotion) (1f - (damageElapsed.toFloat() / FLASH_DURATION_MILLIS)).coerceIn(0f, 1f) * 0.5f
        else (1f - (damageElapsed.toFloat() / FLASH_DURATION_MILLIS)).coerceIn(0f, 1f)
    val shakeAmplitude = SHAKE_MAX_PX * shakeProgress
    // 6c slow-mo critical: light additive shake during the 2s slow-mo window.
    val slowMoElapsed = (now - gameState.bossSlowMotionStartedAtMillis).coerceAtLeast(0L)
    val slowMoActive = gameState.bossSlowMotionStartedAtMillis > 0L && slowMoElapsed < 2000L
    val slowMoShakeAmp = if (slowMoActive && !reduceMotion) {
        // Ramp-in 0..200ms, hold til 1700ms, ramp-out 1700..2000ms.
        val a = when {
            slowMoElapsed < 200L -> slowMoElapsed.toFloat() / 200f
            slowMoElapsed < 1700L -> 1f
            else -> 1f - (slowMoElapsed - 1700L).toFloat() / 300f
        }
        a.coerceIn(0f, 1f) * 5f                                            // ~5px max, subtle
    } else 0f
    val shakeX =
        if (shakeAmplitude > 0f) (sin(damageElapsed / 18.0) * shakeAmplitude).toFloat()
        else if (slowMoShakeAmp > 0f) (sin(slowMoElapsed / 14.0) * slowMoShakeAmp).toFloat()
        else 0f
    val shakeY =
        if (shakeAmplitude > 0f) (cos(damageElapsed / 21.0) * shakeAmplitude).toFloat()
        else if (slowMoShakeAmp > 0f) (cos(slowMoElapsed / 17.0) * slowMoShakeAmp).toFloat()
        else 0f

    // 32d: chapter-based ambient tint. Inline (NOT derivedStateOf) — same closure
    // capture issue as music intensity: `gameState` is a plain Kotlin object, not
    // a Compose State, so derivedStateOf cache wouldn't invalidate. Inline recomputes
    // every recomposition (~125Hz) which is cheap (single int → Color lookup).
    val stageTint = stageTintColor(gameState.currentChapterId)

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
        // Layer 1: stage tint — subtle color bias matching chapter palette.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(stageTint)
                .zIndex(1f)
        )
        // Layer 1.5: 32d hazard visibility overlay (NEBULA_FOG dim, ICE edges).
        HazardOverlay(
            hazard = gameState.currentHazard,
            reduceMotion = reduceMotion,
            modifier = Modifier.zIndex(2f),
        )

        IndicatorStatus(
            gameTime = gameState.gameTimeIndicator,
            hp = gameState.ship.hp,
            mineralsEarnedTotal = gameState.mineralsEarnedTotal,
            comboCount = gameState.comboCount,
            comboTier = gameState.comboTier,
            lastEnemyKillMillis = gameState.lastEnemyKillMillis,
            lastMineralPickupMillis = gameState.lastMineralPickupMillis,
            lastBoosterPickupMillis = gameState.lastBoosterPickupMillis,
            hasReviveToken = gameState.hasReviveToken,
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
        // 20b Smart bomb button — 30% smaller (42dp wrapper) + 8dp from right edge.
        // Sits above right movement button (at 124dp from bottom).
        SmartBombButton(
            count = gameState.smartBombs,
            onDispatch = { gameState.dispatchSmartBomb() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 156.dp)
                .zIndex(310f)
        )
        // 1c: Compact boss HP bar (200dp wide). Pinned 16dp BELOW the Settings icon
        // (top-right). Settings ends ~y=76dp (top padding 16 + size 60), so 16dp gap
        // gives top=92dp.
        BossHpBar(
            enemies = gameState.enemies,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 92.dp, end = 12.dp)
                .zIndex(300f)
        )

        // Banner priority: BossRank (post-kill) > BossIntro > WaveClear > StageBanner > Combo.
        // Higher-priority banner suppresses lower-priority ones to keep UI clean.
        val bossRankActive = gameState.bossKillRankShownMillis > 0L &&
            (now - gameState.bossKillRankShownMillis) < 1800L
        val bossIntroActive = gameState.bossIntroShownAtMillis > 0L &&
            (now - gameState.bossIntroShownAtMillis) < 1500L
        // 5c: Neon glow stage banner replaces plain Text for stage messages.
        // Stays at exact center (anchor banner — most important narrative event).
        // Hidden when boss-kill rank or boss intro is active.
        if (!bossRankActive && !bossIntroActive) {
            StageBanner(
                message = gameState.gameMessage,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
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
                impactSparks = gameState.impactSparks,
                pickupBursts = gameState.pickupBursts,
                pickupPopups = gameState.pickupPopups,
                bossIntroShownAtMillis = gameState.bossIntroShownAtMillis,
                lastBoosterPickupMillis = gameState.lastBoosterPickupMillis,
                lastMineralPickupMillis = gameState.lastMineralPickupMillis,
                chargeProgress = gameState.chargeProgress,
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
        // 6c: Slow-motion critical pulse vignette — red radial gradient pulsing 2× sec,
        // gated by reduceMotion. Layered above hp vignette so boss-low-HP reads even
        // when player is healthy.
        if (slowMoActive && !reduceMotion) {
            // Pulse 0..0.6 alpha at 2Hz, dampened by ramp envelope.
            val pulseEnv = when {
                slowMoElapsed < 200L -> slowMoElapsed.toFloat() / 200f
                slowMoElapsed < 1700L -> 1f
                else -> (1f - (slowMoElapsed - 1700L).toFloat() / 300f).coerceAtLeast(0f)
            }
            val pulseAlpha = (0.30f + 0.30f * sin(slowMoElapsed / 100.0).toFloat()) * pulseEnv
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonRedAlert.copy(alpha = pulseAlpha.coerceIn(0f, 0.6f)),
                            ),
                        )
                    )
                    .zIndex(155f)
            )
        }

        // Combo popup (Kc) — offset 180dp ABOVE center. Hidden when boss-kill rank
        // or boss intro is active (priority).
        if (!bossRankActive && !bossIntroActive) {
            ComboPopup(
                tier = gameState.comboPopupTier,
                shownAtMillis = gameState.comboPopupShownMillis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 180.dp)
                    .zIndex(400f)
            )
        }

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
        // Chromatic aberration on player damage — simulated by left-edge red and
        // right-edge cyan gradient bands fading inward. Full RGB channel split
        // requires shaders; this approximation reads as "painful hit" cinematic.
        val chromaticElapsed = damageElapsed
        val chromaticProgress = if (gameState.lastShipDamageMillis > 0L &&
            chromaticElapsed < 240L && !reduceMotion
        ) {
            (1f - chromaticElapsed.toFloat() / 240f).coerceIn(0f, 1f)
        } else 0f
        if (chromaticProgress > 0f) {
            // Red tint shifted left edge.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF1430).copy(alpha = 0.45f * chromaticProgress),
                                Color.Transparent,
                            ),
                            endX = 600f,
                        ),
                    )
                    .zIndex(252f)
            )
            // Cyan tint shifted right edge.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = 0.40f * chromaticProgress),
                            ),
                            startX = 400f,
                        ),
                    )
                    .zIndex(252f)
            )
        }
        // 12c: Wave clear bonus banner — offset 180dp BELOW center. Hidden when
        // boss-kill rank is active.
        if (!bossRankActive) {
            WaveClearBanner(
                shownAtMillis = gameState.waveClearBannerShownMillis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 180.dp)
                    .zIndex(420f)
            )
        }
        // 21c: Boss intro overlay — pulsing red border + boss name + alarm SFX.
        BossIntroOverlay(
            bossName = gameState.bossIntroName,
            shownAtMillis = gameState.bossIntroShownAtMillis,
            modifier = Modifier.zIndex(440f),
        )
        // 24b: Boss kill rank S/A/B/C/D overlay — 1.8s after boss kill.
        BossRankOverlay(
            rank = gameState.bossKillRank,
            shownAtMillis = gameState.bossKillRankShownMillis,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(445f),
        )
        // 14c: Auto-revive banner — center text "REVIVED!" 1.6s. Highest priority
        // banner since it pre-empts what would have been a GAME_OVER.
        RevivedBanner(
            shownAtMillis = gameState.revivedShownAtMillis,
            modifier = Modifier.zIndex(460f),
        )
        // 34d: FinalBoss phase 2/3 transition banner. Renders only when an enemy
        // with currentPhase >= 2 has phaseTransitionMillis within last 1.4s.
        val finalBoss = gameState.enemies.firstOrNull { it.currentPhase > 0 }
        if (finalBoss != null) {
            PhaseTransitionBanner(
                phase = finalBoss.currentPhase,
                phaseTransitionMillis = finalBoss.phaseTransitionMillis,
                modifier = Modifier.zIndex(455f),
            )
        }
        // 9b: Achievement banner (slide-in 3s).
        AchievementBanner(
            achievement = gameState.achievementUnlocked,
            shownAtMillis = gameState.achievementShownAtMillis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(450f)
        )
        // 47x: Story dialogue — round 24: TOP anchored (was bottom, overlapped ship).
        // Ship is locked at maxYOffset (screenHeight-140) → bottom half is gameplay
        // zone; top zone (below HUD + BossHpBar) is safe for dialogue card.
        StoryOverlay(
            line = gameState.storyLine,
            shownAtMillis = gameState.storyShownMillis,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(448f)
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
        // Ship destroy phase 2 (200-300ms): full-screen white flash. Phase 1
        // (implosion) is in GameWorld; phase 3 (BANG explosions) triggered with
        // 300ms delay from GameState.onShipDestroyed.
        val destroyedElapsed = if (gameState.ship.destroyedAtMillis > 0L)
            now - gameState.ship.destroyedAtMillis else -1L
        if (destroyedElapsed in 200L..300L) {
            val flashT = (destroyedElapsed - 200L).toFloat() / 100f
            val flashAlpha = (1f - kotlin.math.abs(flashT - 0.5f) * 2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
                    .zIndex(270f)
            )
        }
    }
}
