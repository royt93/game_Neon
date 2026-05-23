package com.tranphuloi.neon.ui.game.world

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.booster.BoosterUI

/**
 * Round 59 — Canvas-based draw of the booster sprite layer (sprite + glow +
 * optional PIERCING/PLASMA tint overlay). Rarity ring border + glyph text
 * stay as Composable overlays in GameWorld — same call as EnemyCanvas where
 * HP bars remained Composable, because Border + Text aren't a simple
 * `drawCircle` / `drawText` recipe (animated alpha, font metrics, etc.).
 *
 * Visual parity with the pre-refactor block in GameWorld:
 *   neonGlow(glowColor, 0.85, 1.8) → drawCircle(radialGradient) same recipe.
 *   ColorFilter.Modulate via tintColorHex (Round 54) → drawImage with
 *     ColorFilter.tint(argb) overlay on top of the base sprite.
 */
@Composable
fun BoosterCanvas(
    boosters: List<BoosterUI>,
    sprites: BoosterSprites,
    modifier: Modifier = Modifier,
) {
    if (boosters.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (b in boosters) {
            drawBooster(b, sprites, density)
        }
    }
}

@Immutable
data class BoosterSprites(
    val byDrawableId: Map<Int, ImageBitmap>,
)

@Composable
fun rememberBoosterSprites(): BoosterSprites {
    // booster_health / booster_red_lasers / booster_shield / booster_triple_laser /
    // booster_ultimate_weapon are .webp (BitmapDrawable) — safe to read via the
    // standard imageResource path. booster_revive is a VectorDrawable .xml,
    // which imageResource cannot cast → must rasterize manually (see
    // [drawableToImageBitmap]). This split keeps the cheap path cheap for the
    // 5 bitmap sprites while handling the one vector cleanly.
    val health = ImageBitmap.imageResource(R.drawable.booster_health)
    val redLasers = ImageBitmap.imageResource(R.drawable.booster_red_lasers)
    val shield = ImageBitmap.imageResource(R.drawable.booster_shield)
    val tripleLaser = ImageBitmap.imageResource(R.drawable.booster_triple_laser)
    val ultimate = ImageBitmap.imageResource(R.drawable.booster_ultimate_weapon)
    val revive = drawableToImageBitmap(R.drawable.booster_revive)
    return remember(health, redLasers, revive, shield, tripleLaser, ultimate) {
        BoosterSprites(
            mapOf(
                R.drawable.booster_health to health,
                R.drawable.booster_red_lasers to redLasers,
                R.drawable.booster_revive to revive,
                R.drawable.booster_shield to shield,
                R.drawable.booster_triple_laser to tripleLaser,
                R.drawable.booster_ultimate_weapon to ultimate,
            )
        )
    }
}

/**
 * Universal `@DrawableRes` → [ImageBitmap] loader. Handles both
 * [BitmapDrawable] (webp/png) and [android.graphics.drawable.VectorDrawable]
 * (xml). For vectors, rasterizes at the drawable's intrinsic size into an
 * ARGB_8888 [Bitmap] once at composition time. Cached via `remember(id)` so
 * the rasterization runs only on first composition + on resource changes.
 *
 * Use this in `rememberXyzSprites()` whenever a Canvas-rendered entity may
 * point at a vector drawable (booster_revive is the current case).
 */
@Composable
private fun drawableToImageBitmap(@DrawableRes id: Int): ImageBitmap {
    val context = LocalContext.current
    return remember(id) {
        val drawable = ContextCompat.getDrawable(context, id)
            ?: error("Drawable not found: id=$id")
        if (drawable is BitmapDrawable) {
            drawable.bitmap.asImageBitmap()
        } else {
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }
    }
}

private fun DrawScope.drawBooster(
    booster: BoosterUI,
    sprites: BoosterSprites,
    density: Density,
) {
    val bitmap = sprites.byDrawableId[booster.drawableId] ?: return
    with(density) {
        val sizePx = booster.size.dp.toPx()
        val xPx = booster.xOffset.dp.toPx()
        val yPx = booster.yOffset.dp.toPx()
        val cx = xPx + sizePx / 2f
        val cy = yPx + sizePx / 2f
        val glowR = (sizePx / 2f) * 1.8f

        val glowColor = if (booster.tintColorHex != 0L) Color(booster.tintColorHex) else NeonGold
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.85f),
                    glowColor.copy(alpha = 0.34f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        val dstOffset = IntOffset(xPx.toInt(), yPx.toInt())
        val dstSize = IntSize(sizePx.toInt(), sizePx.toInt())
        drawImage(
            image = bitmap,
            dstOffset = dstOffset,
            dstSize = dstSize,
            filterQuality = FilterQuality.Low,
        )

        // Round 54 — PIERCING/PLASMA tint overlay on top of the base sprite so
        // the icon is distinguishable from LASER / ULTIMATE which reuse the
        // same drawables. Drawn as a second tinted drawImage rather than as a
        // ColorFilter on the base, so the underlying sprite detail is still
        // visible through the tint.
        if (booster.tintColorHex != 0L) {
            drawImage(
                image = bitmap,
                dstOffset = dstOffset,
                dstSize = dstSize,
                alpha = 0.55f,
                colorFilter = ColorFilter.tint(Color(booster.tintColorHex)),
                filterQuality = FilterQuality.Low,
            )
        }
    }
}
