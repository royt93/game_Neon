package com.tranphuloi.neon.ui.game.background

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import com.tranphuloi.neon.utils.Logger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Single Canvas that renders the entire space ambience layer:
 *   nebula → galaxy → 5 star layers (back-to-front) → comet → sparkle trail → dust
 *
 * Performance design:
 *   - One Canvas instance instead of one-per-entity (was 31 separate Canvas before).
 *   - Far stars use plain `drawCircle(color)` — cheapest path on GPU.
 *   - Only nearest stars (layer 4) + comet + nebula use radial gradient `Brush`.
 *   - Star colors pre-packed as ARGB Long → wrapped in Color value class on demand
 *     (Color is a value class wrapping ULong — zero allocation).
 *   - Galaxy spiral path is REMEMBERED — built once, transformed each frame via rotate{}.
 */
@Composable
fun SpaceBackground(
    state: BackgroundState,
    modifier: Modifier = Modifier,
) {
    // Entity positions are stored in DP-space (range matches configuration.screenWidthDp).
    // Canvas drawScope works in PIXELS, so we must scale every coord by density.
    val density = LocalDensity.current.density
    // Galaxy path is identical every frame except for rotation — cache it.
    val galaxyPath = remember { Path() }
    // Log canvas size once + every time it changes — confirms full-screen coverage.
    val lastLoggedSize = remember { mutableLongStateOf(0L) }

    Canvas(modifier = modifier) {
        val canvasW = size.width
        val canvasH = size.height
        // Encode (w,h) as a single Long key to detect changes cheaply.
        val sizeKey = (canvasW.toLong() shl 32) or canvasH.toLong()
        if (lastLoggedSize.longValue != sizeKey) {
            lastLoggedSize.longValue = sizeKey
            Logger.d("SpaceBackground draw size: ${canvasW.toInt()}px × ${canvasH.toInt()}px (density=$density, dp=${(canvasW / density).toInt()}×${(canvasH / density).toInt()})")
        }

        // 1) Nebula blobs (dimmest, farthest layer).
        state.nebula.forEach { blob ->
            val alpha = blob.baseAlpha * blob.alphaMultiplier()
            val cx = blob.xCenter * density
            val cy = blob.yCenter * density
            val r = blob.radius * density
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blob.color().copy(alpha = alpha),
                        blob.color().copy(alpha = alpha * 0.5f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }

        // 2) Galaxy spiral.
        state.galaxy?.let { g ->
            val gcx = g.xCenter * density
            val gcy = g.yCenter * density
            val gr = g.radius * density
            galaxyPath.reset()
            // Build a 2-arm logarithmic spiral (cheap approximation).
            val arms = 2
            val turns = 1.5f
            val pointsPerTurn = 24
            val totalPoints = (turns * pointsPerTurn).toInt()
            for (a in 0 until arms) {
                val armOffset = (PI.toFloat() * 2f / arms) * a
                for (i in 0..totalPoints) {
                    val tt = i.toFloat() / totalPoints
                    val theta = tt * turns * 2f * PI.toFloat() + armOffset
                    val rr = tt * gr
                    val px = gcx + rr * cos(theta)
                    val py = gcy + rr * sin(theta)
                    if (i == 0) galaxyPath.moveTo(px, py) else galaxyPath.lineTo(px, py)
                }
            }
            rotate(degrees = g.rotation, pivot = Offset(gcx, gcy)) {
                // Soft glow core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            g.coreColor().copy(alpha = 0.30f),
                            g.coreColor().copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(gcx, gcy),
                        radius = gr * 0.6f,
                    ),
                    radius = gr * 0.6f,
                    center = Offset(gcx, gcy),
                )
                // Spiral arms
                drawPath(
                    path = galaxyPath,
                    color = g.armColor().copy(alpha = 0.18f),
                    style = Stroke(width = 1.5f * density),
                )
            }
        }

        // 3) Stars — back-to-front (layer 0 → 4). Deeper layers use plain drawCircle.
        state.stars.forEach { star ->
            // Compute alpha (with twinkle for layer 4).
            val alpha = if (star.twinkles) {
                val s = sin(star.twinklePhase)
                star.baseAlpha * (0.7f + 0.3f * s)
            } else {
                star.baseAlpha
            }
            val color = star.color().copy(alpha = alpha)
            val cx = star.xOffset * density
            val cy = star.yOffset * density

            if (star.layer >= 4) {
                // Bright near-layer star: halo + core.
                val haloR = star.baseSize * 1.6f * density
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = haloR,
                    ),
                    radius = haloR,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = color,
                    radius = star.baseSize * 0.5f * density,
                    center = Offset(cx, cy),
                )
            } else {
                drawCircle(
                    color = color,
                    radius = star.baseSize * 0.5f * density,
                    center = Offset(cx, cy),
                )
            }
        }

        // 4) Comet sparkle trail.
        state.comet?.let { c ->
            val cometColor = c.color()
            c.sparkles.forEach { spark ->
                drawCircle(
                    color = cometColor.copy(alpha = spark.alpha * 0.7f),
                    radius = spark.size * density,
                    center = Offset(spark.xOffset * density, spark.yOffset * density),
                )
            }
            // 5) Comet head.
            if (c.t < 1f) {
                val (px, py) = c.position()
                val hx = px * density
                val hy = py * density
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cometColor.copy(alpha = 0.95f),
                            cometColor.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        center = Offset(hx, hy),
                        radius = 14f * density,
                    ),
                    radius = 14f * density,
                    center = Offset(hx, hy),
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f * density,
                    center = Offset(hx, hy),
                )
            }
        }

        // 6) Dust particles.
        state.dust.forEach { d ->
            drawCircle(
                color = Color.White.copy(alpha = d.alpha),
                radius = 1f * density,
                center = Offset(d.xOffset * density, d.yOffset * density),
            )
        }
    }
}
