package com.tranphuloi.neon.ui.game.background

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import com.tranphuloi.neon.utils.Logger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native Android View renderer for [SpaceBackground]. Replaces Compose Canvas to
 * eliminate Compose draw-pipeline overhead (lambda capture, recomposition, modifier
 * chain, value-class color allocation).
 *
 * Performance gains over Compose Canvas:
 *  - Native `Canvas.drawCircle` is ~3-5× faster (C++ Skia vs Compose draw scope wrapping)
 *  - Single Paint instance reused across all entities (no per-frame allocation)
 *  - Galaxy Path stored as field (no remember dance)
 *  - `invalidate()` is per-View, not whole-tree recomposition
 *  - No Composable function re-execution per state change
 */
class SpaceBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
) : View(context, attrs) {

    init {
        Logger.d("SpaceBackgroundView created (native canvas)")
        // Avoid spurious focus / touch handling (this view is decoration only).
        isFocusable = false
        isFocusableInTouchMode = false
    }

    private val density: Float = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val galaxyPath = Path()
    private var galaxyPathBuiltForCenter: Long = 0L
    private var lastLoggedSize: Long = 0L

    // Self-animation: track last frame timestamp to compute delta-time.
    // postInvalidateOnAnimation drives ~display-rate redraw decoupled from the
    // game tick controller — fixes "stars jumping" caused by 15Hz controller tick.
    private var lastFrameNs: Long = 0L

    /**
     * When false, view stops self-animating (used during pause to save CPU).
     * Setting to true after pause resets [lastFrameNs] so the first delta-time isn't huge.
     */
    var running: Boolean = true
        set(value) {
            val wasRunning = field
            field = value
            if (value && !wasRunning) {
                lastFrameNs = 0L
                postInvalidateOnAnimation()
            }
        }

    /**
     * Latest [BackgroundState] snapshot. Setting this triggers a single [invalidate]
     * which schedules ONE redraw on next frame — much cheaper than Compose
     * recomposition.
     */
    var state: BackgroundState = BackgroundState(
        stars = emptyList(),
        dust = emptyList(),
        nebula = emptyList(),
        comet = null,
        galaxy = null,
    )
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val sizeKey = (w.toLong() shl 32) or h.toLong()
        if (lastLoggedSize != sizeKey) {
            lastLoggedSize = sizeKey
            Logger.d("SpaceBackgroundView onDraw size: ${w.toInt()}px × ${h.toInt()}px (density=$density, NATIVE canvas)")
        }

        // Delta-time since last frame. Cap at 50ms to absorb jank / first frame.
        val nowNs = System.nanoTime()
        val deltaSec = if (lastFrameNs == 0L) 0f
        else ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = nowNs

        // Advance entity simulation in-place (mutates the shared list held by controller).
        if (deltaSec > 0f) {
            state.stars.forEach { it.advance(deltaSec) }
            state.dust.forEach { it.advance(deltaSec) }
            state.nebula.forEach { it.advance(deltaSec) }
            state.galaxy?.advance(deltaSec)
        }

        // 1) Nebula blobs.
        state.nebula.forEach { blob ->
            val alpha = (blob.baseAlpha * blob.alphaMultiplier()).coerceIn(0f, 1f)
            if (alpha <= 0.01f) return@forEach
            val cx = blob.xCenter * density
            val cy = blob.yCenter * density
            val r = blob.radius * density
            val baseColor = blob.color()
            val argb = colorWithAlpha(baseColor.red, baseColor.green, baseColor.blue, alpha)
            paint.shader = RadialGradient(
                cx, cy, r,
                argb,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
        }

        // 2) Galaxy spiral — path cached (built once unless geometry changes).
        state.galaxy?.let { g ->
            val gcx = g.xCenter * density
            val gcy = g.yCenter * density
            val gr = g.radius * density
            val galaxyKey = (gcx.toLong() shl 32) or gcy.toLong()
            if (galaxyPathBuiltForCenter != galaxyKey) {
                galaxyPathBuiltForCenter = galaxyKey
                galaxyPath.reset()
                val arms = 2
                val turns = 1.5f
                val pointsPerTurn = 12                                 // even fewer (was 16) — native renders cheaper anyway
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
                Logger.d("SpaceBackgroundView: galaxy path cached ($totalPoints × $arms points)")
            }

            canvas.save()
            canvas.rotate(g.rotation, gcx, gcy)
            // Core: simple solid alpha (no Brush).
            val coreColor = g.coreColor()
            paint.color = colorWithAlpha(coreColor.red, coreColor.green, coreColor.blue, 0.18f)
            canvas.drawCircle(gcx, gcy, gr * 0.4f, paint)
            // Arms.
            val armColor = g.armColor()
            strokePaint.color = colorWithAlpha(armColor.red, armColor.green, armColor.blue, 0.15f)
            strokePaint.strokeWidth = 1.5f * density
            canvas.drawPath(galaxyPath, strokePaint)
            canvas.restore()
        }

        // 3) Stars — back-to-front. All drawn as solid circles (no Brush).
        state.stars.forEach { star ->
            val alpha = if (star.twinkles) {
                star.baseAlpha * (0.7f + 0.3f * sin(star.twinklePhase))
            } else {
                star.baseAlpha
            }
            val baseColor = star.color()
            paint.color = colorWithAlpha(baseColor.red, baseColor.green, baseColor.blue, alpha.coerceIn(0f, 1f))
            val cx = star.xOffset * density
            val cy = star.yOffset * density
            val radius = if (star.layer >= 4) star.baseSize * 0.7f * density
                         else star.baseSize * 0.5f * density
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // 4) Comet sparkle trail.
        state.comet?.let { c ->
            val cometColor = c.color()
            val cometRgb = Triple(cometColor.red, cometColor.green, cometColor.blue)
            c.sparkles.forEach { spark ->
                paint.color = colorWithAlpha(cometRgb.first, cometRgb.second, cometRgb.third, (spark.alpha * 0.7f).coerceIn(0f, 1f))
                canvas.drawCircle(spark.xOffset * density, spark.yOffset * density, spark.size * density, paint)
            }
            // Comet head — radial gradient (rare event so OK).
            if (c.t < 1f) {
                val (px, py) = c.position()
                val hx = px * density
                val hy = py * density
                val headR = 14f * density
                paint.shader = RadialGradient(
                    hx, hy, headR,
                    colorWithAlpha(cometRgb.first, cometRgb.second, cometRgb.third, 0.9f),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawCircle(hx, hy, headR, paint)
                paint.shader = null
                paint.color = Color.WHITE
                canvas.drawCircle(hx, hy, 3f * density, paint)
            }
        }

        // 5) Dust particles.
        state.dust.forEach { d ->
            paint.color = colorWithAlpha(1f, 1f, 1f, d.alpha)
            canvas.drawCircle(d.xOffset * density, d.yOffset * density, 1f * density, paint)
        }

        // Schedule next display-rate frame (decoupled from game tick).
        if (running) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Stop animation loop when view is removed from hierarchy.
        running = false
    }

    /** Pack RGB float (0..1) + alpha into ARGB Int for native Paint. */
    private fun colorWithAlpha(r: Float, g: Float, b: Float, a: Float): Int {
        val ai = (a * 255f).toInt().coerceIn(0, 255)
        val ri = (r * 255f).toInt().coerceIn(0, 255)
        val gi = (g * 255f).toInt().coerceIn(0, 255)
        val bi = (b * 255f).toInt().coerceIn(0, 255)
        return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
    }
}
