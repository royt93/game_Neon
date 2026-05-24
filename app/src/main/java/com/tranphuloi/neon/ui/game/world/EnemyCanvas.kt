package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round 66b — Pure-vector enemy rendering. Replaces 14 webp drawables with
 * geometric polygon shapes per family. Faction visual identity is encoded
 * in shape + color (kept from original sprite hue):
 *
 *   enemy_light_blue_* (5 variants) → cyan dart triangles (small fast attackers).
 *   enemy_green_*      (4 variants) → green hexagons (medium balanced).
 *   enemy_red_*        (3 variants) → red diamonds (heavy hitters).
 *   enemy_*_boss       (2 variants) → large 8-pointed star / multi-layer hex.
 *
 * Variant number within family modulates either rotation angle or detail
 * count so 5 light-blue enemies don't all look identical. Player still reads
 * "alien fleet" through family color + general silhouette.
 *
 * Hit flash + status effect tints preserved via Color.mix on the body color.
 * Boss entry trail (4 fading copies) preserved via repeated draw at offset.
 */
@Composable
fun EnemyCanvas(
    enemies: List<EnemyUI>,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (enemies.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (enemy in enemies) {
            drawEnemy(enemy, nowMillis, density)
        }
    }
}

/** Round 66b — sprite preload no longer needed; stub kept for back-compat. */
@Immutable
@Deprecated("Round 66b — pure-vector EnemyCanvas no longer needs sprites.")
data class EnemySprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66b — pure-vector EnemyCanvas no longer needs sprites.")
fun rememberEnemySprites(): EnemySprites = EnemySprites()

private const val HP_BAR_VERTICAL_SPACE_DP = 5

// Family colors — match original sprite palette intent.
private val LIGHT_BLUE_BODY = Color(0xFF4FD4FF)
private val GREEN_BODY = Color(0xFF6EFFAA)
private val RED_BODY = Color(0xFFFF5555)
private val LIGHT_BLUE_ACCENT = Color(0xFF1799CC)
private val GREEN_ACCENT = Color(0xFF24B86E)
private val RED_ACCENT = Color(0xFFCC1144)

private fun DrawScope.drawEnemy(
    enemy: EnemyUI,
    nowMillis: Long,
    density: Density,
) {
    with(density) {
        val xPx = enemy.xOffset.dp.toPx()
        val hpBarOffsetPx = if (enemy.isBoss) 0f else HP_BAR_VERTICAL_SPACE_DP.dp.toPx()
        val yPx = enemy.yOffset.dp.toPx() + hpBarOffsetPx
        val wPx = enemy.width.dp.toPx()
        val hPx = enemy.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f

        // Hit flash factor (0..1, decays over 120ms).
        val sinceHit = nowMillis - enemy.lastImpactMillis
        val hitFlash = if (enemy.lastImpactMillis > 0L && sinceHit in 0..120) {
            (1f - sinceHit / 120f).coerceIn(0f, 1f)
        } else 0f

        // Glow halo — Magenta. Round 77 (R77d): boss có 2 aura layers + corner markers.
        val glowIntensity = 0.45f + hitFlash * 0.4f
        val glowRadiusFactor = if (enemy.isBoss) 2.2f + hitFlash * 0.4f
            else 1.4f + hitFlash * 0.4f                    // Boss aura BIGGER halo
        val glowR = (minOf(wPx, hPx) / 2f) * glowRadiusFactor
        val haloColor = if (enemy.isBoss) Color(0xFFFF2D55) else NeonMagenta
        // Round 78 (#6 perf) — was Brush.radialGradient per-enemy per-frame.
        // 30 enemies × ~5 allocs each → 150 allocs/frame just for halo. Replaced
        // with drawSoftHalo (3 drawCircles, no Brush/Shader). Visually equivalent
        // at typical halo sizes.
        drawSoftHalo(haloColor, glowIntensity, glowR, Offset(cx, cy))
        // Round 77 (R77d) — Boss inner secondary aura ring (rotating effect via animation deferred).
        if (enemy.isBoss) {
            drawCircle(
                color = haloColor.copy(alpha = 0.45f + hitFlash * 0.3f),
                radius = (minOf(wPx, hPx) / 2f) * 1.35f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.06f),
            )
            // 4 corner markers — small glowing diamonds at NE/SE/SW/NW của bounding ring
            val markerR = (minOf(wPx, hPx) / 2f) * 1.55f
            val markerSize = wPx * 0.10f
            for (i in 0 until 4) {
                val ang = (45.0 + i * 90.0) * Math.PI / 180.0
                val mx = cx + (markerR * kotlin.math.cos(ang)).toFloat()
                val my = cy + (markerR * kotlin.math.sin(ang)).toFloat()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(mx, my - markerSize)
                    lineTo(mx + markerSize, my)
                    lineTo(mx, my + markerSize)
                    lineTo(mx - markerSize, my)
                    close()
                }
                drawPath(path, haloColor.copy(alpha = 0.85f))
            }
        }

        // Body color, blended with hit flash white + status effect tint.
        val baseBody = bodyColorFor(enemy.drawableId)
        val accent = accentColorFor(enemy.drawableId)
        val bodyColor = blendForFlash(baseBody, hitFlash, enemy.activeStatusEffectTints, nowMillis)

        // Boss entry-phase thrust trail — 4 fading copies stacked upward.
        if (enemy.isBoss && enemy.isInEntryPhase) {
            val trailDy = 30.dp.toPx()
            for (i in 1..4) {
                val trailAlpha = (1f - i * 0.22f).coerceIn(0f, 1f) * 0.55f
                if (trailAlpha <= 0f) continue
                drawEnemyShape(
                    drawableId = enemy.drawableId,
                    cx = cx,
                    cy = cy - i * trailDy,
                    wPx = wPx,
                    hPx = hPx,
                    body = bodyColor.copy(alpha = trailAlpha),
                    accent = accent.copy(alpha = trailAlpha),
                    isBoss = enemy.isBoss,
                    bossKind = enemy.bossKind,
                )
            }
        }

        // Main body shape per family / boss kind.
        drawEnemyShape(
            drawableId = enemy.drawableId,
            cx = cx,
            cy = cy,
            wPx = wPx,
            hPx = hPx,
            body = bodyColor,
            accent = accent,
            isBoss = enemy.isBoss,
            bossKind = enemy.bossKind,
        )
    }
}

/**
 * Map drawableId → primary body color. Preserves original sprite family hue.
 */
private fun bodyColorFor(drawableId: Int): Color = when (drawableId) {
    R.drawable.enemy_light_blue_1, R.drawable.enemy_light_blue_2,
    R.drawable.enemy_light_blue_3, R.drawable.enemy_light_blue_4,
    R.drawable.enemy_light_blue_5 -> LIGHT_BLUE_BODY
    R.drawable.enemy_green_1, R.drawable.enemy_green_2,
    R.drawable.enemy_green_3, R.drawable.enemy_green_4,
    R.drawable.enemy_green_boss -> GREEN_BODY
    R.drawable.enemy_red_1, R.drawable.enemy_red_2,
    R.drawable.enemy_red_3, R.drawable.enemy_red_boss -> RED_BODY
    // Round 74 (R73d) — ELITE family = violet, BERSERKER = orange.
    R.drawable.enemy_cross_1, R.drawable.enemy_cross_2,
    R.drawable.enemy_orb_1, R.drawable.enemy_orb_2 -> Color(0xFFB14CFF)        // violet ELITE
    R.drawable.enemy_chevron_1, R.drawable.enemy_chevron_2,
    R.drawable.enemy_spike_1, R.drawable.enemy_spike_2 -> Color(0xFFFF9020)    // orange BERSERKER
    else -> Color(0xFFCCCCCC)
}

private fun accentColorFor(drawableId: Int): Color = when (drawableId) {
    R.drawable.enemy_light_blue_1, R.drawable.enemy_light_blue_2,
    R.drawable.enemy_light_blue_3, R.drawable.enemy_light_blue_4,
    R.drawable.enemy_light_blue_5 -> LIGHT_BLUE_ACCENT
    R.drawable.enemy_green_1, R.drawable.enemy_green_2,
    R.drawable.enemy_green_3, R.drawable.enemy_green_4,
    R.drawable.enemy_green_boss -> GREEN_ACCENT
    R.drawable.enemy_red_1, R.drawable.enemy_red_2,
    R.drawable.enemy_red_3, R.drawable.enemy_red_boss -> RED_ACCENT
    R.drawable.enemy_cross_1, R.drawable.enemy_cross_2,
    R.drawable.enemy_orb_1, R.drawable.enemy_orb_2 -> Color(0xFF7020CC)
    R.drawable.enemy_chevron_1, R.drawable.enemy_chevron_2,
    R.drawable.enemy_spike_1, R.drawable.enemy_spike_2 -> Color(0xFFCC5000)
    else -> Color(0xFF666666)
}

/**
 * Blend base body color with (a) white tint from hit flash and (b) per-effect
 * pulsing status color. Order of preference: status effects > hit flash > base.
 * Multiple status effects mix by averaging their ARGB values.
 */
private fun blendForFlash(
    base: Color,
    hitFlash: Float,
    statusTintsArgb: List<Long>,
    nowMillis: Long,
): Color {
    var result = base
    if (statusTintsArgb.isNotEmpty()) {
        val pulse = (0.50f + 0.35f * kotlin.math.sin(nowMillis / 160.0).toFloat()).coerceIn(0f, 1f)
        // Average the status colors then mix at pulse weight.
        var r = 0f; var g = 0f; var b = 0f
        statusTintsArgb.forEach { argb ->
            val c = Color(argb.toInt())
            r += c.red; g += c.green; b += c.blue
        }
        val n = statusTintsArgb.size
        val avg = Color(r / n, g / n, b / n, 1f)
        result = Color(
            red = result.red + (avg.red - result.red) * pulse,
            green = result.green + (avg.green - result.green) * pulse,
            blue = result.blue + (avg.blue - result.blue) * pulse,
            alpha = 1f,
        )
    }
    if (hitFlash > 0f) {
        result = Color(
            red = result.red + (1f - result.red) * hitFlash,
            green = result.green + (1f - result.green) * hitFlash,
            blue = result.blue + (1f - result.blue) * hitFlash,
            alpha = 1f,
        )
    }
    return result
}

/**
 * Dispatch table — pick shape recipe by drawableId. Each shape inscribed in
 * `(cx, cy)` ± `wPx × hPx`. Variant within family modulates a small detail
 * (rotation / vertex count) to break up monotony.
 */
private fun DrawScope.drawEnemyShape(
    drawableId: Int,
    cx: Float, cy: Float, wPx: Float, hPx: Float,
    body: Color, accent: Color, isBoss: Boolean,
    bossKind: com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind? = null,
) {
    // Round 71 (Issue 4d) — Boss dispatch via bossKind nếu boss, else family
    // dispatch theo drawableId.
    if (isBoss && bossKind != null) {
        when (bossKind) {
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.STAR ->
                // Round 78 (#2 spec follow-up) — STAR now renders as SUN (corona +
                // disc + radial flares). "A star is a sun" — fitting reinterpretation.
                drawBossSun(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CROSS ->
                drawBossCross(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.ORB ->
                // Round 78 (#2) — was drawBossOrb (orb + satellites). User asked
                // for "killer eye" — much more menacing/memorable silhouette.
                drawBossEye(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FRACTAL ->
                // Round 78 (#2 spec follow-up) — FRACTAL renders as ATOM (electron
                // orbits + nucleus). User listed "atom" as menacing boss shape.
                drawBossAtom(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SPIDER ->
                drawBossSpider(cx, cy, wPx, hPx, body, accent)
        }
        return
    }
    when (drawableId) {
        // Light blue family — Round 78 spec-follow-up: 5 fully distinct silhouettes
        // (HEART/TRIANGLE/CLUB/CIRCLE/SPADE). User: "cơ, chuồn, bích" = card suits.
        R.drawable.enemy_light_blue_1 -> drawHeart(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_2 -> drawTriangle(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_3 -> drawCardClub(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_4 -> drawCircleEnemy(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_5 -> drawCardSpade(cx, cy, wPx, hPx, body, accent)
        // Green family — Round 78: HEXAGON + VIRUS + EYE + CARD_DIAMOND.
        // User: "rô" = card diamond suit (vertical rhombus).
        R.drawable.enemy_green_1 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_green_2 -> drawVirus(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_green_3 -> drawEye(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_green_4 -> drawCardDiamond(cx, cy, wPx, hPx, body, accent)
        // Red family — Round 78: DIAMOND + HEART (red) + CIRCLE.
        // Heart in red = playing-card "cơ" tribute. Circle covers user's "circle" spec.
        R.drawable.enemy_red_1 -> drawDiamond(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_red_2 -> drawHeart(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_red_3 -> drawCircleEnemy(cx, cy, wPx, hPx, body, accent)
        // Round 74 (R73d) — Wave 9a: 8 new shape recipes cho ELITE + BERSERKER family.
        // ELITE: cross + orb (violet).
        R.drawable.enemy_cross_1 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_cross_2 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_orb_1 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_orb_2 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 1)
        // BERSERKER: chevron + spike (orange).
        R.drawable.enemy_chevron_1 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_chevron_2 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_spike_1 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_spike_2 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 1)
        // Boss drawables fallback (bossKind null — defensive).
        R.drawable.enemy_green_boss, R.drawable.enemy_red_boss ->
            drawBossStar(cx, cy, wPx, hPx, body, accent)
        else -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 74 (R73d) — Wave 9a: 4 new shape recipes (2 variant mỗi).
// ─────────────────────────────────────────────────────────────────────────

/** ELITE: 4-arm cross with center disc + tip glow. variant changes arm length. */
private fun DrawScope.drawCross(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val armWFactor = 0.20f + variant * 0.05f
    val armW = minOf(wPx, hPx) * armWFactor
    val armLen = minOf(wPx, hPx) * (0.40f + variant * 0.05f)
    val centerR = minOf(wPx, hPx) * 0.20f
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2))
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW))
    drawCircle(body, centerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, centerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Tip dots
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy - armLen))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy + armLen))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx - armLen, cy))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx + armLen, cy))
}

/** ELITE: Floating orb with 1 or 2 orbital rings. variant=0 single, variant=1 double. */
private fun DrawScope.drawOrb(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val r = minOf(wPx, hPx) * 0.40f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.22f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Orbital ring(s)
    drawCircle(
        color = accent,
        radius = r * 1.20f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.05f),
    )
    if (variant == 1) {
        drawCircle(
            color = accent.copy(alpha = 0.6f),
            radius = r * 1.45f,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.035f),
        )
    }
}

/** BERSERKER: Chevron arrow pointing DOWN (toward player). variant=0 single, variant=1 double-stack. */
private fun DrawScope.drawChevron(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = wPx * 0.42f
    val drawOne: (Float) -> Unit = { yShift ->
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - halfW, cy - hPx * 0.25f + yShift)
            lineTo(cx, cy + hPx * 0.20f + yShift)
            lineTo(cx + halfW, cy - hPx * 0.25f + yShift)
            lineTo(cx + halfW * 0.7f, cy - hPx * 0.30f + yShift)
            lineTo(cx, cy + hPx * 0.10f + yShift)
            lineTo(cx - halfW * 0.7f, cy - hPx * 0.30f + yShift)
            close()
        }
        drawPath(path, body)
    }
    drawOne(0f)
    if (variant == 1) drawOne(hPx * 0.35f)
    // Pip center
    drawCircle(accent, wPx * 0.07f, androidx.compose.ui.geometry.Offset(cx, cy))
}

/** BERSERKER: Spiked star — 8 pointy spikes radiating. variant changes spike length. */
private fun DrawScope.drawSpike(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val outerR = minOf(wPx, hPx) * (0.42f + variant * 0.05f)
    val innerR = outerR * 0.40f
    val spikes = 8
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until spikes * 2) {
            val angle = -Math.PI / 2 + i * Math.PI / spikes
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.04f))
    drawCircle(accent, innerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 71 (Issue 4d) — 4 new boss silhouettes (STAR đã có).
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawBossCross(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // 4-arm cross spinner (LevelTwoBoss). Center disc + 4 arms với hollow tip.
    val armW = minOf(wPx, hPx) * 0.18f
    val armLen = minOf(wPx, hPx) * 0.5f
    val centerR = minOf(wPx, hPx) * 0.25f
    // Vertical arm
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2),
    )
    // Horizontal arm
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW),
    )
    // Center disc
    drawCircle(body, centerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, centerR * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Tip circles
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy - armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy + armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx - armLen, cy))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx + armLen, cy))
}

private fun DrawScope.drawBossOrb(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // Large orb + 3 ring satellites (MidBoss OFFENSIVE).
    val r = minOf(wPx, hPx) * 0.4f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(
        color = body,
        radius = r * 0.85f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f),
    )
    // 3 orbiting satellite discs
    for (i in 0 until 3) {
        val a = i * 120.0 * Math.PI / 180.0
        val sx = cx + (r * 1.15f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (r * 1.15f * kotlin.math.sin(a)).toFloat()
        drawCircle(accent, r * 0.18f, androidx.compose.ui.geometry.Offset(sx, sy))
    }
}

private fun DrawScope.drawBossFractal(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // Recursive triangle (MidBoss DEFENSIVE / SWARM).
    val r = minOf(wPx, hPx) * 0.45f
    // Outer triangle pointing DOWN (toward player)
    val outer = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx - r * 0.866f, cy - r * 0.5f)
        lineTo(cx + r * 0.866f, cy - r * 0.5f)
        close()
    }
    drawPath(outer, body)
    drawPath(outer, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f))
    // Inner upward triangle (Sierpinski child)
    val innerR = r * 0.5f
    val inner = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - innerR)
        lineTo(cx - innerR * 0.866f, cy + innerR * 0.5f)
        lineTo(cx + innerR * 0.866f, cy + innerR * 0.5f)
        close()
    }
    drawPath(inner, accent)
    // Center dot
    drawCircle(body, r * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun DrawScope.drawBossSpider(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // 8 legs radiating from oval body (FinalBoss).
    val bodyR = minOf(wPx, hPx) * 0.3f
    val legLen = minOf(wPx, hPx) * 0.55f
    val legW = bodyR * 0.18f
    // 8 legs at 45° spacing
    for (i in 0 until 8) {
        val a = i * 45.0 * Math.PI / 180.0
        val ex = cx + (legLen * kotlin.math.cos(a)).toFloat()
        val ey = cy + (legLen * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = body,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = legW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Joint glow
        drawCircle(accent, legW * 0.6f, androidx.compose.ui.geometry.Offset(ex, ey))
    }
    // Body — oval shape (use draw circle with scaled = horizontal)
    drawCircle(body, bodyR, androidx.compose.ui.geometry.Offset(cx, cy))
    // 2 "eyes" indicating it's looking
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx - bodyR * 0.4f, cy - bodyR * 0.1f))
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx + bodyR * 0.4f, cy - bodyR * 0.1f))
}

// ─────────── Shape recipes ───────────

/**
 * Dart shape — triangle pointing DOWN (enemies attack downward).
 * Round 77 (R77e) — Mỗi variant 0..4 có unique additive modifier:
 *   v0 baseline / v1 thruster trail / v2 side-spikes / v3 swept-wings / v4 heavy armor pip.
 */
private fun DrawScope.drawDart(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.42f
    val halfH = h * 0.45f
    val tipY = cy + halfH
    val baseY = cy - halfH
    val notch = h * (0.10f + variant * 0.04f)
    val path = Path().apply {
        moveTo(cx, tipY)
        lineTo(cx + halfW, baseY)
        lineTo(cx, baseY + notch)
        lineTo(cx - halfW, baseY)
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.10f, Offset(cx, cy + h * 0.10f))
    // Round 77 — additive modifier per variant.
    when (variant) {
        1 -> {
            // Thruster trail behind (upward extra lines)
            drawLine(accent, Offset(cx - w * 0.10f, baseY), Offset(cx - w * 0.15f, baseY - h * 0.18f),
                strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(accent, Offset(cx + w * 0.10f, baseY), Offset(cx + w * 0.15f, baseY - h * 0.18f),
                strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        2 -> {
            // Side-spikes — 2 nhỏ ở mép cánh
            drawCircle(accent, w * 0.06f, Offset(cx + halfW, baseY + h * 0.05f))
            drawCircle(accent, w * 0.06f, Offset(cx - halfW, baseY + h * 0.05f))
        }
        3 -> {
            // Swept-wings — extra triangle nhỏ phía mép cánh
            val sweepW = w * 0.18f
            val sweepH = h * 0.18f
            val swp = Path().apply {
                moveTo(cx + halfW, baseY)
                lineTo(cx + halfW + sweepW, baseY + sweepH * 0.5f)
                lineTo(cx + halfW, baseY + sweepH)
                close()
            }
            drawPath(swp, accent)
            val swpL = Path().apply {
                moveTo(cx - halfW, baseY)
                lineTo(cx - halfW - sweepW, baseY + sweepH * 0.5f)
                lineTo(cx - halfW, baseY + sweepH)
                close()
            }
            drawPath(swpL, accent)
        }
        4 -> {
            // Heavy armor pip — extra inner triangle layer
            val innerScale = 0.6f
            val innerPath = Path().apply {
                moveTo(cx, tipY - h * 0.05f)
                lineTo(cx + halfW * innerScale, baseY + h * 0.05f)
                lineTo(cx, baseY + notch + h * 0.05f)
                lineTo(cx - halfW * innerScale, baseY + h * 0.05f)
                close()
            }
            drawPath(innerPath, accent.copy(alpha = 0.65f))
            // Center heavy pip
            drawCircle(Color.White.copy(alpha = 0.65f), w * 0.07f, Offset(cx, cy))
        }
    }
}

/**
 * Hexagon — flat-top, slightly elongated vertically. variant rotates 10° each.
 * Round 77 (R77e) — Mỗi variant 0..3 unique additive:
 *   v0 baseline / v1 inner-hex / v2 corner orbs / v3 center cross.
 */
private fun DrawScope.drawHexagon(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val rx = w * 0.45f
    val ry = h * 0.50f
    val baseAngle = Math.toRadians((variant * 10).toDouble())
    val path = Path().apply {
        for (i in 0 until 6) {
            val a = baseAngle + 2.0 * Math.PI * i / 6.0
            val x = cx + (rx * cos(a)).toFloat()
            val y = cy + (ry * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.12f, Offset(cx, cy))
    when (variant) {
        1 -> {
            // Inner hex shell
            val ix = rx * 0.55f; val iy = ry * 0.55f
            val ip = Path().apply {
                for (i in 0 until 6) {
                    val a = baseAngle + 2.0 * Math.PI * i / 6.0
                    val x = cx + (ix * cos(a)).toFloat()
                    val y = cy + (iy * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(ip, accent, style = Stroke(width = w * 0.04f))
        }
        2 -> {
            // 6 corner orbs
            for (i in 0 until 6) {
                val a = baseAngle + 2.0 * Math.PI * i / 6.0
                val ox = cx + (rx * cos(a)).toFloat()
                val oy = cy + (ry * sin(a)).toFloat()
                drawCircle(accent, w * 0.05f, Offset(ox, oy))
            }
        }
        3 -> {
            // Center cross (4-tip mini star)
            drawLine(accent, Offset(cx, cy - ry * 0.45f), Offset(cx, cy + ry * 0.45f),
                strokeWidth = w * 0.05f)
            drawLine(accent, Offset(cx - rx * 0.45f, cy), Offset(cx + rx * 0.45f, cy),
                strokeWidth = w * 0.05f)
        }
    }
}

/**
 * Diamond — vertical, wider than tall, with 4-spike accent.
 * Round 77 audit fix — additive modifier per variant 0..2:
 *   v0 baseline / v1 horizontal spikes (đối xứng L-R) / v2 corner mini-diamonds.
 */
private fun DrawScope.drawDiamond(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.50f
    val path = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.07f))
    val spikeR = w * 0.10f
    drawCircle(color = accent, radius = spikeR, center = Offset(cx, cy))
    when (variant) {
        1 -> {
            // Horizontal spikes — 2 pips trái + phải để biến tấu visual.
            drawCircle(accent, spikeR * 0.7f, Offset(cx - halfW * 0.5f, cy))
            drawCircle(accent, spikeR * 0.7f, Offset(cx + halfW * 0.5f, cy))
            // Plus vertical center pip
            drawCircle(accent, spikeR * 0.5f, Offset(cx, cy - halfH * 0.6f))
        }
        2 -> {
            // Corner mini-diamonds at 4 cardinal points
            val miniR = w * 0.07f
            for (i in 0 until 4) {
                val a = i * 90.0 * Math.PI / 180.0
                val mx = cx + (halfW * 0.75f * kotlin.math.cos(a)).toFloat()
                val my = cy + (halfH * 0.75f * kotlin.math.sin(a)).toFloat()
                val mini = Path().apply {
                    moveTo(mx, my - miniR)
                    lineTo(mx + miniR, my)
                    lineTo(mx, my + miniR)
                    lineTo(mx - miniR, my)
                    close()
                }
                drawPath(mini, accent)
            }
        }
    }
}

/** Boss shape — large 8-pointed star with inner hexagon detail. */
private fun DrawScope.drawBossStar(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.50f
    val innerR = outerR * 0.55f
    val path = Path().apply {
        val step = Math.PI / 8.0                                       // 16 vertices, 8-pointed star
        for (i in 0 until 16) {
            val a = -Math.PI / 2.0 + i * step
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.05f))
    // Inner hexagon core
    val coreR = outerR * 0.40f
    val core = Path().apply {
        for (i in 0 until 6) {
            val a = 2.0 * Math.PI * i / 6.0
            val x = cx + (coreR * cos(a)).toFloat()
            val y = cy + (coreR * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = core, color = accent)
    drawCircle(color = Color.White.copy(alpha = 0.75f), radius = coreR * 0.30f, center = Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 78 (#1+#2 shape diversity) — new visually distinct enemy shapes
// to break up family monotony. Each replaces one drawable in the dispatcher
// so within a family (cyan/green/red) different individuals look different.
// ─────────────────────────────────────────────────────────────────────────

/** Heart shape — symmetric lobes top, point at bottom. */
private fun DrawScope.drawHeart(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.45f
    // Build heart via 2 circles + downward triangle merge.
    val lobeR = halfW * 0.55f
    drawCircle(body, lobeR, Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(body, lobeR, Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    val triPath = Path().apply {
        moveTo(cx - halfW, cy - halfH * 0.18f)
        lineTo(cx + halfW, cy - halfH * 0.18f)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(triPath, body)
    // Accent outline (stroke approximation: redraw rim accents)
    drawCircle(accent, lobeR * 0.35f, Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, lobeR * 0.35f, Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.07f, Offset(cx, cy + halfH * 0.5f))
}

/** Simple downward triangle (distinct from drawDart which has notch). */
private fun DrawScope.drawTriangle(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.48f
    val path = Path().apply {
        moveTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent, style = Stroke(width = w * 0.06f))
    // Center pip + 2 side dots for "alien glyph" feel.
    drawCircle(accent, w * 0.08f, Offset(cx, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.05f, Offset(cx - halfW * 0.30f, cy + halfH * 0.20f))
    drawCircle(accent, w * 0.05f, Offset(cx + halfW * 0.30f, cy + halfH * 0.20f))
}

/** Eye shape — oval body, iris circle, pupil dot. Creepy "watching you" vibe. */
private fun DrawScope.drawEye(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val rx = w * 0.48f
    val ry = h * 0.30f
    // Outer eye outline (lens shape via 2 overlapping arcs approximated as ellipse).
    drawOval(
        color = body,
        topLeft = Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
    )
    drawOval(
        color = accent,
        topLeft = Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
        style = Stroke(width = w * 0.05f),
    )
    // Iris — white ring
    drawCircle(Color.White.copy(alpha = 0.85f), ry * 0.85f, Offset(cx, cy))
    // Pupil — dark accent core
    drawCircle(accent, ry * 0.50f, Offset(cx, cy))
    // Highlight glint
    drawCircle(Color.White, ry * 0.18f, Offset(cx + ry * 0.25f, cy - ry * 0.25f))
}

/** Virus shape — central blob with 8 protrusions radiating outward (biological). */
private fun DrawScope.drawVirus(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val coreR = minOf(w, h) * 0.30f
    val spineLen = minOf(w, h) * 0.22f
    val tipR = minOf(w, h) * 0.07f
    val spines = 8
    for (i in 0 until spines) {
        val a = i * 2.0 * Math.PI / spines
        val ex = cx + ((coreR + spineLen) * cos(a)).toFloat()
        val ey = cy + ((coreR + spineLen) * sin(a)).toFloat()
        drawLine(
            color = body,
            start = Offset(cx, cy),
            end = Offset(ex, ey),
            strokeWidth = w * 0.07f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Knob at tip
        drawCircle(accent, tipR, Offset(ex, ey))
    }
    // Central blob with darker core
    drawCircle(body, coreR, Offset(cx, cy))
    drawCircle(accent, coreR * 0.50f, Offset(cx, cy))
    // Inner dots — virus "RNA"
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.18f,
        Offset(cx - coreR * 0.30f, cy - coreR * 0.15f))
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.15f,
        Offset(cx + coreR * 0.25f, cy + coreR * 0.20f))
}

/** Simple Circle enemy — disc with concentric ring + center pip. */
private fun DrawScope.drawCircleEnemy(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val r = minOf(w, h) * 0.42f
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = w * 0.06f))
    drawCircle(accent, r * 0.40f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.18f, Offset(cx - r * 0.15f, cy - r * 0.15f))
}

/** Spade ♠ — heart shape inverted with stem at bottom (cards suit "bích"). */
private fun DrawScope.drawCardSpade(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.40f
    val halfH = h * 0.40f
    val lobeR = halfW * 0.55f
    // Inverted heart (point up, lobes down) + stem
    drawCircle(body, lobeR, Offset(cx - halfW * 0.45f, cy + halfH * 0.20f))
    drawCircle(body, lobeR, Offset(cx + halfW * 0.45f, cy + halfH * 0.20f))
    val triPath = Path().apply {
        moveTo(cx - halfW, cy + halfH * 0.30f)
        lineTo(cx + halfW, cy + halfH * 0.30f)
        lineTo(cx, cy - halfH)
        close()
    }
    drawPath(triPath, body)
    // Stem at bottom
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy + halfH * 0.30f),
        size = Size(w * 0.10f, h * 0.20f))
    // Triangle stem base
    val baseTri = Path().apply {
        moveTo(cx - w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx + w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx, cy + halfH * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    drawCircle(accent, w * 0.06f, Offset(cx, cy + halfH * 0.05f))
}

/** Club ♣ — 3 circles cluster + stem (cards suit "chuồn"). */
private fun DrawScope.drawCardClub(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val lobeR = minOf(w, h) * 0.20f
    val offsetD = lobeR * 0.85f
    // 3 lobes: top + bottom-left + bottom-right
    drawCircle(body, lobeR, Offset(cx, cy - offsetD))
    drawCircle(body, lobeR, Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(body, lobeR, Offset(cx + offsetD, cy + offsetD * 0.55f))
    // Stem connecting to bottom (small rect)
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy + offsetD * 0.30f),
        size = Size(w * 0.10f, h * 0.25f))
    val baseTri = Path().apply {
        moveTo(cx - w * 0.15f, cy + h * 0.45f)
        lineTo(cx + w * 0.15f, cy + h * 0.45f)
        lineTo(cx, cy + offsetD * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    // Accent in center
    drawCircle(accent, lobeR * 0.40f, Offset(cx, cy - offsetD))
    drawCircle(accent, lobeR * 0.40f, Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(accent, lobeR * 0.40f, Offset(cx + offsetD, cy + offsetD * 0.55f))
}

/** Card Diamond ♦ — vertical rhombus (cards suit "rô"). Same as drawDiamond v0 but
 * with thinner aspect to read as the playing-card glyph. */
private fun DrawScope.drawCardDiamond(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.32f
    val halfH = h * 0.48f
    val path = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent, style = Stroke(width = w * 0.07f))
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.08f, Offset(cx, cy))
}

/** Boss SUN — central disc + outer corona ring + 12 radial flares (R78 #2). */
private fun DrawScope.drawBossSun(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val coreR = minOf(w, h) * 0.30f
    val coronaR = minOf(w, h) * 0.42f
    val flareInner = coronaR * 1.05f
    val flareOuter = coronaR * 1.35f
    // 12 radial flares (alternating long/short for dynamic sun-look).
    for (i in 0 until 12) {
        val a = i * 30.0 * Math.PI / 180.0
        val outerR = if (i % 2 == 0) flareOuter else flareInner * 1.10f
        val sx = cx + (flareInner * kotlin.math.cos(a)).toFloat()
        val sy = cy + (flareInner * kotlin.math.sin(a)).toFloat()
        val ex = cx + (outerR * kotlin.math.cos(a)).toFloat()
        val ey = cy + (outerR * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = accent,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = w * (if (i % 2 == 0) 0.06f else 0.04f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
    // Corona ring (slightly translucent, gives the "atmosphere" look).
    drawCircle(accent.copy(alpha = 0.55f), coronaR, Offset(cx, cy),
        style = Stroke(width = w * 0.05f))
    // Main disc body
    drawCircle(body, coreR, Offset(cx, cy))
    // Inner brightness (white-ish center hot spot)
    drawCircle(Color.White.copy(alpha = 0.85f), coreR * 0.55f, Offset(cx, cy))
    drawCircle(Color.White, coreR * 0.18f, Offset(cx - coreR * 0.20f, cy - coreR * 0.20f))
}

/** Boss ATOM — nucleus + 3 elliptical electron orbits at 60° rotation (R78 #2). */
private fun DrawScope.drawBossAtom(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val nucR = minOf(w, h) * 0.18f
    val orbitRx = minOf(w, h) * 0.48f
    val orbitRy = minOf(w, h) * 0.18f
    // 3 elliptical orbits — at 0°, 60°, 120°. Manual rotation per orbit by
    // computing the bounding box after rotation. Compose Canvas doesn't have a
    // rotate-around-pivot for DrawScope (only top-level), so we use the
    // canvas-saved rotate via `withTransform`.
    for (i in 0 until 3) {
        val deg = i * 60f
        withTransform(
            transformBlock = { rotate(degrees = deg, pivot = Offset(cx, cy)) },
            drawBlock = {
                drawOval(
                    color = accent,
                    topLeft = Offset(cx - orbitRx, cy - orbitRy),
                    size = Size(orbitRx * 2, orbitRy * 2),
                    style = Stroke(width = w * 0.04f),
                )
                drawCircle(body, w * 0.05f, Offset(cx + orbitRx, cy))
            },
        )
    }
    // Central nucleus — 2-layer disc
    drawCircle(body, nucR, Offset(cx, cy))
    drawCircle(accent, nucR * 0.55f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), nucR * 0.30f, Offset(cx, cy))
}

/** Boss EYE — large menacing eye with iris + pupil + outer eyelid. Replaces drawBossOrb. */
private fun DrawScope.drawBossEye(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.48f
    val midR = outerR * 0.75f
    val innerR = outerR * 0.45f
    val pupilR = outerR * 0.22f
    // Outer "eyelid" body
    drawCircle(body, outerR, Offset(cx, cy))
    drawCircle(accent, outerR, Offset(cx, cy), style = Stroke(width = w * 0.04f))
    // White sclera
    drawCircle(Color.White.copy(alpha = 0.92f), midR, Offset(cx, cy))
    // Iris (body color)
    drawCircle(body, innerR, Offset(cx, cy))
    drawCircle(accent, innerR, Offset(cx, cy), style = Stroke(width = w * 0.03f))
    // Pupil — black/dark accent
    drawCircle(accent, pupilR, Offset(cx, cy))
    drawCircle(Color.Black.copy(alpha = 0.85f), pupilR * 0.85f, Offset(cx, cy))
    // Highlight
    drawCircle(Color.White, pupilR * 0.35f, Offset(cx + pupilR * 0.40f, cy - pupilR * 0.45f))
    // 6 radial "lashes" around outer rim — eldritch detail
    for (i in 0 until 6) {
        val a = i * 60.0 * Math.PI / 180.0
        val sx = cx + ((outerR + outerR * 0.08f) * kotlin.math.cos(a)).toFloat()
        val sy = cy + ((outerR + outerR * 0.08f) * kotlin.math.sin(a)).toFloat()
        val ex = cx + ((outerR + outerR * 0.30f) * kotlin.math.cos(a)).toFloat()
        val ey = cy + ((outerR + outerR * 0.30f) * kotlin.math.sin(a)).toFloat()
        drawLine(accent, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
