package com.tranphuloi.neon.ui.game.controls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

/**
 * 1c: Compact top-screen boss HP bar.
 * Compacted per user feedback — single tight row 200dp wide × ~22dp tall.
 */
@Composable
fun BossHpBar(
    enemies: List<EnemyUI>,
    modifier: Modifier = Modifier,
) {
    val boss = enemies.firstOrNull { it.isBoss } ?: return
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val ratio = (boss.currentHp / boss.initialHp).coerceIn(0f, 1f)

    val pulse = rememberInfiniteTransition(label = "bossPulse")
    val warningAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bossWarningAlpha",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(200.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = boss.displayName,
                color = palette.redAlert.copy(alpha = warningAlpha),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
            Box(modifier = Modifier.width(6.dp))
            Text(
                text = "${boss.currentHp.toInt()}/${boss.initialHp.toInt()}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // Wave 17m — hiện CHIÊU THỨC dưới tên để người chơi nhận ra từng boss khác
        // nhau (user: "không thấy boss riêng biệt"). Đọc theo bossKind.
        val skill = bossSkillLabel(boss.bossKind)
        if (skill.isNotEmpty()) {
            Text(
                text = "⚔ $skill",
                color = Color(0xFFFFC400).copy(alpha = 0.9f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.55f))
                .neonGlow(
                    color = palette.redAlert,
                    intensity = 0.25f + (1f - ratio) * 0.35f,
                    radiusFactor = 1.2f,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = ratio)
                    .clip(MaterialTheme.shapes.small)
                    .background(palette.redAlert),
            )
        }
    }
}

/** Wave 17m — chiêu thức ngắn theo BossKind (hiện dưới tên trên HP bar) để
 *  người chơi nhận ra từng boss khác nhau. Phủ cả 45 kind (mid + final). */
// Wave 25c — nhãn chiêu đọc từ bảng chung `bossMetaFor` (gom 4 when → 1).
private fun bossSkillLabel(kind: BossKind?): String =
    if (kind == null) "" else com.tranphuloi.neon.ui.game.enemy.ship.model.bossMetaFor(kind).skill
