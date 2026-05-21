package com.tranphuloi.neon.core

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.common.Never
import com.tranphuloi.neon.ui.game.common.Once
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Tinker is process-wide / module-level, so each test scrubs the map before
 * and after to avoid bleed-through.
 */
class TinkerTest {

    @Before
    fun clearBefore() = tinkerClearAll()

    @After
    fun clearAfter() = tinkerClearAll()

    private fun freshId() = UUID.randomUUID().toString()

    @Test
    fun `Never repeatTime never invokes work`() {
        var calls = 0
        val id = freshId()
        repeat(5) { tinker(id, Never, { calls++ }) }
        assertEquals(0, calls)
    }

    @Test
    fun `Once invokes work only on first call`() {
        var calls = 0
        val id = freshId()
        repeat(5) { tinker(id, Once, { calls++ }) }
        assertEquals(1, calls)
    }

    @Test
    fun `Millis invokes work on first call immediately`() {
        var calls = 0
        val id = freshId()
        tinker(id, Millis(10_000), { calls++ })
        assertEquals(1, calls)
    }

    @Test
    fun `Millis does not invoke again before interval elapsed`() {
        var calls = 0
        val id = freshId()
        // Large interval; the back-to-back call should not refire.
        repeat(10) { tinker(id, Millis(60_000), { calls++ }) }
        assertEquals(1, calls)
    }

    @Test
    fun `Millis refires after interval elapsed`() {
        var calls = 0
        val id = freshId()
        // 1ms interval — first call fires immediately; sleep then second fires.
        tinker(id, Millis(1), { calls++ })
        Thread.sleep(20)
        tinker(id, Millis(1), { calls++ })
        assertEquals(2, calls)
    }

    @Test
    fun `tinkerClearAll resets the map so Once fires again`() {
        var calls = 0
        val id = freshId()
        tinker(id, Once, { calls++ })
        assertEquals(1, calls)
        tinker(id, Once, { calls++ })
        assertEquals(1, calls)
        tinkerClearAll()
        tinker(id, Once, { calls++ })
        assertEquals(2, calls)
    }

    @Test
    fun `independent ids are tracked independently`() {
        var a = 0
        var b = 0
        val idA = freshId()
        val idB = freshId()
        tinker(idA, Once, { a++ })
        tinker(idB, Once, { b++ })
        tinker(idA, Once, { a++ })
        tinker(idB, Once, { b++ })
        assertEquals(1, a)
        assertEquals(1, b)
    }
}
