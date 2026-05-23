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
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectUI

/**
 * Round 59 — Canvas-based draw of a space-object list. Mirrors the
 * [LaserCanvas] / [EnemyCanvas] pattern: one Canvas + DrawScope pass replaces
 * a `forEach { Image(painterResource, Modifier.size+offset+neonGlow+rotate) }`
 * Compose loop.
 *
 * Visual parity with the pre-refactor block in GameWorld:
 *   neonGlow(NeonViolet, 0.35, 1.4) → drawCircle(radialGradient) same recipe.
 *   rotate(it.rotation) → DrawScope.rotate(pivot = sprite center).
 */
@Composable
fun SpaceObjectCanvas(
    spaceObjects: List<SpaceObjectUI>,
    sprites: SpaceObjectSprites,
    modifier: Modifier = Modifier,
) {
    if (spaceObjects.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (obj in spaceObjects) {
            drawSpaceObject(obj, sprites, density)
        }
    }
}

@Immutable
data class SpaceObjectSprites(
    val byDrawableId: Map<Int, ImageBitmap>,
)

@Composable
fun rememberSpaceObjectSprites(): SpaceObjectSprites {
    val rock1 = ImageBitmap.imageResource(R.drawable.ic_space_rock_1)
    val rock2 = ImageBitmap.imageResource(R.drawable.ic_space_rock_2)
    val rock3 = ImageBitmap.imageResource(R.drawable.ic_space_rock_3)
    val rock4 = ImageBitmap.imageResource(R.drawable.ic_space_rock_4)
    return remember(rock1, rock2, rock3, rock4) {
        SpaceObjectSprites(
            mapOf(
                R.drawable.ic_space_rock_1 to rock1,
                R.drawable.ic_space_rock_2 to rock2,
                R.drawable.ic_space_rock_3 to rock3,
                R.drawable.ic_space_rock_4 to rock4,
            )
        )
    }
}

private fun DrawScope.drawSpaceObject(
    obj: SpaceObjectUI,
    sprites: SpaceObjectSprites,
    density: Density,
) {
    val bitmap = sprites.byDrawableId[obj.drawableId] ?: return
    with(density) {
        val sizePx = obj.size.dp.toPx()
        val xPx = obj.xOffset.dp.toPx()
        val yPx = obj.yOffset.dp.toPx()
        val cx = xPx + sizePx / 2f
        val cy = yPx + sizePx / 2f
        val glowR = (sizePx / 2f) * 1.4f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonViolet.copy(alpha = 0.35f),
                    NeonViolet.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        val dstOffset = IntOffset(xPx.toInt(), yPx.toInt())
        val dstSize = IntSize(sizePx.toInt(), sizePx.toInt())
        if (obj.rotation != 0f) {
            rotate(degrees = obj.rotation, pivot = Offset(cx, cy)) {
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
