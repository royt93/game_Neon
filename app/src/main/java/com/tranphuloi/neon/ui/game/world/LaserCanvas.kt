package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI

/**
 * Round 66 — Pure-vector laser rendering (pilot for the "no PNG textures"
 * direction). Replaces the round 49 `drawImage(ImageBitmap)` path with three
 * stacked draw primitives per laser:
 *   1. Radial gradient halo (unchanged from round 49 — drives the neon glow).
 *   2. Outer body: drawRoundRect tinted to `glow` color, full alpha.
 *   3. Inner core: drawRoundRect white at alpha 0.85, half the width — gives
 *      the "hot center" look characteristic of neon beams.
 *
 * Why pilot here: lasers are the simplest entities in the game (vertical bars
 * with glow). Migration from bitmap → vector is essentially lossless
 * aesthetically (the original sprites were just colored stripes too) AND
 * eliminates 5 webp drawables from the APK. If user approves the look, the
 * same pattern can expand to boosters/space-rocks/enemies/ship in subsequent
 * rounds (66b/c/d).
 *
 * API change vs round 49: the `LaserSprites` parameter is gone. Callers no
 * longer need `rememberLaserSprites()` either. GameWorld.kt updated in the
 * same commit. The `drawableId` field on `LaserUI` is now unused for
 * rendering (kept for backward compat with persistence + mapper tests).
 */
@Composable
fun LaserCanvas(
    lasers: List<LaserUI>,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    modifier: Modifier = Modifier,
) {
    if (lasers.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (laser in lasers) {
            drawLaser(laser, glow, intensity, radiusFactor, density)
        }
    }
}

/**
 * Round 66 — kept as a type alias for backward compatibility. Future cleanup
 * round can remove this + [rememberLaserSprites] once no callers reference
 * them. Marked @Deprecated to surface usage sites.
 */
@Immutable
@Deprecated("Round 66 — pure-vector LaserCanvas no longer needs sprites. Remove call site.")
data class LaserSprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66 — pure-vector LaserCanvas no longer needs sprites. Remove call site.")
fun rememberLaserSprites(): LaserSprites = LaserSprites()

private fun DrawScope.drawLaser(
    laser: LaserUI,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    density: Density,
) {
    with(density) {
        val xPx = laser.xOffset.dp.toPx()
        val yPx = laser.yOffset.dp.toPx()
        val wPx = laser.width.dp.toPx()
        val hPx = laser.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f
        val glowR = (minOf(wPx, hPx) / 2f) * radiusFactor

        // Wave 17h — MÀU theo LOẠI đạn (signature) thay vì 1 màu ship chung cho
        // mọi đạn (gốc rễ "đạn nhìn giống nhau"). NORMAL giữ màu skin (`glow`) để
        // đạn mặc định khớp tàu; đạn đặc biệt hiện màu riêng (Lửa cam, Plasma
        // xanh, Atomic lục, Pháo Hoa hồng…) → nhìn phát biết ngay loại.
        val bulletColor =
            if (laser.bulletType == com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL) glow
            else Color(
                com.tranphuloi.neon.ui.game.ship.laser.BulletTypeColorMap.argbFor(laser.bulletType),
            )

        // Round 78 (#6 perf) — was Brush.radialGradient per-laser per-frame.
        // Up to 30 ship + 30 enemy + ultimate lasers = ~80 brush allocs/frame.
        // Replaced with drawSoftHalo (3 drawCircles, no Brush/Shader allocation).
        // Wave 17h — đạn đặc biệt: halo ĐẬM hơn (×1.6) để màu signature "viền đậm"
        // bật rõ; NORMAL giữ intensity gốc (khớp tông tàu).
        val haloIntensity =
            if (laser.bulletType == com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL) intensity
            else (intensity * 1.6f).coerceAtMost(1f)
        drawSoftHalo(bulletColor, haloIntensity, glowR, Offset(cx, cy))

        // Round 71 (Issue 4a) — dispatch per BulletType. Each laser has unique
        // vector silhouette in-game matching InfoScreen Bullets tab.
        val drawBody: DrawScope.() -> Unit = {
            drawLaserBody(laser.bulletType, xPx, yPx, wPx, hPx, bulletColor)
        }
        if (laser.rotation != 0f) {
            rotate(degrees = laser.rotation, pivot = Offset(cx, cy)) { drawBody() }
        } else {
            drawBody()
        }
    }
}

/** Round 71 (Issue 4a) — dispatch table for in-game laser shape. */
private fun DrawScope.drawLaserBody(
    bulletType: BulletType,
    xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color,
) {
    val bodyCorner = (wPx / 2f).coerceAtMost(hPx / 2f)
    when (bulletType) {
        BulletType.NORMAL -> drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
        BulletType.PIERCING -> drawNeedleBody(xPx, yPx, wPx, hPx, glow)
        BulletType.PLASMA -> drawOrbBody(xPx, yPx, wPx, hPx, glow)
        BulletType.FIRE -> drawFireBody(xPx, yPx, wPx, hPx, glow)
        BulletType.HOMING -> drawHomingBody(xPx, yPx, wPx, hPx, glow)
        BulletType.BOUNCE -> drawBounceBody(xPx, yPx, wPx, hPx, glow)
        BulletType.GIANT -> drawGiantBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
        BulletType.SMOKE -> drawSmokeBody(xPx, yPx, wPx, hPx, glow)
        BulletType.ZIGZAG -> drawZigzagBody(xPx, yPx, wPx, hPx, glow)
        BulletType.KAMEHAMEHA -> drawBeamBody(xPx, yPx, wPx, hPx, glow)
        BulletType.ATOMIC -> drawAtomicBody(xPx, yPx, wPx, hPx, glow)
        BulletType.SPLIT -> drawSplitBody(xPx, yPx, wPx, hPx, glow)
        // Wave 16 — body VECTOR RIÊNG cho 6 đạn trào phúng (hết đụng hàng shape).
        BulletType.LOTTERY -> drawLotteryBody(xPx, yPx, wPx, hPx, glow)
        BulletType.FIREWORK -> drawFireworkBody(xPx, yPx, wPx, hPx, glow)
        BulletType.AIRBURST -> drawFireworkBody(xPx, yPx, wPx, hPx, glow)
        BulletType.BRICK -> drawBrickBody(xPx, yPx, wPx, hPx, glow)
        BulletType.BANH_MI -> drawBanhMiBody(xPx, yPx, wPx, hPx, glow)
        BulletType.DURIAN -> drawDurianBody(xPx, yPx, wPx, hPx, glow)
        BulletType.HEART -> drawHeartBody(xPx, yPx, wPx, hPx, glow)
        // Wave 18 — batch 3 đạn trào phúng (body vector riêng).
        BulletType.BUBBLE_TEA -> drawBobaBody(xPx, yPx, wPx, hPx, glow)
        BulletType.FISH_SAUCE -> drawBottleBody(xPx, yPx, wPx, hPx, glow)
        BulletType.SANDAL -> drawSandalBody(xPx, yPx, wPx, hPx, glow)
        BulletType.QR_CODE -> drawQrBody(xPx, yPx, wPx, hPx, glow)
        // Task 02 — Sét Chain: tia zigzag mảnh + lõi trắng sáng.
        BulletType.LIGHTNING -> drawLightningBody(xPx, yPx, wPx, hPx, glow)
    }
}

/** Task 02 — thân đạn sét: bolt zigzag + lõi trắng (đọc rõ là "tia điện"). */
private fun DrawScope.drawLightningBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val amp = wPx * 1.1f
    fun bolt() = PathPool.acquire().apply {
        val step = hPx / 4f
        moveTo(cx, yPx)
        lineTo(cx + amp / 2, yPx + step)
        lineTo(cx - amp / 2, yPx + step * 2)
        lineTo(cx + amp / 2, yPx + step * 3)
        lineTo(cx, yPx + hPx)
    }
    val outer = bolt()
    drawPath(outer, glow, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = wPx * 0.9f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round,
    ))
    PathPool.release(outer)
    val core = bolt()
    drawPath(core, Color.White.copy(alpha = 0.9f), style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = wPx * 0.35f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round,
    ))
    PathPool.release(core)
}

// ── Wave 18 — body VECTOR cho batch 3 (ly trà sữa + trân châu, chai nước mắm,
// dép tổ ong, mã QR). ──

/** Trà Sữa — ly bo góc + ống hút + 3 hạt trân châu tối ở đáy. */
private fun DrawScope.drawBobaBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawRoundRect(
        glow, Offset(xPx, yPx), Size(wPx, hPx),
        androidx.compose.ui.geometry.CornerRadius(wPx * 0.3f),
    )
    // Ống hút (vạch trắng chéo).
    drawLine(
        Color.White.copy(alpha = 0.8f),
        Offset(xPx + wPx * 0.62f, yPx - hPx * 0.05f),
        Offset(xPx + wPx * 0.45f, yPx + hPx * 0.55f),
        strokeWidth = wPx * 0.12f,
    )
    // Trân châu (3 hạt tối ở đáy).
    val pr = wPx * 0.17f
    val py = yPx + hPx * 0.78f
    val pearl = Color.Black.copy(alpha = 0.5f)
    drawCircle(pearl, pr, Offset(xPx + wPx * 0.3f, py))
    drawCircle(pearl, pr, Offset(xPx + wPx * 0.7f, py))
    drawCircle(pearl, pr, Offset(xPx + wPx * 0.5f, py - pr * 1.5f))
}

/** Nước Mắm — chai: cổ hẹp trên + thân bo góc + nhãn trắng. */
private fun DrawScope.drawBottleBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val neckW = wPx * 0.42f
    drawRect(glow, Offset(cx - neckW / 2f, yPx), Size(neckW, hPx * 0.3f))
    drawRoundRect(
        glow, Offset(xPx, yPx + hPx * 0.28f), Size(wPx, hPx * 0.72f),
        androidx.compose.ui.geometry.CornerRadius(wPx * 0.3f),
    )
    drawRect(
        Color.White.copy(alpha = 0.7f),
        Offset(xPx + wPx * 0.2f, yPx + hPx * 0.5f), Size(wPx * 0.6f, hPx * 0.22f),
    )
}

/** Dép Lào — đế oval + quai chữ V. */
private fun DrawScope.drawSandalBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    drawOval(glow, Offset(xPx, yPx), Size(wPx, hPx))
    val sw = wPx * 0.14f
    val strap = Color.White.copy(alpha = 0.85f)
    drawLine(
        strap, Offset(cx, yPx + hPx * 0.32f), Offset(xPx + wPx * 0.25f, yPx + hPx * 0.72f),
        strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
    drawLine(
        strap, Offset(cx, yPx + hPx * 0.32f), Offset(xPx + wPx * 0.75f, yPx + hPx * 0.72f),
        strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

/** Mã QR — ô vuông nền + vài ô trắng (3 marker góc + chấm rải) như mã QR. */
private fun DrawScope.drawQrBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val s = minOf(wPx, hPx)
    val x = xPx + (wPx - s) / 2f
    val y = yPx + (hPx - s) / 2f
    drawRect(glow, Offset(x, y), Size(s, s))
    val c = s / 4f
    val cell = Color.White.copy(alpha = 0.9f)
    // 3 marker góc + 3 chấm rải. Danh sách ô là hằng cấp module (QR_CELLS) →
    // KHÔNG cấp phát list/Pair mỗi frame trong hot render path.
    for ((ix, iy) in QR_CELLS) {
        drawRect(cell, Offset(x + ix * c, y + iy * c), Size(c, c))
    }
}

/** Wave 18 — vị trí ô trắng của body Mã QR (hằng, tránh alloc mỗi frame). */
private val QR_CELLS: List<Pair<Int, Int>> =
    listOf(0 to 0, 3 to 0, 0 to 3, 2 to 1, 1 to 2, 3 to 3)

private fun DrawScope.drawBounceBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.45f
    // Ball + 2 motion arcs behind to indicate ricochet trail.
    val trailR1 = r * 0.7f
    val trailR2 = r * 0.45f
    drawCircle(glow.copy(alpha = 0.35f), trailR1,
        Offset(cx - wPx * 0.4f, cy + hPx * 0.25f))
    drawCircle(glow.copy(alpha = 0.5f), trailR2,
        Offset(cx + wPx * 0.4f, cy + hPx * 0.15f))
    // Main ball
    drawCircle(glow, r, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.4f, Offset(cx, cy))
}

private fun DrawScope.drawGiantBody(
    xPx: Float, yPx: Float, wPx: Float, hPx: Float, bodyCorner: Float, glow: Color,
) {
    // Mega-capsule with INNER segment line — distinguishes from NORMAL capsule
    // (which has just outer body + white core). GIANT looks "engineered" with
    // 3 segment dividers in core.
    drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
    // 2 horizontal segment dividers across the white core
    val coreW = wPx * 0.5f
    val coreX = xPx + (wPx - coreW) / 2f
    val divY1 = yPx + hPx * 0.35f
    val divY2 = yPx + hPx * 0.65f
    drawLine(glow, Offset(coreX, divY1), Offset(coreX + coreW, divY1),
        strokeWidth = wPx * 0.08f)
    drawLine(glow, Offset(coreX, divY2), Offset(coreX + coreW, divY2),
        strokeWidth = wPx * 0.08f)
}

private fun DrawScope.drawHomingBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val bodyCorner = (wPx / 2f).coerceAtMost(hPx / 2f)
    drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
    // Targeting ring around the bullet (signals lock-on)
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val ringR = maxOf(wPx, hPx) * 0.55f
    drawCircle(
        color = glow,
        radius = ringR,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.35f),
    )
}

private fun DrawScope.drawNeedleBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val path = PathPool.acquire().apply {
        moveTo(cx, yPx)                                  // pointed top
        lineTo(xPx + wPx, yPx + hPx)
        lineTo(xPx, yPx + hPx)
        close()
    }
    drawPath(path, glow)
    val corePath = PathPool.acquire().apply {
        moveTo(cx, yPx + hPx * 0.15f)
        lineTo(cx + wPx * 0.25f, yPx + hPx * 0.95f)
        lineTo(cx - wPx * 0.25f, yPx + hPx * 0.95f)
        close()
    }
    drawPath(corePath, Color.White.copy(alpha = 0.85f))
    PathPool.release(corePath)
}

private fun DrawScope.drawOrbBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.5f
    drawCircle(glow, r, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.5f, Offset(cx, cy))
}

// ── Wave 16 — body VECTOR RIÊNG cho 6 đạn trào phúng (in-game): vé (ticket),
// pháo hoa (starburst), gạch (rect+mạch), bánh mì (oval+rạch), sầu riêng
// (tròn+gai), tim (heart). Không còn dùng chung body với NORMAL/PLASMA/… ──

/** Vé Số — tấm vé bo góc + chấm số trắng giữa. */
private fun DrawScope.drawLotteryBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val w = maxOf(wPx, 7f)
    val x = xPx + wPx / 2f - w / 2f
    drawRoundRect(glow, Offset(x, yPx), Size(w, hPx),
        androidx.compose.ui.geometry.CornerRadius(w * 0.35f))
    drawCircle(Color.White.copy(alpha = 0.9f), w * 0.32f, Offset(xPx + wPx / 2f, yPx + hPx / 2f))
}

/** Pháo Hoa — tâm sáng + nan toả (starburst). */
private fun DrawScope.drawFireworkBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f; val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.5f
    for (i in 0 until 8) {
        val a = i * 45.0 * Math.PI / 180.0
        drawLine(glow, Offset(cx, cy),
            Offset(cx + (r * kotlin.math.cos(a)).toFloat(), cy + (r * kotlin.math.sin(a)).toFloat()),
            strokeWidth = r * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    drawCircle(Color.White, r * 0.32f, Offset(cx, cy))
}

/** Cục Gạch — chữ nhật góc vuông + mạch vữa. */
private fun DrawScope.drawBrickBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawRect(glow, Offset(xPx, yPx), Size(wPx, hPx))
    drawLine(Color.Black.copy(alpha = 0.35f),
        Offset(xPx, yPx + hPx / 2f), Offset(xPx + wPx, yPx + hPx / 2f), strokeWidth = hPx * 0.06f)
}

/** Bánh Mì — ổ bầu dục + vạch rạch chéo. */
private fun DrawScope.drawBanhMiBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val w = maxOf(wPx, 8f)
    drawOval(glow, Offset(xPx + wPx / 2f - w / 2f, yPx), Size(w, hPx))
    drawLine(Color.White.copy(alpha = 0.6f),
        Offset(xPx + wPx / 2f - w * 0.2f, yPx + hPx * 0.35f),
        Offset(xPx + wPx / 2f + w * 0.2f, yPx + hPx * 0.65f), strokeWidth = w * 0.12f)
}

/** Sầu Riêng — tròn + gai nhọn quanh. */
private fun DrawScope.drawDurianBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f; val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.36f
    drawCircle(glow, r, Offset(cx, cy))
    for (i in 0 until 10) {
        val a = i * 36.0 * Math.PI / 180.0
        drawLine(glow, Offset(cx + (r * kotlin.math.cos(a)).toFloat(), cy + (r * kotlin.math.sin(a)).toFloat()),
            Offset(cx + (r * 1.6f * kotlin.math.cos(a)).toFloat(), cy + (r * 1.6f * kotlin.math.sin(a)).toFloat()),
            strokeWidth = r * 0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Like/Tim — trái tim (2 thuỳ + đáy nhọn). */
private fun DrawScope.drawHeartBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f; val cy = yPx + hPx / 2f
    val s = maxOf(wPx, hPx) * 0.5f
    val lobe = s * 0.42f
    drawCircle(glow, lobe, Offset(cx - lobe * 0.8f, cy - lobe * 0.4f))
    drawCircle(glow, lobe, Offset(cx + lobe * 0.8f, cy - lobe * 0.4f))
    val tri = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - lobe * 1.7f, cy - lobe * 0.1f)
        lineTo(cx + lobe * 1.7f, cy - lobe * 0.1f)
        lineTo(cx, cy + s * 0.95f)
        close()
    }
    drawPath(tri, glow)
}

private fun DrawScope.drawFireBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    // Wave 16 — đầu đạn nhỏ hơn, NGỌN LỬA TO HƠN (đuôi lửa cam→vàng) cho "thấy lửa".
    drawCapsuleBody(xPx, yPx, wPx, hPx * 0.5f, wPx / 2f, glow)
    val cx = xPx + wPx / 2f
    val flameW = wPx * 2.2f                              // lửa rộng hơn thân đạn
    val flameTop = yPx + hPx * 0.42f
    val flameBot = yPx + hPx * 1.35f                     // tràn xuống dưới đạn
    // Outer orange flame.
    val outer = PathPool.acquire().apply {
        moveTo(cx, flameBot)
        cubicTo(cx + flameW / 2f, flameTop + (flameBot - flameTop) * 0.4f, cx + flameW * 0.28f, flameTop, cx, flameTop - hPx * 0.1f)
        cubicTo(cx - flameW * 0.28f, flameTop, cx - flameW / 2f, flameTop + (flameBot - flameTop) * 0.4f, cx, flameBot)
        close()
    }
    drawPath(outer, Color(0xFFFF6A00).copy(alpha = 0.9f))
    PathPool.release(outer)
    // Inner yellow core.
    val inner = PathPool.acquire().apply {
        moveTo(cx, flameBot - hPx * 0.18f)
        cubicTo(cx + flameW * 0.28f, flameTop + (flameBot - flameTop) * 0.45f, cx + flameW * 0.14f, flameTop + hPx * 0.1f, cx, flameTop + hPx * 0.05f)
        cubicTo(cx - flameW * 0.14f, flameTop + hPx * 0.1f, cx - flameW * 0.28f, flameTop + (flameBot - flameTop) * 0.45f, cx, flameBot - hPx * 0.18f)
        close()
    }
    drawPath(inner, Color(0xFFFFD040))
    PathPool.release(inner)
}

private fun DrawScope.drawSmokeBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawCapsuleBody(xPx, yPx, wPx, hPx * 0.7f, wPx / 2f, glow)
    // Cloud puff at bottom
    val cx = xPx + wPx / 2f
    drawCircle(glow.copy(alpha = 0.5f), wPx * 0.55f, Offset(cx, yPx + hPx * 0.85f))
}

private fun DrawScope.drawZigzagBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    // Use wider stroke + zigzag path. wPx scaled wider for visibility.
    val widerW = wPx * 1.8f
    val cx = xPx + wPx / 2f
    val path = PathPool.acquire().apply {
        val step = hPx / 4f
        moveTo(cx - widerW / 2, yPx)
        lineTo(cx + widerW / 2, yPx + step)
        lineTo(cx - widerW / 2, yPx + step * 2)
        lineTo(cx + widerW / 2, yPx + step * 3)
        lineTo(cx - widerW / 2, yPx + hPx)
    }
    drawPath(path, glow, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = wPx * 0.7f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round,
    ))
    PathPool.release(path)
    PathPool.release(path)
}

private fun DrawScope.drawBeamBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    // Wide horizontal-style beam (use full available width as height for "beam" look)
    val beamW = wPx * 1.6f
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    drawRoundRect(
        color = glow,
        topLeft = Offset(cx - beamW / 2, cy - hPx * 0.25f),
        size = Size(beamW, hPx * 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(hPx * 0.25f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(cx - beamW / 2 + beamW * 0.05f, cy - hPx * 0.12f),
        size = Size(beamW * 0.9f, hPx * 0.24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(hPx * 0.12f),
    )
}

private fun DrawScope.drawAtomicBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.5f
    // Nucleus
    drawCircle(glow, r * 0.45f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.2f, Offset(cx, cy))
    // 3 orbiting electrons
    for (i in 0 until 3) {
        val angle = i * 120.0 * Math.PI / 180.0
        val ex = cx + (r * 0.9f * kotlin.math.cos(angle)).toFloat()
        val ey = cy + (r * 0.9f * kotlin.math.sin(angle)).toFloat()
        drawCircle(glow.copy(alpha = 0.85f), r * 0.15f, Offset(ex, ey))
    }
}

private fun DrawScope.drawSplitBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawCapsuleBody(xPx, yPx + hPx * 0.3f, wPx, hPx * 0.65f, wPx / 2f, glow)
    // 3 branches at top
    val cx = xPx + wPx / 2f
    val branchTop = yPx + hPx * 0.3f
    val branchLen = hPx * 0.35f
    for (i in -1..1) {
        val angle = i * 30.0 * Math.PI / 180.0
        val tipX = cx + (branchLen * kotlin.math.sin(angle)).toFloat()
        val tipY = branchTop - (branchLen * kotlin.math.cos(angle)).toFloat()
        drawLine(
            color = glow,
            start = Offset(cx, branchTop),
            end = Offset(tipX, tipY),
            strokeWidth = wPx * 0.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

/**
 * Round 66 — the per-laser body draw. Extracted so the rotate() block can
 * reuse it cleanly. Two stacked rounded rectangles: outer tinted to `glow`,
 * inner white-hot core at ~50% the width with high alpha for the neon "bright
 * center" feel.
 */
private fun DrawScope.drawCapsuleBody(
    xPx: Float,
    yPx: Float,
    wPx: Float,
    hPx: Float,
    cornerRadius: Float,
    glow: Color,
) {
    // Outer body — full tinted color.
    drawRoundRect(
        color = glow,
        topLeft = Offset(xPx, yPx),
        size = Size(wPx, hPx),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
    )
    // Inner hot core — white, 50% width centered, slightly inset vertically
    // so the rounded ends stay tinted.
    val coreW = wPx * 0.5f
    val coreInsetY = hPx * 0.10f
    val coreH = (hPx - coreInsetY * 2f).coerceAtLeast(1f)
    val coreX = xPx + (wPx - coreW) / 2f
    val coreY = yPx + coreInsetY
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(coreX, coreY),
        size = Size(coreW, coreH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(coreW / 2f),
    )
}
