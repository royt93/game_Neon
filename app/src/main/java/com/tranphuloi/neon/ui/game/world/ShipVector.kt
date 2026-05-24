package com.tranphuloi.neon.ui.game.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

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
    val wings = Path().apply {
        moveTo(cx - wingHalfW, wingTopY)                          // outer-left top
        lineTo(cx - wingHalfW * 0.55f, wingBottomY)               // outer-left bottom
        lineTo(cx - wingNotchX, wingBottomY - h * 0.05f)          // inner-left
        lineTo(cx + wingNotchX, wingBottomY - h * 0.05f)          // inner-right
        lineTo(cx + wingHalfW * 0.55f, wingBottomY)               // outer-right bottom
        lineTo(cx + wingHalfW, wingTopY)                          // outer-right top
        close()
    }
    drawPath(path = wings, color = color)

    // 2. Body — long pentagon (arrow). Tip up at y=h*0.05.
    val bodyHalfW = w * 0.18f
    val tipY = h * 0.05f
    val midY = cy - h * 0.05f
    val bodyBottomY = cy + h * 0.42f
    val body = Path().apply {
        moveTo(cx, tipY)                                          // nose tip
        lineTo(cx + bodyHalfW, midY)                              // right shoulder
        lineTo(cx + bodyHalfW * 0.85f, bodyBottomY)               // right tail
        lineTo(cx - bodyHalfW * 0.85f, bodyBottomY)               // left tail
        lineTo(cx - bodyHalfW, midY)                              // left shoulder
        close()
    }
    drawPath(path = body, color = color)

    // 3. Inner highlight — narrower body, white-translucent for "molten core".
    val coreHalfW = bodyHalfW * 0.40f
    val core = Path().apply {
        moveTo(cx, tipY + h * 0.04f)
        lineTo(cx + coreHalfW, midY + h * 0.02f)
        lineTo(cx + coreHalfW * 0.85f, bodyBottomY - h * 0.04f)
        lineTo(cx - coreHalfW * 0.85f, bodyBottomY - h * 0.04f)
        lineTo(cx - coreHalfW, midY + h * 0.02f)
        close()
    }
    drawPath(path = core, color = Color.White.copy(alpha = 0.55f))

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
    val body = Path().apply {
        moveTo(cx - bodyHalfW * 0.6f, tipY)
        lineTo(cx + bodyHalfW * 0.6f, tipY)
        lineTo(cx + bodyHalfW, cy)
        lineTo(cx + bodyHalfW * 0.9f, bodyBottomY)
        lineTo(cx - bodyHalfW * 0.9f, bodyBottomY)
        lineTo(cx - bodyHalfW, cy)
        close()
    }
    drawPath(body, color)
    // Cargo bay highlight
    drawPath(
        Path().apply {
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
    val wings = Path().apply {
        moveTo(cx - w * 0.10f, cy)                                // inner-top
        lineTo(cx - wingHalfW, cy + h * 0.40f)                    // outer-left tip
        lineTo(cx - w * 0.10f, cy + h * 0.34f)                    // inner-bot
        lineTo(cx + w * 0.10f, cy + h * 0.34f)
        lineTo(cx + wingHalfW, cy + h * 0.40f)
        lineTo(cx + w * 0.10f, cy)
        close()
    }
    drawPath(wings, color)
    // Long thin body
    val bodyHalfW = w * 0.10f
    val tipY = h * 0.02f
    val bodyBottomY = cy + h * 0.45f
    val body = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx + bodyHalfW, cy + h * 0.10f)
        lineTo(cx + bodyHalfW * 0.7f, bodyBottomY)
        lineTo(cx - bodyHalfW * 0.7f, bodyBottomY)
        lineTo(cx - bodyHalfW, cy + h * 0.10f)
        close()
    }
    drawPath(body, color)
    // Subtle highlight stripe (stealth = less bright)
    drawPath(
        Path().apply {
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
    val finPath = Path().apply {
        moveTo(cx - w * 0.10f, finY)
        lineTo(cx - finHalfW, finY + h * 0.15f)
        lineTo(cx - w * 0.10f, finY + h * 0.15f)
        lineTo(cx + w * 0.10f, finY + h * 0.15f)
        lineTo(cx + finHalfW, finY + h * 0.15f)
        lineTo(cx + w * 0.10f, finY)
        close()
    }
    drawPath(finPath, color)
    // Long narrow missile body
    val bodyHalfW = w * 0.10f
    val tipY = h * 0.0f
    val bodyBottomY = cy + h * 0.45f
    val body = Path().apply {
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
    // Bright core stripe (interceptor = high speed glow)
    drawPath(
        Path().apply {
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
