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
    // Round 82 — 10 R81 enemies, each unique body color (themed).
    R.drawable.enemy_spinning_saw -> Color(0xFFCCCCCC)               // steel
    R.drawable.enemy_tentacle_squid -> Color(0xFF60E0C0)             // teal
    R.drawable.enemy_mine_layer -> Color(0xFF707080)                 // gray
    R.drawable.enemy_shield_drone -> Color(0xFF40A0FF)               // sky blue
    R.drawable.enemy_sniper -> Color(0xFF8B5C2E)                     // brown wood
    R.drawable.enemy_bomber_crawler -> Color(0xFF606040)             // olive
    R.drawable.enemy_mirror_twin -> Color(0xFFFF60A0)                // pink twin
    R.drawable.enemy_phantom -> Color(0xFFB14CFF)                    // violet
    R.drawable.enemy_healer -> Color(0xFF60FFAA)                     // mint
    R.drawable.enemy_kamikaze -> Color(0xFFFFE040)                   // warning yellow
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
    // Round 82 — accents for 10 R81 enemies.
    R.drawable.enemy_spinning_saw -> Color(0xFFFF2D55)
    R.drawable.enemy_tentacle_squid -> Color(0xFFFFD040)
    R.drawable.enemy_mine_layer -> Color(0xFFFF2D55)
    R.drawable.enemy_shield_drone -> Color(0xFF80E0FF)
    R.drawable.enemy_sniper -> Color(0xFF404040)
    R.drawable.enemy_bomber_crawler -> Color(0xFFFFE040)
    R.drawable.enemy_mirror_twin -> Color(0xFFFFFFFF)
    R.drawable.enemy_phantom -> Color(0xFFE8E8F0)
    R.drawable.enemy_healer -> Color(0xFFFFFFFF)
    R.drawable.enemy_kamikaze -> Color(0xFFFF6020)
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
            // Round 79 (#1) — 4 new boss shapes for chapter visual uniqueness.
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.DEATH_MOON ->
                drawBossDeathMoon(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HAUNTED_KID ->
                drawBossHauntedKid(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HELL_LORD ->
                drawBossHellLord(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SATAN_GLYPH ->
                drawBossSatanGlyph(cx, cy, wPx, hPx, body, accent)
            // Round 81 — 12 new bosses dispatch.
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HEN_MOTHER ->
                drawBossHenMother(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.BUFFALO_RAGE ->
                drawBossBuffalo(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.DUMB_RAT ->
                drawBossRat(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FIERCE_TIGER ->
                drawBossTiger(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SEXY_DIVA ->
                drawBossDiva(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TROLL_TOWER ->
                drawBossTrollTower(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TWIN_SUMMITS ->
                drawBossTwinSummits(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.VOID_GLOBES ->
                drawBossVoidGlobes(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.WHITE_DRAGON ->
                drawBossWhiteDragon(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HAMMER_SICKLE ->
                drawBossHammerSickle(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.MONEY_TYCOON ->
                drawBossMoneyTycoon(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.GOLDEN_TYCOON ->
                drawBossGoldenTycoon(cx, cy, wPx, hPx, body, accent)
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
        // Round 74 (R73d) + Round 80 (#0) — ELITE/BERSERKER variants. Variant 1
        // của mỗi family được remap sang "weird flying ship" silhouettes per user
        // feedback "share enemy thêm các shape là tàu bay kì quặc". Front faces
        // DOWN toward player → reads as face-to-face confrontation.
        // ELITE: cross + UFO (violet).
        R.drawable.enemy_cross_1 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_cross_2 -> drawEnemyUFO(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_orb_1 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_orb_2 -> drawEnemyDrone(cx, cy, wPx, hPx, body, accent)
        // BERSERKER: chevron + MantaRay / Mech (orange).
        R.drawable.enemy_chevron_1 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_chevron_2 -> drawEnemyMantaRay(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_spike_1 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_spike_2 -> drawEnemyMech(cx, cy, wPx, hPx, body, accent)
        // Round 82 — 10 R81 new enemy in-game dispatch.
        R.drawable.enemy_spinning_saw -> drawEnemySpinningSaw(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_tentacle_squid -> drawEnemyTentacleSquid(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_mine_layer -> drawEnemyMineLayer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_shield_drone -> drawEnemyShieldDrone(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_sniper -> drawEnemySniper(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_bomber_crawler -> drawEnemyBomberCrawler(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_mirror_twin -> drawEnemyMirrorTwin(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_phantom -> drawEnemyPhantom(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_healer -> drawEnemyHealer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_kamikaze -> drawEnemyKamikaze(cx, cy, wPx, hPx, body, accent)
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

// ─────────────────────────────────────────────────────────────────────────
// Round 79 (#1) — 4 new boss shape recipes to give each chapter encounter a
// unique silhouette (was 5 distinct over 9 encounters).
// ─────────────────────────────────────────────────────────────────────────

/** Boss DEATH MOON — full moon disc with skull-crater details + crack fissures. */
private fun DrawScope.drawBossDeathMoon(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val r = minOf(w, h) * 0.45f
    // Main moon body
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = w * 0.04f))
    // 2 large craters (skull eye sockets)
    val socketR = r * 0.18f
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        Offset(cx - r * 0.30f, cy - r * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        Offset(cx + r * 0.30f, cy - r * 0.15f))
    // Nose (triangular dark)
    val nosePath = Path().apply {
        moveTo(cx, cy + r * 0.05f)
        lineTo(cx - r * 0.10f, cy + r * 0.20f)
        lineTo(cx + r * 0.10f, cy + r * 0.20f)
        close()
    }
    drawPath(nosePath, Color.Black.copy(alpha = 0.70f))
    // Teeth row (mouth grin)
    val teethCount = 5
    val teethY = cy + r * 0.40f
    val teethStartX = cx - r * 0.30f
    val teethWidth = r * 0.60f
    val teethStep = teethWidth / teethCount
    val teethH = r * 0.12f
    for (i in 0 until teethCount) {
        drawRect(Color.Black.copy(alpha = 0.70f),
            topLeft = Offset(teethStartX + i * teethStep + teethStep * 0.10f, teethY),
            size = Size(teethStep * 0.80f, teethH))
    }
    // Crack fissures (4 jagged lines radiating from center)
    for (i in 0 until 3) {
        val ang = (i * 70.0 + 100.0) * Math.PI / 180.0
        val sx = cx + (r * 0.55f * kotlin.math.cos(ang)).toFloat()
        val sy = cy + (r * 0.55f * kotlin.math.sin(ang)).toFloat()
        val ex = cx + (r * 0.95f * kotlin.math.cos(ang)).toFloat()
        val ey = cy + (r * 0.95f * kotlin.math.sin(ang)).toFloat()
        drawLine(accent.copy(alpha = 0.6f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.025f)
    }
    // Highlight glow upper-left
    drawCircle(Color.White.copy(alpha = 0.20f), r * 0.30f,
        Offset(cx - r * 0.40f, cy - r * 0.45f))
}

/** Boss HAUNTED KID — ghost child silhouette with hollow eyes + wavy bottom. */
private fun DrawScope.drawBossHauntedKid(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val headR = minOf(w, h) * 0.25f
    val bodyW = w * 0.55f
    val bodyH = h * 0.50f
    // Body — bell-shape sheet from top-rounded to wavy bottom
    val bodyPath = Path().apply {
        // Top arc (head connects)
        moveTo(cx - bodyW / 2f, cy - bodyH * 0.10f)
        // Left side down
        lineTo(cx - bodyW / 2f, cy + bodyH * 0.40f)
        // Wavy bottom — 5 waves
        val waveCount = 5
        val waveStep = bodyW / waveCount
        for (i in 0..waveCount) {
            val wx = cx - bodyW / 2f + i * waveStep
            val wy = if (i % 2 == 0) cy + bodyH * 0.50f else cy + bodyH * 0.35f
            lineTo(wx, wy)
        }
        // Right side up
        lineTo(cx + bodyW / 2f, cy - bodyH * 0.10f)
        // Top arc (close via head — go up)
        lineTo(cx - bodyW / 2f, cy - bodyH * 0.10f)
        close()
    }
    drawPath(bodyPath, body.copy(alpha = 0.85f))
    // Head — circle on top
    drawCircle(body.copy(alpha = 0.85f), headR, Offset(cx, cy - bodyH * 0.30f))
    // Hollow eyes (2 dark sockets)
    val eyeR = headR * 0.30f
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        Offset(cx - headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        Offset(cx + headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    // Glowing eye-pupil
    drawCircle(accent, eyeR * 0.40f,
        Offset(cx - headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    drawCircle(accent, eyeR * 0.40f,
        Offset(cx + headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    // Open mouth (small "O" of horror)
    val mouthY = cy - bodyH * 0.30f + headR * 0.30f
    drawCircle(Color.Black.copy(alpha = 0.85f), headR * 0.20f, Offset(cx, mouthY))
    // Outline highlight
    drawCircle(Color.White.copy(alpha = 0.3f), headR * 0.5f,
        Offset(cx - headR * 0.25f, cy - bodyH * 0.30f - headR * 0.30f))
}

/** Boss HELL LORD — devil head with 2 curved horns + glowing eyes + fangs. */
private fun DrawScope.drawBossHellLord(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val headR = minOf(w, h) * 0.36f
    // Main head (oval-ish, slightly elongated down)
    drawOval(body,
        topLeft = Offset(cx - headR, cy - headR * 0.90f),
        size = Size(headR * 2, headR * 2.0f))
    drawOval(accent,
        topLeft = Offset(cx - headR, cy - headR * 0.90f),
        size = Size(headR * 2, headR * 2.0f),
        style = Stroke(width = w * 0.035f))
    // 2 curved horns
    for (sign in intArrayOf(-1, 1)) {
        val baseX = cx + sign * headR * 0.70f
        val baseY = cy - headR * 0.70f
        val midX = cx + sign * headR * 1.10f
        val midY = cy - headR * 1.30f
        val tipX = cx + sign * headR * 0.85f
        val tipY = cy - headR * 1.70f
        val hornPath = Path().apply {
            moveTo(baseX, baseY)
            cubicTo(midX, midY, midX * 1.05f, midY, tipX, tipY)
            // Inner curve back
            cubicTo(midX * 0.85f, midY * 1.10f, baseX + sign * headR * 0.10f, baseY - headR * 0.05f, baseX, baseY)
            close()
        }
        drawPath(hornPath, body)
        drawPath(hornPath, accent, style = Stroke(width = w * 0.03f))
    }
    // Glowing eyes (2 red slits)
    val eyeY = cy - headR * 0.10f
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = Offset(cx - headR * 0.55f, eyeY - headR * 0.08f),
        size = Size(headR * 0.40f, headR * 0.18f))
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = Offset(cx + headR * 0.15f, eyeY - headR * 0.08f),
        size = Size(headR * 0.40f, headR * 0.18f))
    // Eye glints
    drawCircle(Color.White, headR * 0.06f,
        Offset(cx - headR * 0.40f, eyeY - headR * 0.03f))
    drawCircle(Color.White, headR * 0.06f,
        Offset(cx + headR * 0.30f, eyeY - headR * 0.03f))
    // Mouth grin with fangs
    val mouthY = cy + headR * 0.40f
    drawArc(color = Color.Black.copy(alpha = 0.80f),
        startAngle = 20f, sweepAngle = 140f,
        useCenter = true,
        topLeft = Offset(cx - headR * 0.45f, mouthY - headR * 0.20f),
        size = Size(headR * 0.90f, headR * 0.45f))
    // 4 fangs (white triangles dropping from upper lip)
    for (i in 0 until 4) {
        val fx = cx - headR * 0.30f + i * headR * 0.20f
        val fpath = Path().apply {
            moveTo(fx - headR * 0.05f, mouthY)
            lineTo(fx + headR * 0.05f, mouthY)
            lineTo(fx, mouthY + headR * 0.15f)
            close()
        }
        drawPath(fpath, Color.White.copy(alpha = 0.85f))
    }
}

/** Boss SATAN GLYPH — inverted pentagram with all-seeing eye in center. */
private fun DrawScope.drawBossSatanGlyph(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.45f
    val innerR = outerR * 0.40f
    // Outer protective ring
    drawCircle(body, outerR * 1.10f, Offset(cx, cy),
        style = Stroke(width = w * 0.045f))
    drawCircle(accent.copy(alpha = 0.55f), outerR * 1.10f, Offset(cx, cy))
    // Inverted pentagram (5-point star, point DOWN)
    val starPath = Path().apply {
        val rotation = Math.PI / 2.0  // start pointing down
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, body)
    drawPath(starPath, Color.White.copy(alpha = 0.65f),
        style = Stroke(width = w * 0.025f))
    // Central all-seeing eye (oval body + iris + pupil)
    val eyeRx = innerR * 0.85f
    val eyeRy = innerR * 0.55f
    drawOval(Color.White,
        topLeft = Offset(cx - eyeRx, cy - eyeRy),
        size = Size(eyeRx * 2, eyeRy * 2))
    drawOval(accent,
        topLeft = Offset(cx - eyeRx, cy - eyeRy),
        size = Size(eyeRx * 2, eyeRy * 2),
        style = Stroke(width = w * 0.025f))
    drawCircle(accent, eyeRy * 0.65f, Offset(cx, cy))
    drawCircle(Color.Black, eyeRy * 0.35f, Offset(cx, cy))
    drawCircle(Color.White, eyeRy * 0.10f, Offset(cx + eyeRy * 0.20f, cy - eyeRy * 0.15f))
    // 5 small candle dots at outer star tips
    for (i in 0 until 5) {
        val a = (Math.PI / 2.0) + i * 2 * Math.PI / 5
        val px = cx + (outerR * 1.15f * kotlin.math.cos(a)).toFloat()
        val py = cy + (outerR * 1.15f * kotlin.math.sin(a)).toFloat()
        drawCircle(accent, w * 0.025f, Offset(px, py))
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 80 (#0) — 4 weird flying ship enemy shapes. Front faces DOWN (toward
// player). User feedback: "share enemy thêm các shape là tàu bay kì quặc,
// đối địch face to face với ship player".
// ─────────────────────────────────────────────────────────────────────────

/** UFO Saucer — oval disc with dome on top + light row at bottom. */
private fun DrawScope.drawEnemyUFO(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val saucerRx = w * 0.42f
    val saucerRy = h * 0.15f
    // Main saucer disc (wide ellipse)
    drawOval(body,
        topLeft = Offset(cx - saucerRx, cy + h * 0.10f - saucerRy),
        size = Size(saucerRx * 2, saucerRy * 2))
    drawOval(accent,
        topLeft = Offset(cx - saucerRx, cy + h * 0.10f - saucerRy),
        size = Size(saucerRx * 2, saucerRy * 2),
        style = Stroke(width = w * 0.025f))
    // Dome (smaller half-ellipse on top — alien cockpit)
    val domeRx = saucerRx * 0.50f
    val domeRy = h * 0.22f
    drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - domeRx, cy + h * 0.10f - saucerRy - domeRy),
        size = Size(domeRx * 2, domeRy * 2))
    drawArc(accent, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - domeRx, cy + h * 0.10f - saucerRy - domeRy),
        size = Size(domeRx * 2, domeRy * 2),
        style = Stroke(width = w * 0.025f))
    // Cockpit glow (alien pilot visible inside)
    drawCircle(Color.White.copy(alpha = 0.6f), domeRx * 0.40f,
        Offset(cx, cy + h * 0.10f - saucerRy - domeRy * 0.40f))
    // 5 underside lights pulsing
    val lightY = cy + h * 0.20f
    for (i in 0 until 5) {
        val lx = cx - saucerRx * 0.65f + i * saucerRx * 0.32f
        drawCircle(accent, w * 0.025f, Offset(lx, lightY))
        drawCircle(Color.White, w * 0.012f, Offset(lx, lightY))
    }
}

/** Drone Quad — 4 rotors around square body (face DOWN with cannon protrusion). */
private fun DrawScope.drawEnemyDrone(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val bodyW = w * 0.35f
    val bodyH = h * 0.30f
    // Central body (rotated square)
    drawRect(body,
        topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = Size(bodyW, bodyH))
    drawRect(accent,
        topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = Size(bodyW, bodyH),
        style = Stroke(width = w * 0.025f))
    // 4 rotor arms (cross-arm extending to 4 corners)
    val armLen = w * 0.45f
    for (sign in intArrayOf(-1, 1)) for (vsign in intArrayOf(-1, 1)) {
        val ex = cx + sign * armLen * 0.85f
        val ey = cy + vsign * armLen * 0.65f
        drawLine(body,
            Offset(cx + sign * bodyW * 0.35f, cy + vsign * bodyH * 0.30f),
            Offset(ex, ey),
            strokeWidth = w * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Rotor disc (small circle at end)
        drawCircle(accent, w * 0.06f, Offset(ex, ey))
        drawCircle(Color.White.copy(alpha = 0.65f), w * 0.03f, Offset(ex, ey))
    }
    // Cannon protrusion (face DOWN toward player)
    val cannonW = w * 0.08f
    drawRect(accent,
        topLeft = Offset(cx - cannonW / 2f, cy + bodyH / 2f),
        size = Size(cannonW, h * 0.18f))
    // Sensor "eye" (red dot) on body — alien aggressive look
    drawCircle(Color(0xFFFF2D55), w * 0.05f, Offset(cx, cy))
}

/** Manta Ray Ship — wide delta with tail (gliding alien ship). */
private fun DrawScope.drawEnemyMantaRay(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    // Wide delta-wing body
    val wingPath = Path().apply {
        moveTo(cx, cy + h * 0.45f)              // tail tip (rear)
        cubicTo(cx + w * 0.20f, cy + h * 0.20f,
            cx + w * 0.45f, cy + h * 0.05f,
            cx + w * 0.48f, cy - h * 0.15f)     // right wingtip
        cubicTo(cx + w * 0.30f, cy - h * 0.30f,
            cx + w * 0.10f, cy - h * 0.30f,
            cx, cy - h * 0.18f)                 // front center (head)
        cubicTo(cx - w * 0.10f, cy - h * 0.30f,
            cx - w * 0.30f, cy - h * 0.30f,
            cx - w * 0.48f, cy - h * 0.15f)     // left wingtip
        cubicTo(cx - w * 0.45f, cy + h * 0.05f,
            cx - w * 0.20f, cy + h * 0.20f,
            cx, cy + h * 0.45f)
        close()
    }
    drawPath(wingPath, body)
    drawPath(wingPath, accent, style = Stroke(width = w * 0.025f))
    // 2 dorsal eyes (top of head)
    drawCircle(accent, w * 0.05f, Offset(cx - w * 0.07f, cy - h * 0.20f))
    drawCircle(accent, w * 0.05f, Offset(cx + w * 0.07f, cy - h * 0.20f))
    drawCircle(Color.White, w * 0.02f, Offset(cx - w * 0.07f, cy - h * 0.20f))
    drawCircle(Color.White, w * 0.02f, Offset(cx + w * 0.07f, cy - h * 0.20f))
    // Spine ridge (3 dots running back)
    for (i in 0 until 3) {
        drawCircle(accent.copy(alpha = 0.85f), w * 0.025f,
            Offset(cx, cy - h * 0.05f + i * h * 0.12f))
    }
}

/** Battle Mech — boxy alien ship with twin side cannons + viewport. */
private fun DrawScope.drawEnemyMech(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val bodyW = w * 0.50f
    val bodyH = h * 0.55f
    // Main hull (slightly trapezoidal)
    val hullPath = Path().apply {
        moveTo(cx - bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW / 2f, cy + bodyH / 2f)
        lineTo(cx - bodyW / 2f, cy + bodyH / 2f)
        close()
    }
    drawPath(hullPath, body)
    drawPath(hullPath, accent, style = Stroke(width = w * 0.03f))
    // Viewport (top-front of hull) — face DOWN reading direction
    drawRect(accent,
        topLeft = Offset(cx - bodyW * 0.25f, cy - bodyH * 0.30f),
        size = Size(bodyW * 0.50f, bodyH * 0.15f))
    drawRect(Color.White.copy(alpha = 0.6f),
        topLeft = Offset(cx - bodyW * 0.22f, cy - bodyH * 0.28f),
        size = Size(bodyW * 0.44f, bodyH * 0.10f))
    // 2 side cannons (sticking out from sides at front)
    for (sign in intArrayOf(-1, 1)) {
        val cannonX = cx + sign * bodyW * 0.55f
        drawRect(body,
            topLeft = Offset(cannonX - w * 0.04f, cy - bodyH * 0.20f),
            size = Size(w * 0.08f, bodyH * 0.50f))
        // Cannon tip glow
        drawCircle(accent, w * 0.04f, Offset(cannonX, cy + bodyH * 0.30f))
    }
    // Antenna / sensor on top
    drawLine(accent,
        Offset(cx, cy - bodyH / 2f),
        Offset(cx, cy - bodyH * 0.75f),
        strokeWidth = w * 0.025f)
    drawCircle(Color(0xFFFF2D55), w * 0.03f, Offset(cx, cy - bodyH * 0.78f))
    // Center body marking (cross)
    drawLine(accent.copy(alpha = 0.6f),
        Offset(cx - w * 0.05f, cy), Offset(cx + w * 0.05f, cy),
        strokeWidth = w * 0.02f)
    drawLine(accent.copy(alpha = 0.6f),
        Offset(cx, cy - h * 0.05f), Offset(cx, cy + h * 0.05f),
        strokeWidth = w * 0.02f)
}

// ─────────────────────────────────────────────────────────────────────────
// Round 81 — 12 new boss shape recipes per user roster. Each distinct
// silhouette. NSFW versions are tasteful abstract (no literal anatomy
// render). Trump caricature uses generic "Tycoon" name (no real name).
// ─────────────────────────────────────────────────────────────────────────

/** Hen Mother — chicken silhouette: oval body + small head + beak + comb + 2 legs. */
private fun DrawScope.drawBossHenMother(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyRx = w * 0.30f; val bodyRy = h * 0.25f
    // Egg-shape body
    drawOval(body, topLeft = Offset(cx - bodyRx, cy - bodyRy * 0.5f), size = Size(bodyRx * 2, bodyRy * 2))
    // Head circle
    val headR = w * 0.13f
    drawCircle(body, headR, Offset(cx - bodyRx * 0.85f, cy - bodyRy * 0.85f))
    // Beak (yellow triangle)
    val beakPath = Path().apply {
        moveTo(cx - bodyRx * 1.20f, cy - bodyRy * 0.85f)
        lineTo(cx - bodyRx * 1.50f, cy - bodyRy * 0.95f)
        lineTo(cx - bodyRx * 1.20f, cy - bodyRy * 0.65f)
        close()
    }
    drawPath(beakPath, Color(0xFFFFC020))
    // Comb (red 3-prong on top of head)
    for (i in 0 until 3) {
        val cx2 = cx - bodyRx * 0.85f - headR * 0.20f + i * headR * 0.20f
        val tipY = cy - bodyRy * 0.85f - headR - i * headR * 0.10f
        drawCircle(Color(0xFFFF2D55), headR * 0.18f, Offset(cx2, tipY))
    }
    // Eye
    drawCircle(Color.Black, headR * 0.18f, Offset(cx - bodyRx * 0.95f, cy - bodyRy * 0.95f))
    drawCircle(Color.White, headR * 0.07f, Offset(cx - bodyRx * 0.97f, cy - bodyRy * 0.97f))
    // 2 legs
    for (side in intArrayOf(-1, 1)) {
        drawLine(accent,
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 0.85f),
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 1.30f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Foot
        drawLine(accent,
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 1.30f),
            Offset(cx + side * bodyRx * 0.45f, cy + bodyRy * 1.35f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Wing (curved)
    drawArc(accent, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - bodyRx * 0.5f, cy - bodyRy * 0.3f),
        size = Size(bodyRx * 1.0f, bodyRy * 0.8f),
        style = Stroke(width = w * 0.04f))
    // Tail feathers (3 strokes at back)
    for (i in 0 until 3) {
        val a = (30.0 + i * 25) * Math.PI / 180.0
        val sx = cx + bodyRx * 0.95f
        val sy = cy
        val ex = sx + (w * 0.18f * kotlin.math.cos(a)).toFloat()
        val ey = sy + (w * 0.18f * kotlin.math.sin(a)).toFloat() - h * 0.05f
        drawLine(accent, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Buffalo Rage — bull head silhouette với 2 curved horns + nose ring + ferocious eyes. */
private fun DrawScope.drawBossBuffalo(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headRx = w * 0.32f; val headRy = h * 0.30f
    // Head (oval)
    drawOval(body, topLeft = Offset(cx - headRx, cy - headRy), size = Size(headRx * 2, headRy * 2))
    // 2 curved horns (large)
    for (side in intArrayOf(-1, 1)) {
        val baseX = cx + side * headRx * 0.85f
        val baseY = cy - headRy * 0.50f
        val tipX = cx + side * headRx * 1.50f
        val tipY = cy - headRy * 1.10f
        val hornPath = Path().apply {
            moveTo(baseX, baseY)
            cubicTo(cx + side * headRx * 1.20f, cy - headRy * 0.85f,
                cx + side * headRx * 1.50f, cy - headRy * 0.95f,
                tipX, tipY)
            cubicTo(cx + side * headRx * 1.30f, cy - headRy * 0.75f,
                cx + side * headRx * 1.05f, cy - headRy * 0.40f,
                baseX, baseY)
            close()
        }
        drawPath(hornPath, body)
        drawPath(hornPath, Color.White.copy(alpha = 0.5f), style = Stroke(width = w * 0.02f))
    }
    // Snout (lighter front)
    drawOval(accent.copy(alpha = 0.7f),
        topLeft = Offset(cx - headRx * 0.4f, cy + headRy * 0.25f),
        size = Size(headRx * 0.8f, headRy * 0.55f))
    // Nose ring
    drawCircle(Color(0xFFFFC020), headRx * 0.15f, Offset(cx, cy + headRy * 0.50f),
        style = Stroke(width = w * 0.04f))
    // 2 nostrils
    drawCircle(Color.Black, headRx * 0.06f, Offset(cx - headRx * 0.12f, cy + headRy * 0.35f))
    drawCircle(Color.Black, headRx * 0.06f, Offset(cx + headRx * 0.12f, cy + headRy * 0.35f))
    // 2 angry eyes
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, Offset(cx - headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, Offset(cx + headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color.White, headRx * 0.04f, Offset(cx - headRx * 0.30f, cy - headRy * 0.20f))
    drawCircle(Color.White, headRx * 0.04f, Offset(cx + headRx * 0.40f, cy - headRy * 0.20f))
}

/** Dumb Rat — large mouse head với 2 round ears + long whiskers + buck teeth. */
private fun DrawScope.drawBossRat(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headR = w * 0.30f
    // Head circle
    drawCircle(body, headR, Offset(cx, cy))
    // 2 large round ears (mouse style)
    drawCircle(body, headR * 0.55f, Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(body, headR * 0.55f, Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    // Snout (smaller circle on bottom)
    drawCircle(body, headR * 0.55f, Offset(cx, cy + headR * 0.55f))
    // Nose
    drawCircle(Color(0xFFFF80B0), headR * 0.10f, Offset(cx, cy + headR * 0.40f))
    // 2 buck teeth (long rectangles below nose)
    drawRect(Color.White,
        topLeft = Offset(cx - headR * 0.10f, cy + headR * 0.55f),
        size = Size(headR * 0.08f, headR * 0.30f))
    drawRect(Color.White,
        topLeft = Offset(cx + headR * 0.02f, cy + headR * 0.55f),
        size = Size(headR * 0.08f, headR * 0.30f))
    // Eyes (dumb confused look)
    drawCircle(Color.White, headR * 0.18f, Offset(cx - headR * 0.30f, cy - headR * 0.10f))
    drawCircle(Color.White, headR * 0.18f, Offset(cx + headR * 0.30f, cy - headR * 0.10f))
    drawCircle(Color.Black, headR * 0.10f, Offset(cx - headR * 0.28f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.10f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    // Whiskers (3 each side)
    for (side in intArrayOf(-1, 1)) for (i in 0 until 3) {
        val ang = (i - 1) * 15.0 * Math.PI / 180.0
        val sx = cx + side * headR * 0.20f
        val sy = cy + headR * 0.40f
        val ex = sx + side * (headR * 0.85f) * kotlin.math.cos(ang).toFloat()
        val ey = sy + (headR * 0.50f) * kotlin.math.sin(ang).toFloat()
        drawLine(accent.copy(alpha = 0.85f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.015f)
    }
}

/** Fierce Tiger — fierce tiger head với stripes + fangs + mane fluff. */
private fun DrawScope.drawBossTiger(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headR = w * 0.34f
    val tigerOrange = Color(0xFFFF9020)
    val stripeBlack = Color(0xFF202020)
    // Mane fluff (8 spikes around)
    for (i in 0 until 8) {
        val a = i * 2.0 * Math.PI / 8
        val sx = cx + (headR * 0.95f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (headR * 0.95f * kotlin.math.sin(a)).toFloat()
        val ex = cx + (headR * 1.25f * kotlin.math.cos(a)).toFloat()
        val ey = cy + (headR * 1.25f * kotlin.math.sin(a)).toFloat()
        drawLine(tigerOrange, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Main head (orange)
    drawCircle(tigerOrange, headR, Offset(cx, cy))
    // Stripes (5 black curves across face)
    for (i in 0 until 5) {
        val angOff = (i - 2) * 18.0 * Math.PI / 180.0
        drawArc(stripeBlack,
            startAngle = (40f + i * 25f), sweepAngle = 25f,
            useCenter = false,
            topLeft = Offset(cx - headR * 0.85f, cy - headR * 0.55f),
            size = Size(headR * 1.7f, headR * 1.5f),
            style = Stroke(width = w * 0.03f))
    }
    // 2 ears (triangular)
    for (side in intArrayOf(-1, 1)) {
        val earPath = Path().apply {
            moveTo(cx + side * headR * 0.60f, cy - headR * 0.95f)
            lineTo(cx + side * headR * 0.40f, cy - headR * 0.40f)
            lineTo(cx + side * headR * 0.85f, cy - headR * 0.55f)
            close()
        }
        drawPath(earPath, tigerOrange)
    }
    // Eyes (fierce yellow)
    drawCircle(Color(0xFFFFE040), headR * 0.18f, Offset(cx - headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color(0xFFFFE040), headR * 0.18f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, Offset(cx - headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    // Snout + open mouth with fangs
    drawCircle(Color.White.copy(alpha = 0.8f), headR * 0.18f, Offset(cx, cy + headR * 0.30f))
    drawCircle(Color.Black, headR * 0.08f, Offset(cx, cy + headR * 0.25f))
    // 2 fangs (white triangles going down from mouth)
    for (side in intArrayOf(-1, 1)) {
        val fpath = Path().apply {
            moveTo(cx + side * headR * 0.12f, cy + headR * 0.40f)
            lineTo(cx + side * headR * 0.04f, cy + headR * 0.40f)
            lineTo(cx + side * headR * 0.08f, cy + headR * 0.65f)
            close()
        }
        drawPath(fpath, Color.White)
    }
}

/** Sexy Diva — feminine silhouette with hourglass shape + hair flow + crown gem. */
private fun DrawScope.drawBossDiva(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val divaPink = Color(0xFFFF60A0)
    // Hair (long flowing on sides)
    for (side in intArrayOf(-1, 1)) {
        val hairPath = Path().apply {
            moveTo(cx + side * w * 0.10f, cy - h * 0.40f)
            cubicTo(cx + side * w * 0.40f, cy - h * 0.30f,
                cx + side * w * 0.45f, cy - h * 0.10f,
                cx + side * w * 0.50f, cy + h * 0.20f)
            cubicTo(cx + side * w * 0.55f, cy + h * 0.30f,
                cx + side * w * 0.50f, cy + h * 0.40f,
                cx + side * w * 0.45f, cy + h * 0.50f)
            lineTo(cx + side * w * 0.20f, cy)
            close()
        }
        drawPath(hairPath, body)
    }
    // Head (round)
    drawCircle(divaPink, w * 0.15f, Offset(cx, cy - h * 0.25f))
    // Crown gem (top of head)
    val gemPath = Path().apply {
        moveTo(cx, cy - h * 0.48f)
        lineTo(cx + w * 0.06f, cy - h * 0.38f)
        lineTo(cx, cy - h * 0.30f)
        lineTo(cx - w * 0.06f, cy - h * 0.38f)
        close()
    }
    drawPath(gemPath, Color(0xFFFFD700))
    drawPath(gemPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = w * 0.015f))
    // Eyes (closed/sultry — 2 curved lines)
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.30f),
        size = Size(w * 0.06f, h * 0.04f),
        style = Stroke(width = w * 0.012f))
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + w * 0.04f, cy - h * 0.30f),
        size = Size(w * 0.06f, h * 0.04f),
        style = Stroke(width = w * 0.012f))
    // Lips (heart-shape red)
    drawCircle(Color(0xFFCC2030), w * 0.018f, Offset(cx - w * 0.025f, cy - h * 0.20f))
    drawCircle(Color(0xFFCC2030), w * 0.018f, Offset(cx + w * 0.025f, cy - h * 0.20f))
    drawPath(Path().apply {
        moveTo(cx - w * 0.04f, cy - h * 0.18f)
        lineTo(cx + w * 0.04f, cy - h * 0.18f)
        lineTo(cx, cy - h * 0.13f)
        close()
    }, Color(0xFFCC2030))
    // Body (hourglass — neck + shoulders + waist + hips)
    val bodyPath = Path().apply {
        moveTo(cx - w * 0.06f, cy - h * 0.12f)
        cubicTo(cx - w * 0.25f, cy - h * 0.05f, cx - w * 0.30f, cy + h * 0.05f, cx - w * 0.10f, cy + h * 0.15f)
        cubicTo(cx - w * 0.25f, cy + h * 0.30f, cx - w * 0.28f, cy + h * 0.40f, cx - w * 0.10f, cy + h * 0.50f)
        lineTo(cx + w * 0.10f, cy + h * 0.50f)
        cubicTo(cx + w * 0.28f, cy + h * 0.40f, cx + w * 0.25f, cy + h * 0.30f, cx + w * 0.10f, cy + h * 0.15f)
        cubicTo(cx + w * 0.30f, cy + h * 0.05f, cx + w * 0.25f, cy - h * 0.05f, cx + w * 0.06f, cy - h * 0.12f)
        close()
    }
    drawPath(bodyPath, body)
}

/** Troll Tower — tall pointed obelisk tower with crown + glowing tip (tasteful phallic). */
private fun DrawScope.drawBossTrollTower(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    // Tall tapered spire (very tall, narrow)
    val spirePath = Path().apply {
        moveTo(cx, cy - h * 0.45f)
        lineTo(cx + w * 0.10f, cy - h * 0.30f)
        lineTo(cx + w * 0.15f, cy + h * 0.30f)
        lineTo(cx + w * 0.22f, cy + h * 0.45f)
        lineTo(cx - w * 0.22f, cy + h * 0.45f)
        lineTo(cx - w * 0.15f, cy + h * 0.30f)
        lineTo(cx - w * 0.10f, cy - h * 0.30f)
        close()
    }
    drawPath(spirePath, body)
    drawPath(spirePath, accent, style = Stroke(width = w * 0.025f))
    // Crown of jewels at top
    for (i in 0 until 5) {
        val px = cx - w * 0.10f + i * w * 0.05f
        drawCircle(Color(0xFFFFD700), w * 0.022f, Offset(px, cy - h * 0.42f))
    }
    // Tip glow (bright white)
    drawCircle(Color.White, w * 0.06f, Offset(cx, cy - h * 0.45f))
    drawCircle(Color.White.copy(alpha = 0.5f), w * 0.10f, Offset(cx, cy - h * 0.45f))
    // Energy rings around tip (3 expanding circles)
    for (i in 0 until 3) {
        drawCircle(accent.copy(alpha = 0.4f - i * 0.10f), w * (0.12f + i * 0.06f),
            Offset(cx, cy - h * 0.42f),
            style = Stroke(width = w * 0.012f))
    }
    // Stripes on shaft (3 horizontal)
    for (i in 0 until 3) {
        val y = cy - h * 0.20f + i * h * 0.20f
        drawLine(accent.copy(alpha = 0.6f),
            Offset(cx - w * 0.14f, y), Offset(cx + w * 0.14f, y),
            strokeWidth = w * 0.015f)
    }
}

/** Twin Summits — 2 dome peaks side by side (tasteful twin domes for nhũ hoa). */
private fun DrawScope.drawBossTwinSummits(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val domeRx = w * 0.24f; val domeRy = h * 0.30f
    // 2 large dome curves
    for (side in intArrayOf(-1, 1)) {
        val px = cx + side * w * 0.20f
        drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(px - domeRx, cy - domeRy * 0.5f),
            size = Size(domeRx * 2, domeRy * 2))
        drawArc(accent, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(px - domeRx, cy - domeRy * 0.5f),
            size = Size(domeRx * 2, domeRy * 2),
            style = Stroke(width = w * 0.025f))
        // Glowing tip (cannon mouth — milk ray source)
        drawCircle(Color.White.copy(alpha = 0.9f), domeRx * 0.18f, Offset(px, cy - domeRy * 0.30f))
        drawCircle(accent, domeRx * 0.10f, Offset(px, cy - domeRy * 0.30f))
    }
    // Connecting base
    drawRect(body,
        topLeft = Offset(cx - w * 0.40f, cy + domeRy * 0.30f),
        size = Size(w * 0.80f, h * 0.15f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.40f, cy + domeRy * 0.30f),
        size = Size(w * 0.80f, h * 0.15f),
        style = Stroke(width = w * 0.025f))
    // 2 milk-ray emission dots between domes
    drawCircle(Color.White, w * 0.05f, Offset(cx - w * 0.20f, cy - domeRy * 0.10f))
    drawCircle(Color.White, w * 0.05f, Offset(cx + w * 0.20f, cy - domeRy * 0.10f))
    // Glow halos around tips
    drawCircle(Color.White.copy(alpha = 0.30f), domeRx * 0.35f, Offset(cx - w * 0.20f, cy - domeRy * 0.30f))
    drawCircle(Color.White.copy(alpha = 0.30f), domeRx * 0.35f, Offset(cx + w * 0.20f, cy - domeRy * 0.30f))
}

/** Void Globes — 2 large dark spheres clustered (tasteful for cặp mông). */
private fun DrawScope.drawBossVoidGlobes(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val globeR = w * 0.26f
    val darkBody = Color(0xFF202020)
    // 2 large spheres side by side
    drawCircle(darkBody, globeR, Offset(cx - w * 0.18f, cy + h * 0.05f))
    drawCircle(darkBody, globeR, Offset(cx + w * 0.18f, cy + h * 0.05f))
    // Body color overlay (slight tint)
    drawCircle(body.copy(alpha = 0.55f), globeR, Offset(cx - w * 0.18f, cy + h * 0.05f))
    drawCircle(body.copy(alpha = 0.55f), globeR, Offset(cx + w * 0.18f, cy + h * 0.05f))
    // Outline
    drawCircle(accent, globeR, Offset(cx - w * 0.18f, cy + h * 0.05f),
        style = Stroke(width = w * 0.02f))
    drawCircle(accent, globeR, Offset(cx + w * 0.18f, cy + h * 0.05f),
        style = Stroke(width = w * 0.02f))
    // Glow highlights upper-left
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f,
        Offset(cx - w * 0.25f, cy - h * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f,
        Offset(cx + w * 0.11f, cy - h * 0.05f))
    // Center cluster spit (3 brown blobs between)
    for (i in 0 until 3) {
        drawCircle(Color(0xFF6B3010), w * 0.04f - i * w * 0.008f,
            Offset(cx, cy + h * 0.25f + i * h * 0.08f))
    }
    // 2 cleft lines (subtle, accents the rounded form)
    drawLine(Color.Black.copy(alpha = 0.3f),
        Offset(cx - w * 0.18f, cy - h * 0.15f), Offset(cx - w * 0.18f, cy + h * 0.25f),
        strokeWidth = w * 0.015f)
    drawLine(Color.Black.copy(alpha = 0.3f),
        Offset(cx + w * 0.18f, cy - h * 0.15f), Offset(cx + w * 0.18f, cy + h * 0.25f),
        strokeWidth = w * 0.015f)
}

/** White Dragon — long sinuous dragon body với 2 horns + blue eyes + claws + breath flame. */
private fun DrawScope.drawBossWhiteDragon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val white = Color(0xFFE8E8F0)
    val blueEye = Color(0xFF0080FF)
    val flameColor = Color(0xFFFF6020)
    // Sinuous body (S-shape using cubic)
    val bodyPath = Path().apply {
        moveTo(cx + w * 0.30f, cy + h * 0.45f)              // tail tip
        cubicTo(cx + w * 0.10f, cy + h * 0.30f,
            cx - w * 0.20f, cy + h * 0.10f,
            cx - w * 0.10f, cy - h * 0.10f)
        cubicTo(cx + w * 0.05f, cy - h * 0.25f,
            cx + w * 0.15f, cy - h * 0.30f,
            cx + w * 0.05f, cy - h * 0.40f)                  // head
        cubicTo(cx - w * 0.05f, cy - h * 0.30f,
            cx - w * 0.20f, cy - h * 0.20f,
            cx - w * 0.20f, cy + h * 0.05f)
        cubicTo(cx - w * 0.05f, cy + h * 0.25f,
            cx + w * 0.20f, cy + h * 0.40f,
            cx + w * 0.30f, cy + h * 0.45f)
        close()
    }
    drawPath(bodyPath, white)
    drawPath(bodyPath, accent, style = Stroke(width = w * 0.02f))
    // Head detail at top
    drawCircle(white, w * 0.10f, Offset(cx + w * 0.05f, cy - h * 0.35f))
    // 2 horns
    for (side in intArrayOf(-1, 1)) {
        drawLine(white,
            Offset(cx + w * 0.05f + side * w * 0.05f, cy - h * 0.42f),
            Offset(cx + w * 0.05f + side * w * 0.10f, cy - h * 0.50f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Blue eyes (2 small)
    drawCircle(blueEye, w * 0.025f, Offset(cx + w * 0.02f, cy - h * 0.36f))
    drawCircle(blueEye, w * 0.025f, Offset(cx + w * 0.10f, cy - h * 0.36f))
    drawCircle(Color.White, w * 0.010f, Offset(cx + w * 0.02f, cy - h * 0.37f))
    drawCircle(Color.White, w * 0.010f, Offset(cx + w * 0.10f, cy - h * 0.37f))
    // Fire breath (3 flame tongues from mouth)
    for (i in 0 until 3) {
        val angDeg = -120f + i * 12f
        val a = angDeg * Math.PI / 180.0
        val sx = cx + w * 0.05f
        val sy = cy - h * 0.30f
        val ex = sx + (w * 0.30f * kotlin.math.cos(a)).toFloat()
        val ey = sy + (w * 0.30f * kotlin.math.sin(a)).toFloat()
        drawLine(flameColor,
            Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // 2 claws (small triangles)
    for ((x, y) in listOf(cx - w * 0.10f to cy + h * 0.15f, cx + w * 0.20f to cy + h * 0.20f)) {
        val cpath = Path().apply {
            moveTo(x, y)
            lineTo(x - w * 0.03f, y + h * 0.05f)
            lineTo(x - w * 0.06f, y - h * 0.02f)
            close()
        }
        drawPath(cpath, accent)
    }
}

/** Hammer and Sickle — combined symbol Boss (cộng sản). Red star background. */
private fun DrawScope.drawBossHammerSickle(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val red = Color(0xFFCC0000)
    val gold = Color(0xFFFFD700)
    // Red circular background
    drawCircle(red, w * 0.42f, Offset(cx, cy))
    drawCircle(gold, w * 0.42f, Offset(cx, cy), style = Stroke(width = w * 0.025f))
    // Sickle (curved blade left)
    val sicklePath = Path().apply {
        moveTo(cx - w * 0.25f, cy - h * 0.05f)
        cubicTo(cx - w * 0.30f, cy - h * 0.20f,
            cx - w * 0.10f, cy - h * 0.32f,
            cx + w * 0.10f, cy - h * 0.30f)
        cubicTo(cx + w * 0.05f, cy - h * 0.20f,
            cx - w * 0.10f, cy - h * 0.18f,
            cx - w * 0.18f, cy - h * 0.05f)
        close()
    }
    drawPath(sicklePath, gold)
    // Sickle handle (curved line)
    drawArc(gold,
        startAngle = 90f, sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - w * 0.30f, cy - h * 0.05f),
        size = Size(w * 0.30f, h * 0.30f),
        style = Stroke(width = w * 0.03f))
    // Hammer head (rectangle at right)
    drawRect(gold,
        topLeft = Offset(cx + w * 0.05f, cy - h * 0.05f),
        size = Size(w * 0.25f, h * 0.10f))
    // Hammer handle (diagonal line)
    drawLine(gold,
        Offset(cx + w * 0.10f, cy),
        Offset(cx - w * 0.05f, cy + h * 0.25f),
        strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Crossing emphasis
    drawCircle(gold, w * 0.04f, Offset(cx + w * 0.10f, cy))
    // 5-point star at top
    val starOuter = w * 0.10f
    val starInner = starOuter * 0.42f
    val starCx = cx; val starCy = cy - h * 0.32f
    val starPath = Path().apply {
        val rotation = -Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) starOuter else starInner
            val x = starCx + (r * kotlin.math.cos(a)).toFloat()
            val y = starCy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, gold)
}

/** Money Tycoon — fat boss in suit + money bag + dollar signs (tư bản). */
private fun DrawScope.drawBossMoneyTycoon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val moneyGreen = Color(0xFF208040)
    val gold = Color(0xFFFFD700)
    // Round body (fat tycoon torso)
    drawCircle(body, w * 0.35f, Offset(cx, cy + h * 0.05f))
    drawCircle(accent, w * 0.35f, Offset(cx, cy + h * 0.05f), style = Stroke(width = w * 0.025f))
    // Head (round)
    drawCircle(body, w * 0.16f, Offset(cx, cy - h * 0.30f))
    // Top hat (rectangle + brim)
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.12f, cy - h * 0.52f),
        size = Size(w * 0.24f, h * 0.15f))
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.20f, cy - h * 0.40f),
        size = Size(w * 0.40f, h * 0.04f))
    // Hat band (gold)
    drawRect(gold,
        topLeft = Offset(cx - w * 0.12f, cy - h * 0.43f),
        size = Size(w * 0.24f, h * 0.03f))
    // Monocle (right eye)
    drawCircle(Color.White.copy(alpha = 0.7f), w * 0.05f, Offset(cx + w * 0.06f, cy - h * 0.30f))
    drawCircle(Color.Black, w * 0.05f, Offset(cx + w * 0.06f, cy - h * 0.30f), style = Stroke(width = w * 0.015f))
    drawLine(Color.Black,
        Offset(cx + w * 0.10f, cy - h * 0.28f),
        Offset(cx + w * 0.18f, cy - h * 0.20f),
        strokeWidth = w * 0.015f)
    // Left eye (small dot)
    drawCircle(Color.Black, w * 0.018f, Offset(cx - w * 0.06f, cy - h * 0.30f))
    // Mustache (curved)
    drawLine(Color.Black,
        Offset(cx - w * 0.08f, cy - h * 0.22f),
        Offset(cx + w * 0.08f, cy - h * 0.22f),
        strokeWidth = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // $ sign on belly (big)
    val dollarSize = w * 0.15f
    drawArc(gold, startAngle = 60f, sweepAngle = 240f, useCenter = false,
        topLeft = Offset(cx - dollarSize, cy - dollarSize * 0.10f),
        size = Size(dollarSize * 2, dollarSize * 0.8f),
        style = Stroke(width = w * 0.05f))
    drawArc(gold, startAngle = 240f, sweepAngle = 240f, useCenter = false,
        topLeft = Offset(cx - dollarSize, cy + dollarSize * 0.20f),
        size = Size(dollarSize * 2, dollarSize * 0.8f),
        style = Stroke(width = w * 0.05f))
    drawLine(gold,
        Offset(cx, cy - dollarSize * 0.25f),
        Offset(cx, cy + dollarSize * 0.85f),
        strokeWidth = w * 0.04f)
    // Money bag in left hand (small sack with $ on it)
    val bagX = cx - w * 0.45f
    val bagY = cy + h * 0.20f
    drawCircle(Color(0xFF8B5C2E), w * 0.10f, Offset(bagX, bagY))
    drawCircle(moneyGreen, w * 0.04f, Offset(bagX, bagY))
}

/** Golden Tycoon — orange-hair caricature (Trump-inspired, no real name) + golden tie. */
private fun DrawScope.drawBossGoldenTycoon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val orangeHair = Color(0xFFFF9520)
    val gold = Color(0xFFFFC020)
    val flesh = Color(0xFFFFB890)
    // Head (round, flesh)
    drawCircle(flesh, w * 0.22f, Offset(cx, cy - h * 0.15f))
    // Distinctive orange hair (swept forward — caricature)
    val hairPath = Path().apply {
        moveTo(cx - w * 0.25f, cy - h * 0.25f)
        cubicTo(cx - w * 0.30f, cy - h * 0.40f,
            cx + w * 0.10f, cy - h * 0.50f,
            cx + w * 0.30f, cy - h * 0.35f)
        cubicTo(cx + w * 0.20f, cy - h * 0.30f,
            cx + w * 0.10f, cy - h * 0.32f,
            cx + w * 0.05f, cy - h * 0.28f)
        cubicTo(cx - w * 0.10f, cy - h * 0.32f,
            cx - w * 0.15f, cy - h * 0.30f,
            cx - w * 0.25f, cy - h * 0.25f)
        close()
    }
    drawPath(hairPath, orangeHair)
    // Hair detail strokes (sweep lines)
    for (i in 0 until 4) {
        val sx = cx - w * 0.15f + i * w * 0.10f
        drawLine(Color(0xFFCC6010),
            Offset(sx, cy - h * 0.35f),
            Offset(sx + w * 0.05f, cy - h * 0.30f),
            strokeWidth = w * 0.012f)
    }
    // Eyes (small narrow with bag)
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - w * 0.13f, cy - h * 0.18f),
        size = Size(w * 0.08f, h * 0.05f),
        style = Stroke(width = w * 0.014f))
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + w * 0.05f, cy - h * 0.18f),
        size = Size(w * 0.08f, h * 0.05f),
        style = Stroke(width = w * 0.014f))
    // Mouth (pursed/pouty)
    drawCircle(Color(0xFFCC2020), w * 0.04f, Offset(cx, cy - h * 0.02f))
    // Suit (dark body)
    val suitPath = Path().apply {
        moveTo(cx - w * 0.18f, cy + h * 0.05f)
        lineTo(cx + w * 0.18f, cy + h * 0.05f)
        lineTo(cx + w * 0.35f, cy + h * 0.50f)
        lineTo(cx - w * 0.35f, cy + h * 0.50f)
        close()
    }
    drawPath(suitPath, Color(0xFF202040))
    // White shirt collar
    val collarPath = Path().apply {
        moveTo(cx - w * 0.08f, cy + h * 0.05f)
        lineTo(cx + w * 0.08f, cy + h * 0.05f)
        lineTo(cx, cy + h * 0.18f)
        close()
    }
    drawPath(collarPath, Color.White)
    // Golden tie (long, distinctive)
    val tiePath = Path().apply {
        moveTo(cx - w * 0.04f, cy + h * 0.08f)
        lineTo(cx + w * 0.04f, cy + h * 0.08f)
        lineTo(cx + w * 0.05f, cy + h * 0.30f)
        lineTo(cx, cy + h * 0.45f)
        lineTo(cx - w * 0.05f, cy + h * 0.30f)
        close()
    }
    drawPath(tiePath, gold)
    drawPath(tiePath, Color(0xFFCC9000), style = Stroke(width = w * 0.012f))
    // 3 dollar bills floating (visual hint at attack pattern)
    for (i in 0 until 3) {
        val bx = cx - w * 0.45f + i * w * 0.45f
        val by = cy + h * 0.40f - i * h * 0.05f
        drawRect(Color(0xFF80C080),
            topLeft = Offset(bx, by),
            size = Size(w * 0.12f, h * 0.07f))
        drawRect(Color(0xFF208040),
            topLeft = Offset(bx, by),
            size = Size(w * 0.12f, h * 0.07f),
            style = Stroke(width = w * 0.012f))
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 82 (boss + enemy wire) — 10 R81 enemy in-game shape recipes (mirror
// of InfoScreen previews; signature matches drawXXX(cx, cy, wPx, hPx, body, accent)).
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawEnemySpinningSaw(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val outerR = minOf(w, h) * 0.42f
    val toothCount = 12
    val toothPath = Path().apply {
        for (i in 0 until toothCount * 2) {
            val a = -Math.PI / 2 + i * Math.PI / toothCount
            val r = if (i % 2 == 0) outerR else outerR * 0.78f
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(toothPath, body)
    drawCircle(accent, outerR * 0.40f, Offset(cx, cy))
    drawCircle(Color.Black, outerR * 0.15f, Offset(cx, cy))
}

private fun DrawScope.drawEnemyTentacleSquid(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val cxLocal = cx; val cyLocal = cy - h * 0.10f
    val headR = w * 0.28f
    drawCircle(body, headR, Offset(cxLocal, cyLocal))
    for (i in 0 until 6) {
        val baseAngle = -90.0 + (i - 2.5) * 22.0
        val a = baseAngle * Math.PI / 180.0
        val bx = cxLocal + (headR * kotlin.math.cos(a)).toFloat()
        val by = cyLocal + (headR * kotlin.math.sin(a)).toFloat()
        val ex = bx + (headR * 0.85f * kotlin.math.cos(a)).toFloat()
        val ey = by + headR * 1.20f
        val tPath = Path().apply {
            moveTo(bx, by)
            cubicTo(bx + (i - 2.5).toFloat() * w * 0.05f, by + h * 0.10f,
                ex - (i - 2.5).toFloat() * w * 0.03f, ey - h * 0.05f, ex, ey)
        }
        drawPath(tPath, body,
            style = Stroke(width = w * 0.035f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
    drawCircle(accent, headR * 0.18f, Offset(cxLocal - headR * 0.30f, cyLocal))
    drawCircle(accent, headR * 0.18f, Offset(cxLocal + headR * 0.30f, cyLocal))
    drawCircle(Color.Black, headR * 0.08f, Offset(cxLocal - headR * 0.30f, cyLocal))
    drawCircle(Color.Black, headR * 0.08f, Offset(cxLocal + headR * 0.30f, cyLocal))
}

private fun DrawScope.drawEnemyMineLayer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyW = w * 0.60f; val bodyH = h * 0.35f
    drawRect(body, topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f), size = Size(bodyW, bodyH))
    drawRect(accent, topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f), size = Size(bodyW, bodyH),
        style = Stroke(width = w * 0.03f))
    drawCircle(body, w * 0.10f, Offset(cx, cy - bodyH * 0.40f))
    drawRect(Color(0xFF404040),
        topLeft = Offset(cx - bodyW * 0.55f, cy + bodyH * 0.45f),
        size = Size(bodyW * 1.10f, h * 0.10f))
    for (i in 0 until 3) {
        drawCircle(Color(0xFFFF2D55), w * 0.04f, Offset(cx - w * 0.20f + i * w * 0.20f, cy + h * 0.40f))
    }
}

private fun DrawScope.drawEnemyShieldDrone(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyR = w * 0.18f
    val hexPath = Path().apply {
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            val x = cx + (bodyR * kotlin.math.cos(a)).toFloat()
            val y = cy + (bodyR * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(hexPath, body)
    drawArc(accent.copy(alpha = 0.55f),
        startAngle = 30f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = Size(w * 0.76f, h * 0.40f),
        style = Stroke(width = w * 0.04f))
    drawArc(Color.White.copy(alpha = 0.30f),
        startAngle = 30f, sweepAngle = 120f, useCenter = true,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = Size(w * 0.76f, h * 0.40f))
    drawCircle(Color(0xFFFF2D55), bodyR * 0.4f, Offset(cx, cy))
}

private fun DrawScope.drawEnemySniper(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy - h * 0.40f),
        size = Size(w * 0.10f, h * 0.80f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.20f),
        size = Size(w * 0.20f, h * 0.10f))
    drawCircle(Color.Red, w * 0.025f, Offset(cx + w * 0.07f, cy - h * 0.15f))
    drawRect(body,
        topLeft = Offset(cx - w * 0.15f, cy + h * 0.30f),
        size = Size(w * 0.30f, h * 0.10f))
    drawCircle(Color(0xFFFFE040), w * 0.06f, Offset(cx, cy - h * 0.42f))
    drawCircle(Color.White, w * 0.025f, Offset(cx, cy - h * 0.42f))
}

private fun DrawScope.drawEnemyBomberCrawler(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawRect(body,
        topLeft = Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = Size(w * 0.80f, h * 0.30f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = Size(w * 0.80f, h * 0.30f),
        style = Stroke(width = w * 0.025f))
    drawLine(body,
        Offset(cx, cy - h * 0.05f), Offset(cx + w * 0.20f, cy - h * 0.35f),
        strokeWidth = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    for (i in 0 until 4) {
        drawCircle(Color(0xFF404040), h * 0.06f, Offset(cx - w * 0.30f + i * w * 0.20f, cy + h * 0.18f))
    }
    drawCircle(Color(0xFFFFE040), w * 0.03f, Offset(cx + w * 0.22f, cy - h * 0.38f))
}

private fun DrawScope.drawEnemyMirrorTwin(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    for (sign in intArrayOf(-1, 1)) {
        val px = cx + sign * w * 0.18f
        val triPath = Path().apply {
            moveTo(px, cy - h * 0.20f)
            lineTo(px + w * 0.10f, cy + h * 0.20f)
            lineTo(px - w * 0.10f, cy + h * 0.20f)
            close()
        }
        drawPath(triPath, body)
        drawCircle(accent, w * 0.04f, Offset(px, cy))
    }
    drawLine(accent, Offset(cx - w * 0.10f, cy), Offset(cx + w * 0.10f, cy),
        strokeWidth = w * 0.015f)
    drawLine(accent, Offset(cx - w * 0.05f, cy - h * 0.05f), Offset(cx + w * 0.05f, cy - h * 0.05f),
        strokeWidth = w * 0.01f)
    drawLine(accent, Offset(cx - w * 0.05f, cy + h * 0.05f), Offset(cx + w * 0.05f, cy + h * 0.05f),
        strokeWidth = w * 0.01f)
}

private fun DrawScope.drawEnemyPhantom(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val ghostPath = Path().apply {
        moveTo(cx - w * 0.30f, cy - h * 0.20f)
        cubicTo(cx - w * 0.35f, cy - h * 0.45f, cx + w * 0.35f, cy - h * 0.45f, cx + w * 0.30f, cy - h * 0.20f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        val waveCount = 5
        val waveStep = w * 0.60f / waveCount
        for (i in 0..waveCount) {
            val wx = cx + w * 0.30f - i * waveStep
            val wy = if (i % 2 == 0) cy + h * 0.40f else cy + h * 0.30f
            lineTo(wx, wy)
        }
        close()
    }
    drawPath(ghostPath, body.copy(alpha = 0.55f))
    drawPath(ghostPath, accent.copy(alpha = 0.85f), style = Stroke(width = w * 0.025f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, Offset(cx - w * 0.10f, cy - h * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, Offset(cx + w * 0.10f, cy - h * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.04f, Offset(cx, cy + h * 0.05f),
        style = Stroke(width = w * 0.015f))
}

private fun DrawScope.drawEnemyHealer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawCircle(Color(0xFF60FFAA).copy(alpha = 0.25f), w * 0.42f, Offset(cx, cy))
    drawCircle(Color(0xFF60FFAA), w * 0.42f, Offset(cx, cy),
        style = Stroke(width = w * 0.02f))
    drawCircle(body, w * 0.18f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), w * 0.10f, Offset(cx, cy))
    val armW = w * 0.04f
    val armL = w * 0.20f
    drawRect(Color(0xFF60FFAA),
        topLeft = Offset(cx - armW / 2f, cy - armL / 2f), size = Size(armW, armL))
    drawRect(Color(0xFF60FFAA),
        topLeft = Offset(cx - armL / 2f, cy - armW / 2f), size = Size(armL, armW))
    for (i in 0 until 4) {
        val a = i * Math.PI / 2
        drawCircle(Color(0xFF60FFAA), w * 0.025f,
            Offset(cx + (w * 0.32f * kotlin.math.cos(a)).toFloat(),
                cy + (w * 0.32f * kotlin.math.sin(a)).toFloat()))
    }
}

private fun DrawScope.drawEnemyKamikaze(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyPath = Path().apply {
        moveTo(cx, cy - h * 0.30f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        lineTo(cx - w * 0.30f, cy + h * 0.30f)
        close()
    }
    drawPath(bodyPath, Color(0xFFFFE040))
    drawPath(bodyPath, Color.Black, style = Stroke(width = w * 0.04f))
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.02f, cy - h * 0.10f),
        size = Size(w * 0.04f, h * 0.20f))
    drawCircle(Color.Black, w * 0.025f, Offset(cx, cy + h * 0.18f))
    for (i in 0 until 3) {
        val sy = cy + h * 0.35f + i * h * 0.05f
        drawLine(Color(0xFFFF6020),
            Offset(cx - w * 0.20f + i * w * 0.05f, sy),
            Offset(cx + w * 0.20f - i * w * 0.05f, sy),
            strokeWidth = w * 0.02f)
    }
}
