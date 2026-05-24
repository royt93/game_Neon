package com.tranphuloi.neon.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Round 78 (Issue #6 perf) — Cheap 3-circle approximation of a radial gradient halo.
 *
 * The prior implementation called [androidx.compose.ui.graphics.Brush.radialGradient]
 * per-entity per-frame. Each call allocates: 3 Color objects + 1 List + 1
 * RadialGradient brush + (during draw) 1 native Shader. With 30 enemies + 80
 * lasers + space objects + boosters, the brush construction alone produced
 * ~500 allocations/frame, which the user reported as FPS dropping to 30-50 in
 * dense scenes (PERF render log).
 *
 * This helper draws 3 stacked translucent circles with decreasing radii. Each
 * call allocates only 3 Color.copy values (data class, fast) and zero shaders.
 * Visually the result is close: solid alpha rings instead of a smooth gradient,
 * but at sub-30px halo sizes the steps are visually indistinguishable.
 *
 * For a single-instance high-frequency draw (boss aura, ship shield) the
 * gradient is still acceptable and we keep it.
 */
fun DrawScope.drawSoftHalo(
    color: Color,
    baseAlpha: Float,
    radius: Float,
    center: Offset,
) {
    if (baseAlpha <= 0f || radius <= 0f) return
    val a = baseAlpha.coerceIn(0f, 1f)
    drawCircle(color.copy(alpha = a * 0.18f), radius, center)
    drawCircle(color.copy(alpha = a * 0.32f), radius * 0.72f, center)
    drawCircle(color.copy(alpha = a * 0.50f), radius * 0.45f, center)
}
