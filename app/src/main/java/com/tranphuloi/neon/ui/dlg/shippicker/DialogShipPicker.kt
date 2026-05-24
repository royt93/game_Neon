package com.tranphuloi.neon.ui.dlg.shippicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.LocalNeonPalette
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.ship.shape.ShipShape
import com.tranphuloi.neon.ui.game.world.drawShipVector
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Round 73 (Wave 8) — Ship picker bottom sheet. Lists 5 ShipShape with stat
 * profile, unlock requirement (lifetime minerals), visual preview. Selected
 * one persists to Settings.selectedShipShape → wire vào EffectiveStats khi
 * vào run mới.
 *
 * Locked ships: still rendered nhưng disabled tap, badge "🔒 Cần X khoáng".
 */
@Composable
fun DialogShipPicker(onDismiss: () -> Unit) {
    val settings = LocalSettings.current
    val meta = LocalMetaProgression.current
    val scope = rememberCoroutineScope()
    val palette = LocalNeonPalette.current

    val selectedShape by settings.selectedShipShape.collectAsState(initial = ShipShape.FIGHTER)
    val lifetimeMinerals by meta.lifetimeMinerals.collectAsState(initial = 0)
    // Round 74 (R73f) — apply SHIP_UNLOCK_DISCOUNT meta upgrade (max 5 ranks × 10% = 50% off).
    val allMetaRanks by meta.allRanks.collectAsState(initial = emptyMap())
    val unlockDiscountRank = allMetaRanks[
        com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_SHIP_UNLOCK_DISCOUNT
    ] ?: 0
    val unlockDiscountFactor = (1f - unlockDiscountRank *
        com.tranphuloi.neon.ui.game.state.EffectiveStats.META_SHIP_UNLOCK_DISCOUNT_PER_RANK)
        .coerceIn(0.5f, 1f)

    LaunchedEffect(Unit) { Logger.d("DialogShipPicker shown (selected=$selectedShape, mins=$lifetimeMinerals)") }

    NeonBottomSheet(
        title = "CHỌN LOẠI TÀU",
        accentColor = palette.cyan,
        onDismiss = {
            Logger.d("DialogShipPicker dismissed")
            onDismiss()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Mỗi tàu có chỉ số khác nhau. Mở khóa theo khoáng tích lũy.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Text(
                text = "Khoáng tích lũy: $lifetimeMinerals",
                color = palette.gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            )

            // Round 74 (R73f) — show discount banner if user has SHIP_UNLOCK rank.
            if (unlockDiscountRank > 0) {
                Text(
                    text = "✦ Giảm giá ship -${(unlockDiscountRank * 10)}% (TỔ HỢP HÀNG KHÔNG cấp $unlockDiscountRank)",
                    color = palette.violet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            ShipShape.entries.forEach { shape ->
                val color = colorForShape(shape)
                val effectiveCost = (shape.unlockMinerals * unlockDiscountFactor).toInt()
                val unlocked = lifetimeMinerals >= effectiveCost
                val isSelected = shape == selectedShape
                ShipShapeRow(
                    shape = shape,
                    effectiveCost = effectiveCost,
                    discounted = unlockDiscountRank > 0 && shape.unlockMinerals > 0,
                    color = color,
                    unlocked = unlocked,
                    selected = isSelected,
                    onClick = {
                        if (unlocked) {
                            Logger.d("ShipPicker pick=$shape (effectiveCost=$effectiveCost)")
                            scope.launch { settings.setSelectedShipShape(shape) }
                        } else {
                            Logger.d("ShipPicker locked tap=$shape (need $effectiveCost)")
                        }
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private fun colorForShape(shape: ShipShape): Color = when (shape) {
    ShipShape.FIGHTER -> NeonCyan
    ShipShape.BOMBER -> NeonGold
    ShipShape.STEALTH -> NeonViolet
    ShipShape.TANK -> Color(0xFFFF6020)
    ShipShape.INTERCEPTOR -> NeonMagenta
    // Round 79 (#3) — accent colors for 12 new ships, each thematic.
    ShipShape.NGOI_SAO -> Color(0xFFFFD700)              // gold star
    ShipShape.CAU_VONG -> Color(0xFFFF80E0)              // pink rainbow accent
    ShipShape.PHU_THUY -> Color(0xFFB14CFF)              // violet witch
    ShipShape.AURA_GLOW -> Color(0xFF60FFAA)             // mint aura
    ShipShape.SUNG_3_NONG -> Color(0xFFFF3030)           // crimson gun
    ShipShape.OBELISK_SPIRE -> Color(0xFFE0E0E0)         // pale stone
    ShipShape.VIETNAM -> Color(0xFFDA251D)               // VN red
    ShipShape.DIVA -> Color(0xFFFF80B0)                  // diva pink
    ShipShape.CHET_CHOC -> Color(0xFF606060)             // gray reaper steel
    ShipShape.TU_THAN -> Color(0xFF000000)               // black void
    ShipShape.MANG_NHEN_ACE -> Color(0xFFCC2030)         // spider red
    ShipShape.AO_GIAP_THIET -> Color(0xFFCC2020)         // iron red
    // Round 79 audit follow-up — 5 new ships.
    ShipShape.TWIN_DOMES -> Color(0xFFFFA0C0)            // soft pink
    ShipShape.NHAT_BAN -> Color(0xFFBC002D)              // Hinomaru red
    ShipShape.HAN_QUOC -> Color(0xFF0047A0)              // Korea blue
    ShipShape.MY -> Color(0xFF3C3B6E)                    // USA blue
    ShipShape.PHAP -> Color(0xFF002654)                  // France blue
}

@Composable
private fun ShipShapeRow(
    shape: ShipShape,
    effectiveCost: Int,
    discounted: Boolean,
    color: Color,
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgAlpha = if (selected) 0.30f else if (unlocked) 0.10f else 0.04f
    val borderColor = if (selected) color
        else if (unlocked) color.copy(alpha = 0.55f)
        else Color.White.copy(alpha = 0.25f)
    val borderWidth = if (selected) 2.5.dp else 1.dp
    val contentColor = if (unlocked) color else Color.White.copy(alpha = 0.5f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color.copy(alpha = bgAlpha))
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp))
            .let { if (selected) it.neonGlow(color, intensity = 0.4f, radiusFactor = 1.5f) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // 56dp preview
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .border(BorderStroke(1.5.dp, contentColor), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawShipVector(
                    color = contentColor,
                    laserBoosterEnabled = false,
                    shape = shape,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shape.displayName,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 1.sp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Máu ×${shape.hpMul} · Tốc độ ×${shape.speedMul} · ST ×${shape.damageMul}",
                color = Color.White.copy(alpha = if (unlocked) 0.82f else 0.45f),
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (!unlocked) {
                Text(
                    text = if (discounted)
                        "🔒 Cần $effectiveCost khoáng (gốc ${shape.unlockMinerals}, đã giảm giá)"
                    else "🔒 Cần $effectiveCost khoáng tích lũy",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            } else if (shape.unlockMinerals > 0) {
                Text(
                    text = "✓ Đã mở khóa",
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }
        if (selected) {
            Text(text = "✓", color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}
