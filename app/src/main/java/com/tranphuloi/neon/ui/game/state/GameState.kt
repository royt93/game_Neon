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
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.Locale
import java.util.UUID

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberGameState(): GameState {

    val configuration = LocalConfiguration.current
    val screenWidth = rememberSaveable { configuration.screenWidthDp.toFloat() }
    val screenHeight = rememberSaveable { configuration.screenHeightDp.toFloat() }
    val uuidUtils = remember {
        Logger.d("rememberGameState: initial screen=${screenWidth}x${screenHeight}, building UuidUtils")
        UuidUtils()
    }
    val coroutineScope = rememberCoroutineScope()

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

    var ship by rememberSaveable {
        mutableStateOf(
            Ship(xOffset = screenWidth / 2 - 85f / 2, yOffset = screenHeight + 240f)
        )
    }
    var gameStatus by rememberSaveable { mutableStateOf(GameStatus.RUNNING) }
    fun setGameStatus(gameStt: GameStatus) {
        if (gameStatus != gameStt) {
            Logger.d("gameStatus: $gameStatus → $gameStt")
        }
        gameStatus = gameStt
    }
    var lastShipDamageMillis by remember { mutableLongStateOf(0L) }
    var lastBoosterPickupMillis by remember { mutableLongStateOf(0L) }
    // 11c: Kill-cam state — when ship destroyed, GameScreen delays GAME_OVER navigation
    // by KILL_CAM_DURATION_MS to play slow-mo replay.
    var killCamStartedAtMillis by remember { mutableLongStateOf(0L) }
    var bossKillEventMillis by remember { mutableLongStateOf(0L) }      // 30c camera zoom event
    val settingsRepo = com.tranphuloi.neon.data.LocalSettings.current
    val difficultyState = settingsRepo.difficulty
        .collectAsState(initial = com.tranphuloi.neon.data.Difficulty.NORMAL)
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
    // 15c Stats counters for end-of-run breakdown.
    var enemiesKilledTotal by remember { mutableIntStateOf(0) }
    var bossesDefeatedTotal by remember { mutableIntStateOf(0) }
    var maxComboReached by remember { mutableIntStateOf(0) }
    // 20b Smart bomb stack — start with 2, +1 per boss kill.
    var smartBombs by rememberSaveable { mutableIntStateOf(2) }
    val shipController = remember {
        Logger.d("rememberGameState: building ShipController (initial hp=${ship.hp})")
        ShipController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            ship = ship,
            setShip = { ship = it },
            onShipDestroyed = {
                killCamStartedAtMillis = System.currentTimeMillis()
                Logger.d("Kill-cam triggered @ $killCamStartedAtMillis (delay GAME_OVER 1500ms)")
                setGameStatus(GameStatus.GAME_OVER)
                // Mark ship destroyed — drives implosion animation in GameWorld.
                ship = ship.copy(destroyedAtMillis = killCamStartedAtMillis)
                // 3-phase destruction: implosion (0-200ms), white flash (200-300ms),
                // explosion starburst (delayed 300ms so flash → BANG sequence reads).
                val cx = ship.xOffset + ship.width / 2f
                val cy = ship.yOffset + ship.height / 2f
                val size = ship.width
                coroutineScope.launch {
                    delay(300L)
                    explosionsController.addExplosion(cx, cy, size * 1.4f, size * 1.4f)
                    explosionsController.addExplosion(cx - size * 0.5f, cy - size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx + size * 0.5f, cy - size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx - size * 0.5f, cy + size * 0.4f, size * 0.7f, size * 0.7f)
                    explosionsController.addExplosion(cx + size * 0.5f, cy + size * 0.4f, size * 0.7f, size * 0.7f)
                    Logger.d("Ship destruction starburst spawned at ($cx, $cy)")
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
                Logger.d("Ship damaged event @ $lastShipDamageMillis (will trigger shake+flash)")
            },
            onBoosterPickedUp = { x, y ->
                lastBoosterPickupMillis = System.currentTimeMillis()
                pickupBurstController.spawn(x, y)
                Logger.d("Booster picked up @ ($x,$y) ts=$lastBoosterPickupMillis")
            },
            damageMultiplier = { difficultyState.value.multiplier },
        )
    }

    var shipLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    var ultimateLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    var damageNumbers: List<DamageNumber> by remember { mutableStateOf(emptyList()) }
    val damageNumberController = remember {
        DamageNumberController(updateState = { damageNumbers = it })
    }
    val lasersController = remember {
        LasersController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            initialShipLasers = shipLasers,
            initialUltimateLasers = ultimateLasers,
            setShipLasers = { shipLasers = it },
            setUltimateLasers = { ultimateLasers = it },
            onLaserHit = { targetId, damage, x, y, isBoss ->
                damageNumberController.report(targetId, damage, x, y, isBoss)
                impactSparkController.spawnBurst(x, y)
                hitStopController.freezeForHit()
            },
        )
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

    var boosters: List<Booster> by rememberSaveable { mutableStateOf(emptyList()) }
    val boosterController = remember {
        BoosterController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            initialBoosters = boosters,
            updateBoosters = { boosters = it }
        )
    }

    var mineralsEarnedTotal: Int by rememberSaveable { mutableIntStateOf(0) }
    var minerals: List<Mineral> by rememberSaveable { mutableStateOf(emptyList()) }
    var pickupPopups: List<PickupPopup> by remember { mutableStateOf(emptyList()) }
    val pickupPopupController = remember {
        PickupPopupController(updateState = { pickupPopups = it })
    }
    var achievementUnlocked by remember {
        mutableStateOf<Achievement?>(null)
    }
    var achievementShownAtMillis by remember { mutableLongStateOf(0L) }
    val achievementsRepo = com.tranphuloi.neon.data.LocalAchievements.current
    suspend fun unlockAchievement(achievement: Achievement) {
        if (achievementsRepo.unlock(achievement)) {
            achievementUnlocked = achievement
            achievementShownAtMillis = System.currentTimeMillis()
            Logger.d("Achievement unlocked: ${achievement.id} \"${achievement.title}\"")
        }
    }
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
        com.tranphuloi.neon.ui.game.combo.ComboController(
            onTierAdvance = { tier ->
                comboPopupTier = tier
                comboPopupShownMillis = System.currentTimeMillis()
            }
        )
    }

    val mineralsController = remember {
        Logger.d("rememberGameState: building MineralsController")
        MineralsController(
            initialMinerals = minerals,
            updateMinerals = { minerals = it },
            updateMineralsEarnedTotal = { amount ->
                mineralsEarnedTotal += amount
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
            getMagnetRadius = { magnetRadiusState.floatValue }
        )
    }

    var enemies: List<Enemy> by rememberSaveable { mutableStateOf(emptyList()) }
    val enemyController = remember {
        Logger.d("rememberGameState: building EnemyController")
        EnemyController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            getShip = { ship },
            initialEnemies = enemies,
            setEnemies = { enemies = it },
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
                comboController.onEnemyKilled()
                comboCount = comboController.count
                comboTier = comboController.currentTier()
                if (comboCount > maxComboReached) maxComboReached = comboCount
                if (enemy.isBoss) {
                    bossesDefeatedTotal++
                    smartBombs++       // reward: +1 smart bomb per boss kill
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
                        Logger.d("Boss kill rank: ${bossKillRank?.letter} (ttk=${ttk}ms hpRatio=${"%.2f".format(hpRatio)})")
                    }
                } else {
                    hitStopController.freezeForEnemyKill()
                }
                // Achievement triggers
                coroutineScope.launch {
                    unlockAchievement(Achievement.FIRST_BLOOD)
                    if (enemy.isBoss) unlockAchievement(Achievement.FIRST_BOSS)
                    if (comboCount >= 5) unlockAchievement(Achievement.COMBO_5)
                    if (comboCount >= 10) unlockAchievement(Achievement.COMBO_10)
                }
                Logger.d("Enemy killed (id=${enemy.enemyId.take(6)}…) → combo=$comboCount tier=$comboTier boss=${enemy.isBoss}")
            }
        )
    }

    var enemyLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    val enemyLaserController = remember {
        EnemyLasersController(
            screenHeight = screenHeight,
            initialEnemyLasers = enemyLasers
        ) { enemyLasers = it }
    }

    var gameMessage by rememberSaveable { mutableStateOf("") }
    var lastStageAdvanceMillis by remember { mutableLongStateOf(0L) }
    var waveClearBannerShownMillis by remember { mutableLongStateOf(0L) }
    var bossIntroShownAtMillis by remember { mutableLongStateOf(0L) }
    var bossIntroName by remember { mutableStateOf("") }

    val stageController = rememberSaveable(saver = StageController.saver()) {
        StageController(
            onStageAdvance = { idx, newStage ->
                magnetRadiusState.floatValue = (80f + (idx / 5) * 10f).coerceAtMost(200f)
                lastStageAdvanceMillis = System.currentTimeMillis()
                // 12c: Wave clear bonus — when transitioning OUT of a StageGame to next stage,
                // burst 5 minerals at center and trigger banner.
                val previousIdx = idx - 1
                val previousWasGame =
                    previousIdx >= 0 &&
                        previousIdx < com.tranphuloi.neon.ui.game.stage.stages.size &&
                        com.tranphuloi.neon.ui.game.stage.stages[previousIdx] is com.tranphuloi.neon.ui.game.stage.StageGame
                if (previousWasGame) {
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
                    val bossName = when (newStage.enemyType) {
                        com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType -> "LEVEL 1 BOSS"
                        com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType -> "LEVEL 2 BOSS"
                        else -> "BOSS"
                    }
                    bossIntroName = bossName
                    bossIntroShownAtMillis = System.currentTimeMillis()
                    // Snapshot for rank computation — boss spawn time + player hp.
                    bossSpawnedAtMillis = bossIntroShownAtMillis
                    playerHpAtBossSpawn = ship.hp
                    Logger.d("Boss intro: $bossName cinematic triggered (hpSnapshot=${ship.hp})")
                }
                Logger.d("StageController.onStageAdvance idx=$idx → magnet=${magnetRadiusState.floatValue}px")
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
    fun monitorLoopInSec() {
        updateGameTime()
        updateGameTimeIndicator()
        updateGameStage()
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
                        }
                        frameCount++
                        tinker(
                            id = backgroundController.tickId,
                            repeatTime = backgroundController.tickRepeatTime,
                            doWork = { backgroundController.tick() }
                        )
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
                                ) { lasersController.fireUltimateLaser() }
                            }
                        )
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
                        tinker(
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
                                doWork = { lasersController.processShipLasers() }
                            )
                        }
                        if (spaceObjectsController.hasSpaceObjects()) {
                            tinker(
                                id = spaceObjectsController.processSpaceObjectsId,
                                repeatTime = spaceObjectsController.processSpaceObjectsRepeatTime,
                                doWork = { spaceObjectsController.processSpaceObjects() }
                            )
                        }
                        if (enemyLaserController.hasEnemyLasers()) {
                            tinker(
                                id = enemyLaserController.processLasersId,
                                repeatTime = enemyLaserController.processLasersRepeatTime,
                                doWork = { enemyLaserController.processLasers() }
                            )
                        }
                        if (enemyController.hasEnemies()) {
                            tinker(
                                id = enemyController.processEnemiesId,
                                repeatTime = enemyController.processEnemiesRepeatTime,
                                doWork = { enemyController.processEnemies() }
                            )
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
                    // delay() suspends and propagates cancellation; gives ~125Hz cap when running
                    // and keeps CPU idle when paused (gameStatus != RUNNING just sleeps).
                    delay(8)
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
        enemies = enemies.map { enemyMapper(it) },
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
        magnetRadius = magnetRadiusState.floatValue,
        damageNumbers = damageNumbers,
        impactSparks = impactSparks,
        pickupBursts = pickupBursts,
        pickupPopups = pickupPopups,
        bossKillFlashMillis = bossKillFlashMillis,
        achievementUnlocked = achievementUnlocked,
        achievementShownAtMillis = achievementShownAtMillis,
        waveClearBannerShownMillis = waveClearBannerShownMillis,
        bossIntroShownAtMillis = bossIntroShownAtMillis,
        bossIntroName = bossIntroName,
        bossKillRank = bossKillRank,
        bossKillRankShownMillis = bossKillRankShownMillis,
        gameTimeSec = gameTimeSec,
        enemiesKilledTotal = enemiesKilledTotal,
        bossesDefeatedTotal = bossesDefeatedTotal,
        maxComboReached = maxComboReached,
        stagesReached = stageController.currentIndex(),
        smartBombs = smartBombs,
        chargeProgress = shipController.chargeProgress(),
        dispatchSmartBomb = {
            if (smartBombs > 0 && gameStatus == GameStatus.RUNNING) {
                smartBombs--
                Logger.d("SmartBomb dispatch: ${enemies.size} enemies + ${enemyLasers.size} lasers cleared")
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
            }
        },
        killCamStartedAtMillis = killCamStartedAtMillis,
        bossKillEventMillis = bossKillEventMillis,
        moveShipLeft = { shipController.movingLeft = it },
        moveShipRight = { shipController.movingRight = it },
        toggleGameStatus = {
            // Settings button must not be able to revive a destroyed ship.
            if (gameStatus != GameStatus.GAME_OVER) {
                val next = if (gameStatus == GameStatus.RUNNING) GameStatus.PAUSE else GameStatus.RUNNING
                Logger.d("toggleGameStatus: $gameStatus → $next")
                gameStatus = next
            }
        }
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
    val magnetRadius: Float,
    val damageNumbers: List<DamageNumber>,
    val impactSparks: List<com.tranphuloi.neon.ui.game.spark.ImpactSpark>,
    val pickupBursts: List<com.tranphuloi.neon.ui.game.spark.PickupBurst>,
    val pickupPopups: List<PickupPopup>,
    val bossKillFlashMillis: Long,
    val achievementUnlocked: Achievement?,
    val achievementShownAtMillis: Long,
    val waveClearBannerShownMillis: Long,
    val bossIntroShownAtMillis: Long,
    val bossIntroName: String,
    val bossKillRank: com.tranphuloi.neon.ui.game.controls.BossRank?,
    val bossKillRankShownMillis: Long,
    val gameTimeSec: Long,
    val enemiesKilledTotal: Int,
    val bossesDefeatedTotal: Int,
    val maxComboReached: Int,
    val stagesReached: Int,
    val smartBombs: Int,
    val dispatchSmartBomb: () -> Unit,
    val chargeProgress: Float,
    val killCamStartedAtMillis: Long,
    val bossKillEventMillis: Long,
    val moveShipLeft: (Boolean) -> Unit,
    val moveShipRight: (Boolean) -> Unit,
    val toggleGameStatus: () -> Unit,
)

private val boosterMapper = BoosterToBoosterUIMapper()
private val enemyMapper = EnemyToEnemyUIMapper()
private val mineralToMineralUIMapper = MineralToMineralUIMapper()
private val lasersMapper = LaserToLaserUIMapper()
private val spaceObjectsMapper = SpaceObjectToSpaceObjectUIMapper()
