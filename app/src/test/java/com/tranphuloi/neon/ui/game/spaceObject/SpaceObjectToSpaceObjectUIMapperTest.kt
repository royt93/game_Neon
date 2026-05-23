package com.tranphuloi.neon.ui.game.spaceObject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 59 — verifies [SpaceObjectToSpaceObjectUIMapper] field preservation.
 * Concrete [SpaceRock] randomizes drawableId / yOffset / rotationSpeed in its
 * constructor, but xOffset / size / rotation / id are deterministic.
 */
class SpaceObjectToSpaceObjectUIMapperTest {

    private val mapper = SpaceObjectToSpaceObjectUIMapper()

    private fun rock(x: Float = 100f, size: Float = 40f, screenHeight: Float = 800f) =
        SpaceRock(xOffset = x, size = size, screenHeight = screenHeight)

    @Test
    fun `mapper preserves xOffset`() {
        val r = rock(x = 123f)
        assertEquals(123f, mapper(r).xOffset, 0f)
    }

    @Test
    fun `mapper preserves size`() {
        val r = rock(size = 75f)
        assertEquals(75f, mapper(r).size, 0f)
    }

    @Test
    fun `mapper preserves yOffset value at projection time`() {
        val r = rock()
        val ui = mapper(r)
        assertEquals(r.yOffset, ui.yOffset, 0f)
    }

    @Test
    fun `mapper preserves rotation`() {
        val r = rock()
        r.rotation = 42.5f
        assertEquals(42.5f, mapper(r).rotation, 0f)
    }

    @Test
    fun `mapper preserves id reference`() {
        val r = rock()
        val ui = mapper(r)
        assertEquals(r.id, ui.id)
    }

    @Test
    fun `mapper drawableId matches a known RockType drawable`() {
        val r = rock()
        val ui = mapper(r)
        val knownIds = SpaceRock.RockType.values().map { it.drawableId }.toSet()
        assertTrue(
            "drawableId ${ui.drawableId} not in RockType set",
            ui.drawableId in knownIds,
        )
    }

    @Test
    fun `independent instances do not share state`() {
        val a = rock(x = 10f, size = 20f)
        val b = rock(x = 99f, size = 50f)
        val uiA = mapper(a)
        val uiB = mapper(b)
        assertEquals(10f, uiA.xOffset, 0f)
        assertEquals(99f, uiB.xOffset, 0f)
        assertEquals(20f, uiA.size, 0f)
        assertEquals(50f, uiB.size, 0f)
    }

    @Test
    fun `mapper invocation produces non-null UI projection`() {
        assertNotNull(mapper(rock()))
    }
}
