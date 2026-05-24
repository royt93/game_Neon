package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round 72 (Issue 4 user audit) — PURE-CANVAS explosion thay GIF
 * `anim_explosion.gif` (loaded qua Coil 3 AnimatedImageDecoder trước fix).
 *
 * Layers per explosion (450ms lifespan, ticks 30fps):
 *   1. CORE FIREBALL — radial gradient white→gold→orange→transparent.
 *      Bán kính 0 → max trong 0..0.4s, alpha fade 0.4..1.0.
 *   2. 12 RADIAL SPARKS — straight lines từ center, length 0 → max, taper width.
 *   3. SHOCKWAVE RING — expanding circle, stroke tapers, alpha fades.
 *
 * Khi finish: ExplosionBurstOverlay đã handle sparks + ring nhưng dùng cùng
 * với GIF. Nay ExplosionCanvas thay GIF — sparks + ring là 1 lớp duy nhất,
 * không cần ExplosionBurstOverlay nữa (file đó được giữ làm legacy có thể
 * xoá ở round sau).
 */
@Composable
fun ExplosionCanvas(explosion: Explosion) {
    val density = LocalDensity.current.density
    var nowMillis by remember(explosion.startTimeMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(explosion.startTimeMillis) {
        // 30fps tick cho 450ms lifespan (matches old ExplosionBurstOverlay).
        repeat(15) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val t = explosion.progress(nowMillis)
    if (t >= 1f) return

    val sizeDp = (explosion.size + 60f).dp                      // extra cho shockwave ring
    val offsetX = (explosion.xOffset - 30f).dp
    val offsetY = (explosion.yOffset - 30f).dp

    Canvas(
        modifier = Modifier.offset(offsetX, offsetY).size(sizeDp),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = explosion.size * 0.5f * density

        // ── 1. Core fireball — radial gradient ──
        // Bán kính peak ở t=0.4, sau đó shrink + fade.
        val fireR = if (t < 0.4f) maxR * (t / 0.4f)
            else maxR * (1f - (t - 0.4f) / 0.6f * 0.5f)
        val fireAlpha = if (t < 0.4f) 0.95f else (1f - t).coerceAtLeast(0f)
        if (fireR > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fireAlpha),
                        Color(0xFFFFE040).copy(alpha = fireAlpha * 0.85f),
                        Color(0xFFFF8020).copy(alpha = fireAlpha * 0.6f),
                        Color(0xFFFF2010).copy(alpha = fireAlpha * 0.3f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = fireR,
                ),
                radius = fireR,
                center = Offset(cx, cy),
            )
        }

        // ── 2. Radial sparks — 12 spokes ──
        val sparkAlpha = (1f - t).coerceAtLeast(0f) * 0.9f
        val maxSparkLen = maxR * 1.4f
        val sparkLen = maxSparkLen * (0.3f + t * 0.7f)
        val sparkInner = sparkLen * 0.35f
        val sparkW = (4f - t * 3f).coerceAtLeast(1f) * density
        val sparks = 12
        for (i in 0 until sparks) {
            val angle = (i.toFloat() / sparks) * 2f * PI.toFloat()
            val sx = cx + cos(angle) * sparkInner
            val sy = cy + sin(angle) * sparkInner
            val ex = cx + cos(angle) * sparkLen
            val ey = cy + sin(angle) * sparkLen
            drawLine(
                color = NeonGold.copy(alpha = sparkAlpha),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = sparkW,
                cap = StrokeCap.Round,
            )
        }

        // ── 3. Shockwave ring — expanding circle ──
        val ringR = (t * (maxR * 2f + 30f * density))
        val ringAlpha = ((1f - t) * 0.8f).coerceAtLeast(0f)
        if (ringR > 0f) {
            drawCircle(
                color = Color(0xFF00F0FF).copy(alpha = ringAlpha),
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = (5f - t * 4f).coerceAtLeast(0.5f) * density),
            )
        }
    }
}
