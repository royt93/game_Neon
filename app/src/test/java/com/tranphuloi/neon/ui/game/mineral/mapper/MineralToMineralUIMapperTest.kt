package com.tranphuloi.neon.ui.game.mineral.mapper

import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Round 59 — verifies [MineralToMineralUIMapper] field preservation.
 * Mineral is a plain data class with deterministic fields except `alpha`
 * (defaults to 1f, mutates via `process()`).
 */
class MineralToMineralUIMapperTest {

    private val mapper = MineralToMineralUIMapper()

    private fun mineral(x: Float = 50f, y: Float = 80f, w: Float = 20f) =
        Mineral(xOffset = x, yOffset = y, width = w)

    @Test
    fun `mapper preserves xOffset`() {
        val m = mineral(x = 123f)
        assertEquals(123f, mapper(m).xOffset, 0f)
    }

    @Test
    fun `mapper preserves yOffset`() {
        val m = mineral(y = 456f)
        assertEquals(456f, mapper(m).yOffset, 0f)
    }

    @Test
    fun `mapper preserves width`() {
        val m = mineral(w = 25f)
        assertEquals(25f, mapper(m).width, 0f)
    }

    @Test
    fun `mapper preserves default alpha = 1`() {
        val m = mineral()
        assertEquals(1f, mapper(m).alpha, 0f)
    }

    @Test
    fun `mapper preserves mutated alpha after process tick`() {
        // animationYOffset = ctor yOffset - 60. Construct at y=100 (so
        // animationYOffset=40), then drag yOffset down to 40 so the very next
        // process() tick crosses the alpha-decay threshold.
        val m = mineral(y = 100f)
        m.yOffset = 40f
        m.process()
        val ui = mapper(m)
        assertEquals(m.alpha, ui.alpha, 0f)
        assertEquals(1f - 0.009f, ui.alpha, 1e-5f)
    }

    @Test
    fun `mapper preserves position after upward drift`() {
        val m = mineral(y = 100f)
        m.process()
        val ui = mapper(m)
        assertEquals(99f, ui.yOffset, 0f)        // moved up 1px per yOffsetMovementSpeed
    }

    @Test
    fun `independent instances do not share state`() {
        val a = mineral(x = 10f, y = 20f)
        val b = mineral(x = 99f, y = 88f)
        val uiA = mapper(a)
        val uiB = mapper(b)
        assertEquals(10f, uiA.xOffset, 0f)
        assertEquals(99f, uiB.xOffset, 0f)
        assertEquals(20f, uiA.yOffset, 0f)
        assertEquals(88f, uiB.yOffset, 0f)
    }

    @Test
    fun `mapper invocation produces non-null UI projection`() {
        assertNotNull(mapper(mineral()))
    }
}
