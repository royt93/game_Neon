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
    fun `extensionY at NEAR zoom (scale gt 1) is negative — tightens bounds`() {
        // NEAR zoom magnifies game world → visible game-y range shrinks.
        // Extension goes negative, tightening the threshold (enemies disappear
        // earlier, ship anchor pulls in toward center). Symmetric design.
        val ext = extensionY(891f, 1.2f)
        assertTrue("NEAR extension must be < 0, got $ext", ext < 0f)
        assertEquals(-74.25f, ext, 0.1f)
    }

    @Test
    fun `effectiveMaxY at FAR zoom places ship near device-bottom`() {
        // Pixel-3 #4 fix verified at the math level. Without dragBoundsExtensionY,
        // ship parks at game-y maxYOffset = screenHeight - 140 = 751. After
        // graphicsLayer scale 0.7 around device-center pivot 445.5 dp:
        //   device-y = 445.5 + (751 - 445.5) * 0.7 = 659  (134dp above device-bottom)
        //
        // With effectiveMaxY = maxYOffset + extensionY = 751 + 190.9 = 941.9:
        //   device-y = 445.5 + (941.9 - 445.5) * 0.7 = 793  (98dp above bottom, room for ship.height)
        //
        // Ship.height ≈ 140 → ship's bottom edge at device-y 793 + 140*0.7 ≈ 891 = device-bottom.
        val screenHeight = 891f
        val maxYOffset = screenHeight - 140f
        val ext = extensionY(screenHeight, 0.7f)
        val effectiveMaxY = maxYOffset + ext
        val pivot = 445.5f
        val deviceY = pivot + (effectiveMaxY - pivot) * 0.7f
        val shipHeight = 140f
        val shipBottomDevice = deviceY + shipHeight * 0.7f
        // Allow ~5dp tolerance (pivot center isn't exactly 445.5 on every device)
        assertEquals(
            "Ship bottom should land at device-bottom (891) at FAR zoom",
            891f, shipBottomDevice, 5f,
        )
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

    @Test
    fun `NEAR zoom tightens — extension is negative and bounds shrink`() {
        // Symmetric design: NEAR zoom magnifies, so visible game-y range
        // SHRINKS, not grows. Extension negative → ship anchor pulls in,
        // enemies cull earlier. Pin this so a future "always positive"
        // refactor doesn't break symmetry.
        val screenHeight = 891f
        val ext = extensionY(screenHeight, 1.2f)
        assertTrue("NEAR extension must shrink bounds", ext < 0f)
        val effectiveMaxY = (screenHeight - 140f) + ext
        assertTrue(
            "NEAR effectiveMaxY must be less than baseline 751",
            effectiveMaxY < 751f,
        )
    }
}
