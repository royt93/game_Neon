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
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
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

        // Round 71 (Issue 5) — 3-family asteroid: pick family by seed % 3.
        val seed = obj.id.hashCode()
        val family = AsteroidFamily.entries[Math.floorMod(seed, AsteroidFamily.entries.size)]

        // 1. Halo (family color)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    family.glow.copy(alpha = 0.35f),
                    family.glow.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        // 2. Family-aware asteroid: variable vertex 8-14 + family-specific
        //    body color / stroke / crater style.
        rotate(degrees = obj.rotation, pivot = Offset(cx, cy)) {
            drawAsteroidShape(cx, cy, sizePx / 2f, seed, family)
        }
    }
}

/**
 * Round 71 (Issue 5) — 3 asteroid families distinguished by color + texture
 * style. Sub-style variation comes from variable vertex count + per-seed jitter.
 *
 *   ROCK  : violet neon stroke, dark violet body, 10-14 vertex (jagged), 2-3 craters
 *   ICE   : cyan stroke, navy body, 8-10 vertex (smooth crystal), 1-2 sparkle highlights
 *   METAL : gold stroke, charcoal body, 12-14 vertex (angular plated), 3-4 rivet dots
 */
private enum class AsteroidFamily(
    val body: Color,
    val stroke: Color,
    val glow: Color,
    val vertexRange: IntRange,
    val craterStyle: CraterStyle,
) {
    ROCK(
        body = Color(0xFF1A0B2E),
        stroke = NeonViolet,
        glow = NeonViolet,
        vertexRange = 10..14,
        craterStyle = CraterStyle.RING,
    ),
    ICE(
        body = Color(0xFF071E2E),
        stroke = NeonCyan,
        glow = NeonCyan,
        vertexRange = 8..10,
        craterStyle = CraterStyle.SPARKLE,
    ),
    METAL(
        body = Color(0xFF1F1A0B),
        stroke = NeonGold,
        glow = NeonGold,
        vertexRange = 12..14,
        craterStyle = CraterStyle.RIVET,
    );
}

private enum class CraterStyle { RING, SPARKLE, RIVET }

/**
 * Draws an asteroid silhouette: 8-12 vertex polygon with radius jitter so the
 * outline looks rough, filled with dark violet, neon-stroked + 2-3 inner dots
 * to suggest craters.
 */
private fun DrawScope.drawAsteroidShape(
    cx: Float, cy: Float, baseR: Float, seed: Int, family: AsteroidFamily,
) {
    val rand = kotlin.random.Random(seed)
    // Round 71 (Issue 5) — variable vertex count per family.
    val vertexCount = family.vertexRange.let { it.first + rand.nextInt(it.last - it.first + 1) }
    val angleJitter = when (family) {
        AsteroidFamily.ROCK -> 0.28f
        AsteroidFamily.ICE -> 0.15f          // smoother crystal
        AsteroidFamily.METAL -> 0.20f
    }
    val radiusVariance = when (family) {
        AsteroidFamily.ROCK -> 0.30f         // jagged
        AsteroidFamily.ICE -> 0.18f          // smooth
        AsteroidFamily.METAL -> 0.22f
    }
    val angles = FloatArray(vertexCount) { i ->
        val baseAngle = (2.0 * Math.PI * i / vertexCount).toFloat()
        baseAngle + (rand.nextFloat() - 0.5f) * angleJitter
    }
    val radii = FloatArray(vertexCount) {
        baseR * (1f - radiusVariance / 2f + rand.nextFloat() * radiusVariance)
    }
    val outline = Path().apply {
        for (i in 0 until vertexCount) {
            val x = cx + radii[i] * cos(angles[i])
            val y = cy + radii[i] * sin(angles[i])
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = outline, color = family.body)
    drawPath(
        path = outline,
        color = family.stroke,
        style = Stroke(width = baseR * 0.10f),
    )

    // Crater style per family.
    when (family.craterStyle) {
        CraterStyle.RING -> {
            val n = 2 + rand.nextInt(2)
            repeat(n) {
                val a = rand.nextFloat() * Math.PI.toFloat() * 2f
                val d = baseR * (0.20f + rand.nextFloat() * 0.40f)
                val r = baseR * (0.06f + rand.nextFloat() * 0.06f)
                drawCircle(
                    color = family.stroke.copy(alpha = 0.65f),
                    radius = r,
                    center = Offset(cx + d * cos(a), cy + d * sin(a)),
                    style = Stroke(width = baseR * 0.05f),
                )
            }
        }
        CraterStyle.SPARKLE -> {
            // Cross-shaped sparkles for ice — 1-2 highlights.
            val n = 1 + rand.nextInt(2)
            repeat(n) {
                val a = rand.nextFloat() * Math.PI.toFloat() * 2f
                val d = baseR * (0.15f + rand.nextFloat() * 0.45f)
                val px = cx + d * cos(a)
                val py = cy + d * sin(a)
                val len = baseR * (0.10f + rand.nextFloat() * 0.06f)
                drawLine(family.stroke.copy(alpha = 0.85f),
                    Offset(px - len, py), Offset(px + len, py), strokeWidth = baseR * 0.04f)
                drawLine(family.stroke.copy(alpha = 0.85f),
                    Offset(px, py - len), Offset(px, py + len), strokeWidth = baseR * 0.04f)
            }
        }
        CraterStyle.RIVET -> {
            // Filled small dots for metal "rivets" — 3-4.
            val n = 3 + rand.nextInt(2)
            repeat(n) {
                val a = rand.nextFloat() * Math.PI.toFloat() * 2f
                val d = baseR * (0.20f + rand.nextFloat() * 0.45f)
                drawCircle(
                    color = family.stroke,
                    radius = baseR * 0.06f,
                    center = Offset(cx + d * cos(a), cy + d * sin(a)),
                )
            }
        }
    }
}
