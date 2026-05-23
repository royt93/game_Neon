package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectUI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round 66b — Pure-vector space-rock rendering. Replaces drawImage of
 * ic_space_rock_*.webp with a procedural irregular polygon outline + small
 * inner "crater" dots. Variation per rock is derived from the rock's `id`
 * hash so the same rock keeps the same shape across frames (no per-frame
 * randomness flicker).
 *
 * Visual character: angular asteroid silhouette with violet neon stroke,
 * matching the rest of the neon-vector aesthetic. Rotation (already animated
 * in SpaceRock controller) drives DrawScope.rotate so the asteroid tumbles.
 */
@Composable
fun SpaceObjectCanvas(
    spaceObjects: List<SpaceObjectUI>,
    modifier: Modifier = Modifier,
) {
    if (spaceObjects.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (obj in spaceObjects) {
            drawSpaceObject(obj, density)
        }
    }
}

/** Round 66b — sprite preload no longer needed; stub kept for back-compat. */
@Immutable
@Deprecated("Round 66b — pure-vector SpaceObjectCanvas no longer needs sprites.")
data class SpaceObjectSprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66b — pure-vector SpaceObjectCanvas no longer needs sprites.")
fun rememberSpaceObjectSprites(): SpaceObjectSprites = SpaceObjectSprites()

private fun DrawScope.drawSpaceObject(obj: SpaceObjectUI, density: Density) {
    with(density) {
        val sizePx = obj.size.dp.toPx()
        val xPx = obj.xOffset.dp.toPx()
        val yPx = obj.yOffset.dp.toPx()
        val cx = xPx + sizePx / 2f
        val cy = yPx + sizePx / 2f
        val glowR = (sizePx / 2f) * 1.4f

        // 1. Halo
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

        // 2. Irregular asteroid polygon + craters.
        // Seed from id hash so each rock has a stable shape between frames.
        val seed = obj.id.hashCode()
        rotate(degrees = obj.rotation, pivot = Offset(cx, cy)) {
            drawAsteroidShape(cx, cy, sizePx / 2f, seed)
        }
    }
}

/**
 * Draws an asteroid silhouette: 8-12 vertex polygon with radius jitter so the
 * outline looks rough, filled with dark violet, neon-stroked + 2-3 inner dots
 * to suggest craters.
 */
private fun DrawScope.drawAsteroidShape(cx: Float, cy: Float, baseR: Float, seed: Int) {
    val vertexCount = 10
    val rand = kotlin.random.Random(seed)
    val angles = FloatArray(vertexCount) { i ->
        // Even angle spacing + small jitter for asymmetry.
        val baseAngle = (2.0 * Math.PI * i / vertexCount).toFloat()
        baseAngle + (rand.nextFloat() - 0.5f) * 0.25f
    }
    val radii = FloatArray(vertexCount) { i ->
        // Jitter radius 80-110% of base.
        baseR * (0.80f + rand.nextFloat() * 0.30f)
    }
    val outline = Path().apply {
        for (i in 0 until vertexCount) {
            val x = cx + radii[i] * cos(angles[i])
            val y = cy + radii[i] * sin(angles[i])
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    // Filled dark interior (slightly transparent for depth) + neon stroke.
    drawPath(path = outline, color = Color(0xFF1A0B2E))               // deep violet body
    drawPath(
        path = outline,
        color = NeonViolet,
        style = Stroke(width = baseR * 0.10f),
    )
    // 2-3 small crater dots inside.
    val craterCount = 2 + (rand.nextInt(2))                            // 2 or 3
    repeat(craterCount) {
        val craterAngle = rand.nextFloat() * Math.PI.toFloat() * 2f
        val craterDist = baseR * (0.20f + rand.nextFloat() * 0.40f)
        val craterR = baseR * (0.06f + rand.nextFloat() * 0.06f)
        drawCircle(
            color = NeonViolet.copy(alpha = 0.65f),
            radius = craterR,
            center = Offset(cx + craterDist * cos(craterAngle), cy + craterDist * sin(craterAngle)),
            style = Stroke(width = baseR * 0.05f),
        )
    }
}
