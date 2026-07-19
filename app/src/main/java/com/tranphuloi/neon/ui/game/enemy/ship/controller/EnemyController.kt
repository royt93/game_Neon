package com.tranphuloi.neon.ui.game.enemy.ship.controller

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.factory.EnemyFactory
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils

class EnemyController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    uuidUtils: UuidUtils,
    private val enemyFactory: EnemyFactory = EnemyFactory(screenWidth, screenHeight),
    private val getShip: () -> Ship,
    initialEnemies: List<Enemy> = emptyList(),
    private val setEnemies: (List<Enemy>) -> Unit,
    private val addMinerals: (xOffset: Float, yOffset: Float, width: Float, mineralAmount: Int) -> Unit,
    private val addExplosion: (xOffset: Float, yOffset: Float, width: Float, height: Float) -> Unit,
    private val onEnemyKilled: (Enemy) -> Unit = {},
    // Wave 16 — SLOW status hook: slowed enemies skip movement on alternate
    // ticks (≈50% speed). Was a no-op before (isSlowed defined but never read).
    private val isSlowed: (enemyId: String) -> Boolean = { false },
    /**
     * Task 23 — RunModifier.BOSSES_ONLY ("Chỉ boss"): khi true, bỏ qua hoàn
     * toàn spawn enemy thường, chỉ boss được thêm vào. readyForNextStage chỉ
     * cần enemy+space-object list rỗng (không đếm kill) nên an toàn, không
     * treo stage.
     */
    private val bossesOnly: () -> Boolean = { false },
) {

    /** Wave 16 — alternating gate for SLOW (skip move every other tick). */
    private var slowTick: Int = 0

    init {
        Logger.d("EnemyController init: initialEnemies=${initialEnemies.size}, screen=${screenWidth}x${screenHeight}")
    }

    /**
     * Round 78 (#4 spec fix follow-up) — propagate FAR-zoom spawn margin to
     * the factory + formation helper. GameState sets this when user changes
     * camera zoom; subsequent spawns extend X bounds into the visual margins.
     */
    fun setSpawnXMargin(margin: Float) {
        enemyFactory.spawnXMargin = margin
        // FormationXOffset is owned by EnemyFactory — pipe through.
        enemyFactory.formationXOffsetMutable.spawnXMargin = margin
        Logger.d("EnemyController.setSpawnXMargin: $margin")
    }

    /**
     * Wave 11d Pixel-2 feedback #6 fix — Y-axis equivalent of [setSpawnXMargin].
     *
     * RegularEnemy.process() uses raw `screenHeight` to flag outOfScreen, so at
     * camera FAR (scale=0.7) enemies whose `yOffset + height > 891` get culled
     * while they're still mid-device visually (device-y range maps to game-y
     * `-190 .. 1082`). User reported enemies disappearing before reaching the
     * device-bottom edge.
     *
     * Fix: track `extraYSpan` here and override the per-enemy outOfScreen flag
     * in [processEnemies] by re-computing against `screenHeight + extraYSpan`.
     * Wired from GameState's `LaunchedEffect(liveCameraZoom)` alongside
     * setSpawnXMargin + lasersController.setExtraXSpan.
     */
    // Audit-7 hardening — @Volatile cho cross-thread visibility (Main writes
    // from LaunchedEffect, IO loop reads inside processEnemies).
    @Volatile
    private var extraYSpan: Float = 0f

    fun setExtraYSpan(margin: Float) {
        if (extraYSpan != margin) {
            Logger.d("EnemyController.setExtraYSpan: $extraYSpan → $margin (enemy outOfScreen threshold extended)")
            extraYSpan = margin
        }
    }

    /**
     * Round 79 (#1) — set current chapter context so EnemyFactory picks the
     * right bossKind override on next boss spawn (eliminate visual dups across
     * chapter encounters). Call from GameState when chapter advances.
     */
    fun setCurrentChapterId(chapterId: Int) {
        enemyFactory.currentChapterId = chapterId
        Logger.d("EnemyController.setCurrentChapterId: $chapterId")
    }

    private var enemies: List<Enemy> = initialEnemies

    val addEnemyId = uuidUtils.getUuid()
    fun addEnemy(type: EnemyType) {
        // Round 47 — entity cap (perf fix). At peak (chapter 2 NEBULA_FOG) the
        // enemies list hit 53 in user repro logs → 53 EnemyUI allocations per
        // tick from the mapper @ ~125Hz → GC pressure. Bosses bypass the cap
        // (always spawn) so boss waves can never be skipped due to swarm
        // overflow.
        val isBossSpawn = type is com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType ||
            type is com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
        if (!isBossSpawn && bossesOnly()) {
            Logger.v { "EnemyController.addEnemy: SKIPPED (BOSSES_ONLY modifier active)" }
            return
        }
        if (!isBossSpawn && enemies.size >= MAX_REGULAR_ENEMIES) {
            // Round 47 + audit fix — must be Logger.v: stage script attempts a
            // spawn every 200-1000ms, so when the cap holds we'd otherwise
            // pump 2-5 Logger.d lines/sec right back into logcat.
            Logger.v { "EnemyController.addEnemy: SKIPPED (cap=$MAX_REGULAR_ENEMIES reached, current=${enemies.size})" }
            return
        }
        val newEnemies = enemyFactory(type = type, getShip = getShip)
        // Round 55 — formation cap leak fix. EnemyFactory returns 1..N enemies
        // (Row=rowCount, VFormation=count, ZigZag=1). Previous check only
        // gated when ALREADY at cap; a Row of 5 spawning at enemies.size=29
        // produced 34 active, breaking the cap by up to formation size. User
        // log showed 32 enemies in chapter 1 with no boss → SmartBomb cleared
        // 32. Trim formation tail to remaining slots so cap is strictly
        // enforced. Bosses still bypass entirely.
        val toAdd = if (isBossSpawn) {
            newEnemies
        } else {
            val remainingSlots = (MAX_REGULAR_ENEMIES - enemies.size).coerceAtLeast(0)
            val clamped = newEnemies.take(remainingSlots)
            if (clamped.size < newEnemies.size) {
                Logger.v {
                    "EnemyController.addEnemy: TRIMMED formation ${newEnemies.size}→${clamped.size} " +
                        "(cap=$MAX_REGULAR_ENEMIES, remaining=$remainingSlots)"
                }
            }
            clamped
        }
        // Wave 11d Bug #4 fix — reject candidate enemies that would spawn on top
        // of an EXISTING enemy. Within-batch overlap (members of the same
        // formation) is NOT checked here: Row/V/SineWave formations are
        // designed with their own spacing rules (FormationXOffset.rowXOffset
        // distanceBetween, V xStep = w*1.4, SineWave yStep = h*1.4) so a Row
        // of 5 wide enemies must already be non-overlapping by construction.
        //
        // Audit P2 fix — earlier the check included `accepted.asSequence()`
        // for batch-internal overlap; that risked silently nuking wide-Row
        // formations where intra-row spacing approaches the enemy width.
        // Multiplier relaxed 0.5 → 0.45 so candidates just touching edges
        // pass — only true overlaps (centers within ~90% of width sum) reject.
        //
        // Audit-2 Risk #2 note (multi-formation same-tick bunching) — earlier
        // a "skip if other.yOffset < candidate.height" guard was added here
        // to prevent the hypothetical case of two formations firing in the
        // same loop tick where SineWave's i=0 at (centerX, 0) gets rejected
        // by a Row's middle member at (~centerX, 0). That guard had a fatal
        // arithmetic side effect: for same-height enemies the y-threshold
        // (= h) always exceeds yLimit (= 0.9·h), so the X-axis collision
        // check never fires once Y was clamped — effectively disabling Bug
        // #4 overlap rejection entirely. Reverted.
        //
        // Current state: original Bug #4 fix kept. Risk #2 stays a known
        // theoretical edge case — Stage.kt fires exactly ONE formation per
        // tick (one of ZigZag/Row/V/SineWave), so two formations don't
        // share a tick in production. If a future stage script adds
        // simultaneous multi-formation spawning, revisit with a proper
        // spawn-timestamp on Enemy interface instead of a y-threshold hack.
        //
        // Bosses bypass entirely.
        val finalToAdd = if (isBossSpawn) {
            toAdd
        } else {
            val accepted = mutableListOf<Enemy>()
            val existingSnapshot = this.enemies
            for (candidate in toAdd) {
                val collides = existingSnapshot.any { other ->
                    bboxOverlaps(
                        ax = candidate.xOffset, ay = candidate.yOffset,
                        aw = candidate.width, ah = candidate.height,
                        bx = other.xOffset, by = other.yOffset,
                        bw = other.width, bh = other.height,
                    )
                }
                if (collides) {
                    Logger.v {
                        "EnemyController.addEnemy: SKIPPED overlap candidate at (${candidate.xOffset},${candidate.yOffset})"
                    }
                } else {
                    accepted.add(candidate)
                }
            }
            accepted
        }
        this.enemies += finalToAdd
        Logger.v { "EnemyController.addEnemy: type=${type::class.simpleName} spawned ${finalToAdd.size}/${toAdd.size} (active=${this.enemies.size})" }
        updateEnemies()
    }

    companion object {
        /** Round 47 — soft cap for non-boss enemies. Bosses bypass this gate. */
        const val MAX_REGULAR_ENEMIES = 30

        /**
         * Audit-3 #1 fix — overlap predicate extracted to a pure helper so
         * tests can drive the production code directly instead of mirroring
         * the math in a test-side copy.
         *
         * Pixel-2 #3 fix — multiplier TIGHTENED from 0.45 back to 0.5 because
         * user reported "enemy vẫn còn overlap sau" after relaxation. The
         * relaxation was originally a safety buffer for hypothetical wide-Row
         * formations (width > 60dp); current data has width ≤ 46dp, so the
         * extra buffer just allowed visually-overlapping clusters from
         * knockback / ZigZag bounce / spawn timing edge cases to settle in.
         *
         * Contract:
         *  - Returns true iff the two BBOXes' centers are within 50% of each
         *    summed dimension on BOTH axes. Strict `<` so edge-touching pairs
         *    (dx == 0.5·(w_a + w_b)) pass.
         *  - Symmetric: bboxOverlaps(a, b) == bboxOverlaps(b, a).
         *  - At width=46 (current max) Row spacing 60.83 > xLimit 46 → safe.
         *  - Numeric breakpoint pinned by tests: at width 58.7+ the spawn
         *    check starts rejecting valid Row formations — retune
         *    `FormationXOffset.rowXOffset.distanceBetween` first.
         */
        @androidx.annotation.VisibleForTesting
        internal fun bboxOverlaps(
            ax: Float, ay: Float, aw: Float, ah: Float,
            bx: Float, by: Float, bw: Float, bh: Float,
        ): Boolean {
            val dx = kotlin.math.abs(ax - bx)
            val dy = kotlin.math.abs(ay - by)
            val xLimit = (aw + bw) * 0.5f
            val yLimit = (ah + bh) * 0.5f
            return dx < xLimit && dy < yLimit
        }
    }

    val processEnemiesId = uuidUtils.getUuid()
    val processEnemiesRepeatTime = Millis(5)
    fun processEnemies() {
        var leftScreen = 0
        // Pixel-2 #6 fix — re-compute outOfScreen using extended Y threshold so
        // enemies survive until they reach the device-visible bottom at FAR zoom.
        // We OVERRIDE the per-enemy outOfScreen flag (which was set in process()
        // against raw screenHeight). Negative extraYSpan (NEAR zoom) tightens
        // the threshold — symmetric.
        val effectiveHeight = screenHeight + extraYSpan
        slowTick++
        enemies.forEach {
            // Wave 16 — SLOW: slowed enemies move only every other tick (~50%).
            val skipMove = isSlowed(it.enemyId) && (slowTick % 2 == 0)
            if (!skipMove) it.process()
            val visuallyOffScreen = it.yOffset + it.height > effectiveHeight
            if (it.destroyed) {
                enemies -= it
                addMinerals(
                    it.xOffset,
                    it.yOffset + it.height / 2,
                    it.width,
                    it.minerals
                )
                addExplosion(
                    it.xOffset + it.width / 2,
                    it.yOffset + it.height / 2,
                    it.width,
                    it.height
                )
                onEnemyKilled(it)
                // Task 27 — Amip Vũ Trụ mitosis-on-death. isMitosisChild guard
                // chặn con sinh ra tách tiếp (không cần đụng Enemy interface).
                if (it is com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy &&
                    it.type.splitsOnDeath && !it.isMitosisChild
                ) {
                    enemies += enemyFactory.spawnMitosisChildren(it)
                }
            } else if (visuallyOffScreen) {
                enemies -= it
                leftScreen++
            }
        }
        // Pixel-2 #3 fix — runtime separation force. Knockback velocity (when
        // ship rams an enemy), ZigZag bounce off screen edges, and overlapping
        // spawns slipping past the pre-spawn check all create visible clusters
        // post-spawn. Per-tick gentle nudge pushes overlapping pairs apart
        // along their connecting vector. Step 0.4dp/tick @ ~200Hz = 80dp/sec
        // separation rate — fast enough to resolve clusters within ~1s,
        // gentle enough to not visibly teleport enemies. Y axis untouched so
        // formation vertical staggers preserve their design.
        applySeparationForces()
        // Round 37 — was logging "$leftScreen enemies left screen" per 5ms tick.
        // Mid-wave that fired multiple times per second. Active enemy count is
        // available via UI; left-screen events aren't actionable signal.
        updateEnemies()
    }

    private fun applySeparationForces() {
        val list = enemies
        if (list.size < 2) return
        val step = 0.4f
        // Audit-5 P1 fix — partition once at top, avoid 870 casts/tick at peak.
        // Filter to RegularEnemies; bosses skip separation (intimidation
        // intent). Also early-out the inner loop on Y-distance before sqrt.
        val regulars = list.mapNotNull {
            it as? com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
        }
        if (regulars.size < 2) return
        for (i in regulars.indices) {
            val a = regulars[i]
            for (j in i + 1 until regulars.size) {
                val b = regulars[j]
                val dy = b.yOffset - a.yOffset
                val minDist = (a.width + b.width) * 0.5f
                // Early Y-axis reject — avoids sqrt for far-apart pairs.
                if (kotlin.math.abs(dy) >= minDist) continue
                val dx = b.xOffset - a.xOffset
                val distSq = dx * dx + dy * dy
                val minDistSq = minDist * minDist
                if (distSq > 0.01f && distSq < minDistSq) {
                    val dist = kotlin.math.sqrt(distSq.toDouble()).toFloat()
                    val pushX = (dx / dist) * step
                    // Audit-5 P1 fix — accumulate into separationVel (consumed
                    // by RegularEnemy.process() AFTER formation movement) so
                    // SineWave's per-tick xOffset = sineAnchorX + sin(phase)
                    // doesn't clobber the push. Pre-fix the controller mutated
                    // xOffset directly → SineWave / ZigZag re-stepped overrode
                    // it the same frame.
                    a.separationVel -= pushX
                    b.separationVel += pushX
                }
            }
        }
    }

    fun hasEnemies() = enemies.isNotEmpty()

    private fun updateEnemies() {
        setEnemies(enemies)
    }
}
