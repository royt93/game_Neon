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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.first
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
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

/**
 * Round 76 (R76d) → audit fix — short chapter display name for HUD badge.
 * Reads từ Chapter enum (single source of truth) thay vì duplicate string
 * hardcode. Lowercase first letter của each word để badge gọn hơn ALL CAPS.
 */
private fun chapterDisplayNameFor(chapterId: Int): String {
    val chapter = com.tranphuloi.neon.ui.game.stage.Chapter.entries.firstOrNull { it.id == chapterId }
        ?: return ""
    // Title Case từ "VÀNH ĐAI TIỂU HÀNH TINH" → "Vành Đai Tiểu Hành Tinh".
    return chapter.displayName.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * Pixel-3 round 4 — flash overlay alpha curve. Linear fade `1 - t/total`
 * felt instantaneous on device (user: "không phủ full screen"). New curve:
 *
 *   ramp up 0→1 over [0, peakMs]   (sharp visible flash-in)
 *   ramp down 1→0 over [peakMs, totalMs]   (gentle fade-out)
 *
 * Returns 0 outside [0, totalMs]. Used by both UltimateLaser + SmartBomb
 * flash overlays at different peak/total values.
 */
private fun flashCurve(elapsedMs: Long, peakMs: Long, totalMs: Long): Float {
    if (elapsedMs < 0L || elapsedMs >= totalMs) return 0f
    return if (elapsedMs < peakMs) {
        elapsedMs.toFloat() / peakMs
    } else {
        (1f - (elapsedMs - peakMs).toFloat() / (totalMs - peakMs)).coerceAtLeast(0f)
    }
}

@Composable
fun GameScreen(
    onGamePause: () -> Unit,
    onGameOver: (score: String) -> Unit,
    onOpenBuffPicker: () -> Unit = {},
    /** Round 51 (26x Photo mode) — flips true when GamePause "Chụp ảnh"
     *  tapped (signal from MainActivity). LaunchedEffect runs the capture
     *  flow + calls [onPhotoCaptureConsumed]. */
    photoCaptureRequested: Boolean = false,
    onPhotoCaptureConsumed: () -> Unit = {},
) {
    LaunchedEffect(Unit) { Logger.d("GameScreen entered") }
    val haptic = LocalHaptic.current
    val sfx = LocalSfx.current
    val settings = LocalSettings.current
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val vibrationEnabled by settings.vibrationEnabled.collectAsState(initial = true)
    val tutorialShown by settings.tutorialShown.collectAsState(initial = true) // optimistic to avoid flash on first compose

    val gameState = rememberGameState()

    // Round 51 (26x Photo mode) — capture flow (must be AFTER rememberGameState
    // so gameState is in scope):
    //  (1) GamePause sets photoCaptureRequested=true + pops back.
    //  (2) This effect fires; toggles gameState.photoModeActive=true so HUD
    //      gating below hides overlays on the next frame.
    //  (3) delay(120ms) lets Compose re-render the HUD-hidden state.
    //  (4) PhotoCapture.captureAndShare snapshots the activity window and
    //      launches the share chooser.
    //  (5) Clears photoModeActive + consumes the request signal.
    val view = androidx.compose.ui.platform.LocalView.current
    val captureContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(photoCaptureRequested) {
        if (!photoCaptureRequested) return@LaunchedEffect
        gameState.startPhotoCapture()
        // Round 51 audit pass 2 — wrap in try/finally so that if the
        // coroutine is cancelled mid-capture (user navigates Game → Menu,
        // Activity destroyed mid-flow, etc.) the HUD-hidden state still
        // gets reset. Without this, photoModeActive could stick at true and
        // the HUD would stay invisible forever.
        try {
            kotlinx.coroutines.delay(120L)
            com.tranphuloi.neon.ui.game.photo.PhotoCapture.captureAndShare(captureContext, view.rootView)
            kotlinx.coroutines.delay(60L)
        } finally {
            gameState.finishPhotoCapture()
            onPhotoCaptureConsumed()
        }
    }

    // Round 34 (42x) — post-boss roguelike buff picker. When bossKillBuffOfferMillis
    // changes (boss killed, non-final), wait for boss-rank overlay to finish
    // (~2s) then navigate to BuffPicker. Game continues running behind sheet.
    LaunchedEffect(gameState.bossKillBuffOfferMillis) {
        if (gameState.bossKillBuffOfferMillis > 0L) {
            Logger.d("BuffPicker offer triggered @ ${gameState.bossKillBuffOfferMillis}, delaying 2.2s for rank overlay")
            kotlinx.coroutines.delay(2200L)
            Logger.d("BuffPicker navigating now")
            onOpenBuffPicker()
        }
    }

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
    val metaRepo = com.tranphuloi.neon.data.LocalMetaProgression.current
    val achievementsRepo = com.tranphuloi.neon.data.LocalAchievements.current
    // Wave 11c P1-3 fix — idempotency guard. LaunchedEffect re-fires on every
    // gameStatus transition; if the kill-cam auto-GAME_OVER (FinalBoss path)
    // races with a manual transition the flush could double-count.
    var telemetryFlushed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.GAME_OVER && !telemetryFlushed) {
            telemetryFlushed = true
            // Round 23 — TIME_ATTACK timer expiry is also a "victory" (ship alive,
            // no kill-cam). Branch alongside FinalBoss defeat.
            val isVictory = gameState.finalBossDefeated || gameState.timeAttackEnded
            Logger.d("GameScreen detected GAME_OVER → ${if (isVictory) "VICTORY path" else "death kill-cam path"} (score=${gameState.mineralsEarnedTotal}, finalBoss=${gameState.finalBossDefeated}, timeAttack=${gameState.timeAttackEnded})")
            // 34d: differentiate victory feedback from death. Victory = no kill-cam delay,
            // celebratory PICKUP sfx + HEAVY haptic. Death = original LONG haptic + sad sfx.
            if (isVictory) {
                Logger.d("GAME_OVER victory feedback: HEAVY haptic + PICKUP sfx")
                if (vibrationEnabled) haptic.vibrate(HapticPattern.HEAVY)
                sfx.play(SfxEvent.PICKUP)
            } else {
                Logger.d("GAME_OVER death feedback: LONG haptic + GAME_OVER sfx (1.5s kill-cam delay)")
                if (vibrationEnabled) haptic.vibrate(HapticPattern.LONG)
                sfx.play(SfxEvent.GAME_OVER)
            }
            // 15c: snapshot end-of-run stats for DialogGameOver to read.
            // Wave 11c — single thread-safe telemetry snapshot, shared between
            // RunStats (display) and recordRunMetrics (persistence). The
            // snapshot is taken under lock inside GameState.snapshotRunTelemetry,
            // so concurrent IO-thread mutations to the underlying buffers can
            // never tear the read.
            val telemetry = gameState.snapshotRunTelemetry()
            runStats.value = com.tranphuloi.neon.data.RunStats(
                score = gameState.mineralsEarnedTotal.toIntOrNull() ?: 0,
                timeSec = gameState.gameTimeSec,
                enemiesKilled = gameState.enemiesKilledTotal,
                bossesDefeated = gameState.bossesDefeatedTotal,
                maxCombo = gameState.maxComboReached,
                stagesReached = gameState.stagesReached,
                victoryAchieved = isVictory,
                gameModeKey = gameState.gameMode.key,
                bulletKills = telemetry.bulletKills,
                bossKills = telemetry.bossKills,
                ranksAchieved = telemetry.ranksAchieved,
                shipSkin = telemetry.shipSkin,
                shipTimeMillis = telemetry.shipTimeMillis,
            )
            // Wave 11b — persist run telemetry. Single atomic DataStore edit
            // (see MetaProgressionRepository.recordRunMetrics). Note:
            // regularEnemyKills excludes boss kills — bosses are tracked
            // per-kind in bossKills. Lifetime "enemies killed" counter
            // therefore counts non-boss kills only.
            metaRepo.recordRunMetrics(
                regularEnemyKills = gameState.enemiesKilledTotal,
                bulletKills = telemetry.bulletKills,
                bossKills = telemetry.bossKills,
                shipTimeMillisBySkin = mapOf(telemetry.shipSkin to telemetry.shipTimeMillis),
                ranksAchieved = telemetry.ranksAchieved,
            )
            // Task 03 — trao XP cho TÀU đang dùng: địch thường + 10×boss. Level suy
            // ra qua ShipXpLevels; bonus HP áp ở run kế (đọc 1 lần/run trong GameState).
            metaRepo.addShipXp(
                gameState.shipShape.key,
                com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.xpForRun(
                    enemiesKilled = gameState.enemiesKilledTotal,
                    bossesDefeated = gameState.bossesDefeatedTotal,
                ),
            )
            // Task 08 — thưởng THỬ THÁCH HẰNG NGÀY nếu run này chơi đúng modifier
            // của ngày (1 lần/ngày, chống farm qua claimDailyChallenge).
            run {
                val today = com.tranphuloi.neon.data.LeaderboardRepository.todayUtcDayKey()
                val runMod = com.tranphuloi.neon.ui.game.modifier.RunModifier.fromKey(settings.lastModifier.first())
                if (runMod == com.tranphuloi.neon.ui.game.modifier.DailyChallenge.modifierFor(today)) {
                    val granted = metaRepo.claimDailyChallenge(
                        today, com.tranphuloi.neon.ui.game.modifier.DailyChallenge.REWARD_MINERALS,
                    )
                    if (granted > 0) Logger.d("Daily challenge complete → +$granted◇")
                }
            }
            // Wave 11c — telemetry-driven achievement checks. Runs once after
            // recordRunMetrics commits, so totals are fresh. .first() pulls a
            // single emission from each Flow; suspend keeps us inside this
            // LaunchedEffect's scope. Idempotent — repository.unlock returns
            // false if already unlocked.
            // Wave 22 (#4) — mở thành tựu lifetime cũng thưởng khoáng theo bậc.
            suspend fun awardLifetime(a: com.tranphuloi.neon.data.Achievement) {
                if (achievementsRepo.unlock(a)) {
                    metaRepo.addMinerals(com.tranphuloi.neon.data.achievementReward(a.tier))
                }
            }
            val plasmaTotal = metaRepo.bulletKills(com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA)
                .first()
            if (plasmaTotal >= 100) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.PLASMA_MASTER)
            }
            val homingTotal = metaRepo.bulletKills(com.tranphuloi.neon.ui.game.ship.laser.BulletType.HOMING)
                .first()
            if (homingTotal >= 100) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.HOMING_VETERAN)
            }
            val allBossKindKills = metaRepo.allBossKills.first()
            if (allBossKindKills.values.all { it >= 1 }) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.BOSS_ALL_KINDS)
            }
            val sRankCount = metaRepo.rankCount(com.tranphuloi.neon.ui.game.controls.BossRank.S)
                .first()
            if (sRankCount >= 10) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.S_RANK_10)
            }
            val cyanTime = metaRepo.shipTimeMillis(com.tranphuloi.neon.data.ShipSkin.AURA_CYAN)
                .first()
            if (cyanTime >= 3_600_000L) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.CYAN_HOUR)
            }
            val lifetimeEnemies = metaRepo.lifetimeEnemyKills.first()
            if (lifetimeEnemies >= 1000L) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.LIFETIME_KILLS_1000)
            }
            // Task 07 — ngưỡng qua AchievementUnlocks (single-source, tested).
            val lightningTotal = metaRepo.bulletKills(com.tranphuloi.neon.ui.game.ship.laser.BulletType.LIGHTNING).first()
            if (com.tranphuloi.neon.ui.game.state.AchievementUnlocks.lightningMaster(lightningTotal)) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.LIGHTNING_MASTER)
            }
            val shipXpMap = metaRepo.allShipXp.first()
            if (com.tranphuloi.neon.ui.game.state.AchievementUnlocks.shipMaxLevel(shipXpMap)) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.SHIP_MAX_LEVEL)
            }
            val ownedShipRanks = metaRepo.allRanks.first()
            val ownedShipCount = com.tranphuloi.neon.ui.game.ship.shape.ShipShape.entries.count {
                com.tranphuloi.neon.ui.game.ship.shape.ShipShopLogic.isOwned(it, ownedShipRanks)
            }
            if (com.tranphuloi.neon.ui.game.state.AchievementUnlocks.shipCollector(ownedShipCount)) {
                awardLifetime(com.tranphuloi.neon.data.Achievement.SHIP_COLLECTOR)
            }
            Logger.d("Snapshot RunStats: score=${gameState.mineralsEarnedTotal}, time=${gameState.gameTimeSec}s, enemies=${gameState.enemiesKilledTotal}, bosses=${gameState.bossesDefeatedTotal}, maxCombo=${gameState.maxComboReached}, stages=${gameState.stagesReached}, mode=${gameState.gameMode.key}")
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
            // Round 70 (Issue 9) — Per-tier haptic downgrade:
            //   - RAMPAGE/UNSTOPPABLE: LIGHT_TICK thay MEDIUM (mỗi kill → subtle).
            //   - GODLIKE: MEDIUM thay HEAVY (vẫn nhấn mạnh nhưng không "always
            //     on" trong combo dài 10+ kills).
            //   HapticPattern's minIntervalMs throttle xử lý hết chain vibration.
            if (vibrationEnabled) {
                when (gameState.comboTier) {
                    ComboTier.RAMPAGE, ComboTier.UNSTOPPABLE -> haptic.vibrate(HapticPattern.LIGHT_TICK)
                    ComboTier.GODLIKE -> haptic.vibrate(HapticPattern.MEDIUM)
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
    // 21c → Round 74 (R73e): Boss intro alarm với PER-BOSS audio cue (pitch shift
    // sfx_explosion). Wave 25c — pitch per-boss giờ ở bảng `BossMeta.introPitch`
    // (gom 4 when→1); đây chỉ đọc bảng.
    LaunchedEffect(gameState.bossIntroShownAtMillis) {
        if (gameState.bossIntroShownAtMillis > 0L) {
            if (vibrationEnabled) haptic.vibrate(HapticPattern.HEAVY)
            // Wave 25c — pitch đọc từ bảng chung `bossMetaFor` (gom 4 when → 1).
            val rate = gameState.bossIntroBossKind
                ?.let { com.tranphuloi.neon.ui.game.enemy.ship.model.bossMetaFor(it).introPitch }
                ?: 1.0f
            sfx.play(SfxEvent.EXPLOSION, rate)
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
        Logger.v { "Music intensity → $intensityTarget (animating from ${animatedIntensity.value})" }
        animatedIntensity.animateTo(intensityTarget, animationSpec = tween(500))
    }
    val effectiveMusicVolume = (musicVolumePref * animatedIntensity.value).toInt().coerceIn(0, 100)
    LaunchedEffect(effectiveMusicVolume) {
        audioHolder.setVolume(effectiveMusicVolume)
    }

    // Round 64 — pitch modulation REVERTED. Round 62 introduced boss=1.05 +
    // low-HP=0.92 pitch shift on BGM ExoPlayer.PlaybackParameters. User feedback
    // (runtime log Round 63 verify): "music nền có vẻ như bị overlay" — the
    // pitch shift made the multi-instrument BGM tracks sound off-key (different
    // instruments shifting non-uniformly through Sonic algorithm). Volume
    // intensity (Round 19/8c) preserved — it's the proven driver. setPitch
    // method on AudioPlayerHolder kept as public API in case future need.
    DisposableEffect(audioHolder) {
        onDispose {
            // Restore baseline volume so splash/menus aren't stuck attenuated.
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

        // Round 51 (26x Photo mode) — `hudVisible = !photoModeActive`. While
        // capturing, all HUD overlays (status, buffs row, settings cog,
        // power-up indicators, smart bomb, secondary weapon, boss HP bar) are
        // skipped at the Composable level so the captured frame is clean —
        // no buttons / text / bars overlaying the ship + entities.
        val hudVisible = !gameState.photoModeActive
        if (hudVisible) IndicatorStatus(
            gameTime = gameState.gameTimeIndicator,
            hp = gameState.ship.hp,
            mineralsEarnedTotal = gameState.mineralsEarnedTotal,
            comboCount = gameState.comboCount,
            comboTier = gameState.comboTier,
            lastEnemyKillMillis = gameState.lastEnemyKillMillis,
            lastMineralPickupMillis = gameState.lastMineralPickupMillis,
            lastBoosterPickupMillis = gameState.lastBoosterPickupMillis,
            hasReviveToken = gameState.hasReviveToken,
            // Round 76 (R76d) — pass enriched stats.
            currentChapterId = gameState.currentChapterId,
            currentChapterName = chapterDisplayNameFor(gameState.currentChapterId),
            stagesReached = gameState.stagesReached,
            enemiesKilledTotal = gameState.enemiesKilledTotal,
            bossesDefeatedTotal = gameState.bossesDefeatedTotal,
            shipShape = gameState.shipShape,
            // Wave 18 — nhãn đạn dời vào cột trái (hàng khoáng) trong IndicatorStatus,
            // hết đè thanh HP boss ở TopCenter/TopEnd (user: "top view to quá che UI").
            activeBulletName = gameState.ship.activeBulletType.displayName,
            activeBulletColorArgb = gameState.ship.activeBulletType.colorArgb,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(300f)
        )
        // Show stacked roguelike buffs as small chip row below IndicatorStatus.
        // Top padding clears the IndicatorStatus Left column max height: top
        // pad 16 + HP pill 60 + HP bar 12 + mineral row 28 + revive 28 + combo
        // ≈ 170dp. Using 178dp gives a 4-8dp visual gap before the chip row.
        if (hudVisible) com.tranphuloi.neon.ui.game.controls.ActiveBuffsHud(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 178.dp)
                .zIndex(300f),
        )
        if (hudVisible) ButtonSettings(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(300f)
        ) {
            Logger.d("Settings button pressed → toggleGameStatus + open pause")
            gameState.toggleGameStatus()
            onGamePause()
        }
        // Power-up duration indicators (Ec).
        if (hudVisible) PowerUpIndicators(
            ship = gameState.ship,
            droneCount = gameState.drones.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .zIndex(300f)
        )
        // 20b Smart bomb button — 30% smaller (42dp wrapper) + 8dp from right edge.
        // Sits above right movement button (at 124dp from bottom).
        // Pixel-3 round 5 — restored original padding 156/206 dp now that
        // labels are removed (no Column wrapper height to compensate for).
        if (hudVisible) SmartBombButton(
            count = gameState.smartBombs,
            onDispatch = { gameState.dispatchSmartBomb() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 156.dp)
                .zIndex(310f)
        )
        // Round 40 (29x) → 41 — secondary weapon button. Glyph reflects active
        // weapon (MISSILE / MINE / BURST) picked in Settings.
        if (hudVisible) com.tranphuloi.neon.ui.game.controls.SecondaryWeaponButton(
            weapon = gameState.activeSecondaryWeapon,
            cooldownProgress = gameState.secondaryCooldownProgress,
            onFire = { gameState.fireSecondary() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 206.dp)
                .zIndex(310f),
        )
        // Task 05 — ship active ability button (stacked above secondary weapon).
        if (hudVisible) com.tranphuloi.neon.ui.game.controls.AbilityButton(
            ability = gameState.shipAbility,
            cooldownProgress = gameState.abilityCooldownProgress,
            onActivate = { gameState.activateAbility() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 256.dp)
                .zIndex(310f),
        )
        // 1c: Compact boss HP bar (200dp wide). Pinned 16dp BELOW the Settings icon
        // (top-right). Settings ends ~y=76dp (top padding 16 + size 60), so 16dp gap
        // gives top=92dp.
        if (hudVisible) BossHpBar(
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
        // Wave 16 — full-screen cinematic now lasts 2400ms (was 1500ms banner).
        val bossIntroActive = gameState.bossIntroShownAtMillis > 0L &&
            (now - gameState.bossIntroShownAtMillis) < 2400L
        // 5c: Neon glow stage banner replaces plain Text for stage messages.
        // Stays at exact center (anchor banner — most important narrative event).
        // Hidden when boss-kill rank or boss intro is active.
        if (!bossRankActive && !bossIntroActive) {
            StageBanner(
                message = gameState.gameMessage,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        // Round 77 (R77h) — hold+drag layer over game world. Ship follows finger.
        // Round 77 audit fix — camera zoom coord conversion. graphicsLayer scale
        // pivot center (0.5, 0.5). Touch in screen coord. Game coord =
        // (touch - center) / scale + center.
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val zoomScale = gameState.cameraZoom.pixelScale
        val screenCenterX = configuration.screenWidthDp / 2f
        val screenCenterY = configuration.screenHeightDp / 2f
        fun mapTouchToGame(touchDpX: Float, touchDpY: Float): Pair<Float, Float> {
            val gx = (touchDpX - screenCenterX) / zoomScale + screenCenterX
            val gy = (touchDpY - screenCenterY) / zoomScale + screenCenterY
            return gx to gy
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomScale) {
                    detectDragGestures(
                        onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                            with(density) {
                                val (gx, gy) = mapTouchToGame(
                                    offset.x.toDp().value, offset.y.toDp().value,
                                )
                                gameState.onShipDragStart(gx, gy)
                            }
                        },
                        onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange,
                                   _: androidx.compose.ui.geometry.Offset ->
                            change.consume()
                            with(density) {
                                val (gx, gy) = mapTouchToGame(
                                    change.position.x.toDp().value,
                                    change.position.y.toDp().value,
                                )
                                gameState.onShipDragMove(gx, gy)
                            }
                        },
                        onDragEnd = { gameState.onShipDragEnd() },
                        onDragCancel = { gameState.onShipDragEnd() },
                    )
                },
        ) {
            GameWorld(
                ship = gameState.ship,
                shipLasers = gameState.shipLasers,
                ultimateLasers = gameState.ultimateLasers,
                spaceObjects = gameState.spaceObjects,
                boosters = gameState.boosters,
                drones = gameState.drones,
                enemies = gameState.enemies,
                enemyLasers = gameState.enemyLasers,
                minerals = gameState.minerals,
                explosions = gameState.explosions,
                magnetRadius = gameState.magnetRadius,
                damageNumbers = gameState.damageNumbers,
                impactSparks = gameState.impactSparks,
                trailLines = gameState.trailLines,
                pickupBursts = gameState.pickupBursts,
                pickupPopups = gameState.pickupPopups,
                bossIntroShownAtMillis = gameState.bossIntroShownAtMillis,
                lastBoosterPickupMillis = gameState.lastBoosterPickupMillis,
                lastMineralPickupMillis = gameState.lastMineralPickupMillis,
                chargeProgress = gameState.chargeProgress,
                mines = gameState.mines,
                lastBurstSweepMillis = gameState.lastBurstSweepMillis,
                lastBossHitMillis = gameState.lastBossHitMillis,
                lastBossHitX = gameState.lastBossHitX,
                lastBossHitY = gameState.lastBossHitY,
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
            // Round 77 (R77h) — 2 buttons REMOVED, replaced bằng hold+drag
            // anywhere on game world. ButtonsMovement deleted from compose tree.
            // (ButtonsMovement.kt + moveShipLeft/Right callbacks giữ lại trong
            // GameState class cho back-compat nếu user revert.)
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
            bossTaunt = gameState.bossIntroTaunt,
            shownAtMillis = gameState.bossIntroShownAtMillis,
            onSkip = gameState.onSkipBossIntro,
            modifier = Modifier.zIndex(470f),
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
        // Pixel-2 #2 fix — full-screen Ultimate laser flash overlay (cyan).
        // Visualizes the effect-zone of the 9 vertical beams sweep so user
        // sees coverage instead of just thin beams + per-hit explosions.
        //
        // Pixel-3 round 4 fix — bumped 600→900ms duration + 0.35→0.55 peak
        // alpha + flash-up-then-fade-out curve (peak at 150ms). User reported
        // linear fade felt "không phủ full screen"; flash-peak curve gives a
        // perceptible bright moment before settling.
        val ultElapsed = (now - gameState.ultimateFlashMillis).coerceAtLeast(0L)
        val ultProgress = flashCurve(ultElapsed, peakMs = 150L, totalMs = 900L)
        if (ultProgress > 0f && !reduceMotion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.tranphuloi.neon.common.NeonCyan.copy(alpha = 0.55f * ultProgress))
                    .zIndex(262f),
            )
        }
        // Pixel-3 round 5 — full-screen BURST sweep flash. Pre-fix the BURST
        // band rendered inside GameWorld's graphicsLayer → scaled down to
        // inner 70% at FAR zoom (user reported "splash xanh không full
        // screen"). Now overlay sits at GameScreen-level (outside the
        // graphicsLayer) so it truly covers device fullscreen. Shorter
        // duration than SmartBomb (BURST is secondary weapon, ~500ms) +
        // lighter cyan tint (0.40 peak alpha) to distinguish from Ultimate's
        // heavier cyan (0.55).
        val burstElapsed = (now - gameState.lastBurstSweepMillis).coerceAtLeast(0L)
        val burstProgress = flashCurve(burstElapsed, peakMs = 100L, totalMs = 500L)
        if (burstProgress > 0f && !reduceMotion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.tranphuloi.neon.common.NeonCyan.copy(alpha = 0.40f * burstProgress))
                    .zIndex(261f),
            )
        }
        // Pixel-2 #2 fix — full-screen SmartBomb flash overlay (violet).
        // Communicates the "all enemies cleared" effect zone — pre-fix user
        // only saw per-enemy explosions clustered where enemies happened to
        // be, not a screen-wide AOE.
        //
        // Pixel-3 round 4 fix — bumped 700→1000ms duration + 0.45→0.65 peak
        // alpha + flash-up curve. Heavier than ultimate to convey heavier
        // ability (SmartBomb is finite + clears everything).
        val sbElapsed = (now - gameState.smartBombFlashMillis).coerceAtLeast(0L)
        val sbProgress = flashCurve(sbElapsed, peakMs = 180L, totalMs = 1000L)
        if (sbProgress > 0f && !reduceMotion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.tranphuloi.neon.common.NeonViolet.copy(alpha = 0.65f * sbProgress))
                    .zIndex(263f),
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
