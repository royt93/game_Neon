package com.tranphuloi.neon.ui.game.state

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tranphuloi.neon.core.tinker
import com.tranphuloi.neon.core.tinkerClearAll
import com.tranphuloi.neon.ui.game.background.BackgroundController
import com.tranphuloi.neon.ui.game.background.BackgroundState
import com.tranphuloi.neon.ui.game.booster.Booster
import com.tranphuloi.neon.ui.game.booster.BoosterController
import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
import com.tranphuloi.neon.ui.game.drone.Drone
import com.tranphuloi.neon.ui.game.drone.DroneController
import com.tranphuloi.neon.ui.game.drone.DroneLaser
import com.tranphuloi.neon.ui.game.drone.DroneToDroneUIMapper
import com.tranphuloi.neon.ui.game.drone.DroneUI
import com.tranphuloi.neon.ui.game.booster.BoosterUI
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.damage.DamageNumber
import com.tranphuloi.neon.ui.game.damage.DamageNumberController
import com.tranphuloi.neon.ui.game.hitstop.HitStopController
import com.tranphuloi.neon.data.Achievement
import com.tranphuloi.neon.ui.game.pickup.PickupPopup
import com.tranphuloi.neon.ui.game.pickup.PickupPopupController
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLasersController
import com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController
import com.tranphuloi.neon.ui.game.enemy.ship.mapper.EnemyToEnemyUIMapper
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import com.tranphuloi.neon.ui.game.explosion.controller.ExplosionController
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.laser.LaserToLaserUIMapper
import com.tranphuloi.neon.ui.game.mineral.controller.MineralsController
import com.tranphuloi.neon.ui.game.mineral.mapper.MineralToMineralUIMapper
import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI
import com.tranphuloi.neon.ui.game.settings.GameStatus
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI
import com.tranphuloi.neon.ui.game.ship.laser.LasersController
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.ship.ship.ShipController
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObject
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectToSpaceObjectUIMapper
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectUI
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectsController
import com.tranphuloi.neon.ui.game.stage.StageBoss
import com.tranphuloi.neon.ui.game.stage.StageController
import com.tranphuloi.neon.ui.game.stage.StageGame
import com.tranphuloi.neon.ui.game.stage.StageMessage
import com.tranphuloi.neon.utils.LeakWatch
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils
import com.tranphuloi.neon.utils.observeAsState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.Locale
import java.util.UUID

// ── Wave 16 balance — tunables cho cơ chế đạn trào phúng (đưa ra const để
// dễ chỉnh; xem onLaserHit + knockbackRef). ──
/** Bánh Mì hồi máu mỗi phát trúng. +1 (giảm từ 2) vì pierce×3 + rapid-fire
 *  có thể cộng dồn ~20-40 HP/s nếu để +2 → gần bất tử khi spam vào cụm. */
private const val BANH_MI_HEAL_PER_HIT: Int = 1
/** Cục Gạch hất địch lùi (px) khi trúng. */
private const val BRICK_KNOCKBACK_PX: Float = 32f

@Composable
fun rememberGameState(): GameState {
    // Round 37 — was Logger.d("rememberGameState: composing — entry point") here, but
    // the function recomposes ~125Hz (refreshHandler read at function tail drives the
    // game loop's per-frame recompose). That made the log fire 125×/sec. Entry-point
    // logging happens once inside the `remember { ... UuidUtils() }` block below.
    val configuration = LocalConfiguration.current
    // Task 04 — Context để resolve @StringRes thoại boss/narrative (song ngữ) từ
    // trong game-loop. Đọc read-only trên thread khác an toàn.
    val context = androidx.compose.ui.platform.LocalContext.current
    val screenWidth = rememberSaveable { configuration.screenWidthDp.toFloat() }
    val screenHeight = rememberSaveable { configuration.screenHeightDp.toFloat() }
    val uuidUtils = remember {
        Logger.d("rememberGameState: initial screen=${screenWidth}x${screenHeight}, building UuidUtils")
        UuidUtils()
    }
    val coroutineScope = rememberCoroutineScope()

    // Round 62 — VoiceAnnouncer (TTS) for hype callouts. Captured via
    // CompositionLocal so wiring respects the toggle from MainActivity.
    // Strings are pre-resolved here (not inside callbacks) to avoid retaining
    // Activity Context in long-lived `remember { ... }` lambdas — pre-resolved
    // String values are safe to capture.
    val voiceAnnouncer = com.tranphuloi.neon.ui.game.audio.LocalVoiceAnnouncer.current
    // Wave 11d Bug #3 fix — 3 variants per event for the announcer's randomizer.
    val voiceComboDoubleList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_double),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_double_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_double_3),
    )
    val voiceComboTripleList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_triple),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_triple_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_triple_3),
    )
    val voiceComboRampageList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_rampage),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_rampage_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_rampage_3),
    )
    val voiceComboUnstoppableList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_unstoppable),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_unstoppable_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_unstoppable_3),
    )
    val voiceComboGodlikeList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_godlike),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_godlike_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_combo_godlike_3),
    )
    val voiceBossDownList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_boss_down),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_boss_down_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_boss_down_3),
    )
    val voiceNewBestList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best_3),
    )
    val voiceAchievementUnlockedFmt = androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_achievement_unlocked)
    // Back-compat singletons preserved for any non-variant call sites.
    val voiceNewBest = voiceNewBestList[0]

    var background by remember {
        mutableStateOf(
            BackgroundState(
                stars = emptyList(),
                dust = emptyList(),
                nebula = emptyList(),
                comet = null,
                galaxy = null,
            )
        )
    }
    val backgroundController = remember {
        Logger.d("rememberGameState: building BackgroundController")
        BackgroundController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            updateState = { background = it },
        )
    }

    // -----------------------------------------------------------------
    // Wave 5 — RunContext + EffectiveStats hoisted BEFORE ship/controllers
    // so initial HP / damage / speed multipliers can be applied at
    // construction time.
    // -----------------------------------------------------------------
    val settingsRepo = com.tranphuloi.neon.data.LocalSettings.current
    val difficultyState = settingsRepo.difficulty
        .collectAsState(initial = com.tranphuloi.neon.data.Difficulty.NORMAL)
    val metaRepo = com.tranphuloi.neon.data.LocalMetaProgression.current
    val runPersistenceRepo = com.tranphuloi.neon.data.LocalRunPersistence.current

    // Wave 5 (43x) — pick GameMode for this run by reading SettingsRepository.lastMode
    // ONCE synchronously at composition entry. runBlocking is acceptable here:
    //   - DataStore reads are fast (<10ms typical),
    //   - only fires on first composition of rememberGameState,
    //   - avoids the timing race that collectAsState(initial="campaign") would create
    //     (controller built with initial, then rebuilt when flow emits → state loss).
    val runMode = remember {
        val key = kotlinx.coroutines.runBlocking { settingsRepo.lastMode.first() }
        val mode = com.tranphuloi.neon.ui.game.mode.GameMode.fromKey(key)
        Logger.d("rememberGameState: runMode=$mode (key=$key)")
        mode
    }
    val runModifier = remember {
        val key = kotlinx.coroutines.runBlocking { settingsRepo.lastModifier.first() }
        val mod = com.tranphuloi.neon.ui.game.modifier.RunModifier.fromKey(key)
        Logger.d("rememberGameState: runModifier=$mod (key=$key)")
        mod
    }
    // Wave 11b — snapshot ship skin once at run start for time-played attribution.
    // Re-skinning mid-run is not a supported flow, so the snapshot is safe.
    val runShipSkin = remember {
        kotlinx.coroutines.runBlocking { settingsRepo.shipSkin.first() }
    }
    // Wave 11b — per-run telemetry buffers. Flushed to MetaProgressionRepository
    // by GameScreen on GAME_OVER (single atomic DataStore edit). Plain HashMap +
    // MutableList (not Compose state) — these are only read at end of run, not
    // during recomposition. Reset to empty by remember{} on each GameState.
    // Wave 11b/11c P0 fix — game loop runs on Dispatchers.IO (see launch(IO)
    // below) while snapshot reads happen on Main during recomposition. Plain
    // HashMap/MutableList is not safe across threads. ConcurrentHashMap +
    // synchronizedList provide thread-safe mutation; .merge(key, 1, Int::plus)
    // is atomic (read-modify-write inside CHM lock).
    val bulletKillsThisRun = remember {
        java.util.concurrent.ConcurrentHashMap<com.tranphuloi.neon.ui.game.ship.laser.BulletType, Int>()
    }
    val bossKillsThisRun = remember {
        java.util.concurrent.ConcurrentHashMap<com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind, Int>()
    }
    val ranksAchievedThisRun = remember {
        java.util.Collections.synchronizedList(mutableListOf<com.tranphuloi.neon.ui.game.controls.BossRank>())
    }
    // Wave 11c — last bullet that landed on each enemy, used for precise kill
    // attribution. Updated by onLaserHit. Read by onEnemyKilled; entry cleared
    // after consumption. Off-screen enemies leak entries (bounded by enemy
    // cap = 30; worst-case a few hundred entries per run, GC'd at remember{}).
    val lastBulletTypeByEnemyId = remember {
        java.util.concurrent.ConcurrentHashMap<String, com.tranphuloi.neon.ui.game.ship.laser.BulletType>()
    }
    val runContext = remember(runMode, runModifier) {
        // Round 73 (Wave 8) — wire selectedShipShape vào EffectiveStats.
        // Wave 25 (#trial) — thử TÀU: dùng shape đang thử (bỏ qua khoá), KHÔNG
        // ghi đè selectedShipShape đã lưu.
        val resolvedShape = (com.tranphuloi.neon.ui.game.trial.TrialSession.spec
            as? com.tranphuloi.neon.ui.game.trial.TrialSpec.Ship)?.shape
            ?: kotlinx.coroutines.runBlocking { settingsRepo.selectedShipShape.first() }
        // Task 03 — đọc XP tàu 1 lần → hp bonus theo level (áp qua shipLevelHpMul).
        val shipXp = kotlinx.coroutines.runBlocking { metaRepo.shipXp(resolvedShape.key).first() }
        // Task 10 — đọc cấp prestige 1 lần → buff vĩnh viễn (prestigeMul).
        val prestigeLvl = kotlinx.coroutines.runBlocking { metaRepo.prestigeLevel.first() }
        com.tranphuloi.neon.ui.game.state.RunContext(
            mode = runMode,
            modifier = runModifier,
            difficulty = kotlinx.coroutines.runBlocking { settingsRepo.difficulty.first() },
            metaUpgrades = kotlinx.coroutines.runBlocking { metaRepo.allRanks.first() },
            shipShape = resolvedShape,
            shipLevelHpMul = com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.hpBonusMulForXp(shipXp),
            shipLevel = com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.levelForXp(shipXp),
            prestigeMul = com.tranphuloi.neon.data.prestigeMultiplier(prestigeLvl),
        )
    }
    // Task 12 (đợt 4, Slice 2) — mastery passive đang hiệu lực (null nếu tàu chưa
    // max level). Stat passive đã áp ở EffectiveStats; đây là các cờ HOOK runtime.
    val masteryPassive = remember(runContext) {
        com.tranphuloi.neon.ui.game.ship.shape.ShipPassive
            .activeFor(runContext.shipShape, runContext.shipLevel)
    }
    val passiveStartShield = masteryPassive?.effect ==
        com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.START_SHIELD
    val passiveLifestealHp = if (masteryPassive?.effect ==
        com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.LIFESTEAL) masteryPassive.magnitude.toInt() else 0
    val passiveRegenHp = if (masteryPassive?.effect ==
        com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.REGEN) masteryPassive.magnitude.toInt() else 0
    val passiveComboKeep = masteryPassive?.effect ==
        com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.COMBO_KEEP
    // Task 13 (đợt 4) — Parry controller (cooldown/window phản đạn).
    val parryController = remember { com.tranphuloi.neon.ui.game.ship.shape.ParryController() }
    // Task 05 — kỹ năng chủ động theo tàu (cooldown giảm theo level tàu).
    // effect hiện thực ở slice sau; hiện wiring cooldown + nút HUD.
    val shipAbility = remember { com.tranphuloi.neon.ui.game.ship.shape.ShipAbility.forShip(runContext.shipShape) }
    val abilityCooldownMs = remember { shipAbility.effectiveCooldownMs(runContext.shipLevel) }
    var lastAbilityFireMillis by remember { mutableLongStateOf(0L) }

    // Round 34 (42x) — activeBuffs is a reactive MutableState. Reads here so
    // effectiveStats recomputes when buff picked. baseStats computed once;
    // mergedStats = baseStats × buffMultipliers (recompute on buff change).
    val activeBuffsState = com.tranphuloi.neon.ui.game.buff.LocalActiveBuffs.current
    val activeBuffs by activeBuffsState
    // Round 77 audit fix — reactive cameraZoom for live update + drag coord conversion.
    val liveCameraZoom by settingsRepo.cameraZoom.collectAsState(
        initial = com.tranphuloi.neon.data.CameraZoom.MEDIUM,
    )
    val baseEffectiveStats = remember(runContext) {
        com.tranphuloi.neon.ui.game.state.EffectiveStats.compute(runContext)
    }
    val effectiveStats = remember(baseEffectiveStats, activeBuffs) {
        // Round 36 — merge logic extracted to EffectiveStats.withBuffs() for unit testability.
        val merged = baseEffectiveStats.withBuffs(activeBuffs)
        Logger.d("rememberGameState: effectiveStats computed=$merged (with ${activeBuffs.size} active buffs)")
        Logger.d("  · hpMul=${merged.hpMul} (Ship initial HP × ${merged.hpMul})")
        Logger.d("  · damageMul=${merged.damageMul} (laser impactPower × ${merged.damageMul})")
        Logger.d("  · speedMul=${merged.speedMul} (ShipController movementSpeed × ${merged.speedMul})")
        Logger.d("  · magnetMul=${merged.magnetMul} (MineralsController magnetRadius × ${merged.magnetMul})")
        Logger.d("  · scoreMul=${merged.scoreMul} (mineralsEarned × ${merged.scoreMul})")
        Logger.d("  · noShieldDrops=${merged.noShieldDrops}, bossesOnly=${merged.bossesOnly}")
        merged
    }
    val stageProvider = remember(runMode) {
        // Wave 25 (#trial) — thử BOSS: đấu trường luyện riêng boss đó (vô hạn).
        val trial = com.tranphuloi.neon.ui.game.trial.TrialSession.spec
        if (trial is com.tranphuloi.neon.ui.game.trial.TrialSpec.Boss) {
            Logger.d("rememberGameState: TRIAL boss arena = ${trial.variant.displayName}")
            return@remember com.tranphuloi.neon.ui.game.stage.TrialBossArenaProvider(trial.variant)
        }
        Logger.d("rememberGameState: building stageProvider for mode=${runMode.key}")
        when (runMode) {
            com.tranphuloi.neon.ui.game.mode.GameMode.SURVIVAL ->
                com.tranphuloi.neon.ui.game.stage.SurvivalProvider()
            com.tranphuloi.neon.ui.game.mode.GameMode.BOSS_RUSH ->
                com.tranphuloi.neon.ui.game.stage.BossRushProvider()
            com.tranphuloi.neon.ui.game.mode.GameMode.TIME_ATTACK ->
                com.tranphuloi.neon.ui.game.stage.TimeAttackProvider()
            com.tranphuloi.neon.ui.game.mode.GameMode.ENDLESS ->
                com.tranphuloi.neon.ui.game.stage.EndlessProvider()
            else -> com.tranphuloi.neon.ui.game.stage.StaticListProvider()
        }
    }
    val timeAttackLimitSec = remember(runMode) {
        (stageProvider as? com.tranphuloi.neon.ui.game.stage.TimeAttackProvider)?.timeLimitSec ?: 0
    }

    // Round 23 — apply hpMul at Ship construction. Coerce 100..3000 so extreme
    // modifiers (e.g. TANK + Easy + Fortify max) don't get unplayable.
    // Round 75 (R75a) — wire LEGENDARY_HP (+50 fixed HP if owned rank 1).
    val initialShipHp = remember(effectiveStats, runContext) {
        val baseHp = (1000f * effectiveStats.hpMul).toInt().coerceIn(100, 3000)
        val legendaryBonus = (runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_LEGENDARY_HP] ?: 0) * 50
        (baseHp + legendaryBonus).coerceIn(100, 3500)
    }
    // Wave 12 round 3 — purchased consumable stockpiles, read ONCE here.
    // Reading synchronously via runBlocking matches the existing
    // shipSkin/difficulty reads. The bomb stock seeds a persistent reserve
    // (`stockpileBombsRemaining`, spent per-use below); the revive stock seeds
    // one token in the Ship constructor and is consumed once by the
    // `reviveConsumed`-guarded effect.
    val startingBombStock = remember {
        kotlinx.coroutines.runBlocking {
            metaRepo.stockpileCount(com.tranphuloi.neon.data.ShopItem.SMARTBOMB_STOCKPILE_KEY).first()
        }
    }
    val startingReviveStock = remember {
        kotlinx.coroutines.runBlocking {
            metaRepo.stockpileCount(com.tranphuloi.neon.data.ShopItem.REVIVE_STOCKPILE_KEY).first()
        }
    }
    // Wave 14a Round 2 — 6 "buff 1 run" consumable stocks, read ONCE here;
    // consumed (1 each) by the guarded effect below; effect lasts the whole run.
    fun stockOf(key: String) = kotlinx.coroutines.runBlocking { metaRepo.stockpileCount(key).first() }
    val startX2Min = remember { stockOf(com.tranphuloi.neon.data.ShopItem.X2_MINERALS_KEY) }
    val startShield = remember { stockOf(com.tranphuloi.neon.data.ShopItem.START_SHIELD_KEY) }
    val startX2Score = remember { stockOf(com.tranphuloi.neon.data.ShopItem.X2_SCORE_KEY) }
    val startComboKeep = remember { stockOf(com.tranphuloi.neon.data.ShopItem.COMBO_KEEP_KEY) }
    val startMagnetXL = remember { stockOf(com.tranphuloi.neon.data.ShopItem.MAGNET_XL_KEY) }
    val startRapidFire = remember { stockOf(com.tranphuloi.neon.data.ShopItem.RAPID_FIRE_KEY) }
    // Whole-run multipliers (x2 Khoáng & x2 Điểm both boost yield — score==minerals
    // here so they stack; ×2 each → ×4 if both bought). Magnet XL doubles radius.
    val runYieldMult = remember { (if (startX2Min > 0) 2f else 1f) * (if (startX2Score > 0) 2f else 1f) }
    val runMagnetMult = remember { if (startMagnetXL > 0) 2f else 1f }
    var ship by rememberSaveable {
        mutableStateOf(
            Ship(
                xOffset = screenWidth / 2 - 85f / 2,
                yOffset = screenHeight + 240f,
                hp = initialShipHp,
                // Grant exactly one revive token at spawn if any were purchased.
                // Seeded in the constructor (not a post-composition ship.copy) so
                // it can't race the loadout head-start effect's ship mutation.
                hasReviveToken = startingReviveStock > 0,
                // Wave 14a — Gói Khiên Khởi Đầu: vào trận có sẵn khiên 8s.
                // Task 12 — passive GIÁP THÉP cũng cấp khiên khởi đầu.
                shieldEnabled = startShield > 0 || passiveStartShield,
                shieldEndMillis = if (startShield > 0 || passiveStartShield) System.currentTimeMillis() + 8000L else 0L,
            )
        )
    }
    // Round 45 (36x) — apply pre-game Loadout BulletType head-start (10s) once
    // per run. Uses `Flow.first()` to wait for the DataStore-resolved value
    // rather than `collectAsState`'s placeholder initial (which fires the
    // effect with NORMAL on frame 1 and then "loses" the real value because
    // we'd already flag loadoutApplied=true). rememberSaveable<Boolean>
    // ensures a mid-run config-change rotation doesn't grant a fresh 10s.
    // Wave 17l — đạn loadout áp dụng QUA shipController.setLoadoutBullet (xem
    // LaunchedEffect đặt SAU shipController, vì phải gọi nó). Trước đây set
    // ship.copy trực tiếp ở đây → controller ghi đè NORMAL → "đổi đạn vẫn y hệt".
    var loadoutApplied by rememberSaveable { mutableStateOf(false) }
    var gameStatus by rememberSaveable { mutableStateOf(GameStatus.RUNNING) }
    fun setGameStatus(gameStt: GameStatus) {
        if (gameStatus != gameStt) {
            Logger.d("gameStatus transition: $gameStatus → $gameStt (triggered by setGameStatus call)")
        }
        gameStatus = gameStt
    }
    var lastShipDamageMillis by remember { mutableLongStateOf(0L) }
    var lastBoosterPickupMillis by remember { mutableLongStateOf(0L) }
    // 14c Auto-revive — non-zero when revive token consumed; drives RevivedBanner render.
    var revivedShownAtMillis by remember { mutableLongStateOf(0L) }
    // 11c: Kill-cam state — when ship destroyed, GameScreen delays GAME_OVER navigation
    // by KILL_CAM_DURATION_MS to play slow-mo replay.
    var killCamStartedAtMillis by remember { mutableLongStateOf(0L) }
    var bossKillEventMillis by remember { mutableLongStateOf(0L) }      // 30c camera zoom event
    // 6c Slow-motion critical — when boss HP drops below 20% threshold, slow game tick
    // to 60% for 2s + pulse red vignette + light screen shake. Once per boss (gated
    // by enemyId so a new boss can re-trigger).
    var bossSlowMotionStartedAtMillis by remember { mutableLongStateOf(0L) }
    // Round 34 (42x) — boss kill buff offer trigger. GameScreen watches this
    // and navigates to BuffPicker when value changes from 0.
    var bossKillBuffOfferMillis by remember { mutableLongStateOf(0L) }
    // Round 34 (44x) — current stage hazard reactively tracked. Updated via
    // stageController.onStageAdvance below. ShipController reads this for slip mechanic.
    var currentHazard by remember { mutableStateOf<com.tranphuloi.neon.ui.game.stage.HazardType?>(null) }
    var bossSlowMotionTriggeredForId by remember { mutableStateOf<String?>(null) }
    // (settingsRepo / difficultyState / runMode / runModifier / runContext /
    //  effectiveStats / stageProvider / timeAttackLimitSec hoisted above
    //  ship init — round 23. See block right after backgroundController.)

    // 46x — achievement state hoisted ABOVE shipController so onShipRevived /
    // onShipDestroyed callbacks can call unlockAchievement. Was originally
    // declared right before MineralsController (~line 376); the move is purely
    // ordering, no semantic change.
    var achievementUnlocked by remember { mutableStateOf<Achievement?>(null) }
    var achievementShownAtMillis by remember { mutableLongStateOf(0L) }
    val achievementsRepo = com.tranphuloi.neon.data.LocalAchievements.current
    suspend fun unlockAchievement(achievement: Achievement) {
        if (achievementsRepo.unlock(achievement)) {
            // Wave 22 (#4) — thưởng khoáng theo bậc khi mở mới.
            metaRepo.addMinerals(com.tranphuloi.neon.data.achievementReward(achievement.tier))
            achievementUnlocked = achievement
            achievementShownAtMillis = System.currentTimeMillis()
            Logger.d("Achievement unlocked: id=${achievement.id} tier=${achievement.tier} title=\"${achievement.title}\" desc=\"${achievement.description}\"")
            // Round 62 — TTS callout interpolates the achievement title (already
            // localized via Achievement.title). Throttle inside VoiceAnnouncer
            // ensures back-to-back unlocks (e.g. KILL_50 + COMBO_5 in same frame)
            // only speak the first one.
            // Round 63 — TRIUMPH personality: slight pitch lift + relaxed rate
            // for achievement reveal. Celebration feel.
            voiceAnnouncer.announce(
                voiceAchievementUnlockedFmt.format(achievement.title),
                personality = com.tranphuloi.neon.ui.game.audio.VoicePersonality.TRIUMPH,
            )
        }
    }

    // Explosions controller declared early so onShipDestroyed can spawn a starburst
    // of explosions at the ship's last position when player dies.
    var explosions: List<Explosion> by rememberSaveable { mutableStateOf(emptyList()) }
    val explosionsController = remember {
        ExplosionController(
            initialExplosions = explosions,
            updateExplosions = { explosions = it }
        )
    }
    // Pickup burst controller declared early so onBoosterPickedUp callback can
    // spawn the ring + sparkle effect at the booster's position.
    var pickupBursts: List<com.tranphuloi.neon.ui.game.spark.PickupBurst> by remember {
        mutableStateOf(emptyList())
    }
    val pickupBurstController = remember {
        com.tranphuloi.neon.ui.game.spark.PickupBurstController(
            updateState = { pickupBursts = it }
        )
    }
    // HitStopController declared early so onLaserHit can call freezeForHit().
    var bossKillFlashMillis by remember { mutableLongStateOf(0L) }
    // Pixel-2 #2 fix — full-screen flash overlays for SmartBomb + UltimateLaser
    // fire. Before this, both abilities fired thin 9-beam sweeps + per-enemy
    // explosions but user perceived "splash xanh không phủ full screen".
    // Adds a 600ms colored full-screen flash to visualize the effect zone.
    var ultimateFlashMillis by remember { mutableLongStateOf(0L) }
    var smartBombFlashMillis by remember { mutableLongStateOf(0L) }
    // Round 79 (#4 fix) — boss hit lightning state. Updated on bullet→boss hit
    // via LasersController.onLaserHit(isBoss=true). BossHitLightning Composable
    // reads triggerMillis + position; fires 5-bolt full-screen lightning.
    var lastBossHitMillis by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var lastBossHitX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var lastBossHitY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Wave 17b — throttle nổ trên boss: rapid-fire + đa-đạn dồn vào boss khiến GIF
    // nổ xếp chồng dày che cả thân (user báo "nhiều hình overlay").
    var lastBossExplosionMillis by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val hitStopController = remember {
        HitStopController(onBossKillFlash = { bossKillFlashMillis = System.currentTimeMillis() })
    }
    // 24b Boss kill rank — declared early so enemyController.onEnemyKilled lambda
    // (which references them) compiles. Snapshot ship.hp + clock when boss spawns.
    var bossSpawnedAtMillis by remember { mutableLongStateOf(0L) }
    var playerHpAtBossSpawn by remember { mutableIntStateOf(0) }
    var bossKillRank by remember { mutableStateOf<com.tranphuloi.neon.ui.game.controls.BossRank?>(null) }
    var bossKillRankShownMillis by remember { mutableLongStateOf(0L) }
    // ImpactSparkController declared early so onShipDamaged lambda can spawn
    // sparks at ship hit points (enemy-laser → ship collisions).
    var impactSparks: List<com.tranphuloi.neon.ui.game.spark.ImpactSpark> by remember {
        mutableStateOf(emptyList())
    }
    val impactSparkController = remember {
        com.tranphuloi.neon.ui.game.spark.ImpactSparkController(
            updateState = { impactSparks = it }
        )
    }
    // Wave 11a Phase 4 (audit follow-up) — real bounce arc + chain bolt visuals
    // replacing 3-point impactSpark clusters that read as "3 explosions" not
    // "line traveled". TrailLineController spawns connected lines on REFLECT
    // absorb + CHAIN_LIGHTNING chain hops; TrailLineOverlay renders + fades.
    var trailLines: List<com.tranphuloi.neon.ui.game.spark.TrailLine> by remember {
        mutableStateOf(emptyList())
    }
    val trailLineController = remember {
        com.tranphuloi.neon.ui.game.spark.TrailLineController(
            updateState = { trailLines = it }
        )
    }
    // 15c Stats counters for end-of-run breakdown.
    var enemiesKilledTotal by remember { mutableIntStateOf(0) }
    var bossesDefeatedTotal by remember { mutableIntStateOf(0) }
    var maxComboReached by remember { mutableIntStateOf(0) }
    // 34d Wave 4 — set true when player defeats the FinalBoss (Galaxy Overlord).
    var finalBossDefeated by remember { mutableStateOf(false) }
    // 20b Smart bomb stack — start with 2, +1 per boss kill.
    // Round 75 (R75a) — wire EXTRA_BOMB SkillNode (+1 per rank, max 3 = +3 bombs)
    // + LEGENDARY_HP rank 1 = +1 bomb. Read once at init via runContext.metaUpgrades.
    val extraBombCount = (runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_EXTRA_BOMB] ?: 0) +
        (runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_LEGENDARY_HP] ?: 0)
    // Earned bombs (base 2 + EXTRA_BOMB/LEGENDARY_HP meta + boss-kill bonuses).
    // Ephemeral per run. Purchased bombs are a SEPARATE persistent reserve
    // (`stockpileBombsRemaining`) so they survive a run if not used.
    var smartBombs by rememberSaveable { mutableIntStateOf(2 + extraBombCount) }
    // Wave 12 round 3 — purchased smart-bomb reserve. True "consumed on use":
    // earned bombs are spent first, this reserve last (see dispatchSmartBomb),
    // so any reserve bombs left at run end persist to the next run. Each reserve
    // use decrements DataStore immediately. `rememberSaveable` survives config
    // change; restored value (not the re-read DataStore count) is authoritative
    // mid-run.
    var stockpileBombsRemaining by rememberSaveable { mutableIntStateOf(startingBombStock) }
    // Wave 12 round 3 — grant + consume the purchased revive token exactly once
    // at run start (token itself seeded in the Ship constructor above). The
    // `rememberSaveable` flag survives config-change recreation so the decrement
    // never double-fires. Mirrors the `loadoutApplied` guard above.
    var reviveConsumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reviveConsumed) return@LaunchedEffect
        if (startingReviveStock > 0) {
            metaRepo.consumeStockpile(com.tranphuloi.neon.data.ShopItem.REVIVE_STOCKPILE_KEY, 1)
            Logger.d("Shop consumable: revive token granted at run start (stock was $startingReviveStock)")
        }
        // Wave 14a Round 2 — consume the 6 "buff 1 run" packs that applied to
        // this run (effects already baked in above: shield ctor, combo window,
        // yield/magnet mults, rapid fire). Each decremented once. Idempotent via
        // the reviveConsumed guard (config-change / process-death safe).
        suspend fun useOne(stock: Int, key: String, label: String) {
            if (stock > 0) {
                metaRepo.consumeStockpile(key, 1)
                Logger.d("Shop consumable: $label applied this run (stock was $stock)")
            }
        }
        useOne(startX2Min, com.tranphuloi.neon.data.ShopItem.X2_MINERALS_KEY, "x2 Khoáng")
        useOne(startShield, com.tranphuloi.neon.data.ShopItem.START_SHIELD_KEY, "Khiên khởi đầu")
        useOne(startX2Score, com.tranphuloi.neon.data.ShopItem.X2_SCORE_KEY, "x2 Điểm")
        useOne(startComboKeep, com.tranphuloi.neon.data.ShopItem.COMBO_KEEP_KEY, "Giữ combo")
        useOne(startMagnetXL, com.tranphuloi.neon.data.ShopItem.MAGNET_XL_KEY, "Nam châm XL")
        useOne(startRapidFire, com.tranphuloi.neon.data.ShopItem.RAPID_FIRE_KEY, "Bắn nhanh")
        reviveConsumed = true
    }
    /**
     * Round 51 (26x Photo mode) — when true, GameScreen hides HUD overlays
     * (movement buttons, smart bomb, secondary weapon, score, combo, banners)
     * so the rendered GameWorld is "clean" for capture. The dialog +
     * capture-effect handle toggle: set true → wait one frame for HUD to
     * recompose hidden → capture → toggle false. `remember` (not
     * rememberSaveable) — purely transient UI flag, never needs to survive
     * config change.
     */
    var photoModeActive by remember { mutableStateOf(false) }
    /**
     * Round 40 (29x) — wall-clock of the last secondary-weapon fire. Cooldown
     * progress = (now - last) / activeWeapon.cooldownMs, clamped to 1.0 (ready).
     * Saved across config changes so a paused-then-rotated run doesn't get a
     * free fire on resume.
     */
    var lastSecondaryFireMillis by rememberSaveable { mutableLongStateOf(0L) }
    /** Round 41 (29x.2) — active mines list (MINE secondary). State lives here so
     *  GameScreen renders + ticks read the same source of truth. */
    var mines by remember {
        mutableStateOf<List<com.tranphuloi.neon.ui.game.ship.weapon.Mine>>(emptyList())
    }
    /** Round 41 (29x.2) — wall-clock when last BURST sweep fired; drives the
     *  fading horizontal sweep visual in GameWorld. 0 = no sweep active. */
    var lastBurstSweepMillis by remember { mutableLongStateOf(0L) }
    /** Round 41 — collected SecondaryWeapon selection from Settings. Task 17 — theo riêng runContext.shipShape. */
    val shipLoadout by remember(runContext.shipShape) {
        settingsRepo.loadoutForShip(runContext.shipShape)
    }.collectAsState(
        initial = com.tranphuloi.neon.data.ShipLoadout(
            bulletType = com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL,
            secondaryWeapon = com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon.MISSILE,
        ),
    )
    val activeSecondaryWeapon = shipLoadout.secondaryWeapon
    /** 46x — count of smart bombs used in current run (for SMART_BOMB_5 achievement). */
    var smartBombsUsedCount by rememberSaveable { mutableIntStateOf(0) }
    /**
     * Round 23 — TIME_ATTACK timer ran out, treat as "victory" (ship still alive,
     * no kill-cam). GameScreen branches on this to use HEAVY haptic + PICKUP sfx
     * + 500ms delay path instead of the death path's LONG haptic + GAME_OVER sfx.
     */
    var timeAttackEnded by remember { mutableStateOf(false) }
    // Round 58 — pickupPopupController hoisted above shipController so the
    // onBulletTypeActivated callback wired into ShipController can reach it.
    // Was declared further down (~line 530); the closure on `pickupPopupController`
    // is captured by value at the lambda's declaration site, so forward refs
    // don't resolve. Moving the val up fixes the unresolved reference.
    var pickupPopups: List<PickupPopup> by remember { mutableStateOf(emptyList()) }
    val pickupPopupController = remember {
        PickupPopupController(updateState = { pickupPopups = it })
    }
    // Round 60 (38x) — MINERAL_SUPERCHARGE callback holder. mineralsController
    // is declared AFTER shipController, so this deferred ref lets shipController
    // capture a stable lambda that we wire up below after mineralsController exists.
    // Plain object holder (not mutableStateOf) so reassignment doesn't trigger
    // recomposition — this is pure event dispatch.
    val mineralSuperchargeRef = remember { object { var run: () -> Unit = {} } }
    // Round 75 (R75c) — deferred ref cho SHIELD_BURST (cần explosions + enemies declared sau).
    val shieldBurstRef = remember {
        object { var run: (x: Float, y: Float, rank: Int) -> Unit = { _, _, _ -> } }
    }
    // Wave 11a Phase 3 — deferred ref cho REFLECT retaliation (cần enemies + lasersController declared sau).
    // P1 audit fix — @Volatile so IO loop (caller) sees Main-thread (composition) assignment.
    val reflectRetaliateRef = remember {
        object { @Volatile var run: (x: Float, y: Float) -> Unit = { _, _ -> } }
    }
    // Task 01 (Slice 4) — deferred ref cho DRONE_BOOSTER spawn (droneController
    // declared sau shipController). Wire xuống dưới sau khi droneController tồn tại.
    val droneSpawnRef = remember { object { @Volatile var run: () -> Unit = {} } }
    // Task 04 — deferred ref set storyLine (onEnemyKilled declared TRƯỚC storyLine
    // state). Wire .run sau khi storyLine khai báo.
    val setStoryLineRef = remember {
        object { @Volatile var run: (com.tranphuloi.neon.ui.game.story.StoryLine) -> Unit = {} }
    }
    // Wave 11a Phase 3 — deferred ref cho CHAIN_LIGHTNING (cần enemies declared sau).
    // primaryTargetId excluded so the chain doesn't re-hit the original target.
    val chainLightningRef = remember {
        object { @Volatile var run: (primaryTargetId: String, primaryDamage: Int, hitX: Float, hitY: Float) -> Unit = { _, _, _, _ -> } }
    }
    // Task 02 — deferred ref cho đạn LIGHTNING (sét lan tuần tự). Tách khỏi
    // chainLightningRef (booster) vì thuật toán khác (visited-set, 3 bước, ×0.7).
    val lightningChainRef = remember {
        object { @Volatile var run: (primaryTargetId: String, primaryDamage: Int, hitX: Float, hitY: Float) -> Unit = { _, _, _, _ -> } }
    }
    // Wave 16 Slice 4b — BRICK knockback. Deferred ref (enemies declared later);
    // pushes the hit enemy back (yOffset up, away from player) on impact.
    val knockbackRef = remember {
        object { @Volatile var run: (targetId: String) -> Unit = { } }
    }
    val shipController = remember {
        Logger.d("rememberGameState: building ShipController (initial hp=${ship.hp})")
        ShipController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            ship = ship,
            // Wrapper guard: once the ship is destroyed, ANY subsequent `setShip(...)`
            // call (from ShipController internals — moveShip, banking lerp, charge-shot
            // timer, fireLasers ship snapshot, iframes tick) must NOT reset the destroy
            // flags. Force-merge `destroyedAtMillis` + `shipSpriteHidden` from the live
            // state into every incoming Ship copy. Without this, controllers propagate
            // their stale `shipSpriteHidden=false` through copy() and clobber the flag
            // ~16ms after we set it to true.
            setShip = { newShip ->
                ship = if (ship.destroyedAtMillis > 0L || ship.shipSpriteHidden) {
                    newShip.copy(
                        destroyedAtMillis = ship.destroyedAtMillis,
                        shipSpriteHidden = ship.shipSpriteHidden,
                    )
                } else {
                    newShip
                }
            },
            onShipDestroyed = {
                killCamStartedAtMillis = System.currentTimeMillis()
                Logger.d("Kill-cam triggered @ $killCamStartedAtMillis (delay GAME_OVER 1500ms) shipPos=(${ship.xOffset.toInt()},${ship.yOffset.toInt()}) hp=${ship.hp}")
                setGameStatus(GameStatus.GAME_OVER)
                // Mark ship destroyed — drives implosion animation in GameWorld.
                ship = ship.copy(destroyedAtMillis = killCamStartedAtMillis)
                Logger.d("Ship.copy(destroyedAtMillis=$killCamStartedAtMillis) — implosion phase begins")
                // 3-phase destruction: implosion (0-200ms), hide sprite (200ms),
                // explosion starburst (300ms so flash → BANG sequence reads).
                val cx = ship.xOffset + ship.width / 2f
                val cy = ship.yOffset + ship.height / 2f
                val size = ship.width
                coroutineScope.launch {
                    delay(200L)
                    // Explicit state mutation — flips ship reference so Compose
                    // recomposes GameWorld with `shipSpriteHidden=true`, removing
                    // ship Box + flame + magnet visual from the tree.
                    ship = ship.copy(shipSpriteHidden = true)
                    Logger.d("Ship sprite hidden via state flag (200ms after destroy)")
                    delay(100L)
                    explosionsController.addExplosion(cx, cy, size * 1.4f, size * 1.4f)
                    explosionsController.addExplosion(cx - size * 0.5f, cy - size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx + size * 0.5f, cy - size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx - size * 0.5f, cy + size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx + size * 0.5f, cy + size * 0.4f, size * 0.7f, size * 0.7f)
                    Logger.d("Ship destruction starburst spawned at ($cx, $cy) — 5 explosions")
                }
            },
            onShipDamaged = {
                lastShipDamageMillis = System.currentTimeMillis()
                // Spawn impact spark burst at ship center so enemy-laser → ship
                // collisions get the same visual feedback as ship-laser → enemy.
                impactSparkController.spawnBurst(
                    ship.xOffset + ship.width / 2f,
                    ship.yOffset + ship.height / 2f,
                )
                Logger.v { "Ship damaged event @ $lastShipDamageMillis (will trigger shake+flash)" }
            },
            onBoosterPickedUp = { x, y ->
                lastBoosterPickupMillis = System.currentTimeMillis()
                pickupBurstController.spawn(x, y)
                Logger.v { "Booster picked up @ ($x,$y) ts=$lastBoosterPickupMillis" }
            },
            // Task 01 (Slice 4) — nhặt DRONE_BOOSTER → spawn drone (deferred ref).
            onDroneBoosterPickedUp = { droneSpawnRef.run() },
            onShipRevived = {
                revivedShownAtMillis = System.currentTimeMillis()
                // Pickup-style burst at ship center for resurrection feel.
                pickupBurstController.spawn(
                    ship.xOffset + ship.width / 2f,
                    ship.yOffset + ship.height / 2f,
                )
                // 46x REVIVE_ONCE achievement.
                coroutineScope.launch { unlockAchievement(Achievement.REVIVE_ONCE) }
                Logger.w("Auto-revive consumed @ $revivedShownAtMillis (banner shown 1.6s)")
            },
            onSpaceObjectHitShip = { x, y ->
                // Big visual smash at rock center — sparks + mini explosion. Ship damage
                // shake/flash already triggered separately via onShipDamaged → updateHp.
                impactSparkController.spawnBurst(x, y)
                explosionsController.addExplosion(x, y, 70f, 70f)
                Logger.v { "SpaceRock impact ship @ ($x,$y) — visual burst" }
            },
            damageMultiplier = { difficultyState.value.multiplier },
            // Balance polish (Finding #4) — trần heal = max hp thật (updateHp cap để
            // HEALING_AURA/REGEN booster/vampire không overheal vượt max).
            maxHpProvider = { initialShipHp },
            // 25x/48x — modifier + skill tree speed multiplier (TRIPLE_SPEED ×3,
            // TANK ×0.7, AGILITY +6%/rank).
            speedMultiplier = { effectiveStats.speedMul },
            // Round 34 (44x) — ICE_PATCHES hazard active reads from currentHazard state
            // which is updated by stageController.onStageAdvance (declared below).
            isIceHazardActive = {
                currentHazard == com.tranphuloi.neon.ui.game.stage.HazardType.ICE_PATCHES
            },
            // Round 58 — visible PIERCING/PLASMA activation hint. Shows the
            // effective stats from round 52 rarity scaling at the ship
            // location for 500ms so player can verify tier-up at runtime.
            onBulletTypeActivated = { type, rarity, x, y ->
                val mapper = com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
                val (text, colorHex) = when (type) {
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.PIERCING -> {
                        val n = com.tranphuloi.neon.ui.game.ship.laser.BulletType
                            .pierceCountForRarity(rarity)
                        "→ PIERCING ×$n" to mapper.PIERCING_TINT_ARGB
                    }
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA -> {
                        val r = (com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA.aoeRadius *
                            com.tranphuloi.neon.ui.game.ship.laser.BulletType
                                .plasmaAoeMultiplierForRarity(rarity)).toInt()
                        "◯ PLASMA ${r}px" to mapper.PLASMA_TINT_ARGB
                    }
                    // Round 67 (Wave 10a) — 3 bullet types with full behaviors.
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.FIRE ->
                        "♨ ${type.displayName}" to mapper.FIRE_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.HOMING ->
                        "◎ ${type.displayName}" to mapper.HOMING_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.BOUNCE ->
                        "⇄ ${type.displayName}" to mapper.BOUNCE_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.GIANT ->
                        "⬤ ${type.displayName}" to mapper.GIANT_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.SMOKE ->
                        "❍ ${type.displayName}" to mapper.SMOKE_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.ZIGZAG ->
                        "⌇ ${type.displayName}" to mapper.ZIGZAG_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.KAMEHAMEHA ->
                        "⊛ ${type.displayName}" to mapper.KAMEHAMEHA_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.ATOMIC ->
                        "⊙ ${type.displayName}" to mapper.ATOMIC_TINT_ARGB
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.SPLIT ->
                        "Ѱ ${type.displayName}" to mapper.SPLIT_TINT_ARGB
                    else -> return@ShipController
                }
                pickupPopupController.spawnBulletTypeActivation(
                    text = text,
                    colorHex = colorHex,
                    xOffset = x,
                    yOffset = y,
                )
            },
            // Round 60 (38x) — MINERAL_SUPERCHARGE pickup delegates to
            // MineralsController via the deferred ref holder. Assignment of
            // `mineralSuperchargeRef.run` happens below, after mineralsController
            // is built, since the controller is declared later in this scope.
            onMineralSupercharge = { mineralSuperchargeRef.run() },
            // Round 75 (R75a) — wire 2 meta upgrades via lambdas (rank lookup).
            bulletDurationRank = { runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_BULLET_DURATION] ?: 0 },
            shieldDurationRank = { runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_SHIELD] ?: 0 },
            dashRank = { runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_DASH] ?: 0 },
            // Round 75 (R75c) — SHIELD_BURST: AoE damage all enemies trong 120dp
            // bán kính + spawn explosion VFX khi shield expires. Damage scale theo rank.
            shieldBurstRank = { runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_SHIELD_BURST] ?: 0 },
            onShieldExpireBurst = { x, y, rank -> shieldBurstRef.run(x, y, rank) },
            // Wave 11a Phase 3 — REFLECT_BOOSTER retaliation. When enemy laser
            // absorbed, find the nearest enemy and deal 30 damage. Visualized
            // via existing damageNumberController + impactSparkController so
            // player sees feedback identical to a real laser hit.
            onReflectAbsorb = { absorbX, absorbY -> reflectRetaliateRef.run(absorbX, absorbY) },
        )
    }

    // Wave 17l — áp đạn loadout SAU khi shipController tồn tại, QUA setLoadoutBullet
    // (đồng bộ ship nội bộ controller). Re-apply mỗi lần vào run nếu khác đạn nền
    // → đổi đạn ở TRANG BỊ rồi TIẾP TỤC là đổi ngay. (Fix gốc "đổi đạn vẫn y hệt".)
    LaunchedEffect(Unit) {
        // Wave 25 (#trial) — nếu đang "chơi thử" 1 loại đạn từ Bách Khoa: dùng thẳng
        // đạn đó (BỎ QUA khoá shop — mục đích là dùng thử), KHÔNG ghi đè loadout đã lưu.
        val trial = com.tranphuloi.neon.ui.game.trial.TrialSession.spec
        if (trial is com.tranphuloi.neon.ui.game.trial.TrialSpec.Bullet) {
            Logger.d("Loadout: TRIAL bullet=${trial.type} (bỏ qua khoá shop)")
            shipController.setLoadoutBullet(trial.type)
            loadoutApplied = true
            return@LaunchedEffect
        }
        val preferred = settingsRepo.loadoutForShip(runContext.shipShape).first().bulletType
        // Wave 17q — đã revert mở-khoá-tạm roy93~: đạn shop-gated chưa mua → về
        // NORMAL ở run-start (khớp gate ở DialogLoadoutPicker).
        val resolved = if (com.tranphuloi.neon.data.ShopItem
                .isShopUnlocked(runContext.metaUpgrades, preferred.shopUnlockId)
        ) {
            preferred
        } else {
            com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL
        }
        if (loadoutApplied && ship.baseBulletType == resolved) return@LaunchedEffect
        Logger.d("Loadout: bullet=$resolved áp dụng CẢ RUN qua ShipController")
        shipController.setLoadoutBullet(resolved)
        loadoutApplied = true
    }

    var shipLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    var ultimateLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    var damageNumbers: List<DamageNumber> by remember { mutableStateOf(emptyList()) }
    val damageNumberController = remember {
        DamageNumberController(updateState = { damageNumbers = it })
    }
    // Round 34 (41x) — StatusEffectController. Applies BURN/SLOW/STUN to enemies
    // via onLaserHit (random 10% chance for now until BulletType refactor wires
    // effects to specific bullet types). Tick processes BURN dmg + drops expired.
    val statusEffectController = remember {
        com.tranphuloi.neon.ui.game.status.StatusEffectController()
    }
    var statusEffectTick by remember { mutableLongStateOf(0L) }     // bumped each loop to force HUD recompose
    val lasersController = remember {
        LasersController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            initialShipLasers = shipLasers,
            initialUltimateLasers = ultimateLasers,
            setShipLasers = { shipLasers = it },
            setUltimateLasers = { ultimateLasers = it },
            onLaserHit = { targetId, damage, x, y, isBoss, bulletType ->
                damageNumberController.report(targetId, damage, x, y, isBoss)
                impactSparkController.spawnBurst(x, y)
                // Wave 11c — record last-hit bullet for precise kill attribution.
                // Overwrites prior; lethal hit's bullet wins.
                lastBulletTypeByEnemyId[targetId] = bulletType
                // Wave 11a Phase 2 — VAMPIRE_BOOSTER lifesteal: heal ship 50% of
                // damage dealt while active. Silent (no per-hit log spam).
                shipController.applyVampireHeal(damage)
                // Wave 11a Phase 3 — CHAIN_LIGHTNING_BOOSTER chains to 2 more
                // nearest enemies (excluding the primary target) at 50% damage.
                // Deferred ref because `enemies` is declared after lasersController.
                if (shipController.isChainLightningActive() && damage > 0) {
                    chainLightningRef.run(targetId, damage, x, y)
                }
                // Task 02 — đạn LIGHTNING: sét lan TUẦN TỰ (visited-set, 3 bước,
                // ×0.7). Độc lập với booster 2-hop ở trên.
                if (bulletType == com.tranphuloi.neon.ui.game.ship.laser.BulletType.LIGHTNING && damage > 0) {
                    lightningChainRef.run(targetId, damage, x, y)
                }
                // Round 79 (#4 fix) — full-screen lightning when bullet hits boss.
                if (isBoss) {
                    lastBossHitMillis = System.currentTimeMillis()
                    lastBossHitX = x
                    lastBossHitY = y
                }
                // Mini explosion at hit point — reuses the GIF explosion system so the
                // hit reads as a real "pháo hoa nổ tung" not just sparks.
                // Wave 17b — trên BOSS: nổ nhỏ hơn (40dp) + GIÃN NHỊP ≥120ms/lần để
                // GIF không xếp chồng dày che thân boss (đọc rõ shape). Địch thường
                // giữ nguyên 45dp mỗi viên (chết nhanh nên không tích tụ).
                if (isBoss) {
                    val nowExp = System.currentTimeMillis()
                    if (nowExp - lastBossExplosionMillis >= 120L) {
                        lastBossExplosionMillis = nowExp
                        explosionsController.addExplosion(x, y, 40f, 40f)
                    }
                } else {
                    explosionsController.addExplosion(x, y, 45f, 45f)
                }
                hitStopController.freezeForHit()
                // Round 34 (41x) — 10% chance to apply random status effect per hit.
                // Boss has reduced chance (5%) so they don't burn-stack-die unfairly.
                val chance = if (isBoss) 0.05f else 0.10f
                if (kotlin.random.Random.nextFloat() < chance) {
                    val effect = com.tranphuloi.neon.ui.game.status.StatusEffect.values().random()
                    statusEffectController.apply(targetId, effect, System.currentTimeMillis())
                }
                // Round 67 (Wave 10a) — FIRE bullet always applies BURN status
                // on hit (100% chance during the FIRE buff window). This is
                // the GUARANTEED effect, separate from the random 10% above.
                if (ship.activeBulletType == com.tranphuloi.neon.ui.game.ship.laser.BulletType.FIRE) {
                    statusEffectController.apply(
                        targetId,
                        com.tranphuloi.neon.ui.game.status.StatusEffect.BURN,
                        System.currentTimeMillis(),
                    )
                }
                // Wave 17 — ATOMIC: ngoài splash AoE 150, để lại "phóng xạ" =
                // gây BURN (DoT) cho MỌI địch trúng (kể cả nạn nhân splash, vì
                // onLaserHit được gọi cho từng nạn nhân với bulletType=ATOMIC).
                // → phân biệt hẳn PLASMA (chỉ splash tức thời, không DoT).
                if (bulletType == com.tranphuloi.neon.ui.game.ship.laser.BulletType.ATOMIC) {
                    statusEffectController.apply(
                        targetId,
                        com.tranphuloi.neon.ui.game.status.StatusEffect.BURN,
                        System.currentTimeMillis(),
                    )
                }
                // Wave 16/18 — đạn trào phúng: CƠ CHẾ RIÊNG (tách bạch).
                // Hiệu ứng-status (SLOW/STUN/CORROSION) lấy từ SSOT thuần
                // [BulletOnHitStatus] (testable). Hiệu ứng KHÔNG-status (hồi máu /
                // hất văng) giữ nhánh riêng vì cần shipController/knockback.
                when (bulletType) {
                    // Bánh Mì — HỒI MÁU tàu mỗi phát trúng (giòn rụm, ăn no).
                    // Wave 17r — QUA shipController (trước set ship.copy trực tiếp →
                    // moveShip ghi đè → heal MẤT, cùng bug loadout).
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.BANH_MI ->
                        if (damage > 0) shipController.healCapped(BANH_MI_HEAL_PER_HIT, maxHp = initialShipHp)
                    // Cục Gạch — HẤT VĂNG địch ra sau.
                    com.tranphuloi.neon.ui.game.ship.laser.BulletType.BRICK ->
                        knockbackRef.run(targetId)
                    // Sầu Riêng (SLOW) · Like/Tim (STUN) · Nước Mắm (CORROSION) ·
                    // Mã QR (SLOW+STUN) — áp toàn bộ status theo SSOT.
                    else -> {
                        val nowStatus = System.currentTimeMillis()
                        com.tranphuloi.neon.ui.game.status.BulletOnHitStatus
                            .statusEffectsFor(bulletType)
                            .forEach { statusEffectController.apply(targetId, it, nowStatus) }
                    }
                }
            },
            // 25x/48x — modifier + skill tree damage multiplier applied per hit.
            // Round 60 (38x) — BERSERK + CRIT_SURGE stack multiplicatively on top.
            // BERSERK ×2 (12s), CRIT_SURGE ×3 (8s); both active = ×6 vs baseline.
            // Round 67 (10a) — active BulletType damage multiplier stacks too.
            // FIRE ×1.2, HOMING ×0.8, BOUNCE ×0.7. Read from current ship state
            // each hit so transitions in/out of bullet-type window apply live.
            damageMultiplier = {
                // Round 75 (R75b) — LEGENDARY_DAMAGE: +25% damage khi HP > 75%.
                // Rank 1 only (maxRank=1). HP fraction tính từ initialHp snapshot.
                val legendaryDmgRank = runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_LEGENDARY_DMG] ?: 0
                val healthyBonus = if (legendaryDmgRank > 0 &&
                    ship.hp.toFloat() / initialShipHp > 0.75f) 1.25f else 1f
                // Round 75 (R75b) — CRIT meta: +10% chance/rank to ×2 damage.
                // Random roll per hit. Up to maxRank 3 = 30% crit rate.
                val critRank = runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_CRIT] ?: 0
                val critRoll = if (critRank > 0 &&
                    kotlin.random.Random.nextFloat() < critRank * 0.10f) 2f else 1f
                // Task 05 — OVERDRIVE (×1.6) + CRIT_FRENZY (×2.2) burst kỹ năng chủ động.
                val nowMs = System.currentTimeMillis()
                val abilityDmgMul = (if (ship.abilityOverdriveEndMillis > nowMs) 1.6f else 1f) *
                    (if (ship.abilityCritEndMillis > nowMs) 2.2f else 1f)
                effectiveStats.damageMul *
                    shipController.berserkDamageMul() *
                    shipController.critSurgeMul() *
                    ship.activeBulletType.damageMultiplier *
                    healthyBonus *
                    critRoll *
                    abilityDmgMul
            },
        ).also {
            // Wave 14a — Gói Bắn Nhanh: rút ngắn nhịp bắn (~1.5×) cả run.
            // Wave 18b — nay nhịp bắn do ITEM ĐẠN quy định (BulletType.fireIntervalMillis);
            // Gói Bắn Nhanh thành HỆ SỐ nhân (0.66 ≈ ×1.5 tốc) áp lên mọi loại đạn.
            if (startRapidFire > 0) it.rapidFireMultiplier = 0.66f
        }
    }

    var spaceObjects: List<SpaceObject> by rememberSaveable { mutableStateOf(emptyList()) }
    val spaceObjectsController = remember {
        SpaceObjectsController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            initialSpaceObjects = spaceObjects,
            setSpaceObjects = { spaceObjects = it }
        )
    }

    // Task 01 (Slice 5) — DRONE_FLEET skill rank (đọc 1 lần/run): rank = số drone
    // tối đa; rank 0 = chưa mở khoá → DRONE_BOOSTER không rơi + drone không spawn.
    val droneRank = (
        runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_DRONE] ?: 0
        ).coerceIn(0, 2)

    var boosters: List<Booster> by rememberSaveable { mutableStateOf(emptyList()) }
    val boosterController = remember {
        BoosterController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            initialBoosters = boosters,
            updateBoosters = { boosters = it },
            // 25x NO_SHIELDS modifier: filter SHIELD_BOOSTER spawns.
            noShieldDrops = { effectiveStats.noShieldDrops },
            // Wave 21 (#3) — TAY KHÔNG modifier: bỏ hết buff rơi.
            noBoosters = { effectiveStats.noBoosters },
            // Round 75 (R75c) — wire REVIVE_DROP meta upgrade.
            reviveDropRank = {
                runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_REVIVE_DROP] ?: 0
            },
            // Task 01 (Slice 5) — chưa mở khoá DRONE_FLEET → không rơi DRONE_BOOSTER.
            droneUnlocked = { droneRank > 0 },
        )
    }

    // ── Task 01 — Drone companion (Slice 2: state + controller + orbit wiring) ──
    // Task 06 — biến thể drone chọn ở loadout (đọc 1 lần/run).
    val selectedDroneVariant = remember {
        kotlinx.coroutines.runBlocking { settingsRepo.selectedDroneVariant.first() }
    }
    // Task 26 — tay thuận điều khiển (đọc 1 lần/run) cho ONE_HAND_BOSS_KILL.
    val controlHandMode = remember {
        kotlinx.coroutines.runBlocking { settingsRepo.controlHandMode.first() }
    }
    var drones: List<Drone> by rememberSaveable { mutableStateOf(emptyList()) }
    val droneController = remember {
        DroneController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            initialDrones = drones,
            // Task 01 (Slice 5) — maxDrones theo rank DRONE_FLEET (0/1/2).
            maxDrones = droneRank,
            setDrones = { drones = it },
        )
    }
    // Task 01 (Slice 4) — nhặt DRONE_BOOSTER → spawn drone (thay TEMP spawn của
    // Slice 2). ShipController gọi onDroneBoosterPickedUp → droneSpawnRef.run().
    droneSpawnRef.run = {
        droneController.addDrone(ship.xOffset + ship.width / 2f, ship.yOffset + ship.height / 2f, selectedDroneVariant)
        // Task 07 — thành tựu nuôi 2 drone cùng lúc.
        if (AchievementUnlocks.droneDuo(droneController.count())) {
            coroutineScope.launch { unlockAchievement(Achievement.DRONE_DUO) }
        }
    }

    var mineralsEarnedTotal: Int by rememberSaveable { mutableIntStateOf(0) }
    var minerals: List<Mineral> by rememberSaveable { mutableStateOf(emptyList()) }
    // 46x — achievementUnlocked / achievementShownAtMillis / unlockAchievement
    // hoisted to line ~188 so onShipRevived can call them. Kept removed here.
    var lastMineralPickupMillis by remember { mutableLongStateOf(0L) }
    var lastEnemyKillMillis by remember { mutableLongStateOf(0L) }
    var comboCount by remember { mutableIntStateOf(0) }
    var comboTier by remember { mutableStateOf(com.tranphuloi.neon.ui.game.combo.ComboTier.NONE) }
    var comboPopupTier by remember { mutableStateOf(com.tranphuloi.neon.ui.game.combo.ComboTier.NONE) }
    var comboPopupShownMillis by remember { mutableLongStateOf(0L) }
    // Holder updated by StageController.onStageAdvance below; closure-captured by mineralsController.
    val magnetRadiusState = remember { androidx.compose.runtime.mutableFloatStateOf(80f) }

    val comboController = remember {
        Logger.d("rememberGameState: building ComboController")
        // Round 75 (R75a) — COMBO_KEEP meta upgrade: +500ms decay window/rank (max 3 = +1.5s).
        val comboKeepRank = runContext.metaUpgrades[com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_COMBO_KEEP] ?: 0
        // Wave 14a — Gói Giữ Combo: cửa sổ giữ combo ×2 cả run.
        // Task 12 — passive TƠ NHỆN cũng nhân đôi cửa sổ giữ combo (như buff giữ combo).
        val comboWindow = (2000L + comboKeepRank * 500L) * (if (startComboKeep > 0 || passiveComboKeep) 2L else 1L)
        com.tranphuloi.neon.ui.game.combo.ComboController(
            resetWindowMillis = comboWindow,
            onTierAdvance = { tier ->
                comboPopupTier = tier
                comboPopupShownMillis = System.currentTimeMillis()
                // Wave 11d Bug #3 fix — pick a random variant per combo tier
                // (announceVariants), with per-event 8s cooldown so the SAME
                // tier can't re-fire its phrase rapidly. Prior code spoke one
                // hard-coded phrase per tier — log evidence showed "Đánh đôi!"
                // 6 times in 2 minutes on Pixel 7 Pro.
                val (eventKey, phrases) = when (tier) {
                    com.tranphuloi.neon.ui.game.combo.ComboTier.DOUBLE -> "combo_double" to voiceComboDoubleList
                    com.tranphuloi.neon.ui.game.combo.ComboTier.TRIPLE -> "combo_triple" to voiceComboTripleList
                    com.tranphuloi.neon.ui.game.combo.ComboTier.RAMPAGE -> "combo_rampage" to voiceComboRampageList
                    com.tranphuloi.neon.ui.game.combo.ComboTier.UNSTOPPABLE -> "combo_unstoppable" to voiceComboUnstoppableList
                    com.tranphuloi.neon.ui.game.combo.ComboTier.GODLIKE -> "combo_godlike" to voiceComboGodlikeList
                    else -> null to emptyList()
                }
                if (eventKey != null && phrases.isNotEmpty()) {
                    voiceAnnouncer.announceVariants(
                        eventKey = eventKey,
                        phrases = phrases,
                        personality = com.tranphuloi.neon.ui.game.audio.VoicePersonality.HYPE,
                    )
                }
            }
        )
    }

    val mineralsController = remember {
        Logger.d("rememberGameState: building MineralsController")
        MineralsController(
            initialMinerals = minerals,
            updateMinerals = { minerals = it },
            updateMineralsEarnedTotal = { amount ->
                // 25x apply RunModifier scoreMul. Round to int — small minerals (1-2)
                // multiplied by 1.5× rounds to 2-3; large minerals (10) round to 15.
                // Round 60 (38x) — SCORE_X3 booster stacks multiplicatively on top
                // (×3 for 15s). Both active = ×~4.5 score gain.
                // Wave 14a — Gói x2 Khoáng / x2 Điểm: nhân thêm runYieldMult cả run.
                val scaled = (amount * effectiveStats.scoreMul * shipController.scoreMul() * runYieldMult)
                    .toInt().coerceAtLeast(amount)
                mineralsEarnedTotal += scaled
                lastMineralPickupMillis = System.currentTimeMillis()
                // 4c: spawn pickup popup at ship center.
                val tier = comboController.currentTier()
                pickupPopupController.spawnMineralPickup(
                    xOffset = ship.xOffset + ship.width / 2f - 20f,
                    yOffset = ship.yOffset - 20f,
                    comboCount = comboController.count,
                    multiplier = tier.multiplier,
                )
                // Pickup burst (ring shockwave + sparkles) at ship — minerals are
                // attracted to ship center via magnet, so the burst reads as
                // "absorbed by the ship".
                pickupBurstController.spawn(
                    ship.xOffset + ship.width / 2f,
                    ship.yOffset + ship.height / 2f,
                )
            },
            getShipCenter = { ship.xOffset + ship.width / 2 to ship.yOffset + ship.height / 2 },
            // 25x apply RunModifier magnetMul to magnet radius (e.g. SUPER_MAGNET ×2).
            // Round 60 (38x) — MAGNET_BOOST stacks ×2 multiplicatively (15s window).
            getMagnetRadius = {
                val base = magnetRadiusState.floatValue * effectiveStats.magnetMul * shipController.magnetBoostMul() * runMagnetMult
                // Wave 11a Phase 3 — GRAVITY_BOOSTER multiplies radius × 100 while
                // active → effectively pulls every on-screen mineral into ship.
                if (shipController.isGravityActive()) base * 100f else base
            },
        )
    }
    // Round 60 (38x) — wire MINERAL_SUPERCHARGE callback now that
    // mineralsController exists. Captured by reference into the holder so the
    // ShipController dispatch (defined above) fires the up-to-date lambda.
    mineralSuperchargeRef.run = { mineralsController.flushAllToShip() }

    var enemies: List<Enemy> by rememberSaveable { mutableStateOf(emptyList()) }
    // Round 75 (R75c) — wire SHIELD_BURST AoE damage now that enemies + explosions
    // are available. Explosions controller declared earlier; enemies set above.
    shieldBurstRef.run = { x, y, rank ->
        val radius = 120f
        val damage = 80f * rank
        explosionsController.addExplosion(xOffset = x - 40f, yOffset = y - 40f, width = 80f, height = 80f)
        enemies.forEach { enemy ->
            val ex = enemy.xOffset + enemy.width / 2
            val ey = enemy.yOffset + enemy.height / 2
            val dx = ex - x
            val dy = ey - y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist <= radius) {
                enemy.onObjectImpact(damage)
                Logger.v { "SHIELD_BURST: enemy ${enemy.enemyId.take(6)} hit for $damage at dist=${dist.toInt()}" }
            }
        }
    }
    // Wave 11a Phase 3 — REFLECT retaliation: find nearest enemy + 30 damage.
    // Audit follow-up: also spawn impact spark + extra burst at ABSORB point
    // (ship side) so player visually sees the laser being eaten, then the
    // retaliation at the enemy. Two-stage VFX = "absorb → bounce" feel.
    reflectRetaliateRef.run = { absorbX, absorbY ->
        // Stage 1: VFX at ship absorb point (the laser dies here)
        impactSparkController.spawnBurst(absorbX, absorbY)
        val nearestEnemy = enemies.minByOrNull { enemy ->
            val ex = enemy.xOffset + enemy.width / 2
            val ey = enemy.yOffset + enemy.height / 2
            val dx = ex - absorbX
            val dy = ey - absorbY
            dx * dx + dy * dy
        }
        if (nearestEnemy != null) {
            // Audit follow-up: REFLECT damage now scales × rarity. Common/Rare/
            // Epic = 30/45/60 damage via ShipController.reflectRetaliationDamage().
            val damage = shipController.reflectRetaliationDamage()
            nearestEnemy.onObjectImpact(damage.toFloat())
            val nx = nearestEnemy.xOffset + nearestEnemy.width / 2
            val ny = nearestEnemy.yOffset + nearestEnemy.height / 2
            damageNumberController.report(nearestEnemy.enemyId, damage, nx, ny, nearestEnemy.isBoss)
            impactSparkController.spawnBurst(nx, ny)
            // Audit follow-up round 2: real connected bounce arc visual
            // (TrailLineOverlay drawLine 300ms fade) replacing prior 3-point
            // impactSpark band-aid. Player now sees actual line travel.
            trailLineController.addBounceArc(absorbX, absorbY, nx, ny)
            Logger.v { "REFLECT: retaliate enemy=${nearestEnemy.enemyId.take(6)} dmg=$damage absorbAt=($absorbX,$absorbY)" }
        }
    }
    // Wave 11a Phase 3 — CHAIN_LIGHTNING chain to 2 more nearest enemies at 50%
    // damage. Audit follow-up: spawn impact sparks along the chain path so
    // player sees the lightning travel (3 sparks per hop = "bolt segment").
    chainLightningRef.run = { primaryTargetId, primaryDamage, hitX, hitY ->
        val chainDmg = (primaryDamage * 0.5f).toInt().coerceAtLeast(1)
        val candidates = enemies
            .filter { it.enemyId != primaryTargetId && it.hp > 0f }
            .sortedBy {
                val ex = it.xOffset + it.width / 2
                val ey = it.yOffset + it.height / 2
                val dx = ex - hitX
                val dy = ey - hitY
                dx * dx + dy * dy
            }
            .take(2)
        var prevX = hitX
        var prevY = hitY
        for (chainTarget in candidates) {
            chainTarget.onObjectImpact(chainDmg.toFloat())
            val cx2 = chainTarget.xOffset + chainTarget.width / 2
            val cy2 = chainTarget.yOffset + chainTarget.height / 2
            damageNumberController.report(chainTarget.enemyId, chainDmg, cx2, cy2, chainTarget.isBoss)
            impactSparkController.spawnBurst(cx2, cy2)
            // Audit follow-up round 2: real jagged lightning bolt visual
            // (TrailLineOverlay drawPath with deterministic zigzag offsets)
            // replacing prior 3-spark trail band-aid.
            trailLineController.addChainBolt(prevX, prevY, cx2, cy2)
            prevX = cx2
            prevY = cy2
        }
    }
    // Task 02 — đạn LIGHTNING: sét lan TUẦN TỰ từ điểm chạm, tối đa 3 hop trong
    // 120px, dmg ×0.7 mỗi bước (LightningChain thuần). Trail zigzag + spark +
    // damage number mỗi hop (tái dùng như booster). excludeIds = địch trúng chính.
    lightningChainRef.run = { primaryTargetId, primaryDamage, hitX, hitY ->
        val hops = com.tranphuloi.neon.ui.game.laser.LightningChain.computeChainTargets(
            startX = hitX,
            startY = hitY,
            enemies = enemies,
            excludeIds = setOf(primaryTargetId),
        )
        // Task 07 — thành tựu 1 phát sét lan trúng đủ 3 địch.
        if (AchievementUnlocks.chainTriple(hops.size)) {
            coroutineScope.launch { unlockAchievement(Achievement.CHAIN_TRIPLE) }
        }
        var prevX = hitX
        var prevY = hitY
        hops.forEachIndexed { index, chainTarget ->
            val hopDmg = com.tranphuloi.neon.ui.game.laser.LightningChain.damageForHop(primaryDamage, index)
            chainTarget.onObjectImpact(hopDmg.toFloat())
            val cx2 = chainTarget.xOffset + chainTarget.width / 2
            val cy2 = chainTarget.yOffset + chainTarget.height / 2
            damageNumberController.report(chainTarget.enemyId, hopDmg, cx2, cy2, chainTarget.isBoss)
            impactSparkController.spawnBurst(cx2, cy2)
            trailLineController.addChainBolt(prevX, prevY, cx2, cy2)
            prevX = cx2
            prevY = cy2
        }
    }
    // Wave 16 Slice 4b — BRICK knockback: đẩy địch trúng đòn lùi lên (ra xa tàu).
    knockbackRef.run = { targetId ->
        enemies.firstOrNull { it.enemyId == targetId }?.let { e ->
            e.yOffset = (e.yOffset - BRICK_KNOCKBACK_PX).coerceAtLeast(-e.height)
        }
    }
    val enemyController = remember {
        Logger.d("rememberGameState: building EnemyController")
        EnemyController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            getShip = { ship },
            initialEnemies = enemies,
            setEnemies = { enemies = it },
            // Wave 16 — SLOW status now actually slows enemy movement (DURIAN bullet).
            isSlowed = { enemyId -> statusEffectController.isSlowed(enemyId) },
            // Task 23 — RunModifier.BOSSES_ONLY ("Chỉ boss"): bỏ qua spawn enemy thường.
            bossesOnly = { effectiveStats.bossesOnly },
            addMinerals = { xOffset: Float, yOffset: Float, width: Float, mineralAmount: Int ->
                mineralsController.addMinerals(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    width = width,
                    mineralAmount = mineralAmount
                )
            },
            addExplosion = { xOffset: Float, yOffset: Float, width: Float, height: Float ->
                explosionsController.addExplosion(
                    xOffset = xOffset,
                    yOffset = yOffset,
                    width = width,
                    height = height
                )
            },
            onEnemyKilled = { enemy ->
                lastEnemyKillMillis = System.currentTimeMillis()
                // Task 12 — passive LIFESTEAL (Hút linh hồn / Lưỡi hái): diệt địch hồi máu.
                if (passiveLifestealHp > 0) shipController.healCapped(passiveLifestealHp, maxHp = initialShipHp)
                comboController.onEnemyKilled()
                comboCount = comboController.count
                comboTier = comboController.currentTier()
                if (comboCount > maxComboReached) maxComboReached = comboCount
                // Wave 11c — attribute kill to the bullet that landed the
                // lethal hit (recorded by onLaserHit into lastBulletTypeByEnemyId).
                // Fallback to ship.activeBulletType for kills that bypass
                // onLaserHit entirely — REFLECT_BOOSTER retaliation, CHAIN_
                // LIGHTNING chain segments, SmartBomb / BURST sweep impacts,
                // secondary weapons (MISSILE/MINE/BURST), and ultimate lasers
                // all call enemy.onObjectImpact() directly. Those count under
                // the currently active bullet type, which matches player intent
                // (the buff was procced by their loadout). Known telemetry
                // imprecision documented for the Statistics screen.
                val killBullet = lastBulletTypeByEnemyId[enemy.enemyId]
                    ?: ship.activeBulletType
                bulletKillsThisRun.merge(killBullet, 1, Int::plus)
                lastBulletTypeByEnemyId.remove(enemy.enemyId)
                if (enemy.isBoss) {
                    bossesDefeatedTotal++
                    // Wave 11b — boss kill breakdown per kind (atomic CHM merge).
                    enemy.bossKind?.let { kind ->
                        bossKillsThisRun.merge(kind, 1, Int::plus)
                    }
                    smartBombs++       // reward: +1 smart bomb per boss kill
                    // Round 62 — TTS callout. Skip for FinalBoss because the
                    // achievement unlock + victory ending will speak afterward
                    // and we don't want a 3-way overlap of utterances.
                    if (enemy !is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss) {
                        // Wave 11d Bug #3 fix — variant picker; 3 phrases per
                        // boss-down event keep the callouts feeling fresh.
                        voiceAnnouncer.announceVariants(
                            eventKey = "boss_down",
                            phrases = voiceBossDownList,
                            personality = com.tranphuloi.neon.ui.game.audio.VoicePersonality.DRAMATIC,
                        )
                    }
                    // Round 34 (42x) — trigger roguelike buff picker after every
                    // boss kill (except FinalBoss → that's victory branch).
                    if (enemy !is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss) {
                        bossKillBuffOfferMillis = System.currentTimeMillis()
                        Logger.d("Boss killed → trigger BuffPicker @ $bossKillBuffOfferMillis")
                    }
                    // 34d: detect FinalBoss kill → triggers victory ending.
                    // Force GAME_OVER 4s later so the GameOver dialog (with VictoryPanel)
                    // shows automatically. Without this, stage script ends after VICTORY!
                    // message but gameStatus stays RUNNING — player has to suicide to see
                    // victory dialog. 4s = enough for "VICTORY!" StageMessage (3s) + a
                    // brief breath, then auto-navigate. Skips kill-cam (ship still alive
                    // → killCamStartedAtMillis = 0L → no slow-mo replay).
                    if (enemy is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss) {
                        finalBossDefeated = true
                        Logger.w("FinalBoss DEFEATED — victory achieved, auto GAME_OVER in 4s")
                        coroutineScope.launch {
                            delay(4000L)
                            setGameStatus(GameStatus.GAME_OVER)
                            Logger.w("Campaign complete → GAME_OVER fired (victory dialog will show)")
                        }
                    }
                } else enemiesKilledTotal++
                if (enemy.isBoss) {
                    hitStopController.freezeForBossKill()
                    bossKillEventMillis = System.currentTimeMillis()        // 30c camera zoom
                    // 24b Boss kill rank — compute from time-to-kill + hp ratio kept.
                    if (bossSpawnedAtMillis > 0L && playerHpAtBossSpawn > 0) {
                        val ttk = System.currentTimeMillis() - bossSpawnedAtMillis
                        val hpRatio = ship.hp.toFloat() / playerHpAtBossSpawn.toFloat()
                        bossKillRank = com.tranphuloi.neon.ui.game.controls.BossRank.compute(ttk, hpRatio)
                        bossKillRankShownMillis = System.currentTimeMillis()
                        // Wave 11b — record rank achieved for distribution stats.
                        bossKillRank?.let { ranksAchievedThisRun.add(it) }
                        Logger.d("Boss kill rank: ${bossKillRank?.letter} (ttk=${ttk}ms hpRatio=${"%.2f".format(hpRatio)})")
                    }
                    // Task 04 — thoại "trăn trối" khi hạ boss (song ngữ qua resource).
                    // Delay nhẹ để không đè freeze/nổ; StoryOverlay hiển thị ~2.8s.
                    val defeatRes = com.tranphuloi.neon.ui.game.story.StoryRegistry
                        .bossDefeatRes(enemy.bossKind)
                    val defeatLine = com.tranphuloi.neon.ui.game.story.StoryLine(
                        speaker = enemy.bossKind?.displayName ?: "Boss",
                        text = context.getString(defeatRes),
                        durationMs = 2800,
                    )
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500L)
                        setStoryLineRef.run(defeatLine)
                    }
                } else {
                    hitStopController.freezeForEnemyKill()
                }
                // Achievement triggers — 9b + 46x expansion
                coroutineScope.launch {
                    unlockAchievement(Achievement.FIRST_BLOOD)
                    if (enemy.isBoss) unlockAchievement(Achievement.FIRST_BOSS)
                    if (comboCount >= 5) unlockAchievement(Achievement.COMBO_5)
                    if (comboCount >= 10) unlockAchievement(Achievement.COMBO_10)
                    // 46x kill milestones
                    if (enemiesKilledTotal >= 50) unlockAchievement(Achievement.KILL_50)
                    if (enemiesKilledTotal >= 200) unlockAchievement(Achievement.KILL_200)
                    if (enemiesKilledTotal >= 500) unlockAchievement(Achievement.KILL_500)
                    if (comboCount >= 20) unlockAchievement(Achievement.COMBO_20)
                    // 46x boss milestones
                    if (enemy.isBoss && bossesDefeatedTotal >= 3) unlockAchievement(Achievement.BOSS_3)
                    if (enemy.isBoss && bossesDefeatedTotal >= 5) unlockAchievement(Achievement.BOSS_5)
                    // Task 26 — hạ boss khi đang chơi chế độ 1 tay.
                    if (enemy.isBoss && AchievementUnlocks.oneHandBossKill(controlHandMode)) {
                        unlockAchievement(Achievement.ONE_HAND_BOSS_KILL)
                    }
                    // 46x FinalBoss kill
                    if (enemy is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss) {
                        unlockAchievement(Achievement.FINAL_BOSS_KILL)
                        if (difficultyState.value == com.tranphuloi.neon.data.Difficulty.HARD) {
                            unlockAchievement(Achievement.HARD_VICTORY)
                        }
                    }
                    // 46x BOSS_RUSH_CLEAR — moved to monitorLoopInSec where stageController is in scope.
                    // 46x rank-S boss
                    if (enemy.isBoss && bossKillRank == com.tranphuloi.neon.ui.game.controls.BossRank.S) {
                        unlockAchievement(Achievement.BOSS_RUSH_S)
                    }
                }
                Logger.v { "Enemy killed (id=${enemy.enemyId.take(6)}…) → combo=$comboCount tier=$comboTier boss=${enemy.isBoss}" }
            }
        )
    }

    var enemyLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    val enemyLaserController = remember {
        EnemyLasersController(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            initialEnemyLasers = enemyLasers,
            setEnemyLasers = { enemyLasers = it },
            // Round 34 (41x) — STUN: skip fire if enemy is stunned this tick.
            isEnemyStunned = { enemyId -> statusEffectController.isStunned(enemyId) },
            // Wave 17 — center tàu (px) cho đạn HOMING bám theo.
            shipPosition = { (ship.xOffset + ship.width / 2f) to (ship.yOffset + ship.height / 2f) },
        )
    }

    var gameMessage by rememberSaveable { mutableStateOf("") }
    var lastStageAdvanceMillis by remember { mutableLongStateOf(0L) }
    var waveClearBannerShownMillis by remember { mutableLongStateOf(0L) }
    var bossIntroShownAtMillis by remember { mutableLongStateOf(0L) }
    var bossIntroName by remember { mutableStateOf("") }
    // Wave 16 — taunt line shown inside the full-screen cinematic intro.
    var bossIntroTaunt by remember { mutableStateOf("") }
    // Round 74 (R73e) — boss kind cho per-boss audio cue (pitch shift).
    var bossIntroBossKind by remember {
        mutableStateOf<com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind?>(null)
    }
    // 47x Wave 5 — story dialogue state. storyLine is what StoryOverlay shows;
    // storyShownMillis drives the slide-in / fade-out. chapterIntroShown gates
    // per-chapter intro firing so we only narrate first entry to each chapter.
    var storyLine by remember { mutableStateOf<com.tranphuloi.neon.ui.game.story.StoryLine?>(null) }
    var storyShownMillis by remember { mutableLongStateOf(0L) }
    val storyLines = remember { mutableListOf<com.tranphuloi.neon.ui.game.story.StoryLine>() }
    // Task 04 — theo dõi phase FinalBoss đã narrate (1..3) để chỉ fire thoại 1 lần
    // mỗi lần lên phase (currentPhase tăng theo HP giảm).
    var finalBossPhaseSeen by remember { mutableIntStateOf(1) }
    // Task 04 — gate hot-path: chỉ quét FinalBoss trong loop KHI đang ở stage boss
    // (set bởi onStageAdvance). Tránh firstOrNull mỗi tick suốt cả run.
    var finalBossActive by remember { mutableStateOf(false) }
    // Task 04 — nối deferred ref (onEnemyKilled dùng để set thoại defeat).
    setStoryLineRef.run = { line ->
        storyLine = line
        storyShownMillis = System.currentTimeMillis()
    }

    // Round 25 — read checkpoint stage index from DataStore for this mode.
    // runBlocking ok here: same justification as runMode (one-time DataStore read
    // at composition entry; <10ms; only fires once per GameScreen entry).
    val initialStageIndex = remember(runMode) {
        val idx = kotlinx.coroutines.runBlocking {
            runPersistenceRepo.checkpointFor(runMode.key).first()
        }
        // Sanity coerce — if checkpoint is out-of-range for current provider's static
        // script, treat as 0 (fresh start).
        val safe = if (stageProvider.hasAt(idx)) idx else 0
        Logger.d("rememberGameState: checkpoint for ${runMode.key} = $idx (safe=$safe)")
        safe
    }
    // Round 26 — if resuming mid-game (initialStageIndex > 0), pre-mark the chapter
    // intro as "already played" so onStageAdvance doesn't re-fire it when player
    // crosses a stage boundary in the same chapter. Without this, every resume
    // would replay the chapter intro narrative.
    var chapterIntroPlayedChapter by rememberSaveable(runMode) {
        val initialChapter = if (initialStageIndex > 0) stageProvider.chapterAt(initialStageIndex).coerceAtLeast(0) else 0
        Logger.d("rememberGameState: chapterIntroPlayedChapter init = $initialChapter (resumed at chapter)")
        mutableIntStateOf(initialChapter)
    }
    // Task 04 — gate riêng cho narrative beat (fire 1 lần/chương ở boss climax).
    var beatPlayedChapter by rememberSaveable(runMode) { mutableIntStateOf(0) }

    val stageController = rememberSaveable(runMode, saver = StageController.saver(stageProvider)) {
        StageController(
            provider = stageProvider,
            stageIndex = initialStageIndex,
            onStageAdvance = { idx, newStage ->
                magnetRadiusState.floatValue = (80f + (idx / 5) * 10f).coerceAtMost(200f)
                lastStageAdvanceMillis = System.currentTimeMillis()
                // Task 04 — cờ gate cho FinalBoss phase-check (chỉ true ở stage boss).
                finalBossActive = newStage is com.tranphuloi.neon.ui.game.stage.StageBoss
                // Round 34 (44x) — refresh currentHazard state from new stage.
                // ShipController reads this reactively for ice slip mechanic.
                currentHazard = if (newStage is com.tranphuloi.neon.ui.game.stage.StageGame) {
                    newStage.hazard
                } else null
                Logger.d("Stage advance: currentHazard updated to $currentHazard")
                // 12c: Wave clear bonus — fires when player completes a wave cluster.
                // Definition tightened in round 20: prev = StageGame AND new ≠ StageGame.
                // Without this gate, the new procedural script (12 short StageGame entries
                // per chapter, 5-9s each) would fire wave clear ~60 times per playthrough.
                // Now fires only at significant transitions (game → boss prelude / mid-boss
                // DANGER / chapter outro) — ~10 times per full campaign.
                //
                // Wave 5: read previous stage via provider (not static `stages`) so
                // non-campaign modes work too.
                val previousIdx = idx - 1
                val previousWasGame = previousIdx >= 0 &&
                    stageProvider.hasAt(previousIdx) &&
                    stageProvider.getAt(previousIdx) is com.tranphuloi.neon.ui.game.stage.StageGame
                val newIsGame =
                    newStage is com.tranphuloi.neon.ui.game.stage.StageGame
                if (previousWasGame && !newIsGame) {
                    waveClearBannerShownMillis = System.currentTimeMillis()
                    val cx = screenWidth / 2f
                    val cy = screenHeight / 3f
                    repeat(5) { i ->
                        mineralsController.addMinerals(
                            xOffset = cx - 40f + i * 20f,
                            yOffset = cy,
                            width = 25f,
                            mineralAmount = 1,
                        )
                    }
                    Logger.d("Wave clear bonus: 5 minerals burst + banner")
                }
                // 21c: Boss intro cinematic — fire when entering a StageBoss.
                if (newStage is com.tranphuloi.neon.ui.game.stage.StageBoss) {
                    // Task 04 — narrative beat NARRATOR 1 lần/chương ngay khi vào
                    // boss climax (trước taunt ~2600ms → beat 300..2500ms rồi taunt).
                    if (newStage.chapterId in 1..5 && newStage.chapterId != beatPlayedChapter) {
                        beatPlayedChapter = newStage.chapterId
                        com.tranphuloi.neon.ui.game.story.StoryRegistry
                            .chapterBeatRes(newStage.chapterId)?.let { resId ->
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(300L)
                                    storyLine = com.tranphuloi.neon.ui.game.story.StoryLine(
                                        speaker = "Đội trưởng",
                                        text = context.getString(resId),
                                        durationMs = 2200,
                                    )
                                    storyShownMillis = System.currentTimeMillis()
                                }
                            }
                    }
                    // Round 85 audit — resolve actual BossKind for (type, chapter)
                    // → use bossKind.displayName as banner. Khớp với in-game visual
                    // + InfoScreen + story speaker. Eliminate ALL dev-jargon banner
                    // ("BOSS CẤP 1" / "TIỂU BOSS TẤN CÔNG"). Single source of truth.
                    val resolvedKind =
                        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKindResolver.resolve(
                            newStage.enemyType,
                            newStage.chapterId,
                        )
                    bossIntroName = resolvedKind?.displayName ?: "Boss"
                    bossIntroBossKind = resolvedKind
                    // 47x Story — boss taunt. Shown inside the cinematic now;
                    // also queued to StoryOverlay AFTER the cinematic as a linger.
                    val taunt = com.tranphuloi.neon.ui.game.story.StoryRegistry
                        .bossTaunt(newStage.enemyType, newStage.chapterId)
                    bossIntroTaunt = taunt?.text ?: ""
                    bossIntroShownAtMillis = System.currentTimeMillis()
                    // Snapshot for rank computation — boss spawn time + player hp.
                    bossSpawnedAtMillis = bossIntroShownAtMillis
                    playerHpAtBossSpawn = ship.hp
                    // Wave 16 — full-screen cinematic: freeze the whole sim via the
                    // existing hit-stop loop gate so the boss fight starts only
                    // after the intro (player tap can skip → endBossIntroFreeze).
                    hitStopController.freezeForBossIntro(HitStopController.BOSS_INTRO_FREEZE_MS)
                    // Wave 17q — san phẳng tilt spawn NGAY khi vào cinematic: freeze
                    // chặn moveShip → reset post-spawn không chạy → ship kẹt nghiêng
                    // suốt 2.4s intro. Gọi trực tiếp để ship thẳng trong cinematic.
                    shipController.settleSpawnRotation()
                    Logger.d("Boss intro: $bossIntroName cinematic triggered (hpSnapshot=${ship.hp})")
                    if (taunt != null) {
                        coroutineScope.launch {
                            // After the cinematic (was 1600L) so it doesn't compete.
                            delay(2600L)
                            storyLine = taunt
                            storyShownMillis = System.currentTimeMillis()
                        }
                    }
                }
                // 47x Story — chapter intro narration. Fire only on FIRST entry
                // to a new chapter id (gated by chapterIntroPlayedChapter).
                val chapterId = when (newStage) {
                    is com.tranphuloi.neon.ui.game.stage.StageGame -> newStage.chapterId
                    is com.tranphuloi.neon.ui.game.stage.StageBoss -> newStage.chapterId
                    is com.tranphuloi.neon.ui.game.stage.StageMessage -> newStage.chapterId
                    else -> 0
                }
                // Round 79 (#1) — propagate chapter ID to EnemyFactory so the
                // next boss spawn picks the right bossKindOverride. Idempotent
                // (Factory just stores int).
                if (chapterId in 1..5) {
                    enemyController.setCurrentChapterId(chapterId)
                }
                if (chapterId in 1..5 && chapterId != chapterIntroPlayedChapter) {
                    chapterIntroPlayedChapter = chapterId
                    val lines = com.tranphuloi.neon.ui.game.story.StoryRegistry
                        .chapterIntro(chapterId)
                    if (lines.isNotEmpty()) {
                        coroutineScope.launch {
                            for ((i, l) in lines.withIndex()) {
                                if (i > 0) delay(l.durationMs.toLong())
                                storyLine = l
                                storyShownMillis = System.currentTimeMillis()
                                delay(50L)                              // ensure new shownMillis applies
                            }
                        }
                    }
                }
                // 43x BOSS_RUSH — heal ship to full HP when entering a StageMessage
                // between bosses ("Next!" gap). Gives the player a clean slate per boss
                // instead of cumulative damage carry-over (which would make rush unwinnable).
                if (runMode == com.tranphuloi.neon.ui.game.mode.GameMode.BOSS_RUSH &&
                    newStage is com.tranphuloi.neon.ui.game.stage.StageMessage &&
                    newStage.message == com.tranphuloi.neon.ui.game.stage.BossRushProvider.BOSS_RUSH_GAP_MESSAGE
                ) {
                    val before = ship.hp
                    shipController.setHp(initialShipHp)   // Wave 17r — qua controller (tránh clobber); polish — max hp thật (không hardcode 1000)
                    Logger.d("BOSS_RUSH: heal ship between bosses (hp $before → $initialShipHp)")
                }
                // Round 25 — persist checkpoint so cold-launch can resume here.
                // Coroutine launch so DataStore write doesn't block the game loop.
                coroutineScope.launch {
                    runPersistenceRepo.saveCheckpoint(runMode.key, idx)
                }
                val chapterInfo = when (newStage) {
                    is com.tranphuloi.neon.ui.game.stage.StageGame -> "chapter=${newStage.chapterId}"
                    is com.tranphuloi.neon.ui.game.stage.StageBoss -> "chapter=${newStage.chapterId}"
                    is com.tranphuloi.neon.ui.game.stage.StageMessage -> "chapter=${newStage.chapterId} message=\"${newStage.message}\""
                    else -> ""
                }
                Logger.d("StageController.onStageAdvance idx=$idx type=${newStage::class.simpleName} $chapterInfo → magnet=${magnetRadiusState.floatValue}px (checkpoint saved async)")
            }
        )
    }
    var gameStage by rememberSaveable { mutableStateOf(stageController.getGameStage(true)) }
    fun updateGameStage() {
        gameStage = stageController.getGameStage(
            readyForNextStage = !enemyController.hasEnemies() && !spaceObjectsController.hasSpaceObjects()
        )
        gameMessage = when (val stage = gameStage) {
            is StageMessage -> stage.message
            else -> ""
        }
    }

    var gameTimeSec by rememberSaveable { mutableLongStateOf(0L) }
    fun updateGameTime() {
        gameTimeSec += 1
    }

    var gameTimeIndicator by rememberSaveable { mutableStateOf("00:00") }
    fun updateGameTimeIndicator() {
        val second = String.format(Locale.US, "%02d", gameTimeSec % 60)
        val minute = String.format(Locale.US, "%02d", gameTimeSec / (60) % 60)
        gameTimeIndicator = "$minute:$second"
    }

    val monitorLoopInSecId = rememberSaveable { UUID.randomUUID().toString() }
    val monitorLoopRepeatTime = remember { Millis(1000) }
    // Round 34 (41x) — status effect tick id + cadence (250ms).
    val statusEffectTickId = rememberSaveable { UUID.randomUUID().toString() }
    val statusEffectRepeatTime = remember { Millis(250) }
    // Round 75 (R75b) — REGEN SkillNode tick @ 1s.
    val regenTickId = rememberSaveable { UUID.randomUUID().toString() }
    fun monitorLoopInSec() {
        updateGameTime()
        updateGameTimeIndicator()
        updateGameStage()
        // 43x TIME_ATTACK — force GAME_OVER when the time limit expires.
        // Ship remains alive (no kill-cam), feedback is celebratory if score > 0
        // and matches the standard victory branch.
        if (timeAttackLimitSec > 0 && gameTimeSec >= timeAttackLimitSec &&
            gameStatus == GameStatus.RUNNING
        ) {
            Logger.d("TIME_ATTACK: time limit ${timeAttackLimitSec}s reached → GAME_OVER (victory path)")
            timeAttackEnded = true
            setGameStatus(GameStatus.GAME_OVER)
        }
        // 46x time / stage milestone achievements — checked once per second.
        coroutineScope.launch {
            if (gameTimeSec >= 300) unlockAchievement(Achievement.SURVIVE_5MIN)
            if (gameTimeSec >= 600) unlockAchievement(Achievement.SURVIVE_10MIN)
            val idx = stageController.currentIndex()
            if (idx >= 30) unlockAchievement(Achievement.STAGE_30)
            if (idx >= 60) unlockAchievement(Achievement.STAGE_60)
            if (idx >= 100) unlockAchievement(Achievement.STAGE_100)
            // 46x endless tier milestones
            if (runMode == com.tranphuloi.neon.ui.game.mode.GameMode.ENDLESS) {
                if (gameTimeSec >= 60) unlockAchievement(Achievement.ENDLESS_60S)
                if (gameTimeSec >= 180) unlockAchievement(Achievement.ENDLESS_180S)
                if (gameTimeSec >= 300) unlockAchievement(Achievement.ENDLESS_300S)
            }
            // 46x time-attack high score
            if (runMode == com.tranphuloi.neon.ui.game.mode.GameMode.TIME_ATTACK &&
                enemiesKilledTotal >= 50
            ) {
                unlockAchievement(Achievement.TIME_ATTACK_HIGH)
            }
            // 46x modifier run — fire as soon as game time > 30s with non-NONE modifier
            if (gameTimeSec >= 30 &&
                runModifier != com.tranphuloi.neon.ui.game.modifier.RunModifier.NONE
            ) {
                unlockAchievement(Achievement.MODIFIER_RUN)
            }
            // 46x BOSS_RUSH_CLEAR — fires only at end-of-script. Use provider's
            // scriptSize() so the gate scales correctly if the script length
            // changes (campaign content updates, mid-boss additions, etc.).
            // BossRushProvider script ends with "ALL CLEAR!" StageMessage.
            if (runMode == com.tranphuloi.neon.ui.game.mode.GameMode.BOSS_RUSH) {
                val sz = stageController.scriptSize()
                if (sz > 0 && stageController.currentIndex() >= sz - 1) {
                    unlockAchievement(Achievement.BOSS_RUSH_CLEAR)
                }
            }
        }
    }

    val lifecycle by LocalLifecycleOwner.current.lifecycle.observeAsState()
    LaunchedEffect(lifecycle) {
        Logger.d("Lifecycle event: $lifecycle")
        // Never override GAME_OVER from a lifecycle tick; only RUNNING ↔ PAUSE.
        if (gameStatus == GameStatus.GAME_OVER) return@LaunchedEffect
        if (lifecycle == Lifecycle.Event.ON_PAUSE || lifecycle == Lifecycle.Event.ON_STOP) {
            setGameStatus(GameStatus.PAUSE)
        } else if (lifecycle == Lifecycle.Event.ON_RESUME || lifecycle == Lifecycle.Event.ON_START) {
            setGameStatus(GameStatus.RUNNING)
        }
    }

    // Round 78 (#4 edge drag fix) — graphicsLayer.scale shrinks the visible
    // world to inner X% of the screen leaving margins. To let the ship reach
    // the actual screen edges, extend ShipController drag bounds by the
    // inverse-zoom margin. graphicsLayer.clip = false (Compose default) so
    // entities at negative game coords render correctly into the screen margin.
    LaunchedEffect(liveCameraZoom) {
        val scale = liveCameraZoom.pixelScale.coerceAtLeast(0.01f)
        val extensionX = screenWidth * (1f / scale - 1f) / 2f
        val extensionY = screenHeight * (1f / scale - 1f) / 2f
        shipController.dragBoundsExtensionX = extensionX
        shipController.dragBoundsExtensionY = extensionY
        // Round 78 (#4 spec fix follow-up) — also extend enemy spawn X bounds
        // so enemies appear at visual screen edges (not only inner 70% area).
        enemyController.setSpawnXMargin(extensionX)
        // Wave 11d Bug #1 fix — extend UltimateLaser sweep bounds to match the
        // extended enemy spawn band. Without this, enemies that spawned into
        // the right-edge FAR-zoom margin survived ChargeShot/Ultimate sweeps.
        lasersController.setExtraXSpan(extensionX)
        // Pixel-2 feedback #6 fix — extend enemy outOfScreen Y threshold so
        // enemies stay visible until they reach the actual device-bottom edge
        // (not just game-coord screenHeight). Mirrors X-axis fix above.
        enemyController.setExtraYSpan(extensionY)
        // Pixel-3 #2 deep-audit fix — UltimateLaser start position AND
        // EnemyLaser destruction threshold both need Y extension. Pre-fix
        // beam sweep + enemy-laser drop both stopped at raw screenHeight,
        // leaving the bottom 134dp band (at FAR zoom) without any effect.
        lasersController.setExtraYSpan(extensionY)
        enemyLaserController.setExtraYSpan(extensionY)
        Logger.d("Camera zoom=${liveCameraZoom.key} scale=$scale → drag/spawn extension X=$extensionX Y=$extensionY")
    }

    var refreshHandler by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) {
        Logger.d("Game loop DisposableEffect setup, screen=${screenWidth}x${screenHeight}")
        var loopRunning = true
        val job = coroutineScope.launch {
            backgroundController.init()
            Logger.d("Background initialized; entering tick loop on IO dispatcher")
            // Run heavy per-tick work (collision, entity processing) on IO so it doesn't compete
            // with Compose recomposition on Main. Pace with delay(8) (~125Hz) which yields the
            // dispatcher between iterations and lets cancel() propagate immediately.
            launch(IO) {
                var frameCount = 0L
                while (loopRunning) {
                    // 11c kill-cam: slow tick by 50% during 1.5s after ship death.
                    val killCamElapsed = System.currentTimeMillis() - killCamStartedAtMillis
                    val inKillCam = killCamStartedAtMillis > 0L && killCamElapsed < 1500L
                    // 6c boss slow-mo critical: 60% effective speed during 2s when boss
                    // hp dropped below 20% of initial. Skip 2 of every 5 ticks.
                    val slowMoElapsed = System.currentTimeMillis() - bossSlowMotionStartedAtMillis
                    val inBossSlowMo = bossSlowMotionStartedAtMillis > 0L && slowMoElapsed < 2000L
                    val statusActiveForLoop = gameStatus == GameStatus.RUNNING ||
                        (gameStatus == GameStatus.GAME_OVER && inKillCam)
                    if (statusActiveForLoop && !hitStopController.isFrozen()) {
                        if (inKillCam) {
                            // Skip every other tick to halve effective speed.
                            if (frameCount % 2L != 0L) {
                                frameCount++
                                yield()
                                continue
                            }
                        } else if (inBossSlowMo) {
                            // 60% speed: skip 2 out of every 5 ticks.
                            val mod = frameCount % 5L
                            if (mod == 1L || mod == 3L) {
                                frameCount++
                                yield()
                                continue
                            }
                        }
                        frameCount++
                        tinker(
                            id = backgroundController.tickId,
                            repeatTime = backgroundController.tickRepeatTime,
                            doWork = { backgroundController.tick() }
                        )
                        // Skip ship-related controllers once destroyed — they would
                        // call `setShip(ship.copy(...))` with stale internal state and
                        // overwrite `shipSpriteHidden=true` back to false within a frame,
                        // preventing the destroy-hide from sticking.
                        if (ship.destroyedAtMillis == 0L) {
                            tinker(
                                id = shipController.moveShipId,
                                repeatTime = shipController.moveShipRepeatTime,
                                doWork = { shipController.moveShip() }
                            )
                            tinker(
                                id = shipController.monitorShipCollisionsId,
                                repeatTime = shipController.monitorShipCollisionsRepeatTime,
                                doWork = {
                                    shipController.monitorShipCollisions(
                                        spaceObjects = spaceObjectsController.spaceObjects,
                                        boosters = boosterController.boosters,
                                        enemies = enemies,
                                        enemyLasers = enemyLaserController.enemyLasers
                                    ) {
                                        lasersController.fireUltimateLaser()
                                        ultimateFlashMillis = System.currentTimeMillis()
                                    }
                                }
                            )
                            // Round 75 (R75b) — REGEN SkillNode tick @ 1s.
                            // +5HP/rank nếu 3s không bị đánh, không stack với HEALING_AURA.
                            tinker(
                                id = regenTickId,
                                repeatTime = com.tranphuloi.neon.ui.game.common.Millis(1000),
                                doWork = {
                                    val regenRank = runContext.metaUpgrades[
                                        com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_REGEN
                                    ] ?: 0
                                    shipController.regenTick(
                                        rank = regenRank,
                                        initialHp = initialShipHp,
                                    )
                                    // Task 12 — passive REGEN (Hào quang hồi): +N hp/giây (không gate).
                                    if (passiveRegenHp > 0) {
                                        shipController.healCapped(passiveRegenHp, maxHp = initialShipHp)
                                    }
                                },
                            )
                        }
                        tinker(
                            id = lasersController.monitorLaserCollisionId,
                            repeatTime = lasersController.monitorLaserCollisionRepeatTime,
                            doWork = {
                                lasersController.monitorLaserCollision(
                                    spaceObjects = spaceObjectsController.spaceObjects,
                                    enemies = enemies
                                )
                            }
                        )
                        // Wave 11a — TIME_FREEZE_BOOSTER gates enemy AI: firing,
                        // laser movement, and enemy movement all pause while
                        // ship.timeFreezeEndMillis is in the future.
                        // Task 05 — TIME_DILATION ability cũng đóng băng địch (timer riêng).
                        val isTimeFrozen = ship.timeFreezeEndMillis > System.currentTimeMillis() ||
                            ship.abilityFreezeEndMillis > System.currentTimeMillis()
                        if (!isTimeFrozen) tinker(
                            id = enemyLaserController.fireEnemyLaserId,
                            repeatTime = enemyLaserController.fireEnemyLaserRepeatTime,
                            doWork = { enemyLaserController.fireEnemyLasers(enemies = enemies) }
                        )
                        tinker(
                            id = mineralsController.processMineralsId,
                            repeatTime = mineralsController.processMineralsRepeatTime,
                            doWork = { mineralsController.processMinerals() }
                        )
                        tinker(
                            id = damageNumberController.tickId,
                            repeatTime = damageNumberController.tickRepeatTime,
                            doWork = { damageNumberController.tick() }
                        )
                        tinker(
                            id = impactSparkController.tickId,
                            repeatTime = impactSparkController.tickRepeatTime,
                            doWork = { impactSparkController.tick() }
                        )
                        // Wave 11a Phase 4 — TrailLine expiry tick (REFLECT bounce arcs + CHAIN_LIGHTNING bolts).
                        tinker(
                            id = trailLineController.tickId,
                            repeatTime = trailLineController.tickRepeatTime,
                            doWork = { trailLineController.tickExpiry() }
                        )
                        tinker(
                            id = pickupBurstController.tickId,
                            repeatTime = pickupBurstController.tickRepeatTime,
                            doWork = { pickupBurstController.tick() }
                        )
                        tinker(
                            id = pickupPopupController.tickId,
                            repeatTime = pickupPopupController.tickRepeatTime,
                            doWork = { pickupPopupController.tick() }
                        )
                        tinker(
                            id = explosionsController.processExplosionsId,
                            repeatTime = explosionsController.processExplosionsRepeatTime,
                            doWork = { explosionsController.processExplosions() }
                        )
                        if (boosterController.hasBoosters()) {
                            tinker(
                                id = boosterController.processBoostersId,
                                repeatTime = boosterController.processBoostersRepeatTime,
                                doWork = { boosterController.processBoosters() }
                            )
                        }
                        // Task 01 — drone orbit quanh tàu + tự bắn địch gần nhất.
                        if (droneController.hasDrones()) {
                            tinker(
                                id = droneController.orbitId,
                                repeatTime = droneController.orbitRepeatTime,
                                doWork = {
                                    droneController.orbitStep(
                                        ship.xOffset + ship.width / 2f,
                                        ship.yOffset + ship.height / 2f,
                                    )
                                }
                            )
                            // Slice 3 — mỗi drone quá cooldown + có địch → DroneShot,
                            // dựng DroneLaser (bay tới địch) rồi bơm vào hệ laser chung.
                            tinker(
                                id = droneController.fireId,
                                repeatTime = droneController.fireRepeatTime,
                                doWork = {
                                    val shots = droneController.fireStep(
                                        System.currentTimeMillis(),
                                        enemies,
                                    )
                                    if (shots.isNotEmpty()) {
                                        lasersController.addDroneLasers(
                                            shots.map { s ->
                                                DroneLaser.aimedAt(
                                                    id = uuidUtils.getUuid(),
                                                    fromX = s.fromX,
                                                    fromY = s.fromY,
                                                    targetX = s.targetX,
                                                    targetY = s.targetY,
                                                    screenWidth = screenWidth,
                                                    screenHeight = screenHeight,
                                                )
                                            }
                                        )
                                    }
                                    // Task 06 — HEAL drone hồi máu tàu (self-gate cooldown).
                                    val heal = droneController.healStep(System.currentTimeMillis())
                                    if (heal > 0) {
                                        val maxHp = initialShipHp   // polish — cap heal ở max hp thật (coerceIn 100..3000)
                                        shipController.healCapped(heal, maxHp)
                                    }
                                }
                            )
                            // Slice 6 — đạn địch trúng drone → drone mất HP; vỡ thì
                            // nổ. Spark mỗi cú, explosion khi vỡ (như va chạm khác).
                            tinker(
                                id = droneController.collisionId,
                                repeatTime = droneController.collisionRepeatTime,
                                doWork = {
                                    val hits = droneController.monitorDroneCollision(
                                        enemyLaserController.enemyLasers,
                                    )
                                    for (h in hits) {
                                        impactSparkController.spawnBurst(h.x, h.y)
                                        if (h.destroyed) {
                                            explosionsController.addExplosion(
                                                h.x - 22f, h.y - 22f, 44f, 44f,
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        if (lasersController.hasUltimateLasers()) {
                            tinker(
                                id = lasersController.processLasersId,
                                repeatTime = lasersController.processLasersRepeatTime,
                                doWork = { lasersController.processLasers() }
                            )
                        }
                        if (lasersController.hasShipLasers()) {
                            tinker(
                                id = lasersController.processShipLasersId,
                                repeatTime = lasersController.processShipLasersRepeatTime,
                                // Round 40 — pass enemies so MissileLaser homing can update targetX.
                                doWork = { lasersController.processShipLasers(enemies) }
                            )
                        }
                        // Task 04 — FinalBoss đổi phase (1→2→3 theo HP) → thoại 1 lần
                        // mỗi phase. Gate finalBossActive (chỉ chạy ở stage boss, không
                        // phải mỗi tick suốt run). Quyết định thoại = hàm pure (tested).
                        if (finalBossActive) {
                            val finalBossNow = enemies.firstOrNull {
                                it is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss
                            }
                            if (finalBossNow != null) {
                                val phaseRes = com.tranphuloi.neon.ui.game.story.StoryRegistry
                                    .phaseLineOnAdvance(finalBossPhaseSeen, finalBossNow.currentPhase)
                                if (phaseRes != null) {
                                    finalBossPhaseSeen = finalBossNow.currentPhase
                                    val spk = finalBossNow.bossKind?.displayName ?: "Bá vương thiên hà"
                                    coroutineScope.launch {
                                        storyLine = com.tranphuloi.neon.ui.game.story.StoryLine(
                                            speaker = spk,
                                            text = context.getString(phaseRes),
                                            durationMs = 3200,
                                        )
                                        storyShownMillis = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                        if (spaceObjectsController.hasSpaceObjects()) {
                            tinker(
                                id = spaceObjectsController.processSpaceObjectsId,
                                repeatTime = spaceObjectsController.processSpaceObjectsRepeatTime,
                                doWork = { spaceObjectsController.processSpaceObjects() }
                            )
                        }
                        if (!isTimeFrozen && enemyLaserController.hasEnemyLasers()) {
                            tinker(
                                id = enemyLaserController.processLasersId,
                                repeatTime = enemyLaserController.processLasersRepeatTime,
                                doWork = { enemyLaserController.processLasers() }
                            )
                        }
                        if (!isTimeFrozen && enemyController.hasEnemies()) {
                            tinker(
                                id = enemyController.processEnemiesId,
                                repeatTime = enemyController.processEnemiesRepeatTime,
                                doWork = { enemyController.processEnemies() }
                            )
                        }
                        // 6c trigger: scan enemies for boss with hp < 20%. Once per boss
                        // (gated by enemyId so we don't re-fire each tick during the 2s window).
                        if (bossSlowMotionStartedAtMillis == 0L || slowMoElapsed >= 2000L) {
                            val critBoss = enemies.firstOrNull {
                                it.isBoss && it.initialHp > 0f &&
                                    it.hp > 0f && it.hp / it.initialHp <= 0.20f &&
                                    it.enemyId != bossSlowMotionTriggeredForId
                            }
                            if (critBoss != null) {
                                bossSlowMotionStartedAtMillis = System.currentTimeMillis()
                                bossSlowMotionTriggeredForId = critBoss.enemyId
                                Logger.d("Boss slow-mo critical TRIGGERED: hp=${critBoss.hp}/${critBoss.initialHp} (ratio=${"%.2f".format(critBoss.hp / critBoss.initialHp)}) bossId=${critBoss.enemyId.take(6)}")
                            }
                        }
                        if (
                            gameStage is StageGame ||
                            enemyController.hasEnemies() ||
                            spaceObjectsController.hasSpaceObjects()
                        ) {
                            tinker(
                                id = lasersController.fireLaserId,
                                repeatTime = lasersController.fireLaserRepeatTime,
                                doWork = { lasersController.fireLasers(ship = ship) }
                            )
                        }
                        // 19b Charge shot — when both arrows held >= 1.5s, dispatch
                        // ultimate laser. ShipController tracks the timer + cooldown.
                        if (shipController.consumeChargeShot()) {
                            lasersController.fireUltimateLaser()
                            ultimateFlashMillis = System.currentTimeMillis()
                        }
                        if (gameStage is StageGame) {
                            val stage = gameStage as StageGame
                            tinker(
                                id = spaceObjectsController.addSpaceRockId,
                                repeatTime = stage.spaceRockSpawnRateMillis,
                                doWork = { spaceObjectsController.addSpaceRock() }
                            )
                            tinker(
                                id = enemyController.addEnemyId,
                                repeatTime = stage.enemyType.spawnRate,
                                doWork = { enemyController.addEnemy(stage.enemyType) }
                            )
                            tinker(
                                id = boosterController.addBoosterId,
                                repeatTime = boosterController.addBoosterRepeatTime,
                                doWork = { boosterController.addBooster() }
                            )
                        } else if (gameStage is StageBoss) {
                            val stage = gameStage as StageBoss
                            tinker(
                                id = stage.bossId,
                                repeatTime = stage.enemyType.spawnRate,
                                doWork = { enemyController.addEnemy(stage.enemyType) }
                            )
                        }
                        tinker(
                            id = monitorLoopInSecId,
                            repeatTime = monitorLoopRepeatTime,
                            doWork = { monitorLoopInSec() }
                        )
                        // Round 34 (41x) — status effect tick every 250ms.
                        // Applies BURN damage, expires effects, cleans dead-enemy entries.
                        tinker(
                            id = statusEffectTickId,
                            repeatTime = statusEffectRepeatTime,
                            doWork = {
                                val now = System.currentTimeMillis()
                                val burnDmg = statusEffectController.processTick(enemies, now)
                                if (burnDmg.isNotEmpty()) {
                                    burnDmg.forEach { (enemyId, dmg) ->
                                        enemies.firstOrNull { it.enemyId == enemyId }
                                            ?.onObjectImpact(dmg)
                                    }
                                }
                                // Bump tick var to force HUD recompose if needed (cheap, mutableLongStateOf)
                                statusEffectTick = now
                            }
                        )
                        // Round 41 (29x.2) — Mine proximity / lifetime check. Inlined (no
                        // separate controller) since the logic is small + mines list is
                        // typically 0-2 active. Detonate when any enemy enters TRIGGER_RADIUS
                        // or after LIFETIME_MS. On detonation: AoE damage all enemies within
                        // AOE_RADIUS + spawn explosion.
                        if (mines.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val remaining = mutableListOf<com.tranphuloi.neon.ui.game.ship.weapon.Mine>()
                            mines.forEach { m ->
                                val triggered = enemies.any { e ->
                                    val cx = e.xOffset + e.width / 2f
                                    val cy = e.yOffset + e.height / 2f
                                    val dx = cx - (m.xOffset + com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE / 2f)
                                    val dy = cy - (m.yOffset + com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE / 2f)
                                    val r = com.tranphuloi.neon.ui.game.ship.weapon.Mine.TRIGGER_RADIUS
                                    dx * dx + dy * dy <= r * r
                                }
                                val expired = (now - m.createdAtMillis) >= com.tranphuloi.neon.ui.game.ship.weapon.Mine.LIFETIME_MS
                                if (triggered || expired) {
                                    val mcx = m.xOffset + com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE / 2f
                                    val mcy = m.yOffset + com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE / 2f
                                    val aoe = com.tranphuloi.neon.ui.game.ship.weapon.Mine.AOE_RADIUS
                                    // Round 52 (40x Item combos) — Mine damage now scales by
                                    // effectiveStats.damageMul so modifiers (DOUBLE_OR_NOTHING ×2,
                                    // GLASS_CANNON ×2, BERSERKER ×1.5) + meta upgrades stack here
                                    // like they already do for ship lasers. Previously Mine.EXPLOSION_DAMAGE
                                    // was hardcoded 80f regardless of run setup.
                                    val dmg = com.tranphuloi.neon.ui.game.ship.weapon.Mine.EXPLOSION_DAMAGE *
                                        effectiveStats.damageMul
                                    enemies.forEach { e ->
                                        val cx = e.xOffset + e.width / 2f
                                        val cy = e.yOffset + e.height / 2f
                                        val dx = cx - mcx
                                        val dy = cy - mcy
                                        if (dx * dx + dy * dy <= aoe * aoe) {
                                            e.onObjectImpact(dmg)
                                            damageNumberController.report(e.enemyId, dmg.toInt(), cx, cy, e.isBoss)
                                        }
                                    }
                                    explosionsController.addExplosion(mcx, mcy, aoe / 2f, aoe / 2f)
                                    Logger.d("Mine detonated @ (${mcx.toInt()},${mcy.toInt()}) trigger=$triggered expired=$expired")
                                } else {
                                    remaining += m
                                }
                            }
                            if (remaining.size != mines.size) mines = remaining
                        }
                        if (frameCount % 1000L == 0L) {
                            // Periodic memory log roughly every 8s at the IO loop's pace.
                            val rt = Runtime.getRuntime()
                            val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                            Logger.d("PERF frame=$frameCount entities=stars:${background.stars.size}+dust:${background.dust.size}+lasers:${shipLasers.size}+enemies:${enemies.size} heapUsed=${usedMb}MB")
                        }
                        // Periodic combo expiry check — combo UI refreshes when no kill in 2s.
                        if (frameCount % 60L == 0L) {
                            val before = comboController.count
                            comboController.checkExpiry()
                            if (before != comboController.count) {
                                comboCount = comboController.count
                                comboTier = comboController.currentTier()
                            }
                        }
                    }
                    refreshHandler = System.currentTimeMillis()
                    // delay() suspends and propagates cancellation; gives ~125Hz cap when running.
                    // Round 77 (R77h) — slow-motion gate. Khi user KHÔNG hold ship
                    // (dragTargetX == null), delay 26ms (~38Hz) → mọi entity di chuyển
                    // 3.3× chậm hơn. Hold lại để resume real-time. Tactical pause-and-aim.
                    val isHeld = shipController.dragTargetX != null
                    delay(if (!isHeld && gameStatus == GameStatus.RUNNING) 26L else 8L)
                    yield()
                }
            }
        }
        onDispose {
            Logger.d("Game loop dispose: cancel job + clear tinkerMap (entities=stars:${background.stars.size},lasers:${shipLasers.size},enemies:${enemies.size},boosters:${boosters.size})")
            loopRunning = false
            job.cancel()
            // Module-level tinkerMap accumulates UUIDs forever otherwise.
            tinkerClearAll()
            // Pixel-2 heap-growth fix — module-level mapper LRU caches persist
            // across restart (top-level `private val` lifetime = app process).
            // Each run leaves up to 64 EnemyUI + 128 LaserUI zombie entries
            // in the cache. After 3-4 restarts heap grows ~10MB from these
            // alone. Force-clear by trimming against an empty alive-set.
            // LeakWatch quiet because the controllers themselves get GC'd —
            // the cache is just retained UI snapshots.
            enemyMapper.trimDead(emptySet())
            lasersMapper.trimDead(emptySet())
            // Hand controllers + state to LeakCanary (no-op in release).
            LeakWatch.watch(shipController, "GameState.onDispose → ShipController must be GC'd")
            LeakWatch.watch(enemyController, "GameState.onDispose → EnemyController must be GC'd")
            LeakWatch.watch(lasersController, "GameState.onDispose → LasersController must be GC'd")
            LeakWatch.watch(boosterController, "GameState.onDispose → BoosterController must be GC'd")
            LeakWatch.watch(spaceObjectsController, "GameState.onDispose → SpaceObjectsController must be GC'd")
            LeakWatch.watch(explosionsController, "GameState.onDispose → ExplosionController must be GC'd")
            LeakWatch.watch(mineralsController, "GameState.onDispose → MineralsController must be GC'd")
            LeakWatch.watch(enemyLaserController, "GameState.onDispose → EnemyLasersController must be GC'd")
            LeakWatch.watch(backgroundController, "GameState.onDispose → BackgroundController must be GC'd")
            LeakWatch.watch(stageController, "GameState.onDispose → StageController must be GC'd")
            LeakWatch.watch(damageNumberController, "GameState.onDispose → DamageNumberController must be GC'd")
            LeakWatch.watch(pickupPopupController, "GameState.onDispose → PickupPopupController must be GC'd")
            LeakWatch.watch(hitStopController, "GameState.onDispose → HitStopController must be GC'd")
            LeakWatch.watch(comboController, "GameState.onDispose → ComboController must be GC'd")
            LeakWatch.watch(impactSparkController, "GameState.onDispose → ImpactSparkController must be GC'd")
            LeakWatch.watch(pickupBurstController, "GameState.onDispose → PickupBurstController must be GC'd")
            LeakWatch.watch(statusEffectController, "GameState.onDispose → StatusEffectController must be GC'd")
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    refreshHandler

    return GameState(
        gameStatus = gameStatus,
        gameMessage = gameMessage,
        background = background,
        ship = ship,
        shipLasers = shipLasers.map { lasersMapper(it) },
        ultimateLasers = ultimateLasers.map { lasersMapper(it) },
        spaceObjects = spaceObjects.map { spaceObjectsMapper(it) },
        boosters = boosters.map { boosterMapper(it) },
        drones = drones.map { droneMapper(it) },
        enemies = enemies.map { e ->
            // Round 35 (42x) — feed active status effects to UI tint overlay.
            // Round 48 — use the singleton emptyList() when no effects active
            // (the common case). The previous `Set.map { ... }` allocated a fresh
            // ArrayList per enemy per tick even when empty → 30 enemies × ~125Hz
            // ≈ 3750 wasted ArrayList<Long>/sec at peak.
            val effects = statusEffectController.effectsFor(e.enemyId)
            val tints = if (effects.isEmpty()) emptyList()
                else effects.map { it.tintColorArgb }
            enemyMapper(e, tints)
        },
        enemyLasers = enemyLasers.map { lasersMapper(it) },
        gameTimeIndicator = gameTimeIndicator,
        minerals = minerals.map { mineralToMineralUIMapper(it) },
        mineralsEarnedTotal = mineralsEarnedTotal.toString(),
        explosions = explosions,
        lastShipDamageMillis = lastShipDamageMillis,
        lastBoosterPickupMillis = lastBoosterPickupMillis,
        lastMineralPickupMillis = lastMineralPickupMillis,
        lastEnemyKillMillis = lastEnemyKillMillis,
        lastStageAdvanceMillis = lastStageAdvanceMillis,
        comboCount = comboCount,
        comboTier = comboTier,
        comboPopupTier = comboPopupTier,
        comboPopupShownMillis = comboPopupShownMillis,
        stageIndex = stageController.currentIndex(),
        currentChapterId = stageController.currentChapterId(),
        currentHazard = stageController.currentHazard(),
        finalBossDefeated = finalBossDefeated,
        magnetRadius = magnetRadiusState.floatValue,
        damageNumbers = damageNumbers,
        impactSparks = impactSparks,
        trailLines = trailLines,
        pickupBursts = pickupBursts,
        pickupPopups = pickupPopups,
        bossKillFlashMillis = bossKillFlashMillis,
        ultimateFlashMillis = ultimateFlashMillis,
        smartBombFlashMillis = smartBombFlashMillis,
        lastBossHitMillis = lastBossHitMillis,
        lastBossHitX = lastBossHitX,
        lastBossHitY = lastBossHitY,
        achievementUnlocked = achievementUnlocked,
        achievementShownAtMillis = achievementShownAtMillis,
        waveClearBannerShownMillis = waveClearBannerShownMillis,
        bossIntroShownAtMillis = bossIntroShownAtMillis,
        bossIntroName = bossIntroName,
        bossIntroTaunt = bossIntroTaunt,
        bossIntroBossKind = bossIntroBossKind,
        onSkipBossIntro = {
            bossIntroShownAtMillis = 0L
            hitStopController.endBossIntroFreeze()
            Logger.d("Boss intro: skipped by player tap")
        },
        bossKillRank = bossKillRank,
        bossKillRankShownMillis = bossKillRankShownMillis,
        gameTimeSec = gameTimeSec,
        enemiesKilledTotal = enemiesKilledTotal,
        bossesDefeatedTotal = bossesDefeatedTotal,
        maxComboReached = maxComboReached,
        stagesReached = stageController.currentIndex(),
        shipShape = runContext.shipShape,
        // Round 77 audit fix — reactive cameraZoom (collectAsState above).
        cameraZoom = liveCameraZoom,
        // Wave 12 round 3 — UI shows earned + purchased-reserve total.
        smartBombs = smartBombs + stockpileBombsRemaining,
        mines = mines,
        lastBurstSweepMillis = lastBurstSweepMillis,
        chargeProgress = shipController.chargeProgress(),
        photoModeActive = photoModeActive,
        startPhotoCapture = {
            if (!photoModeActive) {
                Logger.d("startPhotoCapture: entering photo mode")
                photoModeActive = true
            }
        },
        finishPhotoCapture = {
            if (photoModeActive) {
                Logger.d("finishPhotoCapture: leaving photo mode")
                photoModeActive = false
            }
        },
        dispatchSmartBomb = {
            if ((smartBombs + stockpileBombsRemaining) > 0 && gameStatus == GameStatus.RUNNING) {
                // Spend earned bombs first; the purchased reserve last so unused
                // purchases persist. A reserve use decrements DataStore now
                // (true "consumed on use").
                if (smartBombs > 0) {
                    smartBombs--
                } else {
                    stockpileBombsRemaining--
                    coroutineScope.launch {
                        metaRepo.consumeStockpile(
                            com.tranphuloi.neon.data.ShopItem.SMARTBOMB_STOCKPILE_KEY, 1,
                        )
                    }
                }
                smartBombsUsedCount++
                // 46x SMART_BOMB_5 — used 5 in one run.
                if (smartBombsUsedCount >= 5) {
                    coroutineScope.launch { unlockAchievement(Achievement.SMART_BOMB_5) }
                }
                Logger.d("SmartBomb dispatch: ${enemies.size} enemies + ${enemyLasers.size} lasers cleared (used=$smartBombsUsedCount)")
                // Detonate every on-screen enemy: spawn explosion + minerals + kill counters.
                enemies.toList().forEach { e ->
                    explosionsController.addExplosion(
                        xOffset = e.xOffset + e.width / 2f,
                        yOffset = e.yOffset + e.height / 2f,
                        width = e.width,
                        height = e.height,
                    )
                    mineralsController.addMinerals(
                        xOffset = e.xOffset,
                        yOffset = e.yOffset + e.height / 2f,
                        width = e.width,
                        mineralAmount = e.minerals,
                    )
                    e.hp = 0f       // mark for cleanup in next process tick
                }
                enemyLasers = emptyList()       // clear all enemy lasers
                bossKillEventMillis = System.currentTimeMillis()    // reuse for screen feedback
                // Pixel-2 #2 fix — fire full-screen violet flash overlay so
                // user sees the effect zone covering full screen, not just
                // per-enemy explosions scattered around.
                smartBombFlashMillis = System.currentTimeMillis()
                // Pixel-3 round 4 fix — screen-fill explosion ring. Pre-fix
                // SmartBomb only spawned explosions AT enemy positions, so if
                // no enemies in the extended bottom/top bands (FAR zoom margins
                // outside raw screen), those bands stayed visually empty even
                // though the kill logic cleared everything. Spawn ~8 anchor
                // explosions at fixed positions covering the full extended
                // game-coord rect so user sees AOE coverage edge-to-edge.
                //
                // Extension values mirror the LaunchedEffect(liveCameraZoom)
                // wiring — read from shipController which is the canonical
                // store (set by LaunchedEffect alongside controller setters).
                val ringExtX = shipController.dragBoundsExtensionX
                val ringExtY = shipController.dragBoundsExtensionY
                val ringMargin = 60f
                val ringExplosionSize = 70f
                val ringYTop = -ringExtY + ringMargin
                val ringYBot = screenHeight + ringExtY - ringMargin
                val ringXLeft = -ringExtX + ringMargin
                val ringXRight = screenWidth + ringExtX - ringMargin
                val ringYMid = screenHeight / 2f
                val ringXMid = screenWidth / 2f
                // 8 explosions: 4 corners + 4 mid-edges. Covers extended margins.
                val ringPositions = listOf(
                    ringXLeft to ringYTop,
                    ringXMid to ringYTop,
                    ringXRight to ringYTop,
                    ringXLeft to ringYMid,
                    ringXRight to ringYMid,
                    ringXLeft to ringYBot,
                    ringXMid to ringYBot,
                    ringXRight to ringYBot,
                )
                ringPositions.forEach { (rx, ry) ->
                    explosionsController.addExplosion(
                        xOffset = rx,
                        yOffset = ry,
                        width = ringExplosionSize,
                        height = ringExplosionSize,
                    )
                }
            }
        },
        killCamStartedAtMillis = killCamStartedAtMillis,
        bossKillEventMillis = bossKillEventMillis,
        revivedShownAtMillis = revivedShownAtMillis,
        hasReviveToken = ship.hasReviveToken,
        bossSlowMotionStartedAtMillis = bossSlowMotionStartedAtMillis,
        bossKillBuffOfferMillis = bossKillBuffOfferMillis,
        gameMode = runMode,
        timeAttackLimitSec = timeAttackLimitSec,
        timeAttackEnded = timeAttackEnded,
        storyLine = storyLine,
        storyShownMillis = storyShownMillis,
        // Round 40 → 41 — secondary weapon. Progress 0..1; 1.0 → ready to fire.
        // Cooldown duration comes from the active weapon (Settings).
        // Task 05 — kỹ năng chủ động theo tàu.
        shipAbility = shipAbility,
        abilityCooldownProgress = run {
            if (lastAbilityFireMillis == 0L) 1f
            else ((System.currentTimeMillis() - lastAbilityFireMillis).toFloat() / abilityCooldownMs).coerceIn(0f, 1f)
        },
        activateAbility = {
            val now = System.currentTimeMillis()
            val ready = lastAbilityFireMillis == 0L || (now - lastAbilityFireMillis) >= abilityCooldownMs
            if (ready && gameStatus == GameStatus.RUNNING && !ship.shipSpriteHidden) {
                lastAbilityFireMillis = now
                val cx = ship.xOffset + ship.width / 2f
                val cy = ship.yOffset + ship.height / 2f
                when (shipAbility.effect) {
                    // ── Instant (world-level) ──
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.NOVA -> {
                        // Nổ lan toàn màn: sát thương lớn mọi địch + nổ + spark.
                        enemies.forEach { e ->
                            e.onObjectImpact(300f)
                            explosionsController.addExplosion(
                                e.xOffset + e.width / 2f - 30f, e.yOffset + e.height / 2f - 30f, 60f, 60f,
                            )
                        }
                        impactSparkController.spawnBurst(cx, cy)
                    }
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.REPAIR -> {
                        val maxHp = initialShipHp   // polish — cap heal ở max hp thật (coerceIn 100..3000)
                        shipController.healCapped((maxHp * 0.35f).toInt(), maxHp)
                    }
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.MAGNET_PULSE ->
                        mineralsController.flushAllToShip()
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.LASER_STORM ->
                        repeat(6) { lasersController.fireLasers(ship) }
                    // ── Duration (timer trên Ship, áp lazy ở damage/freeze/collision) ──
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.OVERDRIVE,
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.CRIT_FRENZY,
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.BULWARK,
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.PHASE_DASH,
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.DECOY,
                    com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.TIME_DILATION -> {
                        shipController.activateAbilityTimer(shipAbility.effect, now, shipAbility.durationMs)
                        impactSparkController.spawnBurst(cx, cy)
                    }
                }
                Logger.d("ShipAbility activated: ${shipAbility.name} effect=${shipAbility.effect} cd=${abilityCooldownMs}ms")
            }
        },
        // Task 13 (đợt 4) — Parry: bấm mở cửa sổ reflect (tái dùng absorb+retaliate).
        parryCooldownProgress = parryController.progress(System.currentTimeMillis()),
        activateParry = {
            val now = System.currentTimeMillis()
            if (gameStatus == GameStatus.RUNNING && parryController.tryActivate(now)) {
                shipController.grantReflectUntil(parryController.windowEndMillis)
                Logger.d("Parry activated: window→${parryController.windowEndMillis}")
            }
        },
        activeSecondaryWeapon = activeSecondaryWeapon,
        secondaryCooldownProgress = run {
            if (lastSecondaryFireMillis == 0L) 1f
            else {
                val cd = activeSecondaryWeapon.cooldownMs
                ((System.currentTimeMillis() - lastSecondaryFireMillis).toFloat() / cd).coerceIn(0f, 1f)
            }
        },
        fireSecondary = {
            val cd = activeSecondaryWeapon.cooldownMs
            val now = System.currentTimeMillis()
            val ready = (now - lastSecondaryFireMillis) >= cd
            if (ready && gameStatus == GameStatus.RUNNING && !ship.shipSpriteHidden) {
                when (activeSecondaryWeapon) {
                    com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon.MISSILE -> {
                        val nearest = enemies.minByOrNull {
                            val dx = (it.xOffset + it.width / 2f) - (ship.xOffset + ship.width / 2f)
                            val dy = it.yOffset - ship.yOffset
                            dx * dx + dy * dy
                        }
                        val tx = nearest?.let { it.xOffset + it.width / 2f }
                        lasersController.fireMissile(ship, tx)
                    }
                    com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon.MINE -> {
                        // Drop a mine just behind the ship's tail.
                        val mine = com.tranphuloi.neon.ui.game.ship.weapon.Mine(
                            id = java.util.UUID.randomUUID().toString(),
                            xOffset = ship.xOffset + ship.width / 2f - com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE / 2f,
                            yOffset = ship.yOffset + ship.height + 8f,
                            createdAtMillis = now,
                        )
                        mines = mines + mine
                        Logger.d("fireSecondary MINE: dropped at (${mine.xOffset.toInt()},${mine.yOffset.toInt()}) — total active=${mines.size}")
                    }
                    com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon.BURST -> {
                        // Instant: 40dmg to up to 5 nearest enemies in upper 2/3 of screen.
                        // Round 52 (40x Item combos) — BURST damage now scales by
                        // effectiveStats.damageMul. Matches the Mine fix above + the existing
                        // laser path (LasersController applies damageMultiplier callback).
                        val cutoffY = screenHeight * 2f / 3f
                        val burstDmg = 40f * effectiveStats.damageMul
                        val candidates = enemies
                            .filter { it.yOffset < cutoffY }
                            .sortedBy {
                                val dx = (it.xOffset + it.width / 2f) - (ship.xOffset + ship.width / 2f)
                                val dy = it.yOffset - ship.yOffset
                                dx * dx + dy * dy
                            }
                            .take(5)
                        candidates.forEach { e ->
                            e.onObjectImpact(burstDmg)
                            damageNumberController.report(
                                targetId = e.enemyId,
                                damage = burstDmg.toInt(),
                                xOffset = e.xOffset + e.width / 2f,
                                yOffset = e.yOffset,
                                isBoss = e.isBoss,
                            )
                        }
                        lastBurstSweepMillis = now
                        Logger.d("fireSecondary BURST: hit ${candidates.size} enemy(ies) for ${burstDmg.toInt()} each (damageMul=${effectiveStats.damageMul})")
                    }
                }
                lastSecondaryFireMillis = now
            }
        },
        moveShipLeft = { shipController.movingLeft = it },
        moveShipRight = { shipController.movingRight = it },
        // Round 77 (R77h) — hold+drag callbacks.
        onShipDragStart = { x, y -> shipController.setDragTarget(x, y) },
        onShipDragMove = { x, y -> shipController.setDragTarget(x, y) },
        onShipDragEnd = { shipController.clearDragTarget() },
        toggleGameStatus = {
            // Settings button must not be able to revive a destroyed ship.
            if (gameStatus != GameStatus.GAME_OVER) {
                val next = if (gameStatus == GameStatus.RUNNING) GameStatus.PAUSE else GameStatus.RUNNING
                Logger.d("toggleGameStatus: $gameStatus → $next")
                gameStatus = next
            }
        },
        // Wave 11c — thread-safe snapshot for GAME_OVER flush. ConcurrentHashMap.toMap()
        // is safe under concurrent writers; synchronizedList requires manual lock for
        // iteration (toList walks the list). Called once at GAME_OVER — no per-frame
        // cost. Captures gameTimeSec at call time, not at composition.
        snapshotRunTelemetry = {
            val ranksSnap = synchronized(ranksAchievedThisRun) {
                ranksAchievedThisRun.toList()
            }
            com.tranphuloi.neon.data.RunTelemetrySnapshot(
                bulletKills = bulletKillsThisRun.toMap(),
                bossKills = bossKillsThisRun.toMap(),
                ranksAchieved = ranksSnap,
                shipSkin = runShipSkin,
                shipTimeMillis = gameTimeSec * 1000L,
            )
        },
    )
}

data class GameState(
    val gameStatus: GameStatus,
    val gameMessage: String,
    val background: BackgroundState,
    val ship: Ship,
    val shipLasers: List<LaserUI>,
    val ultimateLasers: List<LaserUI>,
    val spaceObjects: List<SpaceObjectUI>,
    val boosters: List<BoosterUI>,
    val drones: List<DroneUI>,
    val enemies: List<EnemyUI>,
    val enemyLasers: List<LaserUI>,
    val gameTimeIndicator: String,
    val minerals: List<MineralUI>,
    val mineralsEarnedTotal: String,
    val explosions: List<Explosion>,
    val lastShipDamageMillis: Long,
    val lastBoosterPickupMillis: Long,
    val lastMineralPickupMillis: Long,
    val lastEnemyKillMillis: Long,
    val lastStageAdvanceMillis: Long,
    val comboCount: Int,
    val comboTier: com.tranphuloi.neon.ui.game.combo.ComboTier,
    val comboPopupTier: com.tranphuloi.neon.ui.game.combo.ComboTier,
    val comboPopupShownMillis: Long,
    val stageIndex: Int,
    val currentChapterId: Int,
    val currentHazard: com.tranphuloi.neon.ui.game.stage.HazardType?,
    val finalBossDefeated: Boolean,
    val magnetRadius: Float,
    val damageNumbers: List<DamageNumber>,
    val impactSparks: List<com.tranphuloi.neon.ui.game.spark.ImpactSpark>,
    val trailLines: List<com.tranphuloi.neon.ui.game.spark.TrailLine>,
    val pickupBursts: List<com.tranphuloi.neon.ui.game.spark.PickupBurst>,
    val pickupPopups: List<PickupPopup>,
    val bossKillFlashMillis: Long,
    /** Pixel-2 #2 — wall-clock of last UltimateLaser fire; 0 = none. */
    val ultimateFlashMillis: Long = 0L,
    /** Pixel-2 #2 — wall-clock of last SmartBomb dispatch; 0 = none. */
    val smartBombFlashMillis: Long = 0L,
    /** Round 79 (#4) — wall-clock of last bullet→boss hit; 0 if none yet. */
    val lastBossHitMillis: Long,
    /** Round 79 (#4) — boss center in game-coord dp when last hit. */
    val lastBossHitX: Float,
    val lastBossHitY: Float,
    val achievementUnlocked: Achievement?,
    val achievementShownAtMillis: Long,
    val waveClearBannerShownMillis: Long,
    val bossIntroShownAtMillis: Long,
    val bossIntroName: String,
    /** Wave 16 — taunt line shown inside the full-screen cinematic intro. */
    val bossIntroTaunt: String = "",
    /** Round 74 (R73e) — boss kind for per-boss audio cue. Null = non-boss intro. */
    val bossIntroBossKind: com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind? = null,
    /** Wave 16 — player tapped the cinematic intro to skip (ends freeze early). */
    val onSkipBossIntro: () -> Unit = {},
    val bossKillRank: com.tranphuloi.neon.ui.game.controls.BossRank?,
    val bossKillRankShownMillis: Long,
    val gameTimeSec: Long,
    val enemiesKilledTotal: Int,
    val bossesDefeatedTotal: Int,
    val maxComboReached: Int,
    val stagesReached: Int,
    /** Round 76 (R76d) — selected ship shape, exposed for HUD badge. */
    val shipShape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape =
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
    /** Round 77 audit fix — exposed camera zoom for GameScreen drag coord conversion. */
    val cameraZoom: com.tranphuloi.neon.data.CameraZoom =
        com.tranphuloi.neon.data.CameraZoom.MEDIUM,
    val smartBombs: Int,
    val dispatchSmartBomb: () -> Unit,
    /**
     * Round 51 (26x Photo mode) — true while a screenshot capture is in
     * progress. GameScreen reads this to hide HUD overlays so the captured
     * frame is clean. Toggled by `startPhotoCapture` then cleared by the
     * capture LaunchedEffect.
     */
    val photoModeActive: Boolean,
    /** Round 51 (26x) — flips [photoModeActive] true. The LaunchedEffect in
     *  GameScreen drives the capture flow. Idempotent while a capture is in
     *  progress (no-op if already active). */
    val startPhotoCapture: () -> Unit,
    /** Round 51 (26x) — called by the capture LaunchedEffect after the share
     *  intent fires (or fails) to clear [photoModeActive] and restore HUD. */
    val finishPhotoCapture: () -> Unit,
    /** Task 05 — kỹ năng chủ động của tàu đang dùng (map 1-1 ShipShape). */
    val shipAbility: com.tranphuloi.neon.ui.game.ship.shape.ShipAbility,
    /** Task 05 — cooldown kỹ năng: 0=vừa dùng, 1=sẵn sàng (cho vòng nút HUD). */
    val abilityCooldownProgress: Float,
    /** Task 13 (đợt 4) — Parry: tiến trình hồi chiêu (0..1) + kích hoạt phản đạn. */
    val parryCooldownProgress: Float,
    val activateParry: () -> Unit,
    /** Task 05 — kích hoạt kỹ năng; no-op nếu đang cooldown / game không RUNNING. */
    val activateAbility: () -> Unit,
    /** Round 40-41 (29x) — active secondary weapon (MISSILE / MINE / BURST). */
    val activeSecondaryWeapon: com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon,
    /** Round 40 (29x) — secondary weapon cooldown. 0=just fired, 1=ready. */
    val secondaryCooldownProgress: Float,
    /** Round 40 (29x) — fire the active secondary; no-op if on cooldown or game not RUNNING. */
    val fireSecondary: () -> Unit,
    /** Round 41 (29x.2) — active MINE list. Rendered by GameWorld. */
    val mines: List<com.tranphuloi.neon.ui.game.ship.weapon.Mine>,
    /** Round 41 (29x.2) — wall-clock of last BURST sweep (drives fading sweep visual). 0 = none. */
    val lastBurstSweepMillis: Long,
    val chargeProgress: Float,
    val killCamStartedAtMillis: Long,
    val bossKillEventMillis: Long,
    val revivedShownAtMillis: Long,
    val hasReviveToken: Boolean,
    val bossSlowMotionStartedAtMillis: Long,
    /** Round 34 (42x) — set when boss killed; GameScreen LaunchedEffect triggers BuffPicker nav. */
    val bossKillBuffOfferMillis: Long,
    /** Wave 5 (43x) — current run mode, decided at GameState construction. */
    val gameMode: com.tranphuloi.neon.ui.game.mode.GameMode,
    /** TIME_ATTACK only — countdown seconds (0 in other modes). */
    val timeAttackLimitSec: Int,
    /** Round 23 — TIME_ATTACK timer expired; GameScreen uses victory branch. */
    val timeAttackEnded: Boolean,
    /** 47x Wave 5 — current story dialogue line (null when none active). */
    val storyLine: com.tranphuloi.neon.ui.game.story.StoryLine?,
    /** 47x Wave 5 — millis when storyLine became visible (0 when none active). */
    val storyShownMillis: Long,
    val moveShipLeft: (Boolean) -> Unit,
    val moveShipRight: (Boolean) -> Unit,
    /** Round 77 (R77h) — hold+drag ship control. x/y in game world dp. */
    val onShipDragStart: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onShipDragMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onShipDragEnd: () -> Unit = {},
    val toggleGameStatus: () -> Unit,
    // Wave 11c — single thread-safe snapshot callback. Called once by
    // GameScreen at GAME_OVER to build both RunStats (display) and
    // recordRunMetrics (persistence). Replaces 4 individual fields that
    // were re-allocated every recomposition (audit P2-1 — ~125Hz allocation
    // churn) and were also racy with IO-dispatcher mutations (audit P0-1).
    val snapshotRunTelemetry: () -> com.tranphuloi.neon.data.RunTelemetrySnapshot =
        { com.tranphuloi.neon.data.RunTelemetrySnapshot.EMPTY },
)

private val boosterMapper = BoosterToBoosterUIMapper()
private val droneMapper = DroneToDroneUIMapper()
private val enemyMapper = EnemyToEnemyUIMapper()
private val mineralToMineralUIMapper = MineralToMineralUIMapper()
private val lasersMapper = LaserToLaserUIMapper()
private val spaceObjectsMapper = SpaceObjectToSpaceObjectUIMapper()
