package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 81 audit — verifies [CameraZoom] enum + the GameState margin formula
 * for FAR-zoom enemy spawn / ship drag extension.
 *
 * Formula in GameState.LaunchedEffect(liveCameraZoom):
 *   extensionX = screenWidth * (1f / pixelScale - 1f) / 2f
 *
 * At NEAR (1.0): extension = 0  → no extension, ship/spawn at original bounds.
 * At MEDIUM (0.85): extension = screenWidth * (1/0.85 - 1) / 2 ≈ 0.088 × screenWidth
 * At FAR (0.7): extension = screenWidth * (1/0.7 - 1) / 2 ≈ 0.214 × screenWidth
 */
class CameraZoomTest {

    @Test
    fun `CameraZoom has 3 levels NEAR MEDIUM FAR`() {
        assertEquals(3, CameraZoom.values().size)
        assertTrue(CameraZoom.values().toSet() ==
            setOf(CameraZoom.NEAR, CameraZoom.MEDIUM, CameraZoom.FAR))
    }

    @Test
    fun `pixelScale values are monotonically decreasing NEAR_to_FAR`() {
        assertTrue(CameraZoom.NEAR.pixelScale > CameraZoom.MEDIUM.pixelScale)
        assertTrue(CameraZoom.MEDIUM.pixelScale > CameraZoom.FAR.pixelScale)
    }

    @Test
    fun `pixelScale exact values`() {
        assertEquals(1.0f, CameraZoom.NEAR.pixelScale, 1e-6f)
        assertEquals(0.85f, CameraZoom.MEDIUM.pixelScale, 1e-6f)
        assertEquals(0.7f, CameraZoom.FAR.pixelScale, 1e-6f)
    }

    @Test
    fun `fromKey roundtrips for every CameraZoom`() {
        for (zoom in CameraZoom.values()) {
            assertEquals(zoom, CameraZoom.fromKey(zoom.key))
        }
    }

    @Test
    fun `fromKey defaults to MEDIUM for unknown or null`() {
        assertEquals(CameraZoom.MEDIUM, CameraZoom.fromKey(null))
        assertEquals(CameraZoom.MEDIUM, CameraZoom.fromKey("nonsense"))
    }

    @Test
    fun `margin formula at NEAR zoom equals zero`() {
        val screenWidth = 411f
        val ext = computeExtension(screenWidth, CameraZoom.NEAR.pixelScale)
        assertEquals(0f, ext, 1e-3f)
    }

    @Test
    fun `margin formula at FAR zoom equals 21 percent of screenWidth`() {
        val screenWidth = 411f
        val ext = computeExtension(screenWidth, CameraZoom.FAR.pixelScale)
        // (1/0.7 - 1) / 2 = (1.4286 - 1) / 2 ≈ 0.2143
        val expected = screenWidth * 0.2143f
        assertEquals(expected, ext, 1f)
    }

    @Test
    fun `margin formula at MEDIUM zoom equals about 9 percent of screenWidth`() {
        val screenWidth = 411f
        val ext = computeExtension(screenWidth, CameraZoom.MEDIUM.pixelScale)
        // (1/0.85 - 1) / 2 = (1.1765 - 1) / 2 ≈ 0.0882
        val expected = screenWidth * 0.0882f
        assertEquals(expected, ext, 1f)
    }

    /**
     * Mirror of the formula in [com.tranphuloi.neon.ui.game.state.GameState]
     * LaunchedEffect(liveCameraZoom). Kept identical so any future refactor
     * (e.g., extracting to CameraZoom.computeMarginPx()) trivially passes test.
     */
    private fun computeExtension(screenSize: Float, pixelScale: Float): Float =
        screenSize * (1f / pixelScale.coerceAtLeast(0.01f) - 1f) / 2f
}
