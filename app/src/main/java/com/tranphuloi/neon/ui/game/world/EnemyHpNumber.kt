package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import kotlinx.coroutines.delay

/**
 * Round 70 (Issue 8) — HP NUMBER center thay 3-layer EnemyHpBar.
 *
 * Visibility rules:
 *   - Tier-1 enemy (initialHp < 50hp): KHÔNG render (1-shot, không cần info).
 *   - Enemy chưa bị đánh (currentHp == initialHp): KHÔNG render (clean look).
 *   - Enemy đã damaged (currentHp < initialHp): render số HP hiện tại ở
 *     center của enemy. Auto-hide khi enemy heal full hoặc destroy.
 *
 * Color tier (giống EnemyHpBar trước đó):
 *   - hp >66% : cyan
 *   - hp >33% : gold
 *   - hp ≤33% : red
 *
 * Hit-flash: 120ms white pulse khi vừa hit (lastImpactMillis vừa update).
 *
 * Boss: bỏ qua (Caller filter `!isBoss`); boss vẫn dùng `BossHpBar` full-width.
 */
@Composable
fun EnemyHpNumber(
    enemyId: String,
    currentHp: Float,
    initialHp: Float,
    lastImpactMillis: Long,
    enemyWidth: Float,
    modifier: Modifier = Modifier,
) {
    // Skip render nếu tier-1 (HP gốc nhỏ) hoặc chưa bị đánh.
    if (initialHp < TIER_1_HP_THRESHOLD) return
    if (currentHp >= initialHp) return
    if (currentHp <= 0f) return

    val safeInitial = initialHp.coerceAtLeast(1f)
    val hpRatio = (currentHp / safeInitial).coerceIn(0f, 1f)
    val color = when {
        hpRatio > 0.66f -> NeonCyan
        hpRatio > 0.33f -> NeonGold
        else -> NeonRedAlert
    }

    // Hit-flash: tracks last impact, fades over 120ms to color.
    var flashAlpha by remember(enemyId) { mutableFloatStateOf(0f) }
    LaunchedEffect(lastImpactMillis) {
        if (lastImpactMillis <= 0L) return@LaunchedEffect
        flashAlpha = 1f
        val durationMs = 120L
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= durationMs) {
                flashAlpha = 0f
                break
            }
            flashAlpha = 1f - elapsed.toFloat() / durationMs
            delay(16L)
        }
    }
    val displayColor = lerpColor(color, Color.White, flashAlpha)

    // Font size scales với enemy width — enemy nhỏ → số nhỏ hơn, không đè
    // shape. Range 11sp..15sp.
    val fontSp = (enemyWidth * 0.35f).coerceIn(11f, 15f).sp

    Box(
        modifier = modifier.size(enemyWidth.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = currentHp.toInt().toString(),
            color = displayColor,
            fontSize = fontSp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 0.5.sp),
            modifier = Modifier.neonGlow(displayColor, intensity = 0.7f, radiusFactor = 1.3f),
        )
    }
}

/** Tier-1 = enemy 1-shot, render HP number vô ích. Threshold = 50 HP base. */
private const val TIER_1_HP_THRESHOLD = 50f

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * tt,
        green = from.green + (to.green - from.green) * tt,
        blue = from.blue + (to.blue - from.blue) * tt,
        alpha = from.alpha + (to.alpha - from.alpha) * tt,
    )
}
