package com.tranphuloi.neon.ui.game.background

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.io.Serializable

/**
 * Star with parallax depth layer + spectral color + optional twinkle.
 * `baseColorArgb` packed as Long to keep this Serializable for rememberSaveable
 * (Color is a value class wrapping ULong — Long packing is the standard workaround).
 */
@Keep
@Immutable
data class BgStar(
    var xOffset: Float,
    var yOffset: Float,
    val baseSize: Float,
    val maxYOffset: Float,
    val layer: Int,                 // 0=farthest, 4=nearest
    val ySpeed: Float,
    val baseAlpha: Float,
    val baseColorArgb: Long,        // packed ARGB (Long-safe across process death)
    val twinkles: Boolean,
    var twinklePhase: Float = 0f,
    val twinkleSpeed: Float = 1f,
) : Serializable {

    fun tick() {
        yOffset += ySpeed
        if (yOffset > maxYOffset) yOffset = -baseSize
        if (twinkles) {
            twinklePhase += twinkleSpeed
            // Wrap to keep float small/precise.
            if (twinklePhase > 6.28318f) twinklePhase -= 6.28318f
        }
    }

    /** Cached non-allocating color accessor.
     *  IMPORTANT: must use `Color(Long)` top-level function (proper ARGB packing),
     *  NOT `Color(ULong)` primary constructor which expects internal repr w/ color space bits. */
    fun color(): Color = Color(baseColorArgb)
}

/**
 * Drifting nebula blob — large soft radial gradient.
 * Slowly pulses alpha; position stable to keep ambience subtle.
 */
@Keep
@Immutable
data class NebulaBlob(
    val xCenter: Float,
    val yCenter: Float,
    val radius: Float,
    val colorArgb: Long,
    val baseAlpha: Float,
    var pulsePhase: Float,
    val pulseSpeed: Float,
) : Serializable {

    fun tick() {
        pulsePhase += pulseSpeed
        if (pulsePhase > 6.28318f) pulsePhase -= 6.28318f
    }

    fun color(): Color = Color(colorArgb)

    /** Current alpha multiplier in [0.6, 1.0] of baseAlpha. */
    fun alphaMultiplier(): Float = 0.8f + 0.2f * kotlin.math.sin(pulsePhase)
}

/**
 * Tiny dust particle — fastest layer foreground, very small size.
 */
@Keep
@Immutable
data class DustParticle(
    var xOffset: Float,
    var yOffset: Float,
    val ySpeed: Float,
    val maxYOffset: Float,
    val alpha: Float,
) : Serializable {

    fun tick() {
        yOffset += ySpeed
        if (yOffset > maxYOffset) yOffset = -2f
    }
}

/**
 * One-shot comet: parabolic trajectory + sparkle trail.
 * Lifetime tracked via [t]; controller removes when [t] >= 1f.
 */
@Keep
@Immutable
data class Comet(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val controlX: Float,
    val controlY: Float,
    var t: Float,                   // 0..1 progress along bezier
    val tStep: Float,               // increment per tick
    val colorArgb: Long,
    // Immutable list, reassigned atomically each tick — prevents
    // ConcurrentModificationException between IO tick + main render thread.
    var sparkles: List<Sparkle>,
) : Serializable {

    fun position(): Pair<Float, Float> {
        // Quadratic bezier B(t) = (1-t)² P0 + 2(1-t)t P1 + t² P2
        val omt = 1f - t
        val x = omt * omt * startX + 2f * omt * t * controlX + t * t * endX
        val y = omt * omt * startY + 2f * omt * t * controlY + t * t * endY
        return x to y
    }

    fun color(): Color = Color(colorArgb)

    fun isFinished(): Boolean = t >= 1f && sparkles.isEmpty()
}

@Keep
@Immutable
data class Sparkle(
    val xOffset: Float,
    val yOffset: Float,
    val alpha: Float,
    val size: Float,
) : Serializable {

    companion object {
        const val SPARKLE_FADE: Float = 0.04f
    }
}

/**
 * Distant galaxy spiral. Single rotation per minute (~very slow).
 */
@Keep
@Immutable
data class Galaxy(
    val xCenter: Float,
    val yCenter: Float,
    val radius: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val coreColorArgb: Long,
    val armColorArgb: Long,
) : Serializable {

    fun tick() {
        rotation += rotationSpeed
        if (rotation > 360f) rotation -= 360f
    }

    fun coreColor(): Color = Color(coreColorArgb)
    fun armColor(): Color = Color(armColorArgb)
}
