package com.tranphuloi.neon.ui.dlg.loadoutpicker

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.tranphuloi.neon.common.PathPool

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
fun DialogLoadoutPicker(
    onConfirm: () -> Unit,
    // Round 72 fix (user audit) — separate dismiss callback. Trước fix
    // onDismiss = onConfirm → drag-down hoặc ✕ tap → auto navigate Game (bug).
    onDismiss: () -> Unit = {},
) {
    val settings = LocalSettings.current
    val meta = com.tranphuloi.neon.data.LocalMetaProgression.current
    val scope = rememberCoroutineScope()
    // Wave 12 round 3 — shop-gated bullets (KAMEHAMEHA/ATOMIC). allRanks keyed
    // by ShopItem.persistKey; a bullet with a non-null shopUnlockId stays locked
    // until rank > 0.
    val shopRanks by meta.allRanks.collectAsState(initial = emptyMap())
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
            // Round 72 fix — pop back stack KHÔNG commit. User chỉ confirm khi
            // tap nút BẮT ĐẦU explicit (line ~170).
            Logger.d("DialogLoadoutPicker: dismissed (NO commit)")
            onDismiss()
        },
    ) {
        // Round 72 fix (Issue 1) — verticalScroll cho section dài. Trước fix
        // section "Vũ khí phụ" bị cắt vì content > sheet maxHeight (90% screen).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
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
                // Wave 12 round 3 — if the saved pick is shop-locked, the run
                // falls back to NORMAL at start (see GameState), so highlight
                // NORMAL here to match what will actually be used.
                val effectiveBullet = bulletType?.let {
                    if (com.tranphuloi.neon.data.ShopItem.isShopUnlocked(shopRanks, it.shopUnlockId)) it
                    else BulletType.NORMAL
                }
                BulletType.entries.forEach { b ->
                    val color = colorForBullet(b, palette)
                    // Wave 12 round 3 — shop-gated bullets stay locked until bought.
                    val locked = !com.tranphuloi.neon.data.ShopItem
                        .isShopUnlocked(shopRanks, b.shopUnlockId)
                    // Round 71 (Issue 3) — animated bullet preview + multi-line +
                    // tooltip + color-code thickness theo damage tier.
                    LoadoutCard(
                        glyph = b.glyph,
                        title = b.displayName,
                        subtitle = subtitleForBullet(b),
                        tip = if (locked) "🔒 Mua ở Cửa hàng để mở khóa" else tipForBullet(b),
                        color = color,
                        damageTier = damageTier(b.damageMultiplier),
                        selected = b == effectiveBullet && !locked,
                        bulletPreview = b,
                        locked = locked,
                        onClick = {
                            if (locked) {
                                Logger.d("LoadoutPicker: BulletType $b locked — buy in shop")
                            } else {
                                Logger.d("LoadoutPicker: BulletType pick=$b")
                                scope.launch { settings.setPreferredBulletType(b) }
                            }
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
                        tip = "Bắn theo phím phụ. Đạn limit/run.",
                        color = color,
                        damageTier = 1,                                  // secondary always mid tier
                        selected = w == secondary,
                        bulletPreview = null,                            // glyph mode for secondary
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

/**
 * Round 71 (Issue 3) — 4 enrichments:
 *   1. Animated bullet preview (mini Canvas) thay glyph cho bullets.
 *   2. Multi-line subtitle (damage/duration/AoE).
 *   3. Tooltip dòng dưới (✦ tip).
 *   4. Color-code border thickness theo damage tier (1=low, 2=mid, 3=high).
 */
@Composable
private fun LoadoutCard(
    glyph: String,
    title: String,
    subtitle: String,
    tip: String,
    color: Color,
    damageTier: Int,
    selected: Boolean,
    bulletPreview: BulletType?,
    onClick: () -> Unit,
    locked: Boolean = false,
) {
    val bgAlpha = if (selected) 0.30f else if (locked) 0.05f else 0.10f
    // Round 71 — color-code border: tier 1 (×0.x dmg) thin, tier 2 (×1.x) mid, tier 3 (×2+) thick.
    val baseBorder = when (damageTier) { 1 -> 1.dp; 2 -> 1.5.dp; else -> 2.5.dp }
    val borderWidth = if (selected) baseBorder + 1.dp else baseBorder
    // Round 71 (Issue 3) — selected card có border đậm hơn (no animation —
    // simpler + ít cost). Color-code thickness handles damage tier.
    // Wave 12 round 3 — locked cards dim border + content so the 🔒 tip reads
    // as disabled (tap logs a hint rather than selecting).
    val borderColor = when {
        selected -> color
        locked -> color.copy(alpha = 0.3f)
        else -> color.copy(alpha = 0.7f)
    }
    val contentColor = if (locked) color.copy(alpha = 0.45f) else color
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color.copy(alpha = bgAlpha))
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.22f))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(10.dp)),
        ) {
            if (bulletPreview != null) {
                // Round 71 fix (Issue 3 audit) — ANIMATED preview: infinite bob
                // Y-axis 800ms loop. Trước fix là static Canvas → spec gap.
                val bobAnim = rememberInfiniteTransition(label = "bulletBob")
                val bobOffset by bobAnim.animateFloat(
                    initialValue = -2f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "bulletBobOffset",
                )
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(36.dp).offset(y = bobOffset.dp),
                ) {
                    drawBulletPreview(size, bulletPreview, color)
                }
            } else {
                Text(text = glyph, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "✦ $tip",
                color = color.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
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

// Round 71 (Issue 3) — multi-line subtitle: line 1 = combat stats, line 2 = description.
private fun subtitleForBullet(b: BulletType): String = when (b) {
    BulletType.NORMAL -> "Sát thương ×1.0 · Không thời hạn\nĐạn tiêu chuẩn không buff khởi đầu."
    BulletType.PIERCING -> "Sát thương ×1.0 · ⏱10s · Xuyên 3 enemy\nĐạn xuyên qua nhiều địch trước khi biến mất."
    BulletType.PLASMA -> "Sát thương ×1.6 · ⏱10s · AoE 80px\nNổ AoE khi va chạm, sát thương quanh điểm chạm."
    BulletType.FIRE -> "Sát thương ×1.2 · ⏱10s · Cháy 5HP/s\nGây cháy enemy mất máu liên tục 3 giây."
    BulletType.HOMING -> "Sát thương ×0.8 · ⏱10s · Tự đuổi\nĐạn tự nhắm enemy gần nhất, không cần aim."
    BulletType.BOUNCE -> "Sát thương ×0.7 · ⏱12s · Nảy 3 lần\nNảy lại khi va cạnh, trúng nhiều enemy/viên."
    BulletType.GIANT -> "Sát thương ×2.0 · ⏱10s · Size ×2\nĐạn to gấp đôi + damage gấp đôi."
    BulletType.SMOKE -> "Sát thương ×0.8 · ⏱10s · AoE 60px\nĐạn khói AoE chậm enemy đi qua."
    BulletType.ZIGZAG -> "Sát thương ×0.9 · ⏱12s · Sine path\nĐạn bay zigzag né dodge enemy."
    BulletType.KAMEHAMEHA -> "Sát thương ×3.0 · ⏱8s · Pierce-all\nTia năng lượng xuyên thấu vô hạn."
    BulletType.ATOMIC -> "Sát thương ×1.5 · ⏱10s · AoE 150px\nNổ nguyên tử AoE khổng lồ."
    BulletType.SPLIT -> "Sát thương ×0.6 · ⏱12s · Tách 3\nVa chạm phân tách thành 3 mảnh nhỏ."
}

// Round 71 (Issue 3) — tooltip 1-line explaining game mechanic.
private fun tipForBullet(b: BulletType): String = when (b) {
    BulletType.NORMAL -> "Không buff. Phù hợp cho người mới."
    BulletType.PIERCING -> "10s 'head-start' = active ngay khi vào game, hết sau 10s."
    BulletType.PLASMA -> "Hợp khi enemy bay theo cụm dày đặc."
    BulletType.FIRE -> "Hợp với enemy nhiều máu (DoT tích lũy)."
    BulletType.HOMING -> "Tốt cho người mới — đạn auto-aim."
    BulletType.BOUNCE -> "Tốt khi enemy bay sát mép màn hình."
    BulletType.GIANT -> "Combo với boss — damage cao + hit box to."
    BulletType.SMOKE -> "Hợp cho stage có enemy bay theo đường thẳng."
    BulletType.ZIGZAG -> "Khó né hơn cho enemy — hợp boss fight dài."
    BulletType.KAMEHAMEHA -> "Damage ×3 cực mạnh — luôn ưu tiên khi gặp boss."
    BulletType.ATOMIC -> "AoE rộng — hợp khi enemy cụm dày đặc."
    BulletType.SPLIT -> "Damage thấp nhưng phủ rộng — dọn enemy yếu."
}

// Round 71 (Issue 3) — damage tier mapping cho border thickness.
//   ≤0.9 → tier 1 (thin border)
//   1.0-1.7 → tier 2 (mid)
//   ≥1.8 → tier 3 (thick — KAMEHAMEHA, GIANT)
private fun damageTier(mul: Float): Int = when {
    mul < 1.0f -> 1
    mul < 1.8f -> 2
    else -> 3
}

// Round 71 audit fix — mini bullet preview Canvas cho LoadoutCard tile.
// Trước fix có 5 shape + 1 default chung 7 bullets → SAI spec "12 unique".
// Giờ dispatch all 12 distinct recipes (compact version cho tile 36dp).
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBulletPreview(
    canvasSize: androidx.compose.ui.geometry.Size,
    bullet: BulletType,
    color: Color,
) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val w = canvasSize.width
    drawCircle(color = color.copy(alpha = 0.3f), radius = w * 0.42f,
        center = androidx.compose.ui.geometry.Offset(cx, cy))
    val capsuleW = w * 0.25f
    val capsuleH = w * 0.7f
    when (bullet) {
        BulletType.NORMAL -> {
            // Plain capsule + white core
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 2, cy - capsuleH / 2),
                size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2))
            drawRoundRect(color = Color.White.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 4, cy - capsuleH / 2 + capsuleH * 0.12f),
                size = androidx.compose.ui.geometry.Size(capsuleW / 2, capsuleH * 0.76f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 4))
        }
        BulletType.PIERCING -> {
            val path = PathPool.acquire().apply {
                moveTo(cx, cy - w * 0.42f)
                lineTo(cx + w * 0.18f, cy + w * 0.42f)
                lineTo(cx - w * 0.18f, cy + w * 0.42f)
                close()
            }
            drawPath(path, color)
            PathPool.release(path)
        }
        BulletType.PLASMA -> {
            drawCircle(color, w * 0.32f, androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.85f), w * 0.16f,
                androidx.compose.ui.geometry.Offset(cx, cy))
        }
        BulletType.FIRE -> {
            // Capsule + flame trail below
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 2, cy - capsuleH * 0.5f),
                size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2))
            val flamePath = PathPool.acquire().apply {
                moveTo(cx - capsuleW / 2, cy + capsuleH * 0.2f)
                lineTo(cx, cy + capsuleH * 0.5f)
                lineTo(cx + capsuleW / 2, cy + capsuleH * 0.2f)
                close()
            }
            drawPath(flamePath, Color(0xFFFFD040).copy(alpha = 0.9f))
            PathPool.release(flamePath)
        }
        BulletType.HOMING -> {
            // Capsule + targeting ring
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 2, cy - capsuleH / 2),
                size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2))
            drawCircle(color = color, radius = w * 0.32f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
        }
        BulletType.BOUNCE -> {
            // Ball + 2 motion arc dots
            drawCircle(color.copy(alpha = 0.45f), w * 0.12f,
                androidx.compose.ui.geometry.Offset(cx - w * 0.2f, cy + w * 0.18f))
            drawCircle(color.copy(alpha = 0.65f), w * 0.10f,
                androidx.compose.ui.geometry.Offset(cx + w * 0.2f, cy + w * 0.12f))
            drawCircle(color, w * 0.22f, androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.09f,
                androidx.compose.ui.geometry.Offset(cx, cy))
        }
        BulletType.GIANT -> {
            val ww = w * 0.45f; val hh = w * 0.85f
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - ww / 2, cy - hh / 2),
                size = androidx.compose.ui.geometry.Size(ww, hh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(ww / 2))
            // 2 segment dividers consistent với in-game
            drawLine(color, androidx.compose.ui.geometry.Offset(cx - ww / 4, cy - hh * 0.15f),
                androidx.compose.ui.geometry.Offset(cx + ww / 4, cy - hh * 0.15f), strokeWidth = w * 0.025f)
            drawLine(color, androidx.compose.ui.geometry.Offset(cx - ww / 4, cy + hh * 0.15f),
                androidx.compose.ui.geometry.Offset(cx + ww / 4, cy + hh * 0.15f), strokeWidth = w * 0.025f)
        }
        BulletType.SMOKE -> {
            // Capsule + cloud puff
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 2, cy - capsuleH * 0.55f),
                size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2))
            drawCircle(color.copy(alpha = 0.5f), capsuleW * 0.55f,
                androidx.compose.ui.geometry.Offset(cx, cy + capsuleH * 0.3f))
        }
        BulletType.ZIGZAG -> {
            val path = PathPool.acquire().apply {
                val step = capsuleH / 4f
                val widerW = capsuleW * 1.8f
                moveTo(cx - widerW / 2, cy - capsuleH / 2)
                lineTo(cx + widerW / 2, cy - capsuleH / 2 + step)
                lineTo(cx - widerW / 2, cy - capsuleH / 2 + step * 2)
                lineTo(cx + widerW / 2, cy - capsuleH / 2 + step * 3)
                lineTo(cx - widerW / 2, cy + capsuleH / 2)
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = capsuleW * 0.4f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ))
            PathPool.release(path)
        }
        BulletType.KAMEHAMEHA -> {
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.4f, cy - w * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.8f, w * 0.24f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f))
            drawCircle(color.copy(alpha = 0.8f), w * 0.14f,
                androidx.compose.ui.geometry.Offset(cx + w * 0.4f, cy))
        }
        BulletType.ATOMIC -> {
            drawCircle(color, w * 0.18f, androidx.compose.ui.geometry.Offset(cx, cy))
            for (i in 0 until 3) {
                val a = i * 120.0 * Math.PI / 180.0
                val ex = cx + (w * 0.32f * kotlin.math.cos(a)).toFloat()
                val ey = cy + (w * 0.32f * kotlin.math.sin(a)).toFloat()
                drawCircle(color.copy(alpha = 0.85f), w * 0.07f,
                    androidx.compose.ui.geometry.Offset(ex, ey))
            }
        }
        BulletType.SPLIT -> {
            // Capsule + 3 branches at top
            drawRoundRect(color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - capsuleW / 2, cy - capsuleH * 0.2f),
                size = androidx.compose.ui.geometry.Size(capsuleW, capsuleH * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(capsuleW / 2))
            val branchTop = cy - capsuleH * 0.2f
            val branchLen = capsuleH * 0.35f
            for (i in -1..1) {
                val angle = i * 30.0 * Math.PI / 180.0
                val tipX = cx + (branchLen * kotlin.math.sin(angle)).toFloat()
                val tipY = branchTop - (branchLen * kotlin.math.cos(angle)).toFloat()
                drawLine(color = color,
                    start = androidx.compose.ui.geometry.Offset(cx, branchTop),
                    end = androidx.compose.ui.geometry.Offset(tipX, tipY),
                    strokeWidth = capsuleW * 0.4f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
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
