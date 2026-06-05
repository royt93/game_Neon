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

/**
 * Top-left HUD. Round 77 (R77b) — 2-column layout: COMBAT (left) + PROGRESSION
 * (right).
 *
 * Wave 16 (perf) — split the former 15-param monolith into [CombatColumn] +
 * [ProgressionColumn]. Two wins: (1) smaller functions → less JIT at cold-start
 * (the monolith cost ~6.8MB to compile → a Choreographer skip on first frame);
 * (2) the flash-timer + pulse state now lives inside [CombatColumn], so the
 * frequently-recomposing combat half no longer drags the rarely-changing
 * progression half (and vice-versa). Public signature unchanged.
 */
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
    // Round 76 (R76d) — chapter / stage / enemies killed / bosses / ship badge.
    currentChapterId: Int = 0,
    currentChapterName: String = "",
    stagesReached: Int = 0,
    enemiesKilledTotal: Int = 0,
    bossesDefeatedTotal: Int = 0,
    shipShape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape =
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
    // Wave 18 — nhãn đạn đang bắn dời vào cột trái (hàng khoáng) để hết đè boss HP bar.
    activeBulletName: String = "",
    activeBulletColorArgb: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val buttonPaddingEnd = dimensionResource(id = R.dimen.button_padding)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(start = buttonPaddingEnd, top = 8.dp),
    ) {
        CombatColumn(
            gameTime = gameTime,
            hp = hp,
            mineralsEarnedTotal = mineralsEarnedTotal,
            comboCount = comboCount,
            comboTier = comboTier,
            lastEnemyKillMillis = lastEnemyKillMillis,
            lastMineralPickupMillis = lastMineralPickupMillis,
            lastBoosterPickupMillis = lastBoosterPickupMillis,
            hasReviveToken = hasReviveToken,
            activeBulletName = activeBulletName,
            activeBulletColorArgb = activeBulletColorArgb,
        )
        ProgressionColumn(
            currentChapterId = currentChapterId,
            currentChapterName = currentChapterName,
            stagesReached = stagesReached,
            enemiesKilledTotal = enemiesKilledTotal,
            bossesDefeatedTotal = bossesDefeatedTotal,
            shipShape = shipShape,
        )
    }
}

/** Left column — HP capsule + bar, mineral counter, revive token, combo. Owns
 *  the pickup-flash pulse timer (ticks only while a flash is in flight). */
@Composable
private fun CombatColumn(
    gameTime: String,
    hp: Int,
    mineralsEarnedTotal: String,
    comboCount: Int,
    comboTier: ComboTier,
    lastEnemyKillMillis: Long,
    lastMineralPickupMillis: Long,
    lastBoosterPickupMillis: Long,
    hasReviveToken: Boolean,
    activeBulletName: String = "",
    activeBulletColorArgb: Long = 0L,
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
        if (t < 0.2f) 1f + (t / 0.2f) * 0.3f else 1.3f - ((t - 0.2f) / 0.8f) * 0.3f
    } else 1f
    val hpPulse = if (lastBoosterPickupMillis > 0L && boosterFlashElapsed in 0L..350L) {
        val t = boosterFlashElapsed.toFloat() / 350f
        if (t < 0.2f) 1f + (t / 0.2f) * 0.25f else 1.25f - ((t - 0.2f) / 0.8f) * 0.25f
    } else 1f
    val mineralFlashIntensity = if (mineralPulse > 1f) (mineralPulse - 1f) * 1.5f else 0f
    val hpFlashIntensity = if (hpPulse > 1f) (hpPulse - 1f) * 1.5f else 0f

    // Wave 11d Bug #2 fix — compact HUD on tall device (Pixel 7 Pro).
    // Wave 18 — thu gọn thêm (user: "top view to quá che UI"): 44→38dp.
    val height = 38.dp
    val hpRatio = (hp.toFloat() / MAX_HP).coerceIn(0f, 1f)
    val hpColor = when {
        hp >= 700 -> NeonCyan
        hp >= 300 -> NeonGold
        else -> NeonRedAlert
    }

    Column {
        Box(modifier = Modifier.height(height = height)) {
            // Round 67.6 — Vector HP frame (stadium outline + neon glow, color tracks HP tier).
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 96.dp, height = height)
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp, end = 8.dp)
            )
            Text(
                text = gameTime,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 6.dp, bottom = 6.dp)
            )
        }
        // Visual HP bar — glow intensity scales with hp deficit so low HP "screams".
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(72.dp)
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
            // Round 67.6 — Vector mineral gem (diamond gold + cyan core sparkle).
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
            // Wave 18 — nhãn đạn dời về đây (trước ở TopCenter, đè thanh HP boss).
            if (activeBulletName.isNotEmpty()) {
                Text(
                    text = "⦿ $activeBulletName",
                    color = Color(activeBulletColorArgb),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
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
    }
}

/** Right column — chapter / stage / kills / boss-defeats / ship-shape badge.
 *  Pure read-only stats; recomposes only when progression actually advances. */
@Composable
private fun ProgressionColumn(
    currentChapterId: Int,
    currentChapterName: String,
    stagesReached: Int,
    enemiesKilledTotal: Int,
    bossesDefeatedTotal: Int,
    shipShape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape,
) {
    Column {
        if (currentChapterId > 0) {
            // Wave 18 — gộp "Ch.X" + "Stage Y" về 1 dòng (trước 3 dòng → chật top-center).
            Text(
                text = if (stagesReached > 0) "Ch.$currentChapterId · St.$stagesReached"
                else "Ch.$currentChapterId",
                color = NeonGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.neonGlow(NeonGold, intensity = 0.4f, radiusFactor = 1.2f),
            )
            if (currentChapterName.isNotEmpty()) {
                Text(
                    text = currentChapterName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp),
                )
            }
        }
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
    }
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
