package com.tranphuloi.neon.ui.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import com.tranphuloi.neon.ui.game.world.drawShipVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.booster.BoosterType
import com.tranphuloi.neon.ui.game.ship.laser.BulletType

private enum class InfoTab(val label: String, val color: Color) {
    BULLETS("ĐẠN", NeonCyan),
    SHIP("TÀU", NeonGold),                                       // shortened so 5 tabs fit
    ENEMIES("ĐỊCH", NeonMagenta),
    BOSSES("BOSS", NeonRedAlert),
    ITEMS("VẬT PHẨM", NeonViolet),
}

/**
 * Round 67.5 (Bách Khoa info guide) — Round 67.6 polish:
 *   - Fixed edge-to-edge: drop window-insets padding wrapper (Activity is
 *     immersive fullscreen — insets are zero anyway and were causing the
 *     content to flush against system bars on devices where the request was
 *     denied). Use safe content insets only where needed (the back button).
 *   - Real vector icon previews: each InfoCard now renders a mini Canvas
 *     drawing the SAME shape used in-game (ship arrow / enemy hexagons /
 *     boosters / bullet capsules). Player sees what the entity actually looks
 *     like, not just a glyph.
 */
@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onOpenShipPicker: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(InfoTab.BULLETS) }
    // Round 67.7 — fade-in animation for tab content. Tăng dần alpha 0→1 trong
    // 320ms mỗi khi switch tab. Subtle sinh động.
    val tabAlpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(320),
        label = "tab-fade",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBgDeep)
            // Round 67.7 — proper edge-to-edge: safeDrawing covers BOTH status
            // bar (top) + navigation bar (bottom) + display cutout. Outer Box
            // applies it once, không split insets giữa outer/inner như Round 67.6
            // (gây gap nửa-vời ở 1 cạnh).
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "BÁCH KHOA",
                    color = NeonCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.neonGlow(color = NeonCyan, intensity = 0.6f, radiusFactor = 1.4f),
                )
                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)), RoundedCornerShape(20.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text("← QUAY LẠI", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InfoTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(if (isSelected) 2.dp else 1.dp,
                                    tab.color.copy(alpha = if (isSelected) 1f else 0.4f)),
                                RoundedCornerShape(8.dp),
                            )
                            .background(
                                if (isSelected) tab.color.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            color = if (isSelected) tab.color else tab.color.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Round 67.7 — content fades in on tab switch. key(selectedTab)
            // restarts the Animatable from 0 so transition feels deliberate.
            androidx.compose.runtime.key(selectedTab) {
                val fade = remember { androidx.compose.animation.core.Animatable(0f) }
                androidx.compose.runtime.LaunchedEffect(selectedTab) {
                    fade.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(320))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = fade.value }
                        // No extra inset padding — outer Box already handles safeDrawing.
                ) {
                    when (selectedTab) {
                        InfoTab.BULLETS -> BulletsTab()
                        InfoTab.SHIP -> ShipTab(onOpenShipPicker = onOpenShipPicker)
                        InfoTab.ENEMIES -> EnemiesTab()
                        InfoTab.BOSSES -> BossesTab()
                        InfoTab.ITEMS -> ItemsTab()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Bullets tab — uses real ShipLaser/PlasmaShipLaser/etc. capsule preview.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BulletsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(BulletType.entries) { bullet ->
            InfoCard(
                color = bulletColor(bullet),
                title = bullet.displayName,
                subtitle = "Dmg ×${bullet.damageMultiplier} · ${bullet.activeDurationMillis / 1000}s",
                description = bulletDescription(bullet),
                iconDraw = { c -> drawBulletCapsule(c, bullet) },
            )
        }
    }
}

private fun bulletColor(b: BulletType): Color = when (b) {
    BulletType.NORMAL -> NeonCyan
    BulletType.PIERCING -> Color(0xFFFF2DE0)               // magenta
    BulletType.PLASMA -> Color(0xFF00F0FF)                  // cyan
    BulletType.FIRE -> Color(0xFFFF6020)                    // orange
    BulletType.HOMING -> Color(0xFFFF40A0)                  // hot pink
    BulletType.BOUNCE -> Color(0xFF40FFD0)                  // mint
    BulletType.GIANT -> Color(0xFFFFD040)                   // gold
    BulletType.SMOKE -> Color(0xFFA0A0B0)                   // gray-blue
    BulletType.ZIGZAG -> Color(0xFFFFE040)                  // electric yellow
    BulletType.KAMEHAMEHA -> Color(0xFF60E0FF)              // sky cyan
    BulletType.ATOMIC -> Color(0xFF80FF80)                  // radioactive green
    BulletType.SPLIT -> Color(0xFFB060FF)                   // purple
}

private fun bulletDescription(b: BulletType): String = when (b) {
    BulletType.NORMAL -> "Đạn cơ bản. Tốc độ 7 px/tick. Không buff khởi đầu."
    BulletType.PIERCING -> "Xuyên qua tối đa 3 enemy trước khi tiêu hủy. Common 3 / Rare 4 / Epic 5."
    BulletType.PLASMA -> "Đạn lớn với AoE explosion 80px khi trúng. Common 80 / Rare 110 / Epic 140."
    BulletType.FIRE -> "Áp dụng BURN status 100% trên mỗi hit. Enemy nhận 5HP/sec damage trong 3 giây sau."
    BulletType.HOMING -> "Tự đuổi theo enemy gần nhất mỗi tick (MissileLaser pattern)."
    BulletType.BOUNCE -> "Phản xạ off cạnh màn hình. Mỗi viên đạn hit tối đa 3 enemy trước khi tiêu hủy."
    BulletType.GIANT -> "Kích thước ×2 + damage ×2. Không cần charge-up."
    BulletType.SMOKE -> "Để lại vệt khói AoE 60px gây sát thương cộng dồn enemy đi qua."
    BulletType.ZIGZAG -> "Đạn bay theo đường zigzag né enemy dễ hơn đường thẳng."
    BulletType.KAMEHAMEHA -> "Tia laser khổng lồ xuyên thấu vô hạn enemy với damage ×3."
    BulletType.ATOMIC -> "Đạn nguyên tử nổ AoE 150px khổng lồ khi va chạm."
    BulletType.SPLIT -> "Đạn va chạm phân tách thành 3 mảnh nhỏ tiếp tục bay."
}

/**
 * Round 71 (Issue 4a) — Unique vector shape per BulletType. Dispatch table
 * thay capsule-only. Cùng glow halo + center origin (cx, cy), khác body
 * recipe per type. Color come từ bulletColor().
 *
 * Shapes:
 *   NORMAL      = capsule (baseline)
 *   PIERCING    = nhọn dài (needle)
 *   PLASMA      = full orb (circle)
 *   FIRE        = capsule + flame trail
 *   HOMING      = capsule + reticle ring
 *   BOUNCE      = ball + motion arcs
 *   GIANT       = capsule ×1.5
 *   SMOKE       = capsule + cloud puff
 *   ZIGZAG      = chevron stack
 *   KAMEHAMEHA  = wide beam + core
 *   ATOMIC      = nucleus + 3 electrons
 *   SPLIT       = capsule + 3 branches
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBulletCapsule(
    canvasSize: androidx.compose.ui.geometry.Size,
    bullet: BulletType,
) {
    val color = bulletColor(bullet)
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val w = canvasSize.width
    val h = canvasSize.height
    // Universal glow halo
    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = w * 0.42f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
    when (bullet) {
        BulletType.NORMAL -> drawCapsuleBullet(cx, cy, w * 0.25f, h * 0.7f, color)
        BulletType.PIERCING -> drawNeedleBullet(cx, cy, w * 0.18f, h * 0.85f, color)
        BulletType.PLASMA -> drawOrbBullet(cx, cy, w * 0.32f, color)
        BulletType.FIRE -> drawFireBullet(cx, cy, w * 0.25f, h * 0.7f, color)
        BulletType.HOMING -> drawHomingBullet(cx, cy, w * 0.25f, h * 0.7f, color)
        BulletType.BOUNCE -> drawBounceBullet(cx, cy, w * 0.28f, color)
        BulletType.GIANT -> drawGiantBullet(cx, cy, w * 0.45f, h * 0.85f, color)
        BulletType.SMOKE -> drawSmokeBullet(cx, cy, w * 0.25f, h * 0.7f, color)
        BulletType.ZIGZAG -> drawZigzagBullet(cx, cy, w * 0.4f, h * 0.7f, color)
        BulletType.KAMEHAMEHA -> drawBeamBullet(cx, cy, w * 0.75f, h * 0.35f, color)
        BulletType.ATOMIC -> drawAtomicBullet(cx, cy, w * 0.3f, color)
        BulletType.SPLIT -> drawSplitBullet(cx, cy, w * 0.25f, h * 0.7f, color)
    }
}

// Round 71 audit fix — GIANT distinct recipe: bigger capsule + 2 inner
// segment dividers (matches drawGiantBody in LaserCanvas). Trước fix GIANT
// dùng cùng drawCapsuleBullet → SAI spec "12 unique" — chỉ khác size.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGiantBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawCapsuleBullet(cx, cy, w, h, color)
    // 2 horizontal segment dividers across core
    val coreW = w * 0.5f
    drawLine(color = color,
        start = androidx.compose.ui.geometry.Offset(cx - coreW / 2, cy - h * 0.15f),
        end = androidx.compose.ui.geometry.Offset(cx + coreW / 2, cy - h * 0.15f),
        strokeWidth = w * 0.08f)
    drawLine(color = color,
        start = androidx.compose.ui.geometry.Offset(cx - coreW / 2, cy + h * 0.15f),
        end = androidx.compose.ui.geometry.Offset(cx + coreW / 2, cy + h * 0.15f),
        strokeWidth = w * 0.08f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCapsuleBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2, cy - h / 2),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 4, cy - h / 2 + h * 0.12f),
        size = androidx.compose.ui.geometry.Size(w / 2, h * 0.76f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 4),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeedleBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - h / 2)               // top tip
        lineTo(cx + w / 2, cy + h / 2)       // bottom-right
        lineTo(cx - w / 2, cy + h / 2)       // bottom-left
        close()
    }
    drawPath(path, color)
    val corePath = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - h / 2 + h * 0.1f)
        lineTo(cx + w / 4, cy + h / 2 - h * 0.1f)
        lineTo(cx - w / 4, cy + h / 2 - h * 0.1f)
        close()
    }
    drawPath(corePath, Color.White.copy(alpha = 0.85f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbBullet(
    cx: Float, cy: Float, r: Float, color: Color,
) {
    drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Outer ring
    drawCircle(
        color = color,
        radius = r * 1.25f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.12f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFireBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawCapsuleBullet(cx, cy - h * 0.1f, w, h * 0.85f, color)
    // Flame trail behind (below capsule)
    val flamePath = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - w / 2, cy + h / 2 - h * 0.1f)
        lineTo(cx, cy + h * 0.65f)
        lineTo(cx + w / 2, cy + h / 2 - h * 0.1f)
        close()
    }
    drawPath(flamePath, color.copy(alpha = 0.7f))
    drawPath(
        androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - w / 4, cy + h / 2 - h * 0.1f)
            lineTo(cx, cy + h * 0.5f)
            lineTo(cx + w / 4, cy + h / 2 - h * 0.1f)
            close()
        },
        Color(0xFFFFD040).copy(alpha = 0.9f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHomingBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawCapsuleBullet(cx, cy, w, h, color)
    // Reticle ring around bullet (target lock)
    val ringR = h * 0.55f
    drawCircle(
        color = color,
        radius = ringR,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.1f),
    )
    // Crosshair ticks
    val tickLen = ringR * 0.3f
    drawLine(color, androidx.compose.ui.geometry.Offset(cx + ringR, cy),
        androidx.compose.ui.geometry.Offset(cx + ringR + tickLen, cy), strokeWidth = w * 0.1f)
    drawLine(color, androidx.compose.ui.geometry.Offset(cx - ringR, cy),
        androidx.compose.ui.geometry.Offset(cx - ringR - tickLen, cy), strokeWidth = w * 0.1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBounceBullet(
    cx: Float, cy: Float, r: Float, color: Color,
) {
    // Ball + 2 motion arcs (bouncing trail)
    drawCircle(color, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.4f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Trail arcs
    for (i in 1..2) {
        val ang = i * 35.0
        val rad = ang * Math.PI / 180.0
        val tx = cx + (r * 1.5f * kotlin.math.cos(rad)).toFloat()
        val ty = cy + (r * 1.5f * kotlin.math.sin(rad)).toFloat()
        drawCircle(color.copy(alpha = 0.5f / i), r * 0.6f / i,
            androidx.compose.ui.geometry.Offset(tx, ty))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmokeBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawCapsuleBullet(cx, cy - h * 0.1f, w, h * 0.7f, color)
    // 3 cloud puffs behind
    drawCircle(color.copy(alpha = 0.5f), w * 0.45f,
        androidx.compose.ui.geometry.Offset(cx - w * 0.4f, cy + h * 0.35f))
    drawCircle(color.copy(alpha = 0.4f), w * 0.5f,
        androidx.compose.ui.geometry.Offset(cx, cy + h * 0.5f))
    drawCircle(color.copy(alpha = 0.5f), w * 0.4f,
        androidx.compose.ui.geometry.Offset(cx + w * 0.4f, cy + h * 0.35f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZigzagBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        val step = h / 5f
        moveTo(cx - w / 2, cy - h / 2)
        for (i in 1..5) {
            val x = if (i % 2 == 0) cx - w / 2 else cx + w / 2
            lineTo(x, cy - h / 2 + i * step)
        }
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = w * 0.18f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round,
    ))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeamBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    // Wide horizontal energy beam (Kamehameha style)
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2, cy - h / 2),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2 + w * 0.05f, cy - h / 4),
        size = androidx.compose.ui.geometry.Size(w * 0.9f, h / 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 4),
    )
    // Burst at front (right side)
    drawCircle(color.copy(alpha = 0.8f), h * 0.7f,
        androidx.compose.ui.geometry.Offset(cx + w / 2, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAtomicBullet(
    cx: Float, cy: Float, r: Float, color: Color,
) {
    // Central nucleus
    drawCircle(color, r * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.25f, androidx.compose.ui.geometry.Offset(cx, cy))
    // 3 elliptical orbits (drawn as stroked circles at varied angles)
    for (i in 0 until 3) {
        val angle = i * 60.0
        val rad = angle * Math.PI / 180.0
        val ex = cx + (r * 1.1f * kotlin.math.cos(rad)).toFloat()
        val ey = cy + (r * 1.1f * kotlin.math.sin(rad)).toFloat()
        drawCircle(color.copy(alpha = 0.85f), r * 0.18f,
            androidx.compose.ui.geometry.Offset(ex, ey))
    }
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = r * 1.1f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.06f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSplitBullet(
    cx: Float, cy: Float, w: Float, h: Float, color: Color,
) {
    drawCapsuleBullet(cx, cy + h * 0.15f, w, h * 0.6f, color)
    // 3 branches at top (split lines)
    val branchLen = h * 0.5f
    for (i in -1..1) {
        val angle = i * 35.0
        val rad = angle * Math.PI / 180.0
        val tipX = cx + (branchLen * kotlin.math.sin(rad)).toFloat()
        val tipY = cy - h * 0.15f - (branchLen * kotlin.math.cos(rad)).toFloat()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, cy - h * 0.15f),
            end = androidx.compose.ui.geometry.Offset(tipX, tipY),
            strokeWidth = w * 0.22f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(color, w * 0.15f, androidx.compose.ui.geometry.Offset(tipX, tipY))
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Ship tab — uses drawShipVector preview.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ShipTab(onOpenShipPicker: () -> Unit) {
    // Round 72 → R73 — Apply 3-layer Ship info + CTA mở ShipPicker dialog
    // (Wave 8 ship system đã wire vào EffectiveStats Round 73).
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        // CTA to open ShipPicker dialog
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonCyan.copy(alpha = 0.25f))
                .border(BorderStroke(2.dp, NeonCyan), RoundedCornerShape(12.dp))
                .neonGlow(color = NeonCyan, intensity = 0.5f, radiusFactor = 1.4f)
                .clickable { onOpenShipPicker() }
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = "✦ ĐỔI TÀU — Mở picker chọn loại",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 1.sp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Header
        InfoCard(
            color = NeonGold,
            title = "Phi thuyền — 3 lớp tuỳ chỉnh",
            subtitle = "Loại tàu · Màu sắc · Nâng cấp chỉ số",
            description = "1) Chọn LOẠI TÀU (mở khoá theo khoáng tích luỹ) ảnh hưởng HP/Tốc độ/Sát thương\n" +
                "2) Đổi MÀU SẮC (skin) — chỉ thẩm mỹ, miễn phí\n" +
                "3) NÂNG CẤP CHỈ SỐ vĩnh viễn — tốn khoáng",
            iconDraw = { c -> drawShipPreview(c, NeonGold, laserBoosted = false) },
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ── Layer 1: 5 ShipShape ──
        SectionLabel(label = "1. CHỌN LOẠI TÀU", color = NeonCyan)
        Spacer(modifier = Modifier.height(6.dp))
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.entries.forEach { shape ->
            ShipShapeCard(shape)
            Spacer(modifier = Modifier.height(6.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ── Layer 2: 5 ShipSkin individual cards ──
        // Round 77 (R77c) — Each skin riêng card thay 1 summary card.
        SectionLabel(label = "2. ĐỔI MÀU AURA", color = NeonMagenta)
        Spacer(modifier = Modifier.height(6.dp))
        com.tranphuloi.neon.data.ShipSkin.entries.forEach { skin ->
            val skinColor = Color(skin.glowColorHex)
            InfoCard(
                color = skinColor,
                title = "Aura ${skin.displayName}",
                subtitle = "Skin · Cài đặt → Hào quang tàu",
                description = "Đổi màu aura + glow tàu + tia laser sang ${skin.displayName.lowercase()}. " +
                    "Thẩm mỹ thuần — không ảnh hưởng chỉ số gameplay.",
                iconDraw = { c ->
                    drawCircle(color = skinColor.copy(alpha = 0.35f), radius = c.width * 0.42f,
                        center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
                    drawCircle(color = skinColor, radius = c.width * 0.28f,
                        center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = c.width * 0.13f,
                        center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ── Layer 3: MetaUpgrade — 5 stat tracks individual cards + summary ──
        SectionLabel(label = "3. NÂNG CẤP CHỈ SỐ", color = NeonViolet)
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = NeonViolet,
            title = "17 cây nâng cấp · 5 nền tảng",
            subtitle = "Menu → NÂNG CẤP — tốn khoáng tích luỹ",
            description = "5 cây nền tảng: HP (+10%/cấp) / Sát thương (+8%/cấp) / Tốc độ (+6%/cấp) / " +
                "Nam châm (+15%/cấp) / Khiên (+1.5s/cấp). " +
                "+ 10 cây nhánh + 2 cây tối thượng (legendary). " +
                "Tất cả áp dụng vĩnh viễn cho mọi run.",
            iconDraw = { c ->
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.width / 2, c.height * 0.15f)
                    lineTo(c.width * 0.78f, c.height / 2)
                    lineTo(c.width * 0.6f, c.height / 2)
                    lineTo(c.width * 0.6f, c.height * 0.85f)
                    lineTo(c.width * 0.4f, c.height * 0.85f)
                    lineTo(c.width * 0.4f, c.height / 2)
                    lineTo(c.width * 0.22f, c.height / 2)
                    close()
                }
                drawPath(path, NeonViolet)
            },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = NeonGold,
            title = "Tip — kết hợp 3 lớp",
            subtitle = "ShipShape + ShipSkin + MetaUpgrade",
            description = "Mở khoá BOMBER (1000 khoáng) → ×1.25 HP. Combo với meta_hp rank 5 → ×1.5 HP total. " +
                "TANK (5000 khoáng) max meta + LEGENDARY_HP → 3x HP cap.",
            iconDraw = { c ->
                // 3 stacked rings cho 3-layer concept
                val cx = c.width / 2; val cy = c.height / 2
                drawCircle(NeonCyan, c.width * 0.35f,
                    androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                drawCircle(NeonMagenta, c.width * 0.25f,
                    androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                drawCircle(NeonViolet, c.width * 0.15f,
                    androidx.compose.ui.geometry.Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
                drawCircle(NeonGold, c.width * 0.06f,
                    androidx.compose.ui.geometry.Offset(cx, cy))
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 3.dp, height = 14.dp).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ShipShapeCard(shape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape) {
    val color = when (shape) {
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER -> NeonCyan
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.BOMBER -> NeonGold
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.STEALTH -> NeonViolet
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.TANK -> Color(0xFFFF6020)
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.INTERCEPTOR -> NeonMagenta
    }
    val unlockText = if (shape.unlockMinerals == 0) "Mở khoá: Có sẵn"
        else "Mở khoá: ${shape.unlockMinerals} khoáng tích luỹ"
    InfoCard(
        color = color,
        title = shape.displayName,
        subtitle = unlockText,
        description = "Máu ×${shape.hpMul} · Tốc độ ×${shape.speedMul} · Sát thương ×${shape.damageMul}",
        iconDraw = { c -> drawShipPreview(c, color, laserBoosted = false, shape = shape) },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShipPreview(
    canvasSize: androidx.compose.ui.geometry.Size,
    color: Color,
    laserBoosted: Boolean,
    shape: com.tranphuloi.neon.ui.game.ship.shape.ShipShape =
        com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
) {
    // Round 73 — Delegate to drawShipVector dispatch table cho 5 ShipShape.
    // DrawScope.size = canvasSize tự động vì Canvas đã size 48dp khi gọi iconDraw.
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = canvasSize.width * 0.45f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
    drawShipVector(
        color = color,
        laserBoosterEnabled = laserBoosted,
        shape = shape,
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Enemies tab — uses actual EnemyCanvas shape recipes.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun EnemiesTab() {
    // Round 76 (R76b) — Show all 20 individual enemy variants thay 5 family
    // summary cards. Mỗi variant 1 card với preview thật, stats từ
    // EnemyFamily.fromDrawableId() + chapter pool. + status effects ở cuối.
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(enemyVariantSpecs()) { spec ->
            InfoCard(
                color = spec.color,
                title = spec.title,
                subtitle = spec.subtitle,
                description = spec.description,
                iconDraw = { c -> spec.draw(this, c) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard(
                color = NeonMagenta,
                title = "Trạng thái đặc biệt (3)",
                subtitle = "BURN / SLOW / STUN",
                description = "BURN — 5HP/giây trong 3s (cam). SLOW — di chuyển ×0.5 (cyan). " +
                    "STUN — ngưng bắn 2s (vàng). 10% mỗi hit (5% trên boss). " +
                    "Đạn FIRE luôn apply BURN 100%.",
                iconDraw = { c ->
                    val cy = c.height / 2
                    val r = c.height * 0.13f
                    drawCircle(Color(0xFFFF6020), r, androidx.compose.ui.geometry.Offset(c.width * 0.25f, cy))
                    drawCircle(Color(0xFF00F0FF), r, androidx.compose.ui.geometry.Offset(c.width * 0.50f, cy))
                    drawCircle(Color(0xFFFFD040), r, androidx.compose.ui.geometry.Offset(c.width * 0.75f, cy))
                },
            )
        }
    }
}

/** Round 76 (R76b) — spec data for 20 enemy variants.
 *  draw signature: (DrawScope, Size) → Unit (non-extension để dễ store trong data class). */
private data class EnemyVariantSpec(
    val title: String,
    val subtitle: String,
    val description: String,
    val color: Color,
    val draw: (androidx.compose.ui.graphics.drawscope.DrawScope, androidx.compose.ui.geometry.Size) -> Unit,
)

private fun enemyVariantSpecs(): List<EnemyVariantSpec> {
    // SCOUT (5 light blue darts)
    val scoutColor = Color(0xFF4FD4FF); val scoutAccent = Color(0xFF1799CC)
    // FIGHTER (4 green hexagons)
    val fighterColor = Color(0xFF6EFFAA); val fighterAccent = Color(0xFF24B86E)
    // HEAVY (3 red diamonds)
    val heavyColor = Color(0xFFFF5555); val heavyAccent = Color(0xFFCC1144)
    // ELITE (4 violet cross/orb)
    val eliteColor = Color(0xFFB14CFF); val eliteAccent = Color(0xFF7020CC)
    // BERSERKER (4 orange chevron/spike)
    val berserkerColor = Color(0xFFFF9020); val berserkerAccent = Color(0xFFCC5000)
    return listOf(
        // SCOUT family — Chapter 3 (Hành Tinh Băng). Round 78: variant 0 + 1
        // đổi sang HEART + TRIANGLE để mỗi enemy trong family trông khác nhau.
        EnemyVariantSpec(
            title = "1. SCOUT Mark-I", subtitle = "Chapter 3 · HP 126 · Tốc độ nhanh",
            description = "Hình trái tim cyan — variant chuyên trị melee. Thấy đầu tiên ở Hành Tinh Băng.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyHeart(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "2. SCOUT Mark-II", subtitle = "Chapter 3 · HP 126",
            description = "Tam giác đơn giản — alien glyph với center pip + side dots. Spawn formation Row.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyTriangle(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "3. SCOUT Mark-III ♣", subtitle = "Chapter 3 · HP 126",
            description = "Bài chuồn — cluster 3 hình tròn + thân. Spawn formation ZigZag.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyClub(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "4. SCOUT Mark-IV ●", subtitle = "Chapter 3 · HP 126",
            description = "Hình tròn — đĩa cyan với ring viền + center pip. Spawn formation V.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyCircle(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "5. SCOUT Mark-V ♠", subtitle = "Chapter 3 + Chapter 4 + 5",
            description = "Bài bích — heart ngược + thân stem. Outro variant.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemySpade(c, scoutColor, scoutAccent) }),
        // FIGHTER family — Chapter 2 (Mây Tinh Vân)
        EnemyVariantSpec(
            title = "6. FIGHTER Alpha", subtitle = "Chapter 2 · HP 180",
            description = "Hexagon variant 0 — baseline cân bằng. Stats tham chiếu cho mọi family.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyHexagon(c, fighterColor, fighterAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "7. FIGHTER Bravo", subtitle = "Chapter 2 · HP 180",
            description = "Virus xanh — 8 gai radiating + nhân RNA. Biological/alien aesthetic.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyVirus(c, fighterColor, fighterAccent) }),
        EnemyVariantSpec(
            title = "8. FIGHTER Charlie", subtitle = "Chapter 2 · HP 180",
            description = "Mắt nhìn — oval body + iris + pupil. Cảm giác bị quan sát.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyEye(c, fighterColor, fighterAccent) }),
        EnemyVariantSpec(
            title = "9. FIGHTER Delta ♦", subtitle = "Chapter 2 + 4 + 5 · HP 180",
            description = "Bài rô — kim cương dọc (vertical rhombus) xanh. Outro variant lặp Ch4+5.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyCardDiamond(c, fighterColor, fighterAccent) }),
        // HEAVY family — Chapter 1 (Vành Đai Tiểu Hành Tinh)
        EnemyVariantSpec(
            title = "10. HEAVY Tonk-A", subtitle = "Chapter 1 · HP 288 · Tốc độ chậm",
            description = "Diamond variant 0 — heavy hitter, ×1.3 damage. Bắt đầu Vành Đai.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyDiamond(c, heavyColor, heavyAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "11. HEAVY Tonk-B", subtitle = "Chapter 1 + 4 · HP 288",
            description = "Trái tim đỏ — heart shape đỏ chính danh. Round 78: thay diamond v1.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyHeart(c, heavyColor, heavyAccent) }),
        EnemyVariantSpec(
            title = "12. HEAVY Tonk-C ●", subtitle = "Chapter 1 + 4 + 5 · HP 288",
            description = "Hình tròn đỏ — đĩa đỏ với ring viền dày. Endgame anchor.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyCircle(c, heavyColor, heavyAccent) }),
        // ELITE family — Chapter 4 (Trạm Thù Địch)
        EnemyVariantSpec(
            title = "13. ELITE Cross-α", subtitle = "Chapter 4 · HP 225 · ×1.1 dmg",
            description = "Cross variant 0 — 4 cánh + tip glow nhỏ. Arm spinner spread.",
            color = eliteColor, draw = { sc, c -> sc.drawCrossPreview(c, eliteColor, eliteAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "14. ELITE Cross-β", subtitle = "Chapter 4 · HP 225",
            description = "Cross variant 1 — arm dài hơn + tip glow lớn.",
            color = eliteColor, draw = { sc, c -> sc.drawCrossPreview(c, eliteColor, eliteAccent, variant = 1) }),
        EnemyVariantSpec(
            title = "15. ELITE Orb-α", subtitle = "Chapter 4 · HP 225",
            description = "Orb variant 0 — single orbit ring. Floating attacker.",
            color = eliteColor, draw = { sc, c -> sc.drawOrbVariantPreview(c, eliteColor, eliteAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "16. ELITE Orb-β", subtitle = "Chapter 4 · HP 225",
            description = "Orb variant 1 — double orbit ring. More complex visual.",
            color = eliteColor, draw = { sc, c -> sc.drawOrbVariantPreview(c, eliteColor, eliteAccent, variant = 1) }),
        // BERSERKER family — Chapter 5 (Lõi Thiên Hà)
        EnemyVariantSpec(
            title = "17. BERSERKER Chevron-α", subtitle = "Chapter 5 · HP 162 · ×1.4 dmg",
            description = "Chevron variant 0 — mũi tên đơn. Damage cao nhất game.",
            color = berserkerColor, draw = { sc, c -> sc.drawChevronPreview(c, berserkerColor, berserkerAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "18. BERSERKER Chevron-β", subtitle = "Chapter 5 · HP 162",
            description = "Chevron variant 1 — mũi tên kép xếp chồng.",
            color = berserkerColor, draw = { sc, c -> sc.drawChevronPreview(c, berserkerColor, berserkerAccent, variant = 1) }),
        EnemyVariantSpec(
            title = "19. BERSERKER Spike-α", subtitle = "Chapter 5 · HP 162",
            description = "Spike variant 0 — sao gai 8 cánh, spike ngắn.",
            color = berserkerColor, draw = { sc, c -> sc.drawSpikePreviewV(c, berserkerColor, berserkerAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "20. BERSERKER Spike-β", subtitle = "Chapter 5 · HP 162",
            description = "Spike variant 1 — sao gai 8 cánh, spike dài hơn. Endgame menace.",
            color = berserkerColor, draw = { sc, c -> sc.drawSpikePreviewV(c, berserkerColor, berserkerAccent, variant = 1) }),
    )
}

// Round 74 (R73d) — Wave 9a previews cho ELITE (cross) + BERSERKER (spike).
// Round 76 audit fix — variant param matches in-game drawCross (armW + armLen
// scale theo variant 0..1).
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrossPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int = 0,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val armW = minOf(w, h) * (0.18f + variant * 0.05f)
    val armLen = minOf(w, h) * (0.40f + variant * 0.05f)
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2))
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW))
    drawCircle(body, armW * 0.95f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, armW * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// Round 76 (R76b) — additional previews cho variant indexing in 20-enemy list.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbVariantPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.36f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.22f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(
        color = accent, radius = r * 1.20f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f),
    )
    if (variant == 1) {
        drawCircle(
            color = accent.copy(alpha = 0.6f), radius = r * 1.45f,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.035f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChevronPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.40f
    val drawOne: (Float) -> Unit = { yShift ->
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - halfW, cy - h * 0.20f + yShift)
            lineTo(cx, cy + h * 0.18f + yShift)
            lineTo(cx + halfW, cy - h * 0.20f + yShift)
            lineTo(cx + halfW * 0.7f, cy - h * 0.27f + yShift)
            lineTo(cx, cy + h * 0.08f + yShift)
            lineTo(cx - halfW * 0.7f, cy - h * 0.27f + yShift)
            close()
        }
        drawPath(path, body)
    }
    drawOne(0f)
    if (variant == 1) drawOne(h * 0.28f)
    drawCircle(accent, w * 0.06f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpikePreviewV(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * (0.40f + variant * 0.05f)
    val innerR = outerR * 0.40f
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until 16) {
            val angle = -Math.PI / 2 + i * Math.PI / 8
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    drawCircle(accent, innerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpikePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.45f
    val innerR = outerR * 0.40f
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until 16) {
            val angle = -Math.PI / 2 + i * Math.PI / 8
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    drawCircle(accent, innerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// Round 76 audit fix — variant param thêm vào để 20 cards InfoScreen render
// đúng theo in-game (drawDart/Hexagon/Diamond đã có variant trong EnemyCanvas).
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDart(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int = 0,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.42f; val halfH = h * 0.45f
    // variant 0..4 — back notch depth tăng dần (matches in-game drawDart).
    val notch = h * (0.10f + variant * 0.04f)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy + halfH)                              // tip down
        lineTo(cx + halfW, cy - halfH)                      // top-right
        lineTo(cx, cy - halfH + notch)                      // back notch (chevron)
        lineTo(cx - halfW, cy - halfH)                      // top-left
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.10f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.10f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyHexagon(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int = 0,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val rx = w * 0.45f; val ry = h * 0.50f
    // variant 0..3 — rotation 10° per step (matches in-game drawHexagon).
    val baseAngle = Math.toRadians((variant * 10).toDouble())
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until 6) {
            val a = baseAngle + 2.0 * Math.PI * i / 6.0
            val x = cx + (rx * kotlin.math.cos(a)).toFloat()
            val y = cy + (ry * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.12f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDiamond(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int = 0,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.45f; val halfH = h * 0.50f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f))
    val spikeR = w * 0.10f
    drawCircle(accent, spikeR, androidx.compose.ui.geometry.Offset(cx, cy))
    // Round 77 audit fix — match in-game drawDiamond variant modifiers.
    when (variant) {
        1 -> {
            drawCircle(accent, spikeR * 0.7f, androidx.compose.ui.geometry.Offset(cx - halfW * 0.5f, cy))
            drawCircle(accent, spikeR * 0.7f, androidx.compose.ui.geometry.Offset(cx + halfW * 0.5f, cy))
            drawCircle(accent, spikeR * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy - halfH * 0.6f))
        }
        2 -> {
            val miniR = w * 0.07f
            for (i in 0 until 4) {
                val a = i * 90.0 * Math.PI / 180.0
                val mx = cx + (halfW * 0.75f * kotlin.math.cos(a)).toFloat()
                val my = cy + (halfH * 0.75f * kotlin.math.sin(a)).toFloat()
                val mini = androidx.compose.ui.graphics.Path().apply {
                    moveTo(mx, my - miniR)
                    lineTo(mx + miniR, my)
                    lineTo(mx, my + miniR)
                    lineTo(mx - miniR, my)
                    close()
                }
                drawPath(mini, accent)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Bosses tab — uses BossStar shape.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BossesTab() {
    // Round 76 (R76c) — Per-CHAPTER ENCOUNTER breakdown thay summary. User
    // wants more entries. 5 chapter × (1 mid + 1 final) = 10 boss encounters,
    // mapped to 5 BossKind silhouettes. Mỗi chapter có 1-2 encounter card.
    val red = Color(0xFFFF5555); val redAcc = Color(0xFFCC1144)
    val green = Color(0xFF6EFFAA); val greenAcc = Color(0xFF24B86E)
    val gold = NeonGold; val goldAcc = Color(0xFFCC9900)
    val violet = NeonViolet; val violetAcc = Color(0xFF8855CC)
    val magenta = NeonMagenta
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        // Ch 1 — Vành Đai Tiểu Hành Tinh
        InfoCard(
            color = gold,
            title = "Ch1 Mid — Killer Eye Sentinel (OFFENSIVE)",
            subtitle = "Giữa Vành Đai Tiểu Hành Tinh · HP 1200 · EYE",
            description = "Mắt sát thủ khổng lồ — sclera + iris + pupil + 6 mi mắt eldritch. Spray 360° spread + radial barrage. " +
                "Phase 2 (HP<50%) tăng fire rate. Defeat reward: buff picker post-kill.",
            iconDraw = { c -> drawBossEyePreview(c, gold, goldAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = red,
            title = "Ch1 End — Crimson Sun",
            subtitle = "End Vành Đai · HP 3000 · SUN (8-laser ring barrage)",
            description = "Mặt trời đỏ rực — corona ring + 12 radial flares + hot disc center. Pattern Round 74 wired: " +
                "8-laser ring radial mỗi tick. Audio cue 1.4× pitch (sting cao chói tai). HP bar full-width.",
            iconDraw = { c -> drawBossStar(c, red, redAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 2 — Mây Tinh Vân
        InfoCard(
            color = violet,
            title = "Ch2 Mid — Atom Sentinel (DEFENSIVE)",
            subtitle = "Giữa Mây Tinh Vân · HP 1200 · ATOM",
            description = "Mô hình nguyên tử — 3 quỹ đạo điện tử quay quanh hạt nhân tím. " +
                "Orbit + counter pattern. Phase 2 phá quỹ đạo thành 3 sub-pattern.",
            iconDraw = { c -> drawBossFractalPreview(c, violet, violetAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = green,
            title = "Ch2 End — Emerald Cross",
            subtitle = "End Mây Tinh Vân · HP 4000 · CROSS",
            description = "Cross spinner 4 cánh xanh + 4 tip glow. Quay liên tục. " +
                "Pattern Round 74 wired: alternating axis sweep (vertical wall ↔ 4-diagonal). Audio 1.15× pitch.",
            iconDraw = { c -> drawBossCrossPreview(c, green, greenAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 3 — Hành Tinh Băng
        InfoCard(
            color = violet,
            title = "Ch3 Mid — Atom Swarm (SWARM)",
            subtitle = "Giữa Hành Tinh Băng · HP 1200 · ATOM",
            description = "Atom variant SWARM — spawn 4 drone con khi Phase 2. " +
                "Phải clear drone trước khi đánh boss chính.",
            iconDraw = { c -> drawBossFractalPreview(c, violet, violetAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = red,
            title = "Ch3 End — Crimson Sun (reuse)",
            subtitle = "End Hành Tinh Băng · HP 3000 · SUN · cùng visual với Ch1",
            description = "LevelOneBoss reuse từ Ch1 với cùng red palette in-game. Khác biệt nằm ở arena " +
                "ICE_PATCHES — tàu trượt sau khi release movement → dodge ring barrage khó hơn. " +
                "Audio cue cùng STAR pitch 1.4×.",
            iconDraw = { c -> drawBossStar(c, red, redAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 4 — Trạm Thù Địch
        InfoCard(
            color = gold,
            title = "Ch4 Mid — Killer Eye Veteran (OFFENSIVE-2)",
            subtitle = "Giữa Trạm Thù Địch · HP 1200 · EYE",
            description = "Killer Eye pattern. ELITE enemies (cross/orb từ Wave 9a R74) bay xung quanh hỗ trợ. " +
                "Damage tổng hợp cao hơn các chapter trước.",
            iconDraw = { c -> drawBossEyePreview(c, gold, goldAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = green,
            title = "Ch4 End — Emerald Cross (reuse)",
            subtitle = "End Trạm Thù Địch · HP 4000 · CROSS · cùng visual với Ch2",
            description = "LevelTwoBoss reuse từ Ch2 cùng green palette. Khác biệt nằm ở ELITE enemy wave " +
                "đồng hành (cross/orb violet) bay xung quanh boss → tổng pressure cao hơn.",
            iconDraw = { c -> drawBossCrossPreview(c, green, greenAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 5 — Lõi Thiên Hà
        InfoCard(
            color = magenta,
            title = "Ch5 Final — Galaxy Overlord (SPIDER)",
            subtitle = "End Lõi Thiên Hà · HP 22500 · 3-phase · SPIDER",
            description = "Boss cuối game. 8 chân + thân + 2 mắt sáng. " +
                "Phase 1 (HP>15000): fire pattern thông thường. " +
                "Phase 2 (HP≤15000): tăng tốc độ + spawn BERSERKER wave. " +
                "Phase 3 (HP≤7500): ring barrage 360° + tracking lasers. " +
                "Audio cue 0.65× pitch (sting trầm sâu). Victory ending khác theo difficulty.",
            iconDraw = { c -> drawBossSpiderPreview(c, magenta, redAcc) },
        )
    }
}

// Round 78 (#1 shape diversity) — preview helpers mirror các shape recipes
// mới trong EnemyCanvas. Bao gồm: heart/triangle/eye/virus + 4 card suits
// (cardClub/cardDiamond/cardSpade) + circle + bossEye/bossSun/bossAtom.

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyCircle(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.42f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    drawCircle(accent, r * 0.40f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.18f,
        androidx.compose.ui.geometry.Offset(cx - r * 0.15f, cy - r * 0.15f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyClub(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val lobeR = minOf(w, h) * 0.20f
    val offsetD = lobeR * 0.85f
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx, cy - offsetD))
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx + offsetD, cy + offsetD * 0.55f))
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy + offsetD * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.25f))
    val baseTri = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - w * 0.15f, cy + h * 0.45f)
        lineTo(cx + w * 0.15f, cy + h * 0.45f)
        lineTo(cx, cy + offsetD * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    drawCircle(accent, lobeR * 0.40f, androidx.compose.ui.geometry.Offset(cx, cy - offsetD))
    drawCircle(accent, lobeR * 0.40f, androidx.compose.ui.geometry.Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(accent, lobeR * 0.40f, androidx.compose.ui.geometry.Offset(cx + offsetD, cy + offsetD * 0.55f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemySpade(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.40f; val halfH = h * 0.40f
    val lobeR = halfW * 0.55f
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx - halfW * 0.45f, cy + halfH * 0.20f))
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx + halfW * 0.45f, cy + halfH * 0.20f))
    val triPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - halfW, cy + halfH * 0.30f)
        lineTo(cx + halfW, cy + halfH * 0.30f)
        lineTo(cx, cy - halfH)
        close()
    }
    drawPath(triPath, body)
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy + halfH * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.20f))
    val baseTri = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx + w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx, cy + halfH * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    drawCircle(accent, w * 0.06f, androidx.compose.ui.geometry.Offset(cx, cy + halfH * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyCardDiamond(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.32f; val halfH = h * 0.48f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f))
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.08f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyHeart(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.45f; val halfH = h * 0.45f
    val lobeR = halfW * 0.55f
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(body, lobeR, androidx.compose.ui.geometry.Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    val triPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - halfW, cy - halfH * 0.18f)
        lineTo(cx + halfW, cy - halfH * 0.18f)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(triPath, body)
    drawCircle(accent, lobeR * 0.35f, androidx.compose.ui.geometry.Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, lobeR * 0.35f, androidx.compose.ui.geometry.Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.07f, androidx.compose.ui.geometry.Offset(cx, cy + halfH * 0.5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyTriangle(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.45f; val halfH = h * 0.48f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.08f, androidx.compose.ui.geometry.Offset(cx, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.05f, androidx.compose.ui.geometry.Offset(cx - halfW * 0.30f, cy + halfH * 0.20f))
    drawCircle(accent, w * 0.05f, androidx.compose.ui.geometry.Offset(cx + halfW * 0.30f, cy + halfH * 0.20f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyEye(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val rx = w * 0.48f; val ry = h * 0.30f
    drawOval(
        color = body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
    )
    drawOval(
        color = accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f),
    )
    drawCircle(Color.White.copy(alpha = 0.85f), ry * 0.85f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, ry * 0.50f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White, ry * 0.18f, androidx.compose.ui.geometry.Offset(cx + ry * 0.25f, cy - ry * 0.25f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyVirus(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val coreR = minOf(w, h) * 0.30f
    val spineLen = minOf(w, h) * 0.22f
    val tipR = minOf(w, h) * 0.07f
    val spines = 8
    for (i in 0 until spines) {
        val a = i * 2.0 * Math.PI / spines
        val ex = cx + ((coreR + spineLen) * kotlin.math.cos(a)).toFloat()
        val ey = cy + ((coreR + spineLen) * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = body,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = w * 0.07f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawCircle(accent, tipR, androidx.compose.ui.geometry.Offset(ex, ey))
    }
    drawCircle(body, coreR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, coreR * 0.50f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx - coreR * 0.30f, cy - coreR * 0.15f))
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.15f,
        androidx.compose.ui.geometry.Offset(cx + coreR * 0.25f, cy + coreR * 0.20f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossEyePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.45f
    val midR = outerR * 0.75f
    val innerR = outerR * 0.45f
    val pupilR = outerR * 0.22f
    drawCircle(body, outerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, outerR, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    drawCircle(Color.White.copy(alpha = 0.92f), midR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(body, innerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, innerR, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.03f))
    drawCircle(accent, pupilR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.Black.copy(alpha = 0.85f), pupilR * 0.85f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White, pupilR * 0.35f,
        androidx.compose.ui.geometry.Offset(cx + pupilR * 0.40f, cy - pupilR * 0.45f))
    for (i in 0 until 6) {
        val a = i * 60.0 * Math.PI / 180.0
        val sx = cx + ((outerR + outerR * 0.08f) * kotlin.math.cos(a)).toFloat()
        val sy = cy + ((outerR + outerR * 0.08f) * kotlin.math.sin(a)).toFloat()
        val ex = cx + ((outerR + outerR * 0.20f) * kotlin.math.cos(a)).toFloat()
        val ey = cy + ((outerR + outerR * 0.20f) * kotlin.math.sin(a)).toFloat()
        drawLine(accent,
            androidx.compose.ui.geometry.Offset(sx, sy),
            androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = w * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

// Round 72 (Issue 3) — Preview helpers cho 4 boss kinds mới trong InfoScreen.
// Mirror các shape recipes trong EnemyCanvas, scaled cho 48dp icon.

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossCrossPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val armW = minOf(w, h) * 0.18f
    val armLen = minOf(w, h) * 0.5f
    val centerR = minOf(w, h) * 0.25f
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2))
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW))
    drawCircle(body, centerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, centerR * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy - armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy + armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx - armLen, cy))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx + armLen, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossOrbPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.35f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    for (i in 0 until 3) {
        val a = i * 120.0 * Math.PI / 180.0
        val sx = cx + (r * 1.15f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (r * 1.15f * kotlin.math.sin(a)).toFloat()
        drawCircle(accent, r * 0.18f, androidx.compose.ui.geometry.Offset(sx, sy))
    }
}

// Round 78 (#2 spec follow-up) — drawBossFractalPreview now renders as ATOM
// (electron orbits + nucleus) to match in-game BossKind.FRACTAL → drawBossAtom
// dispatch in EnemyCanvas. Function name kept stable so existing call-sites work.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossFractalPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val nucR = minOf(w, h) * 0.16f
    val orbitRx = minOf(w, h) * 0.42f
    val orbitRy = minOf(w, h) * 0.16f
    for (i in 0 until 3) {
        val deg = (i * 60).toFloat()
        withTransform(
            transformBlock = { rotate(degrees = deg, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) },
            drawBlock = {
                drawOval(
                    color = accent,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - orbitRx, cy - orbitRy),
                    size = androidx.compose.ui.geometry.Size(orbitRx * 2, orbitRy * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f),
                )
                drawCircle(body, w * 0.05f, androidx.compose.ui.geometry.Offset(cx + orbitRx, cy))
            },
        )
    }
    drawCircle(body, nucR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, nucR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), nucR * 0.30f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossSpiderPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyR = minOf(w, h) * 0.25f
    val legLen = minOf(w, h) * 0.5f
    val legW = bodyR * 0.18f
    for (i in 0 until 8) {
        val a = i * 45.0 * Math.PI / 180.0
        val ex = cx + (legLen * kotlin.math.cos(a)).toFloat()
        val ey = cy + (legLen * kotlin.math.sin(a)).toFloat()
        drawLine(color = body,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = legW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(accent, legW * 0.6f, androidx.compose.ui.geometry.Offset(ex, ey))
    }
    drawCircle(body, bodyR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx - bodyR * 0.4f, cy - bodyR * 0.1f))
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx + bodyR * 0.4f, cy - bodyR * 0.1f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMidBoss(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.40f
    // 5-pointed star
    val path = androidx.compose.ui.graphics.Path().apply {
        val step = Math.PI / 5.0
        for (i in 0 until 10) {
            val a = -Math.PI / 2 + i * step
            val rr = if (i % 2 == 0) r else r * 0.5f
            val x = cx + (rr * kotlin.math.cos(a)).toFloat()
            val y = cy + (rr * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
}

// Round 78 (#2 spec follow-up) — drawBossStar now renders as SUN to match in-game
// (BossKind.STAR dispatches to drawBossSun in EnemyCanvas). Preview keeps the
// same function name so existing entries don't need to change.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossStar(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val coreR = minOf(w, h) * 0.28f
    val coronaR = minOf(w, h) * 0.40f
    val flareInner = coronaR * 1.05f
    val flareOuter = coronaR * 1.35f
    for (i in 0 until 12) {
        val a = i * 30.0 * Math.PI / 180.0
        val outerR = if (i % 2 == 0) flareOuter else flareInner * 1.10f
        val sx = cx + (flareInner * kotlin.math.cos(a)).toFloat()
        val sy = cy + (flareInner * kotlin.math.sin(a)).toFloat()
        val ex = cx + (outerR * kotlin.math.cos(a)).toFloat()
        val ey = cy + (outerR * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = accent,
            start = androidx.compose.ui.geometry.Offset(sx, sy),
            end = androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = w * (if (i % 2 == 0) 0.06f else 0.04f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
    drawCircle(accent.copy(alpha = 0.55f), coronaR, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f))
    drawCircle(body, coreR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), coreR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Items tab — uses real BoosterCanvas shape recipes.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ItemsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(BoosterType.entries) { booster ->
            // Round 71 (Issue 4e) — BoosterCard với friendly title + multi-line
            // description + tip + duration badges (4 enrichments).
            BoosterCard(
                color = boosterColor(booster),
                title = boosterTitle(booster),
                rarity = "Tỉ lệ rơi: ${"%.1f".format(booster.weight * 100f / 238f)}%",
                description = boosterDescription(booster),
                tip = boosterTip(booster),
                duration = boosterDuration(booster),
                iconDraw = { c -> drawBoosterPreview(c, booster) },
            )
        }
    }
}

private fun boosterColor(b: BoosterType): Color = when (b.drawableId) {
    R.drawable.booster_health -> Color(0xFFA8FF60)
    R.drawable.booster_shield -> Color(0xFF00F0FF)
    R.drawable.booster_red_lasers -> Color(0xFFFF5555)
    R.drawable.booster_triple_laser -> Color(0xFFFFA040)
    R.drawable.booster_ultimate_weapon -> Color(0xFFFFD040)
    R.drawable.booster_revive -> Color(0xFF60FFAA)
    else -> NeonViolet
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoosterPreview(
    canvasSize: androidx.compose.ui.geometry.Size,
    b: BoosterType,
) {
    val w = canvasSize.width
    val h = canvasSize.height
    val cx = w / 2
    val cy = h / 2
    val size = minOf(w, h) * 0.7f
    // Tint color from booster
    val color = boosterColor(b)
    // Glow
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = size * 0.7f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
    // Shape per drawableId — same recipes as BoosterCanvas.kt (Round 66b).
    when (b.drawableId) {
        R.drawable.booster_health -> {
            val armWide = size * 0.30f; val armLong = size * 0.85f
            drawRoundRect(color, androidx.compose.ui.geometry.Offset(cx - armLong / 2, cy - armWide / 2),
                androidx.compose.ui.geometry.Size(armLong, armWide),
                androidx.compose.ui.geometry.CornerRadius(armWide / 2))
            drawRoundRect(color, androidx.compose.ui.geometry.Offset(cx - armWide / 2, cy - armLong / 2),
                androidx.compose.ui.geometry.Size(armWide, armLong),
                androidx.compose.ui.geometry.CornerRadius(armWide / 2))
        }
        R.drawable.booster_shield -> {
            val r = size * 0.42f
            val path = androidx.compose.ui.graphics.Path().apply {
                val start = -Math.PI / 8
                for (i in 0 until 8) {
                    val a = start + i * Math.PI / 4
                    val x = cx + (r * kotlin.math.cos(a)).toFloat()
                    val y = cy + (r * kotlin.math.sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }
        R.drawable.booster_red_lasers -> {
            val halfW = size * 0.40f; val halfH = size * 0.42f
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - halfH)
                lineTo(cx + halfW, cy + halfH)
                lineTo(cx - halfW, cy + halfH)
                close()
            }
            drawPath(path, color)
        }
        R.drawable.booster_triple_laser -> {
            val bw = size * 0.18f; val bh = size * 0.80f
            val gap = size * 0.12f
            listOf(cx - bw - gap, cx, cx + bw + gap).forEach { x ->
                drawRoundRect(color,
                    androidx.compose.ui.geometry.Offset(x - bw / 2, cy - bh / 2),
                    androidx.compose.ui.geometry.Size(bw, bh),
                    androidx.compose.ui.geometry.CornerRadius(bw / 2))
            }
        }
        R.drawable.booster_ultimate_weapon -> {
            // 5-point star
            val outerR = size * 0.45f
            val innerR = outerR * 0.45f
            val path = androidx.compose.ui.graphics.Path().apply {
                val step = Math.PI / 5
                for (i in 0 until 10) {
                    val a = -Math.PI / 2 + i * step
                    val rr = if (i % 2 == 0) outerR else innerR
                    val x = cx + (rr * kotlin.math.cos(a)).toFloat()
                    val y = cy + (rr * kotlin.math.sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color)
        }
        R.drawable.booster_revive -> {
            // Heart: 2 circles + triangle
            val lobeR = size * 0.20f
            val lobeY = cy - size * 0.10f
            drawCircle(color, lobeR, androidx.compose.ui.geometry.Offset(cx - lobeR * 0.85f, lobeY))
            drawCircle(color, lobeR, androidx.compose.ui.geometry.Offset(cx + lobeR * 0.85f, lobeY))
            val tri = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - lobeR * 1.85f, lobeY)
                lineTo(cx, cy + size * 0.42f)
                lineTo(cx + lobeR * 1.85f, lobeY)
                close()
            }
            drawPath(tri, color)
        }
    }
}

// Round 71 (Issue 4e) — Friendly Vietnamese title cho người chơi thường.
private fun boosterTitle(b: BoosterType): String = when (b) {
    BoosterType.HEALTH_BOOSTER -> "Hồi máu"
    BoosterType.SHIELD_BOOSTER -> "Khiên chắn"
    BoosterType.LASER_BOOSTER -> "Đạn mạnh"
    BoosterType.TRIPLE_LASER_BOOSTER -> "Đạn ba"
    BoosterType.ULTIMATE_WEAPON_BOOSTER -> "Vũ khí tối thượng"
    BoosterType.REVIVE_TOKEN -> "Hồi sinh"
    BoosterType.PIERCING_BOOSTER -> "Đạn xuyên"
    BoosterType.PLASMA_BOOSTER -> "Đạn plasma"
    BoosterType.MAGNET_BOOST -> "Hút khoáng"
    BoosterType.CRIT_SURGE -> "Chí mạng"
    BoosterType.SPREAD_SHOT -> "Bắn rộng"
    BoosterType.BERSERK -> "Cuồng nộ"
    BoosterType.PHASE_SHIELD -> "Khiên ảo"
    BoosterType.SCORE_X3 -> "Điểm ×3"
    BoosterType.QUICK_HEAL -> "Hồi máu nhanh"
    BoosterType.MINERAL_SUPERCHARGE -> "Thu khoáng nhanh"
    BoosterType.HEALING_AURA -> "Hồi máu liên tục"
    BoosterType.DOUBLE_FIRE -> "Bắn đôi"
    BoosterType.FIRE_BOOSTER -> "Đạn lửa"
    BoosterType.HOMING_BOOSTER -> "Đạn đuổi"
    BoosterType.BOUNCE_BOOSTER -> "Đạn nảy"
    BoosterType.GIANT_BOOSTER -> "Đạn khổng lồ"
    BoosterType.SMOKE_BOOSTER -> "Đạn khói"
    BoosterType.ZIGZAG_BOOSTER -> "Đạn zigzag"
    BoosterType.KAMEHAMEHA_BOOSTER -> "Đạn năng lượng"
    BoosterType.ATOMIC_BOOSTER -> "Đạn nguyên tử"
    BoosterType.SPLIT_BOOSTER -> "Đạn phân tách"
}

// Round 71 (Issue 4e) — Multi-line WHAT it does, từ ngữ thân thiện thay
// "×rarity (1.0/1.5/2.0)" bằng "Thường/Hiếm/Sử Thi".
private fun boosterDescription(b: BoosterType): String = when (b) {
    BoosterType.HEALTH_BOOSTER ->
        "Hồi 100 máu (Thường) / 150 máu (Hiếm) / 200 máu (Sử Thi).\nDùng ngay khi nhặt."
    BoosterType.SHIELD_BOOSTER ->
        "Tạo khiên chống mọi sát thương.\nThường 10 giây, Hiếm 15, Sử Thi 20."
    BoosterType.LASER_BOOSTER ->
        "Nâng cấp đạn thường thành đạn mạnh hơn.\nThường 15 giây, Hiếm 22, Sử Thi 30."
    BoosterType.TRIPLE_LASER_BOOSTER ->
        "Bắn 3 đường đạn cùng lúc thay vì 1.\nThường 20 giây, Hiếm 30, Sử Thi 40."
    BoosterType.ULTIMATE_WEAPON_BOOSTER ->
        "Quét 9 tia laser ngang màn hình một lần duy nhất.\nGiết gần hết enemy đang có."
    BoosterType.REVIVE_TOKEN ->
        "Khi máu về 0, tự hồi sinh với 300 máu + 1.5 giây bất tử.\nDùng 1 lần mỗi run."
    BoosterType.PIERCING_BOOSTER ->
        "Đạn xuyên qua nhiều enemy thay vì biến mất khi va chạm.\nXuyên 3/4/5 enemy theo cấp Thường/Hiếm/Sử Thi."
    BoosterType.PLASMA_BOOSTER ->
        "Đạn nổ AoE khi trúng enemy, gây sát thương quanh điểm chạm.\nBán kính 80/110/140 px theo cấp."
    BoosterType.MAGNET_BOOST ->
        "Tăng gấp đôi bán kính hút khoáng sản.\nNhặt nhanh hơn trong 15 giây."
    BoosterType.CRIT_SURGE ->
        "Sát thương ×3 trong 8 giây.\nDồn dập diệt enemy / boss nhanh."
    BoosterType.SPREAD_SHOT ->
        "Bắn 5 đường đạn hình quạt trong 10 giây.\nCover rộng diệt nhiều enemy 1 lúc."
    BoosterType.BERSERK ->
        "Sát thương ×2 nhưng nhận sát thương ×1.5 trong 12 giây.\nLiều mạng — chỉ dùng khi máu đầy."
    BoosterType.PHASE_SHIELD ->
        "Bất tử 5 giây + hiệu ứng ma quái quanh tàu.\nLao qua đạn enemy không sao."
    BoosterType.SCORE_X3 ->
        "Mọi khoáng sản nhận được nhân 3 trong 15 giây.\nFarm tiền mua nâng cấp."
    BoosterType.QUICK_HEAL ->
        "Hồi ngay 250 máu (Thường) / 375 (Hiếm) / 500 (Sử Thi).\nDùng khi máu thấp."
    BoosterType.MINERAL_SUPERCHARGE ->
        "Hút ngay tất cả khoáng sản trên màn hình + thưởng +5 mỗi mảnh.\nKhông giới hạn bán kính."
    BoosterType.HEALING_AURA ->
        "Tự động hồi +5 máu mỗi giây trong 10 giây.\nSống sót lâu trong combat dài."
    BoosterType.DOUBLE_FIRE ->
        "Bắn 2 phát đạn xếp chồng mỗi lần thay vì 1.\nGấp đôi tốc độ sát thương trong 10 giây."
    BoosterType.FIRE_BOOSTER ->
        "Đạn gây cháy enemy, mất 5 máu/giây trong 3 giây sau khi trúng.\nKéo dài 10 giây."
    BoosterType.HOMING_BOOSTER ->
        "Đạn tự đuổi theo enemy gần nhất.\nNhắm mắt cũng trúng. Kéo dài 10 giây."
    BoosterType.BOUNCE_BOOSTER ->
        "Đạn nảy lại khi va vào cạnh màn hình, trúng tối đa 3 enemy/viên.\nKéo dài 12 giây."
    BoosterType.GIANT_BOOSTER ->
        "Đạn to gấp đôi + sát thương ×2.\nKéo dài 10 giây."
    // Round 73 audit fix — bỏ "(sắp ra mắt)" defer-notes leak. Description
    // describes current behavior real (damage mul + duration) WITHOUT promise
    // tương lai. Behavior unique (AoE/zigzag/etc) sẽ silently upgrade khi ship.
    BoosterType.SMOKE_BOOSTER ->
        "Đạn khói với hiệu ứng AoE.\nSát thương 80% gốc, kéo dài 10 giây."
    BoosterType.ZIGZAG_BOOSTER ->
        "Đạn bay theo đường zigzag né dodge.\nSát thương 90% gốc, kéo dài 12 giây."
    BoosterType.KAMEHAMEHA_BOOSTER ->
        "Tia năng lượng cực mạnh xuyên thấu.\nSát thương ×3, kéo dài 8 giây."
    BoosterType.ATOMIC_BOOSTER ->
        "Đạn nguyên tử nổ tầm rộng.\nSát thương ×1.5, kéo dài 10 giây."
    BoosterType.SPLIT_BOOSTER ->
        "Đạn phân tách thành nhiều mảnh.\nSát thương 60% gốc, kéo dài 12 giây."
}

// Round 71 (Issue 4e) — "Khi nào nên nhặt" — gameplay tip 1-line.
private fun boosterTip(b: BoosterType): String = when (b) {
    BoosterType.HEALTH_BOOSTER -> "Nhặt khi máu dưới 50%."
    BoosterType.SHIELD_BOOSTER -> "Tốt nhất khi gặp boss hoặc combat dày đặc."
    BoosterType.LASER_BOOSTER -> "Nhặt bất kỳ lúc nào — tăng damage cơ bản."
    BoosterType.TRIPLE_LASER_BOOSTER -> "Tuyệt vời để dọn nhanh đám enemy."
    BoosterType.ULTIMATE_WEAPON_BOOSTER -> "Dùng khi màn hình đầy enemy."
    BoosterType.REVIVE_TOKEN -> "Hiếm — luôn nhặt khi thấy."
    BoosterType.PIERCING_BOOSTER -> "Hiệu quả nhất khi enemy xếp hàng dọc."
    BoosterType.PLASMA_BOOSTER -> "Tốt khi enemy bay theo cụm."
    BoosterType.MAGNET_BOOST -> "Nhặt sau khi giết boss có nhiều khoáng."
    BoosterType.CRIT_SURGE -> "Dùng ngay trước khi boss xuất hiện."
    BoosterType.SPREAD_SHOT -> "Hợp cho stage có enemy bay sideways."
    BoosterType.BERSERK -> "Chỉ nhặt khi máu trên 70%."
    BoosterType.PHASE_SHIELD -> "Dùng để vượt qua đợt enemy laser."
    BoosterType.SCORE_X3 -> "Nhặt khi đang farm — combo với MAGNET."
    BoosterType.QUICK_HEAL -> "Cấp cứu khi máu cực thấp."
    BoosterType.MINERAL_SUPERCHARGE -> "Cuối stage để dọn sạch khoáng."
    BoosterType.HEALING_AURA -> "Tốt cho boss fight dài."
    BoosterType.DOUBLE_FIRE -> "Combo với LASER_BOOSTER để DPS tối đa."
    BoosterType.FIRE_BOOSTER -> "Hiệu quả với enemy nhiều máu."
    BoosterType.HOMING_BOOSTER -> "Hợp cho người mới — không cần aim."
    BoosterType.BOUNCE_BOOSTER -> "Tốt khi enemy bay sát mép màn hình."
    BoosterType.GIANT_BOOSTER -> "Combo boss — damage ×2 đáng giá."
    // Round 73 audit fix — bỏ "(Sắp ra)" leak. Tips describe gameplay context only.
    BoosterType.SMOKE_BOOSTER -> "Hợp cho stage có enemy bay theo cụm dày đặc."
    BoosterType.ZIGZAG_BOOSTER -> "Khó né hơn cho enemy — hợp boss fight dài."
    BoosterType.KAMEHAMEHA_BOOSTER -> "Damage ×3 cực mạnh — luôn nhặt khi thấy."
    BoosterType.ATOMIC_BOOSTER -> "Damage ×1.5 + AoE — hợp khi enemy cụm."
    BoosterType.SPLIT_BOOSTER -> "Damage thấp nhưng phủ rộng — lo dọn enemy yếu."
}

// Round 71 (Issue 4e) — Duration / stack rule badge text.
private fun boosterDuration(b: BoosterType): String = when (b) {
    BoosterType.HEALTH_BOOSTER, BoosterType.ULTIMATE_WEAPON_BOOSTER,
    BoosterType.QUICK_HEAL, BoosterType.MINERAL_SUPERCHARGE -> "Tức thời · Không stack"
    BoosterType.REVIVE_TOKEN -> "Vĩnh viễn · 1 lần/run"
    BoosterType.SHIELD_BOOSTER -> "⏱ 10/15/20s · Không stack"
    BoosterType.LASER_BOOSTER -> "⏱ 15/22/30s · Không stack"
    BoosterType.TRIPLE_LASER_BOOSTER -> "⏱ 20/30/40s · Không stack"
    BoosterType.PIERCING_BOOSTER, BoosterType.PLASMA_BOOSTER -> "⏱ 10s · Stack reset"
    BoosterType.MAGNET_BOOST -> "⏱ 15s · Refresh"
    BoosterType.CRIT_SURGE -> "⏱ 8s · Refresh"
    BoosterType.SPREAD_SHOT -> "⏱ 10s · Refresh"
    BoosterType.BERSERK -> "⏱ 12s · Refresh"
    BoosterType.PHASE_SHIELD -> "⏱ 5s · Refresh"
    BoosterType.SCORE_X3 -> "⏱ 15s · Refresh"
    BoosterType.HEALING_AURA -> "⏱ 10s · Refresh"
    BoosterType.DOUBLE_FIRE -> "⏱ 10s · Refresh"
    BoosterType.FIRE_BOOSTER, BoosterType.HOMING_BOOSTER, BoosterType.GIANT_BOOSTER,
    BoosterType.SMOKE_BOOSTER, BoosterType.ATOMIC_BOOSTER -> "⏱ 10s · Stack reset"
    BoosterType.BOUNCE_BOOSTER, BoosterType.ZIGZAG_BOOSTER, BoosterType.SPLIT_BOOSTER -> "⏱ 12s · Stack reset"
    BoosterType.KAMEHAMEHA_BOOSTER -> "⏱ 8s · Stack reset"
}

// ─────────────────────────────────────────────────────────────────────────
// Shared card
// ─────────────────────────────────────────────────────────────────────────

/**
 * Round 71 (Issue 4e) — Specialized booster card với 4 enrichments:
 *   1. Friendly title (Vietnamese, không enum name).
 *   2. Multi-line description.
 *   3. "Khi nào nên nhặt" tip ở dòng riêng với icon hint 💡 → ✦.
 *   4. Duration / stack rule badge dưới cùng.
 */
@Composable
private fun BoosterCard(
    color: Color,
    title: String,
    rarity: String,
    description: String,
    tip: String,
    duration: String,
    iconDraw: androidx.compose.ui.graphics.drawscope.DrawScope.(
        canvasSize: androidx.compose.ui.geometry.Size,
    ) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(BorderStroke(1.5.dp, color), RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(48.dp)) { iconDraw(size) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = rarity, color = color.copy(alpha = 0.65f), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✦ $tip",
                    color = color.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = duration,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    color: Color,
    title: String,
    subtitle: String,
    description: String,
    iconDraw: androidx.compose.ui.graphics.drawscope.DrawScope.(
        canvasSize: androidx.compose.ui.geometry.Size,
    ) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Round 67.6 — Canvas-based icon preview using real game shape recipes.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(BorderStroke(1.5.dp, color), RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    iconDraw(size)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
    }
}
