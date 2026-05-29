package com.tranphuloi.neon.common

import androidx.compose.ui.graphics.Path
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R80 perf — PathPool behavior tests.
 *
 * Pins: acquire returns reset Path, release returns to pool, MAX_SIZE cap,
 * recycled Path is reused.
 */
class PathPoolTest {

    @After
    fun cleanup() {
        PathPool.clearForTest()
    }

    @Test
    fun `acquire returns a Path instance`() {
        val p = PathPool.acquire()
        assertNotNull(p)
    }

    @Test
    fun `release then acquire returns the same Path instance (reuse)`() {
        val original = PathPool.acquire()
        original.moveTo(10f, 20f)            // mutate state
        PathPool.release(original)
        val recycled = PathPool.acquire()
        assertSame("Pool should return previously released Path", original, recycled)
    }

    @Test
    fun `acquire from empty pool creates a new Path`() {
        PathPool.clearForTest()
        val p1 = PathPool.acquire()
        val p2 = PathPool.acquire()
        // Both came from empty pool → fresh allocations
        assertNotNull(p1)
        assertNotNull(p2)
    }

    @Test
    fun `pool size respects MAX_SIZE cap (excess dropped)`() {
        PathPool.clearForTest()
        // Release more than MAX_SIZE (32) — excess should be dropped
        val released = mutableListOf<Path>()
        for (i in 0 until 50) {
            val p = Path()
            PathPool.release(p)
            released.add(p)
        }
        assertEquals("Pool capped at 32 even after 50 releases",
            32, PathPool.size())
    }

    @Test
    fun `pool starts at size 0`() {
        PathPool.clearForTest()
        assertEquals(0, PathPool.size())
    }

    @Test
    fun `acquire decrements size, release increments`() {
        PathPool.clearForTest()
        val p1 = Path()
        val p2 = Path()
        PathPool.release(p1)
        PathPool.release(p2)
        assertEquals(2, PathPool.size())

        val recycled1 = PathPool.acquire()
        assertEquals(1, PathPool.size())

        val recycled2 = PathPool.acquire()
        assertEquals(0, PathPool.size())

        // Empty pool — next acquire creates new (size stays 0)
        val fresh = PathPool.acquire()
        assertEquals(0, PathPool.size())
        assertNotNull(fresh)
    }

    @Test
    fun `acquire returns Path with reset state (no stale commands)`() {
        // Release a Path with commands in it. Acquire should give back reset
        // Path (no stale moveTo/lineTo). Test indirectly: Path doesn't expose
        // command count, but we verify the instance identity + that subsequent
        // commands work without error.
        val dirty = PathPool.acquire()
        dirty.moveTo(100f, 200f)
        dirty.lineTo(300f, 400f)
        dirty.close()
        PathPool.release(dirty)

        val clean = PathPool.acquire()
        assertSame("Same instance returned", dirty, clean)
        // After reset, can issue new commands without error
        clean.moveTo(0f, 0f)
        clean.lineTo(10f, 10f)
        // No assertion — just verify no exception. Path.reset() called inside
        // acquire() guarantees no stale state.
    }

    @Test
    fun `concurrent acquire-release pattern matches usage in BoosterCanvas`() {
        // Typical usage:
        //   val p = PathPool.acquire().apply { moveTo(...); lineTo(...) }
        //   drawPath(p, color)
        //   PathPool.release(p)
        PathPool.clearForTest()
        repeat(100) {
            val p = PathPool.acquire().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
            // Simulated draw — instance valid
            assertNotNull(p)
            PathPool.release(p)
        }
        // Pool should not exceed MAX_SIZE
        assertTrue("Pool size capped after 100 uses", PathPool.size() <= 32)
    }

    // ── R80 effectiveness metrics ──

    @Test
    fun `metrics track acquires releases hits and dropped`() {
        PathPool.clearForTest()
        // 5 acquire/release cycles. First acquire = miss (empty pool).
        // Subsequent = hits (pool has 1 element after each release).
        val paths = mutableListOf<Path>()
        repeat(5) {
            paths.add(PathPool.acquire())
        }
        for (p in paths) PathPool.release(p)
        val m = PathPool.metrics()
        assertEquals("Total acquires", 5L, m.acquires)
        assertEquals("Total releases", 5L, m.releases)
        // First acquire = miss (pool empty), rest = miss too because we hold 5 paths concurrently
        // before releasing. After 5 acquires of empty pool, all 5 are fresh allocs.
        assertEquals("All 5 acquires were misses (held concurrently before release)", 0L, m.hits)
    }

    @Test
    fun `hit ratio = 1_0 when acquire-release strictly alternating`() {
        PathPool.clearForTest()
        // First cycle: miss (pool starts empty)
        val p1 = PathPool.acquire()
        PathPool.release(p1)
        // Second cycle: hit (pool has 1 element)
        val p2 = PathPool.acquire()
        PathPool.release(p2)
        // Third cycle: hit
        val p3 = PathPool.acquire()
        PathPool.release(p3)

        val m = PathPool.metrics()
        assertEquals("3 acquires", 3L, m.acquires)
        assertEquals("2 hits (cycles 2 and 3)", 2L, m.hits)
        assertTrue("Hit ratio = 2/3", m.hitRatio in 0.65..0.7)
        assertEquals("Release coverage = 100%", 1.0, m.releaseCoverage, 1e-6)
    }

    @Test
    fun `dropped count increments when releasing beyond MAX_SIZE`() {
        PathPool.clearForTest()
        // Release 40 paths — pool caps at 32, 8 dropped
        for (i in 0 until 40) {
            PathPool.release(Path())
        }
        val m = PathPool.metrics()
        assertEquals("8 paths dropped after MAX_SIZE=32 reached", 8L, m.dropped)
        assertEquals(32, PathPool.size())
    }

    @Test
    fun `metrics reset by clearForTest`() {
        PathPool.clearForTest()
        PathPool.acquire()
        PathPool.release(Path())
        assertTrue("Acquires recorded", PathPool.metrics().acquires > 0)
        PathPool.clearForTest()
        assertEquals("Acquires cleared", 0L, PathPool.metrics().acquires)
        assertEquals("Releases cleared", 0L, PathPool.metrics().releases)
        assertEquals("Hits cleared", 0L, PathPool.metrics().hits)
        assertEquals("Dropped cleared", 0L, PathPool.metrics().dropped)
    }

    @Test
    fun `R80 production simulation — 1000 draw cycles deliver high hit ratio`() {
        // Simulate 1000 frames worth of draw calls. Each frame: acquire +
        // (simulate draw) + release. After warmup (first ~32 acquires), pool
        // should reach hit ratio ~99%+ because all subsequent acquires reuse.
        PathPool.clearForTest()
        repeat(1000) {
            val p = PathPool.acquire().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
            PathPool.release(p)
        }
        val m = PathPool.metrics()
        assertEquals(1000L, m.acquires)
        assertEquals(1000L, m.releases)
        assertTrue("Hit ratio should be >= 99.9% (only 1 miss expected — warmup)",
            m.hitRatio >= 0.999)
        assertEquals("All hits except warmup", 999L, m.hits)
    }

    @Test
    fun `R80 effectiveness — concurrent peak load doesn't crash pool`() {
        // Simulate peak combat: 30 enemies × 3 paths each = 90 paths active.
        // Pool cap = 32, so 58 paths will be GC'd. Pool stays bounded.
        PathPool.clearForTest()
        val peak = mutableListOf<Path>()
        for (i in 0 until 90) {
            peak.add(PathPool.acquire())
        }
        for (p in peak) PathPool.release(p)
        assertEquals(32, PathPool.size())
        val m = PathPool.metrics()
        assertEquals(90L, m.acquires)
        assertEquals(90L, m.releases)
        assertEquals("58 paths dropped (90 - 32 cap)", 58L, m.dropped)
    }

    @Test
    fun `release coverage 100 percent when sites well-formed`() {
        PathPool.clearForTest()
        repeat(50) {
            val p = PathPool.acquire()
            PathPool.release(p)
        }
        val m = PathPool.metrics()
        assertEquals("Acquires == releases when sites well-formed",
            1.0, m.releaseCoverage, 1e-6)
    }

    @Test
    fun `Metrics data class equality works for snapshots`() {
        PathPool.clearForTest()
        val s1 = PathPool.metrics()
        val s2 = PathPool.metrics()
        assertEquals(s1, s2)
        PathPool.acquire()
        val s3 = PathPool.metrics()
        assertTrue("snapshot after acquire differs", s1 != s3)
    }
}
