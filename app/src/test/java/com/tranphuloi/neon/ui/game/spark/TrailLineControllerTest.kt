package com.tranphuloi.neon.ui.game.spark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [TrailLineController] — P0 audit fix coverage.
 *
 * Pins:
 *   1. MAX_LINES cap (32) — unbounded growth prevented
 *   2. Oldest-drop semantics when at cap
 *   3. Bounce arc vs chain bolt color/duration/jagged contracts
 *   4. tickExpiry drains old lines + emits update
 */
class TrailLineControllerTest {

    @Test
    fun `MAX_LINES = 32 (audit-fixed cap)`() {
        assertEquals(32, TrailLineController.MAX_LINES)
    }

    @Test
    fun `addBounceArc spawns smooth line with REFLECT yellow color`() {
        var emittedLines: List<TrailLine> = emptyList()
        val ctrl = TrailLineController(updateState = { emittedLines = it })
        ctrl.addBounceArc(0f, 0f, 100f, 100f)
        assertEquals(1, emittedLines.size)
        val line = emittedLines.first()
        assertFalse("REFLECT arc is smooth, not jagged", line.jagged)
        assertEquals(0xFFE0E060L, line.colorArgb)
        assertEquals(300L, line.durationMillis)
    }

    @Test
    fun `addChainBolt spawns jagged line with electric arc blue`() {
        var emittedLines: List<TrailLine> = emptyList()
        val ctrl = TrailLineController(updateState = { emittedLines = it })
        ctrl.addChainBolt(0f, 0f, 100f, 100f)
        assertEquals(1, emittedLines.size)
        val line = emittedLines.first()
        assertTrue("CHAIN_LIGHTNING bolt is jagged for zigzag visual", line.jagged)
        assertEquals(0xFF80B0FFL, line.colorArgb)
        assertEquals(220L, line.durationMillis)
    }

    @Test
    fun `list capped at MAX_LINES when adds exceed cap (oldest dropped)`() {
        var emittedLines: List<TrailLine> = emptyList()
        val ctrl = TrailLineController(updateState = { emittedLines = it })
        // Spam 50 chain bolts — all distinct ids
        for (i in 0 until 50) {
            ctrl.addChainBolt(i.toFloat(), 0f, (i + 100).toFloat(), 0f)
        }
        assertEquals("List capped at MAX_LINES = 32", 32, emittedLines.size)
        // First entry should be the 19th addition (50 added - 32 cap = 18 dropped + 1
        // = entry from 19th-50th survive, oldest survivor is the 19th).
        val firstSurvivor = emittedLines.first()
        assertEquals("Oldest survivor's x1 = 18 (0..49 indexed, 18 dropped)",
            18f, firstSurvivor.x1, 1e-3f)
    }

    @Test
    fun `tickExpiry removes lines past duration`() {
        var emittedLines: List<TrailLine> = emptyList()
        val ctrl = TrailLineController(updateState = { emittedLines = it })
        ctrl.addBounceArc(0f, 0f, 100f, 100f)
        assertEquals(1, emittedLines.size)
        // Manually overwrite to simulate "300ms ago" — easier than waiting
        // (skip the actual wait, just verify the logic path exists)
        // After 400ms the line should expire. Real test: sleep 350ms then tick.
        Thread.sleep(350)
        ctrl.tickExpiry()
        assertEquals("Expired line removed after duration elapses",
            0, emittedLines.size)
    }

    @Test
    fun `hasLines reflects controller state`() {
        var emittedLines: List<TrailLine> = emptyList()
        val ctrl = TrailLineController(updateState = { emittedLines = it })
        assertFalse(ctrl.hasLines())
        ctrl.addBounceArc(0f, 0f, 100f, 100f)
        assertTrue(ctrl.hasLines())
    }

    // ── TrailLine math invariants ──

    @Test
    fun `progress is 0 at creation, 1 at duration end`() {
        val now = System.currentTimeMillis()
        val line = TrailLine(
            id = "test", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0xFFFFFFFFL, createdAtMillis = now,
            durationMillis = 1000L,
        )
        assertEquals(0f, line.progress(now), 1e-3f)
        assertEquals(0.5f, line.progress(now + 500), 1e-3f)
        assertEquals(1f, line.progress(now + 1000), 1e-3f)
        // Clamps at 1f past duration
        assertEquals(1f, line.progress(now + 2000), 1e-3f)
    }

    @Test
    fun `isExpired returns true only after duration elapses`() {
        val now = System.currentTimeMillis()
        val line = TrailLine(
            id = "test", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0xFFFFFFFFL, createdAtMillis = now,
            durationMillis = 300L,
        )
        assertFalse(line.isExpired(now))
        assertFalse(line.isExpired(now + 200))
        assertTrue(line.isExpired(now + 301))
        assertTrue(line.isExpired(now + 1000))
    }

    @Test
    fun `jaggedPoints returns endpoints when jagged false (no jitter)`() {
        val line = TrailLine(
            id = "smooth", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0xFFFFFFFFL, createdAtMillis = 0L,
            jagged = false,
        )
        val pts = line.jaggedPoints()
        assertEquals(2, pts.size)
        assertEquals(0f to 0f, pts[0])
        assertEquals(100f to 0f, pts[1])
    }

    @Test
    fun `jaggedPoints returns 7 points for jagged (segments=6)`() {
        val line = TrailLine(
            id = "lightning", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0xFFFFFFFFL, createdAtMillis = 0L,
            jagged = true,
        )
        val pts = line.jaggedPoints(segments = 6)
        assertEquals("6 segments = 7 endpoints", 7, pts.size)
        // Endpoints unchanged (jitter envelope is 0 at endpoints)
        assertEquals(0f, pts.first().first, 1e-3f)
        assertEquals(0f, pts.first().second, 1e-3f)
        assertEquals(100f, pts.last().first, 1e-3f)
        assertEquals(0f, pts.last().second, 1e-3f)
    }

    @Test
    fun `jaggedPoints deterministic per id (same input → same output)`() {
        val line1 = TrailLine(id = "abc", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0L, createdAtMillis = 0L, jagged = true)
        val line2 = TrailLine(id = "abc", x1 = 0f, y1 = 0f, x2 = 100f, y2 = 0f,
            colorArgb = 0L, createdAtMillis = 0L, jagged = true)
        assertEquals("Same id → same zigzag shape (stable across recomposition)",
            line1.jaggedPoints(), line2.jaggedPoints())
    }
}
