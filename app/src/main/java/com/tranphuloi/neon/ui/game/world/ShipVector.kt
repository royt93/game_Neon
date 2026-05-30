package com.tranphuloi.neon.ui.game.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tranphuloi.neon.common.PathPool

/**
 * Round 73 (Issue 2 user audit) — Dispatch theo ShipShape cho 5 silhouette
 * khác biệt. Mỗi shape có:
 *   FIGHTER     : arrow + swept wings (baseline R66b)
 *   BOMBER      : thân ngắn rộng + cánh thẳng + double engine (heavy)
 *   STEALTH     : thân thon dài + cánh delta (sleek, narrow)
 *   TANK        : thân chữ nhật bệ vệ + cánh ngắn + 4 cannon ports
 *   INTERCEPTOR : thân tên lửa + cánh nhỏ ngang + bright tail (fast)
 *
 * Color from `shipSkin` setting. `laserBoosterEnabled` thickens wing accent.
 * Coordinate: caller Canvas `ship.width × ship.height` (85×90dp typical).
 * Ship faces UP (tip y=0); rotation via caller graphicsLayer.
 */
fun DrawScope.drawShipVector(
    color: Color,
    laserBoosterEnabled: Boolean,
    shape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape =
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
) {
    when (shape) {
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER ->
            drawFighterShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.BOMBER ->
            drawBomberShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.STEALTH ->
            drawStealthShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.TANK ->
            drawTankShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.INTERCEPTOR ->
            drawInterceptorShape(color, laserBoosterEnabled)
        // Round 79 (#3) — 12 new ship shapes. 6 distinct recipes + 6 reuse
        // existing recipes as placeholder (visual deferred to R80 polish).
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.NGOI_SAO ->
            drawNgoiSaoShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.CAU_VONG ->
            drawCauVongShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.PHU_THUY ->
            drawPhuThuyShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.SUNG_3_NONG ->
            drawSung3NongShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.AURA_GLOW ->
            drawAuraGlowShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.OBELISK_SPIRE ->
            drawObeliskSpireShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.VIETNAM ->
            drawVietnamShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.DIVA ->
            drawDivaShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.CHET_CHOC ->
            drawChetChocShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.TU_THAN ->
            drawTuThanShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.MANG_NHEN_ACE ->
            drawMangNhenShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.AO_GIAP_THIET ->
            drawAoGiapThietShape(color, laserBoosterEnabled)
        // Round 79 audit follow-up (gap 2+3) — 5 new ship shapes.
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.TWIN_DOMES ->
            drawTwinDomesShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.NHAT_BAN ->
            drawNhatBanShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.HAN_QUOC ->
            drawHanQuocShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.MY ->
            drawMyShape(color, laserBoosterEnabled)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.PHAP ->
            drawPhapShape(color, laserBoosterEnabled)
    }
}

/** FIGHTER — original Round 66b arrow silhouette. */
private fun DrawScope.drawFighterShape(
    color: Color,
    laserBoosterEnabled: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // 1. Wings — swept-back trapezoid. Wider when laser-booster is on.
    val wingHalfW = w * if (laserBoosterEnabled) 0.50f else 0.42f
    val wingTopY = cy + h * 0.10f
    val wingBottomY = cy + h * 0.36f
    val wingNotchX = w * 0.18f
    val wings = PathPool.acquire().apply {
        moveTo(cx - wingHalfW, wingTopY)                          // outer-left top
        lineTo(cx - wingHalfW * 0.55f, wingBottomY)               // outer-left bottom
        lineTo(cx - wingNotchX, wingBottomY - h * 0.05f)          // inner-left
        lineTo(cx + wingNotchX, wingBottomY - h * 0.05f)          // inner-right
        lineTo(cx + wingHalfW * 0.55f, wingBottomY)               // outer-right bottom
        lineTo(cx + wingHalfW, wingTopY)                          // outer-right top
        close()
    }
    drawPath(path = wings, color = color)
    PathPool.release(wings)

    // 2. Body — long pentagon (arrow). Tip up at y=h*0.05.
    val bodyHalfW = w * 0.18f
    val tipY = h * 0.05f
    val midY = cy - h * 0.05f
    val bodyBottomY = cy + h * 0.42f
    val body = PathPool.acquire().apply {
        moveTo(cx, tipY)                                          // nose tip
        lineTo(cx + bodyHalfW, midY)                              // right shoulder
        lineTo(cx + bodyHalfW * 0.85f, bodyBottomY)               // right tail
        lineTo(cx - bodyHalfW * 0.85f, bodyBottomY)               // left tail
        lineTo(cx - bodyHalfW, midY)                              // left shoulder
        close()
    }
    drawPath(path = body, color = color)
    PathPool.release(body)

    // 3. Inner highlight — narrower body, white-translucent for "molten core".
    val coreHalfW = bodyHalfW * 0.40f
    val core = PathPool.acquire().apply {
        moveTo(cx, tipY + h * 0.04f)
        lineTo(cx + coreHalfW, midY + h * 0.02f)
        lineTo(cx + coreHalfW * 0.85f, bodyBottomY - h * 0.04f)
        lineTo(cx - coreHalfW * 0.85f, bodyBottomY - h * 0.04f)
        lineTo(cx - coreHalfW, midY + h * 0.02f)
        close()
    }
    drawPath(path = core, color = Color.White.copy(alpha = 0.55f))
    PathPool.release(core)

    // 4. Cockpit — small dark circle near the top of body for "pilot dome".
    drawCircle(
        color = Color.Black.copy(alpha = 0.55f),
        radius = w * 0.06f,
        center = Offset(cx, cy - h * 0.08f),
    )
    drawCircle(
        color = color,
        radius = w * 0.04f,
        center = Offset(cx, cy - h * 0.08f),
        style = Stroke(width = w * 0.015f),
    )

    // 5. Engine glow — bright spot at tail.
    drawCircle(
        color = Color.White.copy(alpha = 0.80f),
        radius = w * 0.07f,
        center = Offset(cx, bodyBottomY - h * 0.02f),
    )
}

/** BOMBER — heavy, wide body + straight wings + double engine glow. */
private fun DrawScope.drawBomberShape(color: Color, laserBoosterEnabled: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Wide straight wings
    val wingHalfW = w * if (laserBoosterEnabled) 0.55f else 0.48f
    val wingY = cy + h * 0.18f
    val wingH = h * 0.18f
    drawRect(color = color,
        topLeft = Offset(cx - wingHalfW, wingY),
        size = androidx.compose.ui.geometry.Size(wingHalfW * 2, wingH))
    // Wide rounded body (rectangle with cut corners)
    val bodyHalfW = w * 0.30f
    val tipY = h * 0.10f
    val bodyBottomY = cy + h * 0.42f
    val body = PathPool.acquire().apply {
        moveTo(cx - bodyHalfW * 0.6f, tipY)
        lineTo(cx + bodyHalfW * 0.6f, tipY)
        lineTo(cx + bodyHalfW, cy)
        lineTo(cx + bodyHalfW * 0.9f, bodyBottomY)
        lineTo(cx - bodyHalfW * 0.9f, bodyBottomY)
        lineTo(cx - bodyHalfW, cy)
        close()
    }
    drawPath(body, color)
    PathPool.release(body)
    // Cargo bay highlight
    drawPath(
        PathPool.acquire().apply {
            moveTo(cx - bodyHalfW * 0.4f, tipY + h * 0.08f)
            lineTo(cx + bodyHalfW * 0.4f, tipY + h * 0.08f)
            lineTo(cx + bodyHalfW * 0.6f, bodyBottomY - h * 0.08f)
            lineTo(cx - bodyHalfW * 0.6f, bodyBottomY - h * 0.08f)
            close()
        },
        Color.White.copy(alpha = 0.45f),
    )
    // Cockpit (small)
    drawCircle(Color.Black.copy(alpha = 0.6f), w * 0.05f, Offset(cx, cy - h * 0.10f))
    // Twin engine glows
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.07f,
        Offset(cx - bodyHalfW * 0.5f, bodyBottomY - h * 0.02f))
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.07f,
        Offset(cx + bodyHalfW * 0.5f, bodyBottomY - h * 0.02f))
}

/** STEALTH — thin, long body + delta wings (narrow). */
private fun DrawScope.drawStealthShape(color: Color, laserBoosterEnabled: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Delta wings (triangle pointing back-out)
    val wingHalfW = w * if (laserBoosterEnabled) 0.45f else 0.38f
    val wings = PathPool.acquire().apply {
        moveTo(cx - w * 0.10f, cy)                                // inner-top
        lineTo(cx - wingHalfW, cy + h * 0.40f)                    // outer-left tip
        lineTo(cx - w * 0.10f, cy + h * 0.34f)                    // inner-bot
        lineTo(cx + w * 0.10f, cy + h * 0.34f)
        lineTo(cx + wingHalfW, cy + h * 0.40f)
        lineTo(cx + w * 0.10f, cy)
        close()
    }
    drawPath(wings, color)
    PathPool.release(wings)
    // Long thin body
    val bodyHalfW = w * 0.10f
    val tipY = h * 0.02f
    val bodyBottomY = cy + h * 0.45f
    val body = PathPool.acquire().apply {
        moveTo(cx, tipY)
        lineTo(cx + bodyHalfW, cy + h * 0.10f)
        lineTo(cx + bodyHalfW * 0.7f, bodyBottomY)
        lineTo(cx - bodyHalfW * 0.7f, bodyBottomY)
        lineTo(cx - bodyHalfW, cy + h * 0.10f)
        close()
    }
    drawPath(body, color)
    PathPool.release(body)
    // Subtle highlight stripe (stealth = less bright)
    drawPath(
        PathPool.acquire().apply {
            moveTo(cx, tipY + h * 0.04f)
            lineTo(cx + bodyHalfW * 0.5f, bodyBottomY - h * 0.05f)
            lineTo(cx - bodyHalfW * 0.5f, bodyBottomY - h * 0.05f)
            close()
        },
        Color.White.copy(alpha = 0.35f),
    )
    // Single small cockpit
    drawCircle(Color.Black.copy(alpha = 0.7f), w * 0.04f, Offset(cx, cy - h * 0.04f))
    // Small engine glow
    drawCircle(Color.White.copy(alpha = 0.70f), w * 0.05f, Offset(cx, bodyBottomY - h * 0.02f))
}

/** TANK — boxy, wide hull + short wings + 4 cannon ports. */
private fun DrawScope.drawTankShape(color: Color, laserBoosterEnabled: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Short stubby wings
    val wingHalfW = w * if (laserBoosterEnabled) 0.45f else 0.40f
    val wingY = cy + h * 0.05f
    val wingH = h * 0.30f
    drawRect(color = color,
        topLeft = Offset(cx - wingHalfW, wingY),
        size = androidx.compose.ui.geometry.Size(wingHalfW * 2, wingH))
    // Heavy rectangular body
    val bodyHalfW = w * 0.28f
    val tipY = h * 0.08f
    val bodyBottomY = cy + h * 0.42f
    drawRect(color = color,
        topLeft = Offset(cx - bodyHalfW, tipY),
        size = androidx.compose.ui.geometry.Size(bodyHalfW * 2, bodyBottomY - tipY))
    // Armor plate highlight
    drawRect(color = Color.White.copy(alpha = 0.40f),
        topLeft = Offset(cx - bodyHalfW * 0.6f, tipY + h * 0.05f),
        size = androidx.compose.ui.geometry.Size(bodyHalfW * 1.2f, h * 0.20f))
    // 4 cannon ports along wings
    val portR = w * 0.04f
    drawCircle(Color.Black.copy(alpha = 0.8f), portR, Offset(cx - wingHalfW * 0.8f, wingY + h * 0.05f))
    drawCircle(Color.Black.copy(alpha = 0.8f), portR, Offset(cx - wingHalfW * 0.35f, wingY + h * 0.05f))
    drawCircle(Color.Black.copy(alpha = 0.8f), portR, Offset(cx + wingHalfW * 0.35f, wingY + h * 0.05f))
    drawCircle(Color.Black.copy(alpha = 0.8f), portR, Offset(cx + wingHalfW * 0.8f, wingY + h * 0.05f))
    // Cockpit (rectangular slit)
    drawRect(color = Color.Black.copy(alpha = 0.7f),
        topLeft = Offset(cx - w * 0.08f, cy - h * 0.12f),
        size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.05f))
    // Twin engines (rectangular)
    drawRect(color = Color.White.copy(alpha = 0.80f),
        topLeft = Offset(cx - bodyHalfW * 0.5f - w * 0.04f, bodyBottomY - h * 0.04f),
        size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f))
    drawRect(color = Color.White.copy(alpha = 0.80f),
        topLeft = Offset(cx + bodyHalfW * 0.5f - w * 0.04f, bodyBottomY - h * 0.04f),
        size = androidx.compose.ui.geometry.Size(w * 0.08f, h * 0.08f))
}

/** INTERCEPTOR — missile-style narrow body + small side fins + bright trail. */
private fun DrawScope.drawInterceptorShape(color: Color, laserBoosterEnabled: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    // Small side fins
    val finHalfW = w * if (laserBoosterEnabled) 0.40f else 0.32f
    val finY = cy + h * 0.20f
    val finPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.10f, finY)
        lineTo(cx - finHalfW, finY + h * 0.15f)
        lineTo(cx - w * 0.10f, finY + h * 0.15f)
        lineTo(cx + w * 0.10f, finY + h * 0.15f)
        lineTo(cx + finHalfW, finY + h * 0.15f)
        lineTo(cx + w * 0.10f, finY)
        close()
    }
    drawPath(finPath, color)
    PathPool.release(finPath)
    // Long narrow missile body
    val bodyHalfW = w * 0.10f
    val tipY = h * 0.0f
    val bodyBottomY = cy + h * 0.45f
    val body = PathPool.acquire().apply {
        moveTo(cx, tipY)
        lineTo(cx + bodyHalfW, cy - h * 0.18f)
        lineTo(cx + bodyHalfW, bodyBottomY - h * 0.06f)
        lineTo(cx + bodyHalfW * 0.5f, bodyBottomY)
        lineTo(cx - bodyHalfW * 0.5f, bodyBottomY)
        lineTo(cx - bodyHalfW, bodyBottomY - h * 0.06f)
        lineTo(cx - bodyHalfW, cy - h * 0.18f)
        close()
    }
    drawPath(body, color)
    PathPool.release(body)
    // Bright core stripe (interceptor = high speed glow)
    drawPath(
        PathPool.acquire().apply {
            moveTo(cx, tipY + h * 0.04f)
            lineTo(cx + bodyHalfW * 0.5f, cy - h * 0.10f)
            lineTo(cx + bodyHalfW * 0.4f, bodyBottomY - h * 0.10f)
            lineTo(cx - bodyHalfW * 0.4f, bodyBottomY - h * 0.10f)
            lineTo(cx - bodyHalfW * 0.5f, cy - h * 0.10f)
            close()
        },
        Color.White.copy(alpha = 0.65f),
    )
    // Tiny cockpit
    drawCircle(Color.Black.copy(alpha = 0.6f), w * 0.04f, Offset(cx, cy - h * 0.10f))
    // Bright extended tail glow (3 layered)
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.08f,
        Offset(cx, bodyBottomY - h * 0.02f))
    drawCircle(color.copy(alpha = 0.60f), w * 0.12f,
        Offset(cx, bodyBottomY + h * 0.04f))
    drawCircle(color.copy(alpha = 0.30f), w * 0.16f,
        Offset(cx, bodyBottomY + h * 0.10f))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 79 (#3) — 12 new ship shape recipes. Each distinct silhouette to
// satisfy user request for ship variety. Tasteful + inspired-by per policy
// (no NSFW anatomy, no IP trademark names).
// ─────────────────────────────────────────────────────────────────────────

/** Ngôi Sao — 5-point star body với engine glow đáy. */
private fun DrawScope.drawNgoiSaoShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val outerR = minOf(w, h) * 0.40f
    val innerR = outerR * 0.45f
    val path = PathPool.acquire().apply {
        val rotation = -Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat() + h * 0.05f
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
    PathPool.release(path)
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        innerR * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.05f))
    // Engine trail at bottom
    drawCircle(color.copy(alpha = 0.6f), w * 0.10f,
        androidx.compose.ui.geometry.Offset(cx, h * 0.95f))
}

/** Cầu Vồng — multi-band rainbow stripe ship (curved bands). */
private fun DrawScope.drawCauVongShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val outerR = minOf(w, h) * 0.42f
    // 5 nested arcs in rainbow-stripe pattern (color shifts via alpha)
    val bandColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFFF0040),    // red
        androidx.compose.ui.graphics.Color(0xFFFF9020),    // orange
        androidx.compose.ui.graphics.Color(0xFFFFD040),    // yellow
        androidx.compose.ui.graphics.Color(0xFF40FF80),    // green
        androidx.compose.ui.graphics.Color(0xFF40C0FF),    // blue
        androidx.compose.ui.graphics.Color(0xFFC060FF),    // violet
    )
    for ((i, bandColor) in bandColors.withIndex()) {
        val r = outerR * (0.45f + i * 0.10f)
        drawArc(color = bandColor,
            startAngle = 180f, sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r * 0.5f),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 1.0f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    }
    // Cockpit dome
    drawCircle(color, w * 0.10f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.10f))
}

/** Phù Thuỷ — witch hat top + broom-stick body. */
private fun DrawScope.drawPhuThuyShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Witch hat (triangle with brim)
    val hatBrimY = h * 0.45f
    val hatTipY = h * 0.05f
    val hatPath = PathPool.acquire().apply {
        moveTo(cx, hatTipY)
        lineTo(cx + w * 0.20f, hatBrimY)
        lineTo(cx - w * 0.20f, hatBrimY)
        close()
    }
    drawPath(hatPath, color)
    PathPool.release(hatPath)
    // Brim (wide ellipse)
    drawOval(color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.40f, hatBrimY - h * 0.02f),
        size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.08f))
    // Hat star buckle
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        w * 0.05f, androidx.compose.ui.geometry.Offset(cx, hatBrimY - h * 0.05f))
    // Broomstick body (vertical rect from below hat)
    drawRect(color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, h * 0.50f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.40f))
    // Broom bristles at bottom (spread fan)
    for (i in -2..2) {
        drawLine(color,
            androidx.compose.ui.geometry.Offset(cx, h * 0.88f),
            androidx.compose.ui.geometry.Offset(cx + i * w * 0.08f, h * 0.98f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Súng 3 Nòng — triple-barrel gun fuselage. */
private fun DrawScope.drawSung3NongShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Center barrel (longest)
    drawRoundRect(color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.06f, h * 0.05f),
        size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.60f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
    // Left barrel
    drawRoundRect(color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.24f, h * 0.15f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.50f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
    // Right barrel
    drawRoundRect(color,
        topLeft = androidx.compose.ui.geometry.Offset(cx + w * 0.14f, h * 0.15f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.50f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f))
    // Base body (trapezoid)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.35f, h * 0.65f)
        lineTo(cx + w * 0.35f, h * 0.65f)
        lineTo(cx + w * 0.25f, h * 0.95f)
        lineTo(cx - w * 0.25f, h * 0.95f)
        close()
    }
    drawPath(bodyPath, color)
    PathPool.release(bodyPath)
    // Muzzle flash tips
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
        w * 0.04f, androidx.compose.ui.geometry.Offset(cx, h * 0.06f))
}

/** Aura Glow — circular orb body với 3 expanding aura rings. */
private fun DrawScope.drawAuraGlowShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val coreR = minOf(w, h) * 0.18f
    drawCircle(color.copy(alpha = 0.20f), coreR * 2.2f, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.03f))
    drawCircle(color.copy(alpha = 0.40f), coreR * 1.6f, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    drawCircle(color, coreR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        coreR * 0.4f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Engine glow tail bottom
    drawCircle(color.copy(alpha = 0.5f), w * 0.08f,
        androidx.compose.ui.geometry.Offset(cx, h * 0.92f))
}

/** Obelisk Spire — tall pointed spire (phallic/monument silhouette, tasteful). */
private fun DrawScope.drawObeliskSpireShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Tapered spire body
    val spirePath = PathPool.acquire().apply {
        moveTo(cx, h * 0.05f)
        lineTo(cx + w * 0.12f, h * 0.25f)
        lineTo(cx + w * 0.16f, h * 0.85f)
        lineTo(cx + w * 0.22f, h * 0.95f)
        lineTo(cx - w * 0.22f, h * 0.95f)
        lineTo(cx - w * 0.16f, h * 0.85f)
        lineTo(cx - w * 0.12f, h * 0.25f)
        close()
    }
    drawPath(spirePath, color)
    PathPool.release(spirePath)
    // Tip glow
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        w * 0.04f, androidx.compose.ui.geometry.Offset(cx, h * 0.08f))
    // Mid markings (3 horizontal stripes)
    for (i in 0 until 3) {
        val y = h * (0.40f + i * 0.15f)
        drawLine(color.copy(alpha = 0.5f),
            androidx.compose.ui.geometry.Offset(cx - w * 0.16f, y),
            androidx.compose.ui.geometry.Offset(cx + w * 0.16f, y),
            strokeWidth = w * 0.02f)
    }
}

/** Việt Nam — red body với yellow 5-point star center. */
private fun DrawScope.drawVietnamShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    // Body (red flag-like rectangle with rounded fighter shape)
    val flagColor = androidx.compose.ui.graphics.Color(0xFFDA251D)
    val starColor = androidx.compose.ui.graphics.Color(0xFFFFD700)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.10f)
        lineTo(cx + w * 0.35f, h * 0.50f)
        lineTo(cx + w * 0.30f, h * 0.90f)
        lineTo(cx - w * 0.30f, h * 0.90f)
        lineTo(cx - w * 0.35f, h * 0.50f)
        close()
    }
    drawPath(bodyPath, flagColor)
    PathPool.release(bodyPath)
    // Yellow 5-point star
    val outerR = w * 0.18f
    val innerR = outerR * 0.45f
    val starPath = PathPool.acquire().apply {
        val rotation = -Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, starColor)
    PathPool.release(starPath)
}

/** Diva — feminine silhouette với hourglass curve + crown. */
private fun DrawScope.drawDivaShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Hourglass body (curved silhouette)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.22f, h * 0.15f)
        cubicTo(cx - w * 0.30f, h * 0.30f, cx - w * 0.15f, h * 0.45f, cx - w * 0.10f, h * 0.55f)
        cubicTo(cx - w * 0.25f, h * 0.70f, cx - w * 0.20f, h * 0.85f, cx - w * 0.30f, h * 0.95f)
        lineTo(cx + w * 0.30f, h * 0.95f)
        cubicTo(cx + w * 0.20f, h * 0.85f, cx + w * 0.25f, h * 0.70f, cx + w * 0.10f, h * 0.55f)
        cubicTo(cx + w * 0.15f, h * 0.45f, cx + w * 0.30f, h * 0.30f, cx + w * 0.22f, h * 0.15f)
        close()
    }
    drawPath(bodyPath, color)
    // Audit-10 P0 fix — was `PathPool.release(bodyPath)` TWICE → pool corruption
    // (same Path returned to pool 2x, next acquire() could hand same instance
    // to 2 concurrent callers). Now released exactly once.
    PathPool.release(bodyPath)
    // Crown (3 triangle peaks at top)
    val crownPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.22f, h * 0.15f)
        lineTo(cx - w * 0.15f, h * 0.05f)
        lineTo(cx - w * 0.08f, h * 0.12f)
        lineTo(cx, h * 0.02f)
        lineTo(cx + w * 0.08f, h * 0.12f)
        lineTo(cx + w * 0.15f, h * 0.05f)
        lineTo(cx + w * 0.22f, h * 0.15f)
        close()
    }
    drawPath(crownPath, color)
    PathPool.release(crownPath)
    // Center gem
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        w * 0.04f, androidx.compose.ui.geometry.Offset(cx, h * 0.10f))
}

/** Chết Chóc — scythe blade silhouette with skull pommel. */
private fun DrawScope.drawChetChocShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Scythe handle (vertical curving)
    drawLine(color,
        androidx.compose.ui.geometry.Offset(cx + w * 0.05f, h * 0.20f),
        androidx.compose.ui.geometry.Offset(cx - w * 0.05f, h * 0.95f),
        strokeWidth = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Scythe blade (curved arc top)
    val bladePath = PathPool.acquire().apply {
        moveTo(cx + w * 0.05f, h * 0.20f)
        cubicTo(cx + w * 0.35f, h * 0.10f, cx + w * 0.45f, h * 0.35f, cx + w * 0.20f, h * 0.30f)
        lineTo(cx + w * 0.05f, h * 0.20f)
        close()
    }
    drawPath(bladePath, color)
    drawPath(bladePath, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.02f))
    PathPool.release(bladePath)
    // Skull at bottom
    drawCircle(color, w * 0.10f,
        androidx.compose.ui.geometry.Offset(cx - w * 0.05f, h * 0.95f))
    drawCircle(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f), w * 0.025f,
        androidx.compose.ui.geometry.Offset(cx - w * 0.10f, h * 0.93f))
    drawCircle(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f), w * 0.025f,
        androidx.compose.ui.geometry.Offset(cx + w * 0.0f, h * 0.93f))
}

/** Tử Thần — grim reaper hood + glowing eyes + cloak silhouette. */
private fun DrawScope.drawTuThanShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // Hood (curved trapezoid)
    val hoodPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.30f, h * 0.05f)
        cubicTo(cx - w * 0.40f, h * 0.30f, cx - w * 0.35f, h * 0.55f, cx - w * 0.40f, h * 0.95f)
        lineTo(cx + w * 0.40f, h * 0.95f)
        cubicTo(cx + w * 0.35f, h * 0.55f, cx + w * 0.40f, h * 0.30f, cx + w * 0.30f, h * 0.05f)
        // Hood opening curve
        cubicTo(cx + w * 0.15f, h * 0.08f, cx - w * 0.15f, h * 0.08f, cx - w * 0.30f, h * 0.05f)
        close()
    }
    drawPath(hoodPath, color)
    PathPool.release(hoodPath)
    // Dark face void inside hood
    val faceVoid = PathPool.acquire().apply {
        moveTo(cx - w * 0.20f, h * 0.10f)
        cubicTo(cx - w * 0.25f, h * 0.30f, cx - w * 0.20f, h * 0.50f, cx, h * 0.55f)
        cubicTo(cx + w * 0.20f, h * 0.50f, cx + w * 0.25f, h * 0.30f, cx + w * 0.20f, h * 0.10f)
        close()
    }
    drawPath(faceVoid, androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f))
    PathPool.release(faceVoid)
    // 2 glowing red eyes
    drawCircle(androidx.compose.ui.graphics.Color(0xFFFF2D55),
        w * 0.04f, androidx.compose.ui.geometry.Offset(cx - w * 0.08f, h * 0.30f))
    drawCircle(androidx.compose.ui.graphics.Color(0xFFFF2D55),
        w * 0.04f, androidx.compose.ui.geometry.Offset(cx + w * 0.08f, h * 0.30f))
}

/** Mạng Nhện Ace (Spider-inspired) — web pattern body. */
private fun DrawScope.drawMangNhenShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    // Body (red-blue fighter shape — inspired by Spider hero costume colors)
    val redColor = androidx.compose.ui.graphics.Color(0xFFCC2030)
    val blueColor = androidx.compose.ui.graphics.Color(0xFF2050C0)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.35f, h * 0.50f)
        lineTo(cx + w * 0.25f, h * 0.95f)
        lineTo(cx - w * 0.25f, h * 0.95f)
        lineTo(cx - w * 0.35f, h * 0.50f)
        close()
    }
    drawPath(bodyPath, redColor)
    PathPool.release(bodyPath)
    // Blue lower half
    val lowerPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.30f, h * 0.55f)
        lineTo(cx + w * 0.30f, h * 0.55f)
        lineTo(cx + w * 0.25f, h * 0.95f)
        lineTo(cx - w * 0.25f, h * 0.95f)
        close()
    }
    drawPath(lowerPath, blueColor)
    PathPool.release(lowerPath)
    // Web pattern (radial lines from center + 3 concentric arcs)
    for (i in 0 until 8) {
        val a = i * Math.PI / 4
        val ex = cx + (w * 0.30f * kotlin.math.cos(a)).toFloat()
        val ey = cy + (h * 0.30f * kotlin.math.sin(a)).toFloat()
        drawLine(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f),
            androidx.compose.ui.geometry.Offset(cx, cy), androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = w * 0.015f)
    }
    for (r in listOf(0.08f, 0.16f, 0.24f)) {
        drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
            w * r, androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.012f))
    }
}

/** Áo Giáp Thiết (Iron-inspired) — armored mask + chest reactor arc. */
private fun DrawScope.drawAoGiapThietShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val ironRed = androidx.compose.ui.graphics.Color(0xFFCC2020)
    val ironGold = androidx.compose.ui.graphics.Color(0xFFFFC020)
    // Helmet/body (rounded pentagon)
    val helmetPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.32f, h * 0.30f)
        lineTo(cx + w * 0.28f, h * 0.65f)
        lineTo(cx + w * 0.20f, h * 0.95f)
        lineTo(cx - w * 0.20f, h * 0.95f)
        lineTo(cx - w * 0.28f, h * 0.65f)
        lineTo(cx - w * 0.32f, h * 0.30f)
        close()
    }
    drawPath(helmetPath, ironRed)
    PathPool.release(helmetPath)
    // Mask eyes (gold slits)
    val eyeY = h * 0.30f
    drawRect(ironGold,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.20f, eyeY - h * 0.02f),
        size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.04f))
    drawRect(ironGold,
        topLeft = androidx.compose.ui.geometry.Offset(cx + w * 0.05f, eyeY - h * 0.02f),
        size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.04f))
    // Chest reactor (central circle with cyan glow)
    val reactorY = h * 0.55f
    drawCircle(ironGold, w * 0.10f, androidx.compose.ui.geometry.Offset(cx, reactorY))
    drawCircle(androidx.compose.ui.graphics.Color(0xFF40E0FF), w * 0.07f,
        androidx.compose.ui.geometry.Offset(cx, reactorY))
    drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        w * 0.03f, androidx.compose.ui.geometry.Offset(cx, reactorY))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 79 audit follow-up (gap 2+3) — 5 new ship shapes.
// ─────────────────────────────────────────────────────────────────────────

/** Twin Domes — 2 curved dome-shells on fighter base. Tasteful version of
 *  user's "vú phụ nữ" request — abstract curves, no anatomical detail. */
private fun DrawScope.drawTwinDomesShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    // 2 dome curves at top (rounded ellipses)
    val domeRx = w * 0.16f
    val domeRy = h * 0.16f
    val domeY = h * 0.30f
    drawOval(color,
        topLeft = Offset(cx - w * 0.22f - domeRx, domeY - domeRy),
        size = androidx.compose.ui.geometry.Size(domeRx * 2, domeRy * 2))
    drawOval(color,
        topLeft = Offset(cx + w * 0.22f - domeRx, domeY - domeRy),
        size = androidx.compose.ui.geometry.Size(domeRx * 2, domeRy * 2))
    // Small bright tips (engine intake glow)
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.04f,
        Offset(cx - w * 0.22f, domeY - domeRy * 0.30f))
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.04f,
        Offset(cx + w * 0.22f, domeY - domeRy * 0.30f))
    // Connecting body (fighter base)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.30f, domeY)
        lineTo(cx + w * 0.30f, domeY)
        lineTo(cx + w * 0.25f, h * 0.95f)
        lineTo(cx - w * 0.25f, h * 0.95f)
        close()
    }
    drawPath(bodyPath, color)
    PathPool.release(bodyPath)
    // Cockpit dome center
    drawCircle(color, w * 0.07f, Offset(cx, h * 0.55f))
    drawCircle(Color.White.copy(alpha = 0.7f), w * 0.03f, Offset(cx, h * 0.55f))
}

/** Nhật Bản — round red sun on white body (rising sun motif). */
private fun DrawScope.drawNhatBanShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val white = Color.White
    val red = Color(0xFFBC002D)                       // hinomaru red
    // Main body white pentagon
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.35f, h * 0.40f)
        lineTo(cx + w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.35f, h * 0.40f)
        close()
    }
    drawPath(bodyPath, white)
    drawPath(bodyPath, color, style = Stroke(width = w * 0.025f))
    PathPool.release(bodyPath)
    // Hinomaru red sun center
    drawCircle(red, w * 0.18f, Offset(cx, cy + h * 0.05f))
    // Sun-rays (16 small lines radiating)
    for (i in 0 until 16) {
        val a = i * 2.0 * Math.PI / 16.0
        val sx = cx + (w * 0.18f * kotlin.math.cos(a)).toFloat()
        val sy = cy + h * 0.05f + (w * 0.18f * kotlin.math.sin(a)).toFloat()
        val ex = cx + (w * 0.30f * kotlin.math.cos(a)).toFloat()
        val ey = cy + h * 0.05f + (w * 0.30f * kotlin.math.sin(a)).toFloat()
        drawLine(red.copy(alpha = 0.50f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.012f)
    }
}

/** Hàn Quốc — Taegeuk symbol (yin-yang red-blue) on white body. */
private fun DrawScope.drawHanQuocShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val white = Color.White
    val red = Color(0xFFCD2E3A)
    val blue = Color(0xFF0047A0)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.35f, h * 0.40f)
        lineTo(cx + w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.35f, h * 0.40f)
        close()
    }
    // Audit-10 P0 fix — compound bug: was `drawPath(body) → release(body) →
    // drawPath(body) → release(body)`. After first release the Path was back
    // in pool and could be re-acquired+reset by another caller; the second
    // drawPath would render on a stale/reset Path → wrong outline OR pool
    // double-free (release-twice). Reorder: both draws BEFORE single release.
    drawPath(bodyPath, white)
    drawPath(bodyPath, color, style = Stroke(width = w * 0.025f))
    PathPool.release(bodyPath)
    // Taegeuk (yin-yang split horizontally with curves)
    val symbolR = w * 0.20f
    val symbolCy = cy + h * 0.05f
    // Upper red half-circle
    drawArc(red, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - symbolR, symbolCy - symbolR),
        size = androidx.compose.ui.geometry.Size(symbolR * 2, symbolR * 2))
    // Lower blue half-circle
    drawArc(blue, startAngle = 0f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - symbolR, symbolCy - symbolR),
        size = androidx.compose.ui.geometry.Size(symbolR * 2, symbolR * 2))
    // 2 swirl dots
    drawCircle(red, symbolR * 0.45f, Offset(cx - symbolR * 0.45f, symbolCy))
    drawCircle(blue, symbolR * 0.45f, Offset(cx + symbolR * 0.45f, symbolCy))
    drawCircle(blue, symbolR * 0.15f, Offset(cx - symbolR * 0.45f, symbolCy))
    drawCircle(red, symbolR * 0.15f, Offset(cx + symbolR * 0.45f, symbolCy))
}

/** Mỹ — stars-stripes pattern (rectangular blue field + red stripes). */
private fun DrawScope.drawMyShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val white = Color.White
    val red = Color(0xFFB22234)
    val blue = Color(0xFF3C3B6E)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.35f, h * 0.40f)
        lineTo(cx + w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.35f, h * 0.40f)
        close()
    }
    drawPath(bodyPath, white)
    PathPool.release(bodyPath)
    // 5 red stripes
    val stripeStartY = h * 0.40f
    val stripeEndY = h * 0.95f
    val stripeStep = (stripeEndY - stripeStartY) / 9f
    for (i in 0 until 5) {
        val y = stripeStartY + i * stripeStep * 2f
        drawRect(red,
            topLeft = Offset(cx - w * 0.30f, y),
            size = androidx.compose.ui.geometry.Size(w * 0.60f, stripeStep * 1.0f))
    }
    // Blue canton (top-left) with 5 white stars
    drawRect(blue,
        topLeft = Offset(cx - w * 0.30f, h * 0.10f),
        size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.30f))
    for (row in 0 until 2) for (col in 0 until 3) {
        val sx = cx - w * 0.25f + col * w * 0.10f
        val sy = h * 0.16f + row * h * 0.12f
        drawCircle(white, w * 0.025f, Offset(sx, sy))
    }
    drawPath(bodyPath, color, style = Stroke(width = w * 0.025f))
}

/** Pháp — Bleu-Blanc-Rouge tricolor vertical bands. */
private fun DrawScope.drawPhapShape(color: Color, laserBoost: Boolean) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val blue = Color(0xFF002654)
    val white = Color.White
    val red = Color(0xFFCE1126)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.35f, h * 0.40f)
        lineTo(cx + w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.28f, h * 0.95f)
        lineTo(cx - w * 0.35f, h * 0.40f)
        close()
    }
    drawPath(bodyPath, white)
    // 3 vertical bands (clipped roughly to body bounds)
    val bandTopY = h * 0.20f; val bandBottomY = h * 0.95f
    val bandW = w * 0.20f
    drawRect(blue,
        topLeft = Offset(cx - w * 0.30f, bandTopY),
        size = androidx.compose.ui.geometry.Size(bandW, bandBottomY - bandTopY))
    // Middle is white (already body bg) — skip
    drawRect(red,
        topLeft = Offset(cx + w * 0.10f, bandTopY),
        size = androidx.compose.ui.geometry.Size(bandW, bandBottomY - bandTopY))
    drawPath(bodyPath, color, style = Stroke(width = w * 0.025f))
    PathPool.release(bodyPath)
    PathPool.release(bodyPath)
    // Eiffel Tower nose tip (small triangle)
    val eiffel = PathPool.acquire().apply {
        moveTo(cx, h * 0.08f)
        lineTo(cx + w * 0.06f, h * 0.20f)
        lineTo(cx - w * 0.06f, h * 0.20f)
        close()
    }
    drawPath(eiffel, Color(0xFF888888))
    PathPool.release(eiffel)
}
