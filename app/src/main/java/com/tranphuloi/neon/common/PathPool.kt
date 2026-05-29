package com.tranphuloi.neon.common

import androidx.compose.ui.graphics.Path

/**
 * Thread-safe object pool for [Path] instances. Closes R80 perf debt: prior to
 * this, every shape draw function (BoosterCanvas / EnemyCanvas / LaserCanvas /
 * ExplosionCanvas) allocated fresh `Path()` per frame. At 60fps with 20+ shapes
 * visible, GC churn from per-frame Path allocation contributed to the heap
 * fluctuation 13-42MB observed in R80 runtime log.
 *
 * Pool design:
 *   - Bounded free-list (MAX_SIZE = 32). Excess released paths dropped to GC.
 *   - `acquire()` returns a reset Path ready for new commands. Creates new if
 *     pool empty.
 *   - `release(path)` returns path to pool if not at capacity.
 *   - synchronized block on add/remove — minimal contention since Canvas draws
 *     are typically Main-thread.
 *
 * Usage pattern:
 *   ```
 *   val p = PathPool.acquire().apply { moveTo(...); lineTo(...); close() }
 *   drawPath(p, color)
 *   PathPool.release(p)
 *   ```
 *
 * Caller MUST release after use; forgetting leaks to GC (no worse than
 * pre-pool behavior). Path's `reset()` is called on acquire so caller never
 * sees stale state.
 */
object PathPool {

    private const val MAX_SIZE = 32
    private val pool = ArrayDeque<Path>()
    private val lock = Any()

    // R80 effectiveness metrics — track how often the pool actually reuses
    // (hit) vs creates new Path (miss). High hit ratio = pool delivering
    // perf benefit; low hit ratio = sites still allocating per-frame.
    @Volatile private var totalAcquires: Long = 0L
    @Volatile private var totalHits: Long = 0L          // acquire returned pooled instance
    @Volatile private var totalReleases: Long = 0L
    @Volatile private var totalDropped: Long = 0L       // release beyond MAX_SIZE → GC

    fun acquire(): Path = synchronized(lock) {
        totalAcquires++
        if (pool.isNotEmpty()) {
            totalHits++
            pool.removeFirst().also { it.reset() }
        } else {
            Path()
        }
    }

    fun release(path: Path) {
        synchronized(lock) {
            totalReleases++
            if (pool.size < MAX_SIZE) {
                pool.addLast(path)
            } else {
                totalDropped++
                // else: drop to GC. Pool stays bounded.
            }
        }
    }

    /**
     * Current pool size — exposed for tests + diagnostic logging. Not part of
     * normal usage.
     */
    fun size(): Int = synchronized(lock) { pool.size }

    /**
     * R80 effectiveness snapshot. Returns (acquires, hits, releases, dropped).
     * Hit ratio = hits / acquires; release coverage = releases / acquires.
     * Used by diagnostic logging + integration tests.
     */
    fun metrics(): Metrics = synchronized(lock) {
        Metrics(totalAcquires, totalHits, totalReleases, totalDropped)
    }

    data class Metrics(
        val acquires: Long,
        val hits: Long,
        val releases: Long,
        val dropped: Long,
    ) {
        /** Pool hit ratio (0.0..1.0). 1.0 = every acquire reuses a pooled Path. */
        val hitRatio: Double get() = if (acquires == 0L) 0.0 else hits.toDouble() / acquires
        /** Release coverage (0.0..1.0). 1.0 = every acquire has matching release. */
        val releaseCoverage: Double get() = if (acquires == 0L) 0.0 else releases.toDouble() / acquires
    }

    /** Test-only — flushes pool + metrics. Production code should never call this. */
    internal fun clearForTest() {
        synchronized(lock) {
            pool.clear()
            totalAcquires = 0L
            totalHits = 0L
            totalReleases = 0L
            totalDropped = 0L
        }
    }
}
