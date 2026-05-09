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
import com.tranphuloi.neon.ui.game.booster.Booster
import com.tranphuloi.neon.ui.game.booster.BoosterController
import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
import com.tranphuloi.neon.ui.game.booster.BoosterUI
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.constellation.ConstellationController
import com.tranphuloi.neon.ui.game.constellation.Star
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

    var stars: List<Star> by rememberSaveable { mutableStateOf(emptyList()) }
    val constellationController = remember {
        ConstellationController(
            stars = { stars },
            setStars = { stars = it }
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
    val settingsRepo = com.tranphuloi.neon.data.LocalSettings.current
    val difficultyState = settingsRepo.difficulty
        .collectAsState(initial = com.tranphuloi.neon.data.Difficulty.NORMAL)
    val shipController = remember {
        Logger.d("rememberGameState: building ShipController (initial hp=${ship.hp})")
        ShipController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            ship = ship,
            setShip = { ship = it },
            onShipDestroyed = { setGameStatus(GameStatus.GAME_OVER) },
            onShipDamaged = {
                lastShipDamageMillis = System.currentTimeMillis()
                Logger.d("Ship damaged event @ $lastShipDamageMillis (will trigger shake+flash)")
            },
            onBoosterPickedUp = {
                lastBoosterPickupMillis = System.currentTimeMillis()
                Logger.d("Booster picked up event @ $lastBoosterPickupMillis")
            },
            damageMultiplier = { difficultyState.value.multiplier },
        )
    }

    var shipLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    var ultimateLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    val lasersController = remember {
        LasersController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uuidUtils = uuidUtils,
            initialShipLasers = shipLasers,
            initialUltimateLasers = ultimateLasers,
            setShipLasers = { shipLasers = it },
            setUltimateLasers = { ultimateLasers = it }
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

    var explosions: List<Explosion> by rememberSaveable { mutableStateOf(emptyList()) }
    val explosionsController = remember {
        ExplosionController(
            initialExplosions = explosions,
            updateExplosions = { explosions = it }
        )
    }

    var mineralsEarnedTotal: Int by rememberSaveable { mutableIntStateOf(0) }
    var minerals: List<Mineral> by rememberSaveable { mutableStateOf(emptyList()) }
    var lastMineralPickupMillis by remember { mutableLongStateOf(0L) }
    var lastEnemyKillMillis by remember { mutableLongStateOf(0L) }
    var comboCount by remember { mutableIntStateOf(0) }
    var comboTier by remember { mutableStateOf(com.tranphuloi.neon.ui.game.combo.ComboTier.NONE) }
    var comboPopupTier by remember { mutableStateOf(com.tranphuloi.neon.ui.game.combo.ComboTier.NONE) }
    var comboPopupShownMillis by remember { mutableLongStateOf(0L) }
    // Holder updated by StageController.onStageAdvance below; closure-captured by mineralsController.
    val magnetRadiusState = remember { androidx.compose.runtime.mutableFloatStateOf(80f) }
    val mineralsController = remember {
        Logger.d("rememberGameState: building MineralsController")
        MineralsController(
            initialMinerals = minerals,
            updateMinerals = { minerals = it },
            updateMineralsEarnedTotal = {
                mineralsEarnedTotal += it
                lastMineralPickupMillis = System.currentTimeMillis()
            },
            getShipCenter = { ship.xOffset + ship.width / 2 to ship.yOffset + ship.height / 2 },
            getMagnetRadius = { magnetRadiusState.floatValue }
        )
    }

    val comboController = remember {
        Logger.d("rememberGameState: building ComboController")
        com.tranphuloi.neon.ui.game.combo.ComboController(
            onTierAdvance = { tier ->
                comboPopupTier = tier
                comboPopupShownMillis = System.currentTimeMillis()
            }
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
                Logger.d("Enemy killed (id=${enemy.enemyId.take(6)}…) → combo=$comboCount tier=$comboTier")
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

    val stageController = rememberSaveable(saver = StageController.saver()) {
        StageController(
            onStageAdvance = { idx, _ ->
                magnetRadiusState.floatValue = (80f + (idx / 5) * 10f).coerceAtMost(200f)
                lastStageAdvanceMillis = System.currentTimeMillis()
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
            constellationController.createStars(
                screenHeight = screenHeight,
                screenWidth = screenWidth
            )
            Logger.d("Constellation initialized; entering tick loop on IO dispatcher")
            // Run heavy per-tick work (collision, entity processing) on IO so it doesn't compete
            // with Compose recomposition on Main. Pace with delay(8) (~125Hz) which yields the
            // dispatcher between iterations and lets cancel() propagate immediately.
            launch(IO) {
                var frameCount = 0L
                while (loopRunning) {
                    if (gameStatus == GameStatus.RUNNING) {
                        frameCount++
                        tinker(
                            id = constellationController.animateStarsId,
                            repeatTime = constellationController.animateStarsRepeatTime,
                            doWork = { constellationController.animateStars() }
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
                            Logger.d("PERF frame=$frameCount entities=stars:${stars.size}+lasers:${shipLasers.size}+enemies:${enemies.size} heapUsed=${usedMb}MB")
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
            Logger.d("Game loop dispose: cancel job + clear tinkerMap (entities=stars:${stars.size},lasers:${shipLasers.size},enemies:${enemies.size},boosters:${boosters.size})")
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
            LeakWatch.watch(constellationController, "GameState.onDispose → ConstellationController must be GC'd")
            LeakWatch.watch(stageController, "GameState.onDispose → StageController must be GC'd")
        }
    }

    @Suppress("UNUSED_EXPRESSION")
    refreshHandler

    return GameState(
        gameStatus = gameStatus,
        gameMessage = gameMessage,
        stars = stars,
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
    val stars: List<Star>,
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
    val moveShipLeft: (Boolean) -> Unit,
    val moveShipRight: (Boolean) -> Unit,
    val toggleGameStatus: () -> Unit,
)

private val boosterMapper = BoosterToBoosterUIMapper()
private val enemyMapper = EnemyToEnemyUIMapper()
private val mineralToMineralUIMapper = MineralToMineralUIMapper()
private val lasersMapper = LaserToLaserUIMapper()
private val spaceObjectsMapper = SpaceObjectToSpaceObjectUIMapper()
