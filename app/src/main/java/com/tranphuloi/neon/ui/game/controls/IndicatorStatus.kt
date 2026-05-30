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
import com.tranphuloi.neon.common.PathPool

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
    hasReviveToken: Boolean = false,
    // Round 76 (R76d) — user audit: HUD xấu + ít info. Add chapter / stage /
    // enemies killed / bosses defeated / ship shape badge.
    currentChapterId: Int = 0,
    currentChapterName: String = "",
    stagesReached: Int = 0,
    enemiesKilledTotal: Int = 0,
    bossesDefeatedTotal: Int = 0,
    shipShape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape =
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
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
    // Wave 11d Bug #2 fix — HUD top-left was visually oversized on tall device
    // (Pixel 7 Pro). Reduced by ~25% across the board: capsule height 60→44dp,
    // width 150→120dp, HP fontSize 16→14sp, time 14→11sp, padding-top 16→8dp.
    // Combat info (hp/time) still readable; spec audit prefers compact HUD.
    val buttonPaddingTop = 8.dp
    val height = 44.dp

    val hpRatio = (hp.toFloat() / MAX_HP).coerceIn(0f, 1f)
    val hpColor = when {
        hp >= 700 -> NeonCyan
        hp >= 300 -> NeonGold
        else -> NeonRedAlert
    }

    // Round 77 (R77b) — 2-column layout. Left = COMBAT (HP/mineral/combo/revive).
    // Right = PROGRESSION (chapter/stage/kills/ship). User feedback "HUD to + ít info"
    // mâu thuẫn → giải bằng cách chia layout 2 cột (compact horizontally) + giữ
    // all info (mỗi cột riêng category dễ scan).
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(start = buttonPaddingEnd, top = buttonPaddingTop),
    ) {
        // ── Left column: COMBAT ──
        Column {
        Box(modifier = Modifier.height(height = height)) {
            // Round 67.6 — Vector HP frame replacing button_hp_indicator.webp.
            // Stadium (capsule) outline + neon glow, color tracks HP tier.
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 120.dp, height = height)
                    .graphicsLayer {
                        scaleX = hpPulse
                        scaleY = hpPulse
                    }
                    .neonGlow(
                        color = hpColor,
                        intensity = 0.35f + hpFlashIntensity,
                        radiusFactor = 1.2f + hpFlashIntensity * 0.5f,
                    ),
            ) {
                val r = size.height / 2f
                drawRoundRect(
                    color = hpColor.copy(alpha = 0.10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                )
                drawRoundRect(
                    color = hpColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.04f),
                )
            }
            Text(
                text = "${hp}hp",
                color = hpColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp, end = 10.dp)
            )
            Text(
                text = gameTime,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 6.dp, bottom = 6.dp)
            )
        }
        // Visual HP bar — 110dp wide segmented bar showing hp/MAX_HP ratio,
        // glow intensity scales with hp deficit so low HP "screams".
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(88.dp)
                .height(6.dp)
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
            // Round 67.6 — Vector mineral gem replacing ic_mineral.webp.
            // Diamond/rhombus filled gold with bright cyan core highlight.
            androidx.compose.foundation.Canvas(
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
                    ),
            ) {
                // Diamond/rhombus gem with cyan core sparkle.
                val cx = size.width / 2f
                val cy = size.height / 2f
                val halfW = size.width * 0.42f
                val halfH = size.height * 0.46f
                val path = PathPool.acquire().apply {
                    moveTo(cx, cy - halfH)
                    lineTo(cx + halfW, cy)
                    lineTo(cx, cy + halfH)
                    lineTo(cx - halfW, cy)
                    close()
                }
                drawPath(path, NeonGold)
                drawPath(path, NeonCyan,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.08f))
                PathPool.release(path)
                // Inner sparkle line
                drawLine(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(cx - halfW * 0.3f, cy - halfH * 0.3f),
                    end = androidx.compose.ui.geometry.Offset(cx + halfW * 0.15f, cy + halfH * 0.15f),
                    strokeWidth = size.width * 0.10f,
                )
            }
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
        // ── Left column: Revive + Combo (combat continued) ──
        if (hasReviveToken) {
            Spacer(modifier = Modifier.height(4.dp))
            ReviveTokenBadge()
        }
        Spacer(modifier = Modifier.height(4.dp))
        ComboHud(
            count = comboCount,
            tier = comboTier,
            lastKillMillis = lastEnemyKillMillis,
        )
        } // end Left column

        // ── Right column: PROGRESSION ──
        Column {
        // Round 77 (R77b) — chapter/stage/kills/ship badge stacked vertically.
        if (currentChapterId > 0) {
            Text(
                text = "Ch.$currentChapterId",
                color = NeonGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.neonGlow(NeonGold, intensity = 0.4f, radiusFactor = 1.2f),
            )
            if (currentChapterName.isNotEmpty()) {
                Text(
                    text = currentChapterName,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (stagesReached > 0) {
                Text(
                    text = "Stage $stagesReached",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }
        }
        // Combat counter (right col, stacked).
        if (enemiesKilledTotal > 0 || bossesDefeatedTotal > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⚔ $enemiesKilledTotal",
                color = NeonCyan.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            if (bossesDefeatedTotal > 0) {
                Text(
                    text = "☠ $bossesDefeatedTotal",
                    color = NeonRedAlert.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Round 76 (R76d) — Ship shape badge.
        if (shipShape != com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "◈ ${shipShape.displayName}",
                color = Color(0xFFB14CFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFFB14CFF).copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        } // end Right column
    } // end Row
}

@Composable
private fun ReviveTokenBadge() {
    val pulseColor = Color(0xFF00FFB0)         // matches booster_revive vector
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(pulseColor.copy(alpha = 0.18f))
            .neonGlow(pulseColor, intensity = 0.4f, radiusFactor = 1.2f)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "♥",
            color = pulseColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "HỒI SINH",
            color = pulseColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
