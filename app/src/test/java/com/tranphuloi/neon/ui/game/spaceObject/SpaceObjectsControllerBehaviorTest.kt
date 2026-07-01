package com.tranphuloi.neon.ui.game.spaceObject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [SpaceObjectsController] — spawn bounds (size/x-offset),
 * per-tick movement, and hp<=0 removal.
 */
class SpaceObjectsControllerBehaviorTest {

    private val screenWidth = 400f
    private val screenHeight = 800f

    private fun newController(
        initial: List<SpaceObject> = emptyList(),
    ): Pair<SpaceObjectsController, MutableList<List<SpaceObject>>> {
        val captured = mutableListOf<List<SpaceObject>>()
        val ctrl = SpaceObjectsController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            initialSpaceObjects = initial,
            setSpaceObjects = { captured.add(it) },
        )
        return ctrl to captured
    }

    // ── addSpaceRock() bounds ──

    @Test
    fun `addSpaceRock always spawns size within 20 until 80`() {
        val (ctrl, _) = newController()
        repeat(50) { ctrl.addSpaceRock() }
        ctrl.spaceObjects.forEach { rock ->
            assertTrue("size phải >= 20, thực tế ${rock.size}", rock.size >= 20f)
            assertTrue("size phải < 80, thực tế ${rock.size}", rock.size < 80f)
        }
    }

    @Test
    fun `addSpaceRock always spawns xOffset within screen bounds accounting for its own size`() {
        val (ctrl, _) = newController()
        repeat(50) { ctrl.addSpaceRock() }
        ctrl.spaceObjects.forEach { rock ->
            assertTrue("xOffset không được âm hơn size, thực tế ${rock.xOffset}", rock.xOffset >= rock.size)
            assertTrue(
                "xOffset không được vượt screenWidth - size, thực tế ${rock.xOffset}",
                rock.xOffset <= screenWidth - rock.size,
            )
        }
    }

    @Test
    fun `addSpaceRock accumulates and publishes each spawn`() {
        val (ctrl, captured) = newController()
        ctrl.addSpaceRock()
        ctrl.addSpaceRock()
        assertEquals(2, ctrl.spaceObjects.size)
        assertEquals(2, captured.last().size)
    }

    // ── processSpaceObjects() ──

    @Test
    fun `processSpaceObjects removes objects whose hp dropped to zero or below`() {
        val rock = SpaceRock(xOffset = 100f, size = 40f, screenHeight = screenHeight)
        rock.onObjectImpact(rock.hp)
        val (ctrl, captured) = newController(initial = listOf(rock))

        ctrl.processSpaceObjects()

        assertTrue("rock hp<=0 phải bị loại khỏi list", ctrl.spaceObjects.isEmpty())
        assertTrue("phải publish list rỗng", captured.last().isEmpty())
    }

    @Test
    fun `processSpaceObjects moves surviving objects downward`() {
        val rock = SpaceRock(xOffset = 100f, size = 40f, screenHeight = screenHeight)
        val startY = rock.yOffset
        val (ctrl, _) = newController(initial = listOf(rock))

        ctrl.processSpaceObjects()

        assertEquals(1, ctrl.spaceObjects.size)
        assertTrue("rock còn sống phải trôi xuống (yOffset tăng)", ctrl.spaceObjects.first().yOffset > startY)
    }

    // ── hasSpaceObjects() ──

    @Test
    fun `hasSpaceObjects reflects current list state`() {
        val (ctrl, _) = newController()
        assertFalse(ctrl.hasSpaceObjects())
        ctrl.addSpaceRock()
        assertTrue(ctrl.hasSpaceObjects())
    }
}
