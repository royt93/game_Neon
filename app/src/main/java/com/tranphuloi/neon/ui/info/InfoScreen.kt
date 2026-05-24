package com.tranphuloi.neon.ui.info

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
fun InfoScreen(onBack: () -> Unit) {
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
                        InfoTab.SHIP -> ShipTab()
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
    BulletType.SMOKE -> "(Stub Round 68) Để lại vệt khói AoE 60px gây sát thương cộng dồn. Hành vi đầy đủ: Round 69+."
    BulletType.ZIGZAG -> "(Stub Round 68) Đạn bay zigzag né dodge. Hành vi đầy đủ: Round 69+."
    BulletType.KAMEHAMEHA -> "(Stub Round 68) Tia laser khủng xuyên thấu vô hạn, damage ×3. Hành vi đầy đủ: Round 69+."
    BulletType.ATOMIC -> "(Stub Round 68) Đạn nguyên tử với AoE explosion 150px khổng lồ. Hành vi đầy đủ: Round 69+."
    BulletType.SPLIT -> "(Stub Round 68) Đạn phân tách thành 3 mảnh nhỏ khi va chạm. Hành vi đầy đủ: Round 69+."
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
private fun ShipTab() {
    // Round 72 (Issue 3 user audit) — Apply 3-layer Ship info đúng Round 69 pick.
    // Layer 1: 5 ShipShape (Wave 8 enum từ R68) + stat profile + unlock minerals.
    // Layer 2: 5 ShipSkin color customization.
    // Layer 3: MetaUpgrade stats summary (link MetaUpgradeScreen).
    // ShipPickerScreen UI + EffectiveStats wiring: defer R73 (user pick R72).
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
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

        // ── Layer 2: 5 ShipSkin ──
        SectionLabel(label = "2. ĐỔI MÀU AURA", color = NeonMagenta)
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = NeonMagenta,
            title = "5 màu Aura tàu",
            subtitle = "Cài đặt → Hào quang tàu",
            description = "Cyan / Vàng / Magenta / Tím / Đỏ. Đổi màu aura + glow của tàu + tia laser.\n" +
                "Tiện thẩm mỹ, không ảnh hưởng chỉ số.",
            iconDraw = { c ->
                val colors = listOf(Color(0xFF00F0FF), Color(0xFFFFCB47), Color(0xFFFF2DE0),
                    Color(0xFFB14CFF), Color(0xFFFF2D55))
                val r = c.width * 0.09f
                val gap = c.width * 0.05f
                val totalW = colors.size * (2 * r) + (colors.size - 1) * gap
                val startX = (c.width - totalW) / 2 + r
                colors.forEachIndexed { i, col ->
                    drawCircle(color = col, radius = r,
                        center = androidx.compose.ui.geometry.Offset(
                            startX + i * (2 * r + gap), c.height / 2))
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── Layer 3: MetaUpgrade ──
        SectionLabel(label = "3. NÂNG CẤP CHỈ SỐ", color = NeonViolet)
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = NeonViolet,
            title = "Nâng cấp vĩnh viễn",
            subtitle = "Menu → NÂNG CẤP — tốn khoáng",
            description = "5 cây nâng cấp: HP / Sát thương / Tốc độ / Hút khoáng / Chí mạng.\n" +
                "Mỗi cây 5 cấp. Tốn khoáng tích luỹ. Áp dụng vĩnh viễn cho mọi run.",
            iconDraw = { c ->
                // Placeholder — upward arrow
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
        Spacer(modifier = Modifier.height(12.dp))

        // ── Honest disclosure cho user về defer ──
        InfoCard(
            color = Color.White.copy(alpha = 0.5f),
            title = "⏸ ShipPickerScreen chưa có",
            subtitle = "Defer Round 73 (sau audit user)",
            description = "Round 71 đã thiết lập enum ShipShape + persistence. Round 73 sẽ ship UI picker " +
                "+ wire selectedShipShape vào EffectiveStats. Hiện tại chỉ FIGHTER active mặc định.",
            iconDraw = { c ->
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = c.width * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
            },
        )
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
        iconDraw = { c -> drawShipPreview(c, color, laserBoosted = false) },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShipPreview(
    canvasSize: androidx.compose.ui.geometry.Size,
    color: Color,
    laserBoosted: Boolean,
) {
    val w = canvasSize.width
    val h = canvasSize.height
    val cx = w / 2f
    val cy = h / 2f

    // Halo
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = w * 0.45f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )

    // Wings (wider if laserBoosted)
    val wingHalfW = w * if (laserBoosted) 0.42f else 0.34f
    val wingTopY = cy + h * 0.05f
    val wingBottomY = cy + h * 0.30f
    val wings = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - wingHalfW, wingTopY)
        lineTo(cx - wingHalfW * 0.55f, wingBottomY)
        lineTo(cx - w * 0.14f, wingBottomY - h * 0.04f)
        lineTo(cx + w * 0.14f, wingBottomY - h * 0.04f)
        lineTo(cx + wingHalfW * 0.55f, wingBottomY)
        lineTo(cx + wingHalfW, wingTopY)
        close()
    }
    drawPath(wings, color)
    // Body
    val body = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, h * 0.10f)
        lineTo(cx + w * 0.14f, cy - h * 0.05f)
        lineTo(cx + w * 0.11f, cy + h * 0.36f)
        lineTo(cx - w * 0.11f, cy + h * 0.36f)
        lineTo(cx - w * 0.14f, cy - h * 0.05f)
        close()
    }
    drawPath(body, color)
    // Engine glow
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = w * 0.05f,
        center = androidx.compose.ui.geometry.Offset(cx, cy + h * 0.34f),
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Enemies tab — uses actual EnemyCanvas shape recipes.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun EnemiesTab() {
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        InfoCard(
            color = Color(0xFF4FD4FF),
            title = "Light Blue family (5 variants)",
            subtitle = "Dart triangles · tốc độ cao",
            description = "Triangles pointing DOWN (Chapter 3 — Hành Tinh Băng). " +
                "5 variants với notch khác nhau. Damage thấp, tốc độ cao, HP thấp.",
            iconDraw = { c -> drawEnemyDart(c, Color(0xFF4FD4FF), Color(0xFF1799CC)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = Color(0xFF6EFFAA),
            title = "Green family (4 variants)",
            subtitle = "Hexagons · balanced medium",
            description = "Hexagons rotated theo variant (Chapter 2 — Mây Tinh Vân). " +
                "HP trung bình, damage trung bình.",
            iconDraw = { c -> drawEnemyHexagon(c, Color(0xFF6EFFAA), Color(0xFF24B86E)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = Color(0xFFFF5555),
            title = "Red family (3 variants)",
            subtitle = "Diamonds · heavy hitters",
            description = "Diamonds với extra pip mỗi variant (Chapter 1 — Vành Đai Tiểu Hành Tinh). " +
                "HP cao, damage cao, tốc độ thấp.",
            iconDraw = { c -> drawEnemyDiamond(c, Color(0xFFFF5555), Color(0xFFCC1144)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonMagenta,
            title = "Status Effects (3)",
            subtitle = "BURN / SLOW / STUN",
            description = "BURN — 5HP/sec DoT (cam). SLOW — movement ×0.5 (cyan). " +
                "STUN — stop firing 2s (vàng). 10% per hit (5% on boss). FIRE bullet luôn apply BURN 100%.",
            iconDraw = { c ->
                val cy = c.height / 2
                val r = c.height * 0.13f
                drawCircle(Color(0xFFFF6020), r, androidx.compose.ui.geometry.Offset(c.width * 0.25f, cy))
                drawCircle(Color(0xFF00F0FF), r, androidx.compose.ui.geometry.Offset(c.width * 0.50f, cy))
                drawCircle(Color(0xFFFFD040), r, androidx.compose.ui.geometry.Offset(c.width * 0.75f, cy))
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Round 72 (Issue 3 user audit) — HONEST DISCLOSURE về Wave 9a roadmap.
        // Trước fix tab này không nhắc đến 20 enemies user picked Round 69.
        InfoCard(
            color = Color.White.copy(alpha = 0.5f),
            title = "⏸ 20 enemy variants (Wave 9a)",
            subtitle = "Roadmap Round 73 — chưa ship",
            description = "Round 69 user pick FULL 20 enemies (5 family × 4 variant). Round 71 chỉ ship " +
                "12 hiện có. Round 73 sẽ thêm 8 shape mới (spike/cross/orb/crescent/triangle/octagon/" +
                "hexagram/chevron) + 5 family Scout/Fighter/Heavy/Elite/Berserker với stat profile riêng " +
                "+ assign vào Chapter pools. Hiện tại chỉ thấy 3 shape (dart/hexagon/diamond) là honest.",
            iconDraw = { c ->
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = c.width * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
            },
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDart(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.35f; val halfH = h * 0.40f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy + halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx, cy - halfH + h * 0.10f)
        lineTo(cx - halfW, cy - halfH)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f))
    drawCircle(accent, w * 0.07f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.07f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyHexagon(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val rx = w * 0.36f; val ry = h * 0.42f
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until 6) {
            val a = 2.0 * Math.PI * i / 6.0
            val x = cx + (rx * kotlin.math.cos(a)).toFloat()
            val y = cy + (ry * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f))
    drawCircle(accent, w * 0.09f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDiamond(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.38f; val halfH = h * 0.42f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    drawCircle(accent, w * 0.08f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Bosses tab — uses BossStar shape.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun BossesTab() {
    // Round 72 (Issue 3 user audit) — 5 distinct boss silhouettes (BossKind từ
    // Round 71). Match đúng what's in game now. Attack patterns + audio cue
    // defer R73 (user pick).
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        InfoCard(
            color = Color(0xFFFF5555),
            title = "1. LevelOneBoss — Star",
            subtitle = "End Chapter 1 + Chapter 3 · HP 3000 · BossKind.STAR",
            description = "Ngôi sao 8 cánh + lõi lục giác. Mở khoá ở cuối Vành Đai Tiểu HT (Ch1) " +
                "và lặp lại tại Hành Tinh Băng (Ch3) với palette đổi. Pattern: bắn từng đợt + " +
                "đợt cuối ring barrage. Có HP bar full-width + intro cinematic + buff picker.",
            iconDraw = { c -> drawBossStar(c, Color(0xFFFF5555), Color(0xFFCC1144)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = Color(0xFF6EFFAA),
            title = "2. LevelTwoBoss — Cross",
            subtitle = "End Chapter 2 + Chapter 4 · HP 4000 · BossKind.CROSS",
            description = "Cross spinner 4 cánh + center disc + 4 tip glow. Mid-game boss. " +
                "Quay liên tục, bắn theo trục dọc/ngang xen kẽ. Pattern phức tạp hơn LevelOne.",
            iconDraw = { c -> drawBossCrossPreview(c, Color(0xFF6EFFAA), Color(0xFF24B86E)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonGold,
            title = "3. MidBoss OFFENSIVE — Orb",
            subtitle = "Giữa Chapter 1+4 · HP 1200 · BossKind.ORB",
            description = "Quả cầu lớn + 3 vệ tinh quay quanh. 360° spread spray. " +
                "Phase 2 (HP<50%) — tăng fire rate.",
            iconDraw = { c -> drawBossOrbPreview(c, NeonGold, Color(0xFFCC9900)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonViolet,
            title = "4. MidBoss DEFENSIVE/SWARM — Fractal",
            subtitle = "Giữa Chapter 2+3 · HP 1200 · BossKind.FRACTAL",
            description = "Tam giác lồng nhau (Sierpinski). DEFENSIVE = orbit + counter (Ch2). " +
                "SWARM = spawn 4 drones (Ch3). Phase 2 (HP<50%) — pattern chia 3.",
            iconDraw = { c -> drawBossFractalPreview(c, NeonViolet, Color(0xFF8855CC)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonMagenta,
            title = "5. FinalBoss — Spider",
            subtitle = "End Chapter 5 (Lõi Thiên Hà) · HP 22500 · BossKind.SPIDER · 3-phase",
            description = "8 chân + thân + 2 mắt sáng. Phase 1: fire pattern thông thường. " +
                "Phase 2 (HP≤15000): tăng tốc độ. Phase 3 (HP≤7500): ring barrage 360°. " +
                "Victory ending khác theo difficulty.",
            iconDraw = { c -> drawBossSpiderPreview(c, NeonMagenta, Color(0xFFCC1144)) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        InfoCard(
            color = Color.White.copy(alpha = 0.5f),
            title = "⏸ Attack pattern + audio cue chưa unique",
            subtitle = "Defer Round 73",
            description = "Round 71 đã ship 5 silhouette khác nhau (đã thấy in-game). " +
                "Round 73 sẽ refactor EnemyLasersController để mỗi boss có attack pattern + " +
                "audio cue riêng (STAR=ring, CROSS=spin lasers, ORB=tracking, FRACTAL=split, SPIDER=web).",
            iconDraw = { c ->
                drawCircle(color = Color.White.copy(alpha = 0.3f), radius = c.width * 0.3f,
                    center = androidx.compose.ui.geometry.Offset(c.width / 2, c.height / 2))
            },
        )
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossFractalPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.42f
    val outer = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx - r * 0.866f, cy - r * 0.5f)
        lineTo(cx + r * 0.866f, cy - r * 0.5f)
        close()
    }
    drawPath(outer, body)
    drawPath(outer, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f))
    val innerR = r * 0.5f
    val inner = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - innerR)
        lineTo(cx - innerR * 0.866f, cy + innerR * 0.5f)
        lineTo(cx + innerR * 0.866f, cy + innerR * 0.5f)
        close()
    }
    drawPath(inner, accent)
    drawCircle(body, r * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy))
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossStar(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.42f
    val innerR = outerR * 0.55f
    val path = androidx.compose.ui.graphics.Path().apply {
        val step = Math.PI / 8.0
        for (i in 0 until 16) {
            val a = -Math.PI / 2 + i * step
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    // Inner hex
    val coreR = outerR * 0.35f
    val core = androidx.compose.ui.graphics.Path().apply {
        for (i in 0 until 6) {
            val a = 2.0 * Math.PI * i / 6.0
            val x = cx + (coreR * kotlin.math.cos(a)).toFloat()
            val y = cy + (coreR * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(core, accent)
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
    BoosterType.SMOKE_BOOSTER ->
        "Đạn khói AoE 60 px (sắp ra mắt — hiện tại hoạt động như đạn thường, ×0.8 sát thương).\nKéo dài 10 giây."
    BoosterType.ZIGZAG_BOOSTER ->
        "Đạn bay đường zigzag né dodge enemy (sắp ra mắt — hiện đạn thường, ×0.9 sát thương).\nKéo dài 12 giây."
    BoosterType.KAMEHAMEHA_BOOSTER ->
        "Tia năng lượng xuyên thấu vô hạn, sát thương ×3 (sắp ra mắt — hiện đạn thường, vẫn ×3 sát thương).\nKéo dài 8 giây."
    BoosterType.ATOMIC_BOOSTER ->
        "Đạn nguyên tử nổ AoE 150 px khổng lồ (sắp ra mắt — hiện đạn thường, ×1.5 sát thương).\nKéo dài 10 giây."
    BoosterType.SPLIT_BOOSTER ->
        "Đạn phân tách thành 3 mảnh khi va chạm (sắp ra mắt — hiện đạn thường, ×0.6 sát thương).\nKéo dài 12 giây."
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
    BoosterType.SMOKE_BOOSTER -> "(Sắp ra) — chưa khuyến nghị."
    BoosterType.ZIGZAG_BOOSTER -> "(Sắp ra) — chưa khuyến nghị."
    BoosterType.KAMEHAMEHA_BOOSTER -> "(Sắp ra) — nhặt cho damage ×3 ngay."
    BoosterType.ATOMIC_BOOSTER -> "(Sắp ra) — nhặt cho damage ×1.5."
    BoosterType.SPLIT_BOOSTER -> "(Sắp ra) — chưa khuyến nghị."
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
