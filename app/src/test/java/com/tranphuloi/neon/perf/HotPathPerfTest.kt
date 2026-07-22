package com.tranphuloi.neon.perf

import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.explosion.controller.ExplosionController
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.status.StatusEffect
import com.tranphuloi.neon.ui.game.status.StatusEffectController
import com.tranphuloi.neon.utils.DateUtils
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 1B — hot-path perf timing tests. Not micro-benchmarks (no warmup /
 * JMH-grade isolation): a coarse iteration-budget guard so an accidental
 * O(n²) regression or a blocking call sneaking onto a per-frame hot path
 * fails CI instead of only showing up as jank on-device.
 *
 * Budgets are intentionally generous (order-of-magnitude headroom) to avoid
 * flaking across different dev/CI machines — the goal is to catch a 10-100x
 * regression, not to enforce a tight SLA.
 *
 * Excluded (see plan): [com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBoss.generateLasers]
 * (branches on `System.currentTimeMillis()/1000 % 2` → flaky near second
 * boundary) and `NeonGlow.neonGlow` (Compose Modifier, not runnable on plain JVM).
 */
class HotPathPerfTest {

    @After
    fun cleanup() {
        PathPool.clearForTest()
    }

    private fun timeMillis(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000L
    }

    // ── FinalBoss.generateLasers() — boss fires every tick while alive ──

    @Test
    fun `FinalBoss generateLasers stays within budget across 10k calls`() {
        val boss = FinalBoss(
            screenWidth = 400f,
            screenHeight = 800f,
            getShip = { Ship(xOffset = 200f, yOffset = 600f) },
        )
        val elapsedMs = timeMillis {
            repeat(10_000) { boss.generateLasers() }
        }
        assertTrue(
            "10k generateLasers() calls phải dưới 1000ms, thực tế ${elapsedMs}ms",
            elapsedMs < 1_000L,
        )
    }

    // ── PathPool.acquire()/release() — every shape draw in Canvas hot paths ──

    @Test
    fun `PathPool acquire-release cycle stays within budget across 50k calls`() {
        PathPool.clearForTest()
        val elapsedMs = timeMillis {
            repeat(50_000) {
                val p = PathPool.acquire()
                PathPool.release(p)
            }
        }
        assertTrue(
            "50k acquire/release cycle phải dưới 500ms, thực tế ${elapsedMs}ms",
            elapsedMs < 500L,
        )
    }

    // ── StatusEffectController.apply() — fan-in hotspot #1, fires per laser-hit ──

    @Test
    fun `StatusEffectController apply stays within budget across 10k calls on a shared enemy pool`() {
        val controller = StatusEffectController()
        val enemyIds = (0 until 50).map { "enemy-$it" }
        val elapsedMs = timeMillis {
            repeat(10_000) { i ->
                val enemyId = enemyIds[i % enemyIds.size]
                val type = if (i % 2 == 0) StatusEffect.BURN else StatusEffect.SLOW
                controller.apply(enemyId = enemyId, type = type, nowMillis = i.toLong())
            }
        }
        assertTrue(
            "10k apply() calls (mix refresh/new trên 50 enemy) phải dưới 500ms, thực tế ${elapsedMs}ms",
            elapsedMs < 500L,
        )
    }

    // ── DateUtils.currentTimeMillis() — called every tinker() invocation in the game loop ──

    @Test
    fun `DateUtils currentTimeMillis stays within budget across 100k calls`() {
        val dateUtils = DateUtils()
        val elapsedMs = timeMillis {
            repeat(100_000) { dateUtils.currentTimeMillis() }
        }
        assertTrue(
            "100k currentTimeMillis() calls phải dưới 500ms, thực tế ${elapsedMs}ms",
            elapsedMs < 500L,
        )
    }

    // ── Logger.d() — used pervasively (~155+ call sites), must not become a bottleneck ──

    @Test
    fun `Logger d stays within budget across 10k calls`() {
        val elapsedMs = timeMillis {
            repeat(10_000) { i -> Logger.d("perf-probe iteration $i") }
        }
        assertTrue(
            "10k Logger.d() calls phải dưới 1000ms, thực tế ${elapsedMs}ms",
            elapsedMs < 1_000L,
        )
    }

    // ── Task 02 — LightningChain.computeChainTargets: chạy mỗi khi đạn LIGHTNING
    // trúng địch (có thể nhiều lần/frame khi buff SPREAD/DOUBLE). O(maxSteps × E)
    // — phải rẻ ngay cả khi màn đầy địch (cap 30). Guard chống regression O(E²).
    @Test
    fun `LightningChain computeChainTargets stays within budget on a full screen`() {
        // 30 địch (= EnemyController cap) rải khắp màn; chạy 10k lần (≈ mật độ va
        // chạm đỉnh của nhiều frame gộp lại).
        val enemies = (0 until 30).map { i ->
            perfEnemy("e$i", x = (i % 6) * 60f, y = (i / 6) * 120f)
        }
        val elapsedMs = timeMillis {
            repeat(10_000) {
                com.tranphuloi.neon.ui.game.laser.LightningChain.computeChainTargets(
                    startX = 180f, startY = 400f, enemies = enemies,
                )
            }
        }
        assertTrue(
            "10k chain-target scans (30 địch) phải dưới 500ms, thực tế ${elapsedMs}ms",
            elapsedMs < 500L,
        )
    }

    // ── Task 36 — EnemyController.processEnemies() / applySeparationForces() ──
    // Was `enemies -= it` inside the per-enemy forEach (1 full-list copy per
    // kill, same tick) plus an unconditional mapNotNull rebuild of the
    // RegularEnemy partition on every call. Cap-sized roster (30, some
    // pre-killed via hp=0) exercises both the one-time removal pass and the
    // steady-state reference-identity cache hit path across repeated ticks.
    @Test
    fun `EnemyController processEnemies stays within budget across 10k calls on a full roster`() {
        val type = RegularEnemyType(
            drawableId = 0,
            width = 40f,
            height = 40f,
            hp = 100f,
            impactPower = 1f,
            formation = Row(rowCount = 1),
            xOffsetSpeed = 0f,
            yOffsetSpeed = 5f,
            enemySpawnRate = Once,
        )
        val initialEnemies = (0 until 30).map { i ->
            RegularEnemy(
                screenWidth = 400f,
                screenHeight = 800f,
                xOffset = (i % 6) * 60f,
                type = type,
                initialYOffset = (i / 6) * 120f,
                // ~40% pre-killed to exercise the removal pass once at the start.
                hp = if (i % 5 < 2) 0f else 100f,
            )
        }
        val controller = EnemyController(
            screenWidth = 400f,
            screenHeight = 800f,
            uuidUtils = UuidUtils(),
            getShip = { Ship(xOffset = 200f, yOffset = 700f) },
            initialEnemies = initialEnemies,
            setEnemies = {},
            addMinerals = { _, _, _, _ -> },
            addExplosion = { _, _, _, _ -> },
        )
        val elapsedMs = timeMillis {
            repeat(10_000) { controller.processEnemies() }
        }
        assertTrue(
            "10k processEnemies() calls (30 địch, cap) phải dưới 1000ms, thực tế ${elapsedMs}ms",
            elapsedMs < 1_000L,
        )
    }

    // ── Task 36 — ExplosionController.processExplosions() ──
    // Was `explosions -= it` inside the forEach (1 full-list copy per expired
    // explosion, same tick). MAX_ACTIVE-sized batch, steady-state (none expire
    // mid-test since the 10k-call loop runs well under Explosion's 450ms TTL).
    @Test
    fun `ExplosionController processExplosions stays within budget across 10k calls`() {
        val initialExplosions = (0 until 8).map { i ->
            Explosion(xOffset = i * 40f, yOffset = i * 30f, size = 80f)
        }
        val controller = ExplosionController(
            initialExplosions = initialExplosions,
            updateExplosions = {},
        )
        val elapsedMs = timeMillis {
            repeat(10_000) { controller.processExplosions() }
        }
        assertTrue(
            "10k processExplosions() calls (MAX_ACTIVE=8) phải dưới 500ms, thực tế ${elapsedMs}ms",
            elapsedMs < 500L,
        )
    }

    private fun perfEnemy(id: String, x: Float, y: Float): com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy =
        object : com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy {
            override val enemyId = id
            override val width = 40f
            override val height = 40f
            override var xOffset = x
            override var yOffset = y
            override var hp = 100f
            override val initialHp = 100f
            override val impactPower = 10f
            override val drawableId = 0
            override val minerals = 1
            override val destroyed = false
            override val outOfScreen = false
            override var lastImpactMillis = 0L
            override val isBoss = false
            override val displayName = "perf"
            override fun enemyRect() =
                androidx.compose.ui.geometry.Rect(xOffset, yOffset, xOffset + width, yOffset + height)
            override fun process() {}
            override fun generateLasers(): List<com.tranphuloi.neon.ui.game.laser.Laser> = emptyList()
            override fun onObjectImpact(impactPower: Float) {}
        }
}
