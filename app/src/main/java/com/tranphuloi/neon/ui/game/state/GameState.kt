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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.Locale
import java.util.UUID

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberGameState(): GameState {
    // Round 37 — was Logger.d("rememberGameState: composing — entry point") here, but
    // the function recomposes ~125Hz (refreshHandler read at function tail drives the
    // game loop's per-frame recompose). That made the log fire 125×/sec. Entry-point
    // logging happens once inside the `remember { ... UuidUtils() }` block below.
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
    val runContext = remember(runMode, runModifier) {
        com.tranphuloi.neon.ui.game.state.RunContext(
            mode = runMode,
            modifier = runModifier,
            difficulty = kotlinx.coroutines.runBlocking { settingsRepo.difficulty.first() },
            metaUpgrades = kotlinx.coroutines.runBlocking { metaRepo.allRanks.first() },
        )
    }
    // Round 34 (42x) — activeBuffs is a reactive MutableState. Reads here so
    // effectiveStats recomputes when buff picked. baseStats computed once;
    // mergedStats = baseStats × buffMultipliers (recompute on buff change).
    val activeBuffsState = com.tranphuloi.neon.ui.game.buff.LocalActiveBuffs.current
    val activeBuffs by activeBuffsState
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
    val initialShipHp = remember(effectiveStats) {
        (1000f * effectiveStats.hpMul).toInt().coerceIn(100, 3000)
    }
    var ship by rememberSaveable {
        mutableStateOf(
            Ship(
                xOffset = screenWidth / 2 - 85f / 2,
                yOffset = screenHeight + 240f,
                hp = initialShipHp,
            )
        )
    }
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
            achievementUnlocked = achievement
            achievementShownAtMillis = System.currentTimeMillis()
            Logger.d("Achievement unlocked: id=${achievement.id} tier=${achievement.tier} title=\"${achievement.title}\" desc=\"${achievement.description}\"")
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
    // 34d Wave 4 — set true when player defeats the FinalBoss (Galaxy Overlord).
    var finalBossDefeated by remember { mutableStateOf(false) }
    // 20b Smart bomb stack — start with 2, +1 per boss kill.
    var smartBombs by rememberSaveable { mutableIntStateOf(2) }
    /** 46x — count of smart bombs used in current run (for SMART_BOMB_5 achievement). */
    var smartBombsUsedCount by rememberSaveable { mutableIntStateOf(0) }
    /**
     * Round 23 — TIME_ATTACK timer ran out, treat as "victory" (ship still alive,
     * no kill-cam). GameScreen branches on this to use HEAVY haptic + PICKUP sfx
     * + 500ms delay path instead of the death path's LONG haptic + GAME_OVER sfx.
     */
    var timeAttackEnded by remember { mutableStateOf(false) }
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
                Logger.d("Ship damaged event @ $lastShipDamageMillis (will trigger shake+flash)")
            },
            onBoosterPickedUp = { x, y ->
                lastBoosterPickupMillis = System.currentTimeMillis()
                pickupBurstController.spawn(x, y)
                Logger.d("Booster picked up @ ($x,$y) ts=$lastBoosterPickupMillis")
            },
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
                Logger.d("SpaceRock impact ship @ ($x,$y) — visual burst")
            },
            damageMultiplier = { difficultyState.value.multiplier },
            // 25x/48x — modifier + skill tree speed multiplier (TRIPLE_SPEED ×3,
            // TANK ×0.7, AGILITY +6%/rank).
            speedMultiplier = { effectiveStats.speedMul },
            // Round 34 (44x) — ICE_PATCHES hazard active reads from currentHazard state
            // which is updated by stageController.onStageAdvance (declared below).
            isIceHazardActive = {
                currentHazard == com.tranphuloi.neon.ui.game.stage.HazardType.ICE_PATCHES
            },
        )
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
            onLaserHit = { targetId, damage, x, y, isBoss ->
                damageNumberController.report(targetId, damage, x, y, isBoss)
                impactSparkController.spawnBurst(x, y)
                // Mini explosion at hit point — reuses the GIF explosion system so the
                // hit reads as a real "pháo hoa nổ tung" not just sparks. Smaller size
                // (45-55dp) so it doesn't dwarf small enemies; bigger on boss.
                val miniSize = if (isBoss) 60f else 45f
                explosionsController.addExplosion(x, y, miniSize, miniSize)
                hitStopController.freezeForHit()
                // Round 34 (41x) — 10% chance to apply random status effect per hit.
                // Boss has reduced chance (5%) so they don't burn-stack-die unfairly.
                val chance = if (isBoss) 0.05f else 0.10f
                if (kotlin.random.Random.nextFloat() < chance) {
                    val effect = com.tranphuloi.neon.ui.game.status.StatusEffect.values().random()
                    statusEffectController.apply(targetId, effect, System.currentTimeMillis())
                }
            },
            // 25x/48x — modifier + skill tree damage multiplier applied per hit.
            damageMultiplier = { effectiveStats.damageMul },
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
            updateBoosters = { boosters = it },
            // 25x NO_SHIELDS modifier: filter SHIELD_BOOSTER spawns.
            noShieldDrops = { effectiveStats.noShieldDrops },
        )
    }

    var mineralsEarnedTotal: Int by rememberSaveable { mutableIntStateOf(0) }
    var minerals: List<Mineral> by rememberSaveable { mutableStateOf(emptyList()) }
    var pickupPopups: List<PickupPopup> by remember { mutableStateOf(emptyList()) }
    val pickupPopupController = remember {
        PickupPopupController(updateState = { pickupPopups = it })
    }
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
                // 25x apply RunModifier scoreMul. Round to int — small minerals (1-2)
                // multiplied by 1.5× rounds to 2-3; large minerals (10) round to 15.
                val scaled = (amount * effectiveStats.scoreMul).toInt().coerceAtLeast(amount)
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
            getMagnetRadius = { magnetRadiusState.floatValue * effectiveStats.magnetMul }
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
                        Logger.d("Boss kill rank: ${bossKillRank?.letter} (ttk=${ttk}ms hpRatio=${"%.2f".format(hpRatio)})")
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
                Logger.d("Enemy killed (id=${enemy.enemyId.take(6)}…) → combo=$comboCount tier=$comboTier boss=${enemy.isBoss}")
            }
        )
    }

    var enemyLasers: List<Laser> by remember { mutableStateOf(emptyList()) }
    val enemyLaserController = remember {
        EnemyLasersController(
            screenHeight = screenHeight,
            initialEnemyLasers = enemyLasers,
            setEnemyLasers = { enemyLasers = it },
            // Round 34 (41x) — STUN: skip fire if enemy is stunned this tick.
            isEnemyStunned = { enemyId -> statusEffectController.isStunned(enemyId) },
        )
    }

    var gameMessage by rememberSaveable { mutableStateOf("") }
    var lastStageAdvanceMillis by remember { mutableLongStateOf(0L) }
    var waveClearBannerShownMillis by remember { mutableLongStateOf(0L) }
    var bossIntroShownAtMillis by remember { mutableLongStateOf(0L) }
    var bossIntroName by remember { mutableStateOf("") }
    // 47x Wave 5 — story dialogue state. storyLine is what StoryOverlay shows;
    // storyShownMillis drives the slide-in / fade-out. chapterIntroShown gates
    // per-chapter intro firing so we only narrate first entry to each chapter.
    var storyLine by remember { mutableStateOf<com.tranphuloi.neon.ui.game.story.StoryLine?>(null) }
    var storyShownMillis by remember { mutableLongStateOf(0L) }
    val storyLines = remember { mutableListOf<com.tranphuloi.neon.ui.game.story.StoryLine>() }

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

    val stageController = rememberSaveable(runMode, saver = StageController.saver(stageProvider)) {
        StageController(
            provider = stageProvider,
            stageIndex = initialStageIndex,
            onStageAdvance = { idx, newStage ->
                magnetRadiusState.floatValue = (80f + (idx / 5) * 10f).coerceAtMost(200f)
                lastStageAdvanceMillis = System.currentTimeMillis()
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
                    val bossName = when (val t = newStage.enemyType) {
                        com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType -> "BOSS CẤP 1"
                        com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType -> "BOSS CẤP 2"
                        com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType -> "BÁ VƯƠNG THIÊN HÀ"
                        is com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType -> t.displayName
                        else -> "BOSS"
                    }
                    bossIntroName = bossName
                    bossIntroShownAtMillis = System.currentTimeMillis()
                    // Snapshot for rank computation — boss spawn time + player hp.
                    bossSpawnedAtMillis = bossIntroShownAtMillis
                    playerHpAtBossSpawn = ship.hp
                    Logger.d("Boss intro: $bossName cinematic triggered (hpSnapshot=${ship.hp})")
                    // 47x Story — boss taunt queued AFTER the BossIntroOverlay's
                    // 1.5s priority window so it doesn't compete with the warning
                    // banner. StoryOverlay is bottom-anchored so it won't overlap.
                    val taunt = com.tranphuloi.neon.ui.game.story.StoryRegistry
                        .bossTaunt(newStage.enemyType)
                    if (taunt != null) {
                        coroutineScope.launch {
                            delay(1600L)
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
                    ship = ship.copy(hp = 1000)
                    Logger.d("BOSS_RUSH: heal ship between bosses (hp $before → 1000)")
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
                                    ) { lasersController.fireUltimateLaser() }
                                }
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
        enemies = enemies.map { e ->
            // Round 35 (42x) — feed active status effects to UI tint overlay.
            val tints = statusEffectController.effectsFor(e.enemyId).map { it.tintColorArgb }
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
    val currentChapterId: Int,
    val currentHazard: com.tranphuloi.neon.ui.game.stage.HazardType?,
    val finalBossDefeated: Boolean,
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
    val toggleGameStatus: () -> Unit,
)

private val boosterMapper = BoosterToBoosterUIMapper()
private val enemyMapper = EnemyToEnemyUIMapper()
private val mineralToMineralUIMapper = MineralToMineralUIMapper()
private val lasersMapper = LaserToLaserUIMapper()
private val spaceObjectsMapper = SpaceObjectToSpaceObjectUIMapper()
