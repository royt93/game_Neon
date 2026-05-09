package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.combo.ComboTier

private const val MAX_HP: Int = 1000

@Composable
fun IndicatorStatus(
    gameTime: String,
    hp: Int,
    mineralsEarnedTotal: String,
    comboCount: Int,
    comboTier: ComboTier,
    lastEnemyKillMillis: Long,
    modifier: Modifier = Modifier,
) {

    val buttonPaddingEnd = dimensionResource(id = R.dimen.button_padding)
    val buttonPaddingTop = buttonPaddingEnd * 2
    val height = 60.dp

    val hpRatio = (hp.toFloat() / MAX_HP).coerceIn(0f, 1f)
    val hpColor = when {
        hp >= 700 -> NeonCyan
        hp >= 300 -> NeonGold
        else -> NeonRedAlert
    }

    Column(modifier = modifier.padding(start = buttonPaddingEnd, top = buttonPaddingTop)) {
        Box(modifier = modifier.height(height = height)) {
            Image(
                painter = painterResource(id = R.drawable.button_hp_indicator),
                contentDescription = stringResource(id = R.string.game_hp_indicator),
                modifier = Modifier
                    .align(Alignment.Center)
                    .neonGlow(color = hpColor, intensity = 0.35f, radiusFactor = 1.2f)
            )
            Text(
                text = "${hp}hp",
                color = hpColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, end = 14.dp)
            )
            Text(
                text = gameTime,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 8.dp, bottom = 9.dp)
            )
        }
        // Visual HP bar — 110dp wide segmented bar showing hp/MAX_HP ratio,
        // glow intensity scales with hp deficit so low HP "screams".
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(110.dp)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = hpRatio)
                    .clip(MaterialTheme.shapes.small)
                    .background(hpColor)
                    .neonGlow(
                        color = hpColor,
                        intensity = 0.35f + (1f - hpRatio) * 0.45f,
                        radiusFactor = 1.4f
                    )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mineral),
                contentDescription = stringResource(id = R.string.mineral_content_description),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(22.dp)
                    .neonGlow(color = NeonGold, intensity = 0.5f, radiusFactor = 1.6f)
            )
            Text(
                text = mineralsEarnedTotal,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.h5,
                color = NeonGold
            )
        }
        // 7c: Combo HUD — only renders when count > 0 (auto-hides on expire).
        Spacer(modifier = Modifier.height(4.dp))
        ComboHud(
            count = comboCount,
            tier = comboTier,
            lastKillMillis = lastEnemyKillMillis,
        )
    }
}
