package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

/**
 * Round 57 — Canvas-based draw of an enemy list. Replaces the per-enemy
 * Compose subtree (Column + EnemyHpBar + Box + 1..N Images for sprite +
 * hit-flash + status-effect tints + boss thrust trail) with one Canvas pass
 * for the visual layer.
 *
 * HP bars are still rendered as separate Composables — they're 3-layer
 * animated bars where each tick is small + cached, and converting them to
 * Canvas would cost more in code complexity than it saves in draw time.
 * Likewise `BossEntryLightning` stays Composable.
 *
 * Z-order: caller draws EnemyCanvas first (sprites at base), then overlays
 * HP bars + boss lightning on top.
 *
 * Pre-loaded sprite map: see [rememberEnemySprites]. 14 base drawables —
 * webp on disk, decoded once per Composition.
 *
 * Visual parity with the pre-round-57 forEach pass:
 *   - neonGlow(NeonMagenta, 0.45 + hitFlash*0.4, 1.4 + hitFlash*0.4)
 *     → drawCircle(radialGradient) with the same recipe.
 *   - hit flash 0..120ms white tint → drawImage with ColorFilter.tint(white*flash).
 *   - status-effect tints (BURN/SLOW/STUN, pulsing 3Hz) → N drawImage with
 *     ColorFilter.tint(argb) at pulse alpha.
 *   - boss entry phase thrust trail (4 fading sprites stacked upward) →
 *     4 drawImage at progressive yOffset, alpha 1-i*0.22.
 */
@Composable
fun EnemyCanvas(
    enemies: List<EnemyUI>,
    sprites: EnemySprites,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (enemies.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (enemy in enemies) {
            drawEnemy(enemy, sprites, nowMillis, density)
        }
    }
}

@Immutable
data class EnemySprites(
    val byDrawableId: Map<Int, ImageBitmap>,
)

/**
 * Round 57 — vertical space that the EnemyHpBar Composable occupied in the
 * pre-refactor Column layout (1dp container + 4dp bottom padding). Used by
 * [drawEnemy] to keep sprite Y position identical to pre-refactor for non-boss
 * enemies (bosses had no mini-bar there).
 */
private const val HP_BAR_VERTICAL_SPACE_DP = 5

@Composable
fun rememberEnemySprites(): EnemySprites {
    // 14 base enemy drawables — 3 RegularEnemy families (red/green/light_blue) of
    // variants 1..5 + 2 boss variants. Pre-loaded into a single map keyed by
    // drawable id; cached for the lifetime of GameWorld's composition.
    val green1 = ImageBitmap.imageResource(R.drawable.enemy_green_1)
    val green2 = ImageBitmap.imageResource(R.drawable.enemy_green_2)
    val green3 = ImageBitmap.imageResource(R.drawable.enemy_green_3)
    val green4 = ImageBitmap.imageResource(R.drawable.enemy_green_4)
    val greenBoss = ImageBitmap.imageResource(R.drawable.enemy_green_boss)
    val lb1 = ImageBitmap.imageResource(R.drawable.enemy_light_blue_1)
    val lb2 = ImageBitmap.imageResource(R.drawable.enemy_light_blue_2)
    val lb3 = ImageBitmap.imageResource(R.drawable.enemy_light_blue_3)
    val lb4 = ImageBitmap.imageResource(R.drawable.enemy_light_blue_4)
    val lb5 = ImageBitmap.imageResource(R.drawable.enemy_light_blue_5)
    val red1 = ImageBitmap.imageResource(R.drawable.enemy_red_1)
    val red2 = ImageBitmap.imageResource(R.drawable.enemy_red_2)
    val red3 = ImageBitmap.imageResource(R.drawable.enemy_red_3)
    val redBoss = ImageBitmap.imageResource(R.drawable.enemy_red_boss)
    return remember(
        green1, green2, green3, green4, greenBoss,
        lb1, lb2, lb3, lb4, lb5,
        red1, red2, red3, redBoss,
    ) {
        EnemySprites(
            mapOf(
                R.drawable.enemy_green_1 to green1,
                R.drawable.enemy_green_2 to green2,
                R.drawable.enemy_green_3 to green3,
                R.drawable.enemy_green_4 to green4,
                R.drawable.enemy_green_boss to greenBoss,
                R.drawable.enemy_light_blue_1 to lb1,
                R.drawable.enemy_light_blue_2 to lb2,
                R.drawable.enemy_light_blue_3 to lb3,
                R.drawable.enemy_light_blue_4 to lb4,
                R.drawable.enemy_light_blue_5 to lb5,
                R.drawable.enemy_red_1 to red1,
                R.drawable.enemy_red_2 to red2,
                R.drawable.enemy_red_3 to red3,
                R.drawable.enemy_red_boss to redBoss,
            )
        )
    }
}

private fun DrawScope.drawEnemy(
    enemy: EnemyUI,
    sprites: EnemySprites,
    nowMillis: Long,
    density: Density,
) {
    val bitmap = sprites.byDrawableId[enemy.drawableId] ?: return
    with(density) {
        val xPx = enemy.xOffset.dp.toPx()
        // Pre-round-57 the enemy was rendered inside a Column with EnemyHpBar above
        // the sprite. That bar consumes ~5dp of vertical layout space (1dp container +
        // 4dp bottom padding) for non-boss enemies, so the sprite's actual top edge
        // was at `enemy.yOffset + 5dp`. After moving sprites to Canvas, mirror that
        // offset so pixel positions are unchanged from before. Boss enemies had no
        // mini-bar in the Column → no offset.
        val hpBarOffsetPx = if (enemy.isBoss) 0f else HP_BAR_VERTICAL_SPACE_DP.dp.toPx()
        val yPx = enemy.yOffset.dp.toPx() + hpBarOffsetPx
        val wPx = enemy.width.dp.toPx()
        val hPx = enemy.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f

        // Hit flash factor (0..1, decays over 120ms). Drives glow + white tint.
        val sinceHit = nowMillis - enemy.lastImpactMillis
        val hitFlash = if (enemy.lastImpactMillis > 0L && sinceHit in 0..120) {
            (1f - sinceHit / 120f).coerceIn(0f, 1f)
        } else 0f

        // Glow — radial gradient, mirrors Modifier.neonGlow(NeonMagenta, ...).
        val glowIntensity = 0.45f + hitFlash * 0.4f
        val glowRadiusFactor = 1.4f + hitFlash * 0.4f
        val glowR = (minOf(wPx, hPx) / 2f) * glowRadiusFactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonMagenta.copy(alpha = glowIntensity),
                    NeonMagenta.copy(alpha = glowIntensity * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        val dstOffset = IntOffset(xPx.toInt(), yPx.toInt())
        val dstSize = IntSize(wPx.toInt(), hPx.toInt())

        // Boss entry-phase thrust trail — 4 fading sprite copies stacked upward
        // so the boss looks like it's rocketing in. -30dp per stack.
        if (enemy.isBoss && enemy.isInEntryPhase) {
            val trailDy = 30.dp.toPx()
            for (i in 1..4) {
                val trailAlpha = (1f - i * 0.22f).coerceIn(0f, 1f) * 0.55f
                if (trailAlpha <= 0f) continue
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(xPx.toInt(), (yPx - i * trailDy).toInt()),
                    dstSize = dstSize,
                    alpha = trailAlpha,
                    filterQuality = FilterQuality.Low,
                )
            }
        }

        // Base sprite.
        drawImage(
            image = bitmap,
            dstOffset = dstOffset,
            dstSize = dstSize,
            filterQuality = FilterQuality.Low,
        )

        // Hit flash overlay — white tinted copy on top, fades 120ms.
        if (hitFlash > 0f) {
            drawImage(
                image = bitmap,
                dstOffset = dstOffset,
                dstSize = dstSize,
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = hitFlash)),
                filterQuality = FilterQuality.Low,
            )
        }

        // Status-effect tints (Round 35 / 42x) — one tinted copy per active
        // effect (BURN orange / SLOW cyan / STUN yellow), pulsing alpha ~3Hz.
        if (enemy.activeStatusEffectTints.isNotEmpty()) {
            val pulse = 0.65f + 0.35f * kotlin.math.sin(nowMillis / 160.0).toFloat()
            for (argb in enemy.activeStatusEffectTints) {
                drawImage(
                    image = bitmap,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    alpha = pulse.coerceIn(0f, 1f),
                    colorFilter = ColorFilter.tint(Color(argb.toInt())),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
    }
}
