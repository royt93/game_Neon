package com.tranphuloi.neon.ui.game.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Round 66b — Pure-vector player ship rendering. Replaces the
 * Image(painterResource(ship.drawableId)) in GameWorld with a procedural
 * arrow-style ship: nose triangle + swept-back wings + cockpit dot. Inner
 * highlight bands give the neon "molten core" look.
 *
 * Color is driven by [color] (resolved from `shipSkin` setting upstream), so
 * the 5 aura colors (cyan/gold/magenta/violet/red) all work automatically.
 * The [laserBoosterEnabled] flag thickens the wing accent to mimic the prior
 * `ship_boosted_laser` sprite swap.
 *
 * Coordinate space: caller draws into a Canvas sized `ship.width × ship.height`
 * (typically 85×90 dp). Drawing fills the canvas with the ship facing UP
 * (tip at y=0); banking rotation + spawn rotation applied via the caller's
 * graphicsLayer.
 */
fun DrawScope.drawShipVector(
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
