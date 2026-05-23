package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI

/**
 * Round 59 — Canvas-based draw of the mineral list. All minerals share one
 * drawable (`ic_mineral`) and have no glow / rotation, just alpha + position.
 * Volume is moderate (~10-15 simultaneous after explosions) so the saving
 * vs the prior `forEach { Box { Icon } }` block is small in isolation, but
 * grouped with SpaceObjectCanvas + BoosterCanvas it closes the last
 * per-entity Composable forEach blocks in GameWorld's main render tree.
 *
 * Render size mirrors the pre-refactor block exactly: 25dp square (the
 * Icon's hard-coded `.size(25.dp)` modifier; `width` field on Mineral was
 * not actually consumed there, so we keep that quirk for parity).
 */
@Composable
fun MineralCanvas(
    minerals: List<MineralUI>,
    sprite: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    if (minerals.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (m in minerals) {
            drawMineral(m, sprite, density)
        }
    }
}

@Immutable
data class MineralSprite(val bitmap: ImageBitmap)

@Composable
fun rememberMineralSprite(): MineralSprite {
    val bitmap = ImageBitmap.imageResource(R.drawable.ic_mineral)
    return remember(bitmap) { MineralSprite(bitmap) }
}

private fun DrawScope.drawMineral(
    mineral: MineralUI,
    sprite: ImageBitmap,
    density: Density,
) {
    with(density) {
        val sizePx = 25.dp.toPx()
        val xPx = mineral.xOffset.dp.toPx()
        val yPx = mineral.yOffset.dp.toPx()
        drawImage(
            image = sprite,
            dstOffset = IntOffset(xPx.toInt(), yPx.toInt()),
            dstSize = IntSize(sizePx.toInt(), sizePx.toInt()),
            alpha = mineral.alpha.coerceIn(0f, 1f),
            filterQuality = FilterQuality.Low,
        )
    }
}
