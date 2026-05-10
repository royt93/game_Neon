package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay

private const val MAX_HP: Int = 1000

@Composable
fun IndicatorStatus(
    gameTime: String,
    hp: Int,
    mineralsEarnedTotal: String,
    comboCount: Int,
    comboTier: ComboTier,
    lastEnemyKillMillis: Long,
    lastMineralPickupMillis: Long,
    lastBoosterPickupMillis: Long,
    modifier: Modifier = Modifier,
) {
    // Per-stat flash timer ticks at 50ms only while a flash is in flight (350ms each).
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val mineralFlashElapsed = nowMillis - lastMineralPickupMillis
    val boosterFlashElapsed = nowMillis - lastBoosterPickupMillis
    val anyFlashActive =
        (lastMineralPickupMillis > 0L && mineralFlashElapsed in 0L..350L) ||
            (lastBoosterPickupMillis > 0L && boosterFlashElapsed in 0L..350L)
    LaunchedEffect(anyFlashActive) {
        if (!anyFlashActive) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val mineralPulse = if (lastMineralPickupMillis > 0L && mineralFlashElapsed in 0L..350L) {
        val t = mineralFlashElapsed.toFloat() / 350f
        // Scale 1.0 → 1.3 (0..0.2), settle 1.3 → 1.0 (0.2..1.0).
        if (t < 0.2f) 1f + (t / 0.2f) * 0.3f else 1.3f - ((t - 0.2f) / 0.8f) * 0.3f
    } else 1f
    val hpPulse = if (lastBoosterPickupMillis > 0L && boosterFlashElapsed in 0L..350L) {
        val t = boosterFlashElapsed.toFloat() / 350f
        if (t < 0.2f) 1f + (t / 0.2f) * 0.25f else 1.25f - ((t - 0.2f) / 0.8f) * 0.25f
    } else 1f
    val mineralFlashIntensity = if (mineralPulse > 1f) (mineralPulse - 1f) * 1.5f else 0f
    val hpFlashIntensity = if (hpPulse > 1f) (hpPulse - 1f) * 1.5f else 0f

    val buttonPaddingEnd = dimensionResource(id = R.dimen.button_padding)
    val buttonPaddingTop = 16.dp
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
                    .graphicsLayer {
                        scaleX = hpPulse
                        scaleY = hpPulse
                    }
                    .neonGlow(
                        color = hpColor,
                        intensity = 0.35f + hpFlashIntensity,
                        radiusFactor = 1.2f + hpFlashIntensity * 0.5f,
                    )
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
                    .graphicsLayer {
                        scaleX = mineralPulse
                        scaleY = mineralPulse
                    }
                    .neonGlow(
                        color = NeonGold,
                        intensity = 0.5f + mineralFlashIntensity,
                        radiusFactor = 1.6f + mineralFlashIntensity * 0.5f,
                    )
            )
            Text(
                text = mineralsEarnedTotal,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.h5,
                color = NeonGold,
                modifier = Modifier.graphicsLayer {
                    scaleX = mineralPulse
                    scaleY = mineralPulse
                },
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
