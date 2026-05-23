package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI

/**
 * Round 49 — Canvas-based draw of a laser list.
 *
 * Replaces a `forEach { Image(painterResource, Modifier.neonGlow(...)) }` Compose
 * loop with a single Canvas + DrawScope pass. For N in-flight lasers this
 * drops from N Composable subtrees to one. Each laser becomes a `drawImage` +
 * `drawCircle(radialGradient)` — equivalent visuals to the previous
 * `Modifier.neonGlow`.
 *
 * Z-order: callers invoke this multiple times to preserve the prior layering
 * (ship + ultimate lasers go behind enemies; enemy lasers go in front).
 * Sprites are pre-loaded once into [LaserSprites] at the [GameWorld] level so
 * each Canvas reuses the same ImageBitmap cache without per-call resolution.
 */
@Composable
fun LaserCanvas(
    lasers: List<LaserUI>,
    sprites: LaserSprites,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    modifier: Modifier = Modifier,
) {
    if (lasers.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (laser in lasers) {
            drawLaser(laser, sprites, glow, intensity, radiusFactor, density)
        }
    }
}

@Immutable
data class LaserSprites(
    val byDrawableId: Map<Int, ImageBitmap>,
)

@Composable
fun rememberLaserSprites(): LaserSprites {
    val blue7 = ImageBitmap.imageResource(R.drawable.ic_laser_blue_7)
    val blue11 = ImageBitmap.imageResource(R.drawable.ic_laser_blue_11)
    val red8 = ImageBitmap.imageResource(R.drawable.ic_laser_red_8)
    val red14 = ImageBitmap.imageResource(R.drawable.ic_laser_red_14)
    val red16 = ImageBitmap.imageResource(R.drawable.ic_laser_red_16)
    return remember(blue7, blue11, red8, red14, red16) {
        LaserSprites(
            mapOf(
                R.drawable.ic_laser_blue_7 to blue7,
                R.drawable.ic_laser_blue_11 to blue11,
                R.drawable.ic_laser_red_8 to red8,
                R.drawable.ic_laser_red_14 to red14,
                R.drawable.ic_laser_red_16 to red16,
            )
        )
    }
}

private fun DrawScope.drawLaser(
    laser: LaserUI,
    sprites: LaserSprites,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    density: Density,
) {
    val bitmap = sprites.byDrawableId[laser.drawableId] ?: return
    with(density) {
        val xPx = laser.xOffset.dp.toPx()
        val yPx = laser.yOffset.dp.toPx()
        val wPx = laser.width.dp.toPx()
        val hPx = laser.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f
        val glowR = (minOf(wPx, hPx) / 2f) * radiusFactor

        // Glow (same recipe as Modifier.neonGlow — radial gradient with two
        // opaque stops then transparent edge).
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = intensity),
                    glow.copy(alpha = intensity * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        val dstOffset = IntOffset(xPx.toInt(), yPx.toInt())
        val dstSize = IntSize(wPx.toInt(), hPx.toInt())
        if (laser.rotation != 0f) {
            rotate(degrees = laser.rotation, pivot = Offset(cx, cy)) {
                drawImage(
                    image = bitmap,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    filterQuality = FilterQuality.Low,
                )
            }
        } else {
            drawImage(
                image = bitmap,
                dstOffset = dstOffset,
                dstSize = dstSize,
                filterQuality = FilterQuality.Low,
            )
        }
    }
}
