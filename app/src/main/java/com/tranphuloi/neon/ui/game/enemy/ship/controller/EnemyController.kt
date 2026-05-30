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
) {

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
         * Contract:
         *  - Returns true iff the two BBOXes' centers are within 45% of each
         *    summed dimension on BOTH axes. Strict `<` so edge-touching pairs
         *    (dx == 0.5·(w_a + w_b)) pass.
         *  - Symmetric: bboxOverlaps(a, b) == bboxOverlaps(b, a).
         *  - Multiplier 0.45 (relaxed from 0.5 by audit P2 fix) provides
         *    0.1·w buffer for adjacent Row members at current widths.
         *  - Numeric breakpoint pinned by tests: stays correct up to ~75dp
         *    enemy width on 411dp screens; beyond that
         *    `FormationXOffset.rowXOffset.distanceBetween` must be retuned.
         */
        @androidx.annotation.VisibleForTesting
        internal fun bboxOverlaps(
            ax: Float, ay: Float, aw: Float, ah: Float,
            bx: Float, by: Float, bw: Float, bh: Float,
        ): Boolean {
            val dx = kotlin.math.abs(ax - bx)
            val dy = kotlin.math.abs(ay - by)
            val xLimit = (aw + bw) * 0.45f
            val yLimit = (ah + bh) * 0.45f
            return dx < xLimit && dy < yLimit
        }
    }

    val processEnemiesId = uuidUtils.getUuid()
    val processEnemiesRepeatTime = Millis(5)
    fun processEnemies() {
        var leftScreen = 0
        enemies.forEach {
            it.process()
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
            } else if (it.outOfScreen) {
                enemies -= it
                leftScreen++
            }
        }
        // Round 37 — was logging "$leftScreen enemies left screen" per 5ms tick.
        // Mid-wave that fired multiple times per second. Active enemy count is
        // available via UI; left-screen events aren't actionable signal.
        updateEnemies()
    }

    fun hasEnemies() = enemies.isNotEmpty()

    private fun updateEnemies() {
        setEnemies(enemies)
    }
}
