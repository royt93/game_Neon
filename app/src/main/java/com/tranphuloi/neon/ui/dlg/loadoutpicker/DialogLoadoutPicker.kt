package com.tranphuloi.neon.ui.dlg.loadoutpicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.LocalNeonPalette
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonPalette
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.weapon.SecondaryWeapon
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 6 (36x) round 45 — pre-game loadout picker. Shown between MenuScreen
 * PLAY tap and Game entry. Lets the player choose:
 *
 *  - **Vũ khí chính** ([BulletType]): NORMAL / PIERCING / PLASMA. Ship starts
 *    the run with this active for 10s. NORMAL = no head-start.
 *  - **Vũ khí phụ** ([SecondaryWeapon]): MISSILE / MINE / BURST. Bound to the
 *    secondary fire button for the whole run.
 *
 * Both selections persist to Settings so the next run defaults to the last
 * pick. Tap "BẮT ĐẦU" to commit and proceed to Game.
 */
@Composable
fun DialogLoadoutPicker(onConfirm: () -> Unit) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    // Round 45 fix 1 — was `collectAsState(initial = NORMAL/MISSILE)` which
    // flashed the inert default for 1 frame before DataStore resolved the
    // user's saved pick. `produceState<T?>` keeps the State null until the
    // first real emission, so card selection only highlights after the
    // resolved value arrives. The cards stay un-highlighted for ~10-30ms but
    // no longer briefly show the wrong selection.
    val bulletType: BulletType? by produceState<BulletType?>(initialValue = null, settings) {
        settings.preferredBulletType.collect { value = it }
    }
    val secondary: SecondaryWeapon? by produceState<SecondaryWeapon?>(initialValue = null, settings) {
        settings.secondaryWeapon.collect { value = it }
    }
    // Round 45 fix 3 — palette accents follow Color Blind mode (round 39).
    val palette = LocalNeonPalette.current

    LaunchedEffect(Unit) { Logger.d("DialogLoadoutPicker shown") }

    NeonBottomSheet(
        title = "TRANG BỊ",
        accentColor = palette.gold,
        onDismiss = {
            Logger.d("DialogLoadoutPicker: dismissed (committing $bulletType + $secondary)")
            onConfirm()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Chọn vũ khí cho lượt chơi này",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(18.dp))

            // ─── Vũ khí chính (BulletType) ─────────────────────────
            SectionHeader(label = "VŨ KHÍ CHÍNH", color = palette.cyan)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BulletType.entries.forEach { b ->
                    val color = colorForBullet(b, palette)
                    LoadoutCard(
                        glyph = b.glyph,
                        title = b.displayName,
                        subtitle = subtitleForBullet(b),
                        color = color,
                        selected = b == bulletType,
                        onClick = {
                            Logger.d("LoadoutPicker: BulletType pick=$b")
                            scope.launch { settings.setPreferredBulletType(b) }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Vũ khí phụ (SecondaryWeapon) ──────────────────────
            SectionHeader(label = "VŨ KHÍ PHỤ", color = palette.magenta)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SecondaryWeapon.entries.forEach { w ->
                    val color = colorForSecondary(w, palette)
                    LoadoutCard(
                        glyph = w.glyph,
                        title = w.displayName,
                        subtitle = subtitleForSecondary(w),
                        color = color,
                        selected = w == secondary,
                        onClick = {
                            Logger.d("LoadoutPicker: SecondaryWeapon pick=$w")
                            scope.launch { settings.setSecondaryWeapon(w) }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ─── Confirm button ────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.gold.copy(alpha = 0.18f))
                    .border(BorderStroke(2.dp, palette.gold), RoundedCornerShape(14.dp))
                    .neonGlow(color = palette.gold, intensity = 0.55f, radiusFactor = 1.8f)
                    .clickable {
                        Logger.d("LoadoutPicker: BẮT ĐẦU tapped (committing $bulletType + $secondary)")
                        onConfirm()
                    }
                    .padding(vertical = 14.dp),
            ) {
                Text(
                    text = "▶ BẮT ĐẦU",
                    color = palette.gold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .background(color),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LoadoutCard(
    glyph: String,
    title: String,
    subtitle: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgAlpha = if (selected) 0.30f else 0.10f
    val borderWidth = if (selected) 2.dp else 1.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color.copy(alpha = bgAlpha))
            .border(BorderStroke(borderWidth, color), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.22f))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(10.dp)),
        ) {
            Text(text = glyph, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
        }
        if (selected) {
            Text(text = "✓", color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun colorForBullet(b: BulletType, palette: NeonPalette): Color = when (b) {
    BulletType.NORMAL -> palette.cyan
    BulletType.PIERCING -> palette.gold
    BulletType.PLASMA -> palette.violet
    // Round 67 (Wave 10a) — 3 new bullet types. Default colors so when() is
    // exhaustive; picker tile grid update deferred to Round 67e (UI polish).
    BulletType.FIRE -> palette.redAlert
    BulletType.HOMING -> palette.magenta
    BulletType.BOUNCE -> palette.cyan
    BulletType.GIANT -> palette.gold
    BulletType.SMOKE -> palette.violet
    BulletType.ZIGZAG -> palette.gold
    BulletType.KAMEHAMEHA -> palette.cyan
    BulletType.ATOMIC -> palette.gold
    BulletType.SPLIT -> palette.violet
}

private fun subtitleForBullet(b: BulletType): String = when (b) {
    BulletType.NORMAL -> "Đạn tiêu chuẩn · không buff khởi đầu"
    BulletType.PIERCING -> "Xuyên qua 3 enemy · 10s head-start"
    BulletType.PLASMA -> "+60% dmg · AoE 80px · 10s head-start"
    BulletType.FIRE -> "Đạn lửa · BURN DoT · ${b.activeDurationMillis / 1000}s"
    BulletType.HOMING -> "Đuổi địch · ×${b.damageMultiplier} dmg · ${b.activeDurationMillis / 1000}s"
    BulletType.BOUNCE -> "Phản xạ ×3 hits · ×${b.damageMultiplier} dmg · ${b.activeDurationMillis / 1000}s"
    BulletType.GIANT -> "Đạn khổng lồ · ×${b.damageMultiplier} dmg · ${b.activeDurationMillis / 1000}s"
    BulletType.SMOKE -> "Đạn khói · slow (stub) · ${b.activeDurationMillis / 1000}s"
    BulletType.ZIGZAG -> "Đạn zigzag · sine path (stub) · ${b.activeDurationMillis / 1000}s"
    BulletType.KAMEHAMEHA -> "Kamehameha · pierce-all (stub) · ×${b.damageMultiplier} · ${b.activeDurationMillis / 1000}s"
    BulletType.ATOMIC -> "Đạn nguyên tử · AoE 150dp (stub) · ${b.activeDurationMillis / 1000}s"
    BulletType.SPLIT -> "Đạn phân tách · 3 children (stub) · ${b.activeDurationMillis / 1000}s"
}

private fun colorForSecondary(w: SecondaryWeapon, palette: NeonPalette): Color = when (w) {
    SecondaryWeapon.MISSILE -> palette.cyan
    SecondaryWeapon.MINE -> palette.redAlert
    SecondaryWeapon.BURST -> palette.magenta
}

private fun subtitleForSecondary(w: SecondaryWeapon): String = when (w) {
    SecondaryWeapon.MISSILE -> "Bắn tên lửa dò đường · cd 5s"
    SecondaryWeapon.MINE -> "Đặt mìn sau ship · AoE · cd 7s"
    SecondaryWeapon.BURST -> "Quét 5 enemy gần nhất · cd 8s"
}
