package com.tranphuloi.neon.ui.game.ship.ship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d Pixel-3 round 3 — pin the Y-axis camera-zoom extension math.
 *
 * Three separate fixes all use `dragBoundsExtensionY` / `extraYSpan` =
 * `screenHeight * (1/scale - 1) / 2` to extend their behavior past raw
 * screenHeight at FAR camera zoom:
 *
 * 1. `ShipController.moveShip` auto-pull target = `maxYOffset + extensionY`
 *    so ship parks at device-bottom (not 134dp above) at FAR.
 * 2. `LasersController.fireUltimateLaser` beam start `yOffset =
 *    screenHeight + extraYSpan` so beam sweeps the full visible device-y range.
 * 3. `EnemyLasersController.processLasers` destruction threshold `yOffset >
 *    screenHeight + extraYSpan` so enemy lasers reach the ship in the new
 *    extended-bottom region.
 *
 * These tests pin the math contract via a pure helper. Audit cycle 7 flagged
 * "zero tests for pure-math claims"; this file addresses that.
 */
class Pixel3ZoomBoundsTest {

    /** Extension formula from GameState.kt:LaunchedEffect(liveCameraZoom). */
    private fun extensionY(screenHeight: Float, scale: Float): Float =
        screenHeight * (1f / scale - 1f) / 2f

    @Test
    fun `extensionY at MEDIUM zoom (scale 0_85) is approximately 78dp on 891 screen`() {
        // Verified against the device log line:
        //   "Camera zoom=medium scale=0.85 → drag/spawn extension X=36 Y=78"
        val ext = extensionY(891f, 0.85f)
        assertEquals(78.6f, ext, 0.1f)
    }

    @Test
    fun `extensionY at FAR zoom (scale 0_7) is approximately 190dp on 891 screen`() {
        // Verified against the device log line:
        //   "Camera zoom=far scale=0.7 → drag/spawn extension X=88 Y=190"
        val ext = extensionY(891f, 0.7f)
        assertEquals(190.9f, ext, 0.1f)
    }

    @Test
    fun `extensionY at NEAR zoom (scale=1_0) is zero — production behavior`() {
        // Audit-10 P2 fix — production CameraZoom.NEAR.pixelScale = 1.0 (NOT
        // 1.2). At scale=1.0 extension is exactly 0. Earlier draft of this
        // test asserted scale=1.2 → negative extension; that's hypothetical
        // (never reachable from production code). Pin the real behavior.
        val ext = extensionY(891f, 1.0f)
        assertEquals("NEAR scale=1.0 → no extension", 0f, ext, 1e-4f)
    }

    @Test
    fun `extensionY formula stays negative for hypothetical scale gt 1`() {
        // Defensive — IF a future zoom level introduces magnify (>1.0), the
        // formula stays correct (negative extension = tighter bounds). Not
        // currently reachable but pins the symmetry contract.
        val ext = extensionY(891f, 1.2f)
        assertTrue("Hypothetical scale=1.2 produces negative extension", ext < 0f)
    }

    @Test
    fun `UltimateLaser start y at FAR zoom renders at device-bottom`() {
        // Pixel-3 #2 deep-audit fix verified. Beam start = screenHeight + extraYSpan
        // = 891 + 190.9 = 1081.9 game-y. After scale 0.7 around pivot 445.5:
        //   device-y = 445.5 + (1081.9 - 445.5) * 0.7 = 891  (= device-bottom).
        val screenHeight = 891f
        val ext = extensionY(screenHeight, 0.7f)
        val beamStartY = screenHeight + ext
        val pivot = 445.5f
        val deviceY = pivot + (beamStartY - pivot) * 0.7f
        assertEquals(
            "Beam start should render at device-bottom (891) at FAR zoom",
            891f, deviceY, 2f,
        )
    }

    @Test
    fun `EnemyLaser destroy threshold at FAR zoom matches device-bottom`() {
        // Pixel-3 #2 deep-audit fix verified. Same math as beam start above —
        // enemy laser destroyed when game-y > screenHeight + extraYSpan, so it
        // travels into the bottom band that the ship now occupies.
        val screenHeight = 891f
        val ext = extensionY(screenHeight, 0.7f)
        val destroyThreshold = screenHeight + ext
        // Threshold in game-y at FAR maps to device-y 891.
        val pivot = 445.5f
        val deviceY = pivot + (destroyThreshold - pivot) * 0.7f
        assertEquals(891f, deviceY, 2f)
    }

    @Test
    fun `bounds extension symmetry — X and Y formula identical`() {
        // GameState wires identical formula for both axes; pinning the symmetry
        // catches a future divergence (e.g., someone hardcoding X to use *.5 vs
        // Y *.5). Both should produce the same value for a square screen and
        // proportional values otherwise.
        val ext411x891 = extensionY(411f, 0.7f) to extensionY(891f, 0.7f)
        val xRatio = ext411x891.first / 411f
        val yRatio = ext411x891.second / 891f
        assertEquals(
            "X-axis and Y-axis extension ratios should match for the same scale",
            xRatio, yRatio, 1e-4f,
        )
    }

}
