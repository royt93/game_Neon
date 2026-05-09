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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

/**
 * 1c: Top-screen boss HP bar with name + warning border pulse.
 * Renders only when at least one boss is alive in the enemies list.
 */
@Composable
fun BossHpBar(
    enemies: List<EnemyUI>,
    modifier: Modifier = Modifier,
) {
    // Pick first boss (game design: only 1 boss alive at a time).
    val boss = enemies.firstOrNull { it.isBoss } ?: return
    val ratio = (boss.currentHp / boss.initialHp).coerceIn(0f, 1f)

    val pulse = rememberInfiniteTransition(label = "bossPulse")
    val warningAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bossWarningAlpha",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
    ) {
        Text(
            text = boss.displayName,
            color = NeonRedAlert.copy(alpha = warningAlpha),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
        // Bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.55f))
                .neonGlow(
                    color = NeonRedAlert,
                    intensity = 0.35f + (1f - ratio) * 0.4f,
                    radiusFactor = 1.3f,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = ratio)
                    .clip(MaterialTheme.shapes.small)
                    .background(NeonRedAlert),
            )
        }
        Text(
            text = "${boss.currentHp.toInt()} / ${boss.initialHp.toInt()}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
