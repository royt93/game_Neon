package com.tranphuloi.neon.ui.info

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.withTransform
import com.tranphuloi.neon.ui.game.world.drawBoosterShape
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
import androidx.compose.foundation.layout.offset
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
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
import com.tranphuloi.neon.ui.game.booster.BoosterType
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.laser.BulletTypeColorMap
import com.tranphuloi.neon.common.PathPool

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
        // Pixel-3 #3 — shared decorative starfield. Sits above NeonBgDeep
        // background, below Tab content. Pure decorative, doesn't consume
        // pointer events.
        com.tranphuloi.neon.common.NeonStarfieldBackground(
            modifier = Modifier.fillMaxSize(),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            // Audit-Pixel-2 #1 fix — shared NeonActionBar replaces inline header;
            // Info + Stats screens now use the same affordance (title left,
            // (✕) close right). Pre-fix Info used a `← QUAY LẠI` text button —
            // inconsistent with Stats' icon close. Unified here.
            com.tranphuloi.neon.common.NeonActionBar(
                title = "BÁCH KHOA",
                titleColor = NeonCyan,
                onBack = onBack,
            )
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

// Delegates to [BulletTypeColorMap] so the Bullet preview tab, in-game bullet
// activation popup, and Booster preview tab share one color source. Inline
// hex literals here would silently drift when a tint constant shifts.
private fun bulletColor(b: BulletType): Color = Color(BulletTypeColorMap.argbFor(b))

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
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - h / 2)               // top tip
        lineTo(cx + w / 2, cy + h / 2)       // bottom-right
        lineTo(cx - w / 2, cy + h / 2)       // bottom-left
        close()
    }
    drawPath(path, color)
    PathPool.release(path)
    val corePath = PathPool.acquire().apply {
        moveTo(cx, cy - h / 2 + h * 0.1f)
        lineTo(cx + w / 4, cy + h / 2 - h * 0.1f)
        lineTo(cx - w / 4, cy + h / 2 - h * 0.1f)
        close()
    }
    drawPath(corePath, Color.White.copy(alpha = 0.85f))
    PathPool.release(corePath)
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
    val flamePath = PathPool.acquire().apply {
        moveTo(cx - w / 2, cy + h / 2 - h * 0.1f)
        lineTo(cx, cy + h * 0.65f)
        lineTo(cx + w / 2, cy + h / 2 - h * 0.1f)
        close()
    }
    drawPath(flamePath, color.copy(alpha = 0.7f))
    PathPool.release(flamePath)
    drawPath(
        PathPool.acquire().apply {
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    // Wave 13a (slice D) — Bách Khoa Tàu là TRANG TRA CỨU read-only. Việc
    // mở khoá + chọn tàu đã chuyển sang Cửa hàng (tab Tàu). Banner tĩnh trỏ
    // người chơi sang đó thay cho CTA mở ShipPicker (đã gỡ).
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            Text(
                text = "✦ Mở khoá & chọn tàu ở CỬA HÀNG → tab Tàu",
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 0.5.sp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Header
        InfoCard(
            color = NeonGold,
            title = "Phi thuyền — 3 lớp tuỳ chỉnh",
            subtitle = "Loại tàu · Màu sắc · Nâng cấp chỉ số",
            description = "1) Mua LOẠI TÀU ở Cửa hàng (tab Tàu) — ảnh hưởng HP/Tốc độ/Sát thương\n" +
                "2) Đổi MÀU SẮC (skin) — chỉ thẩm mỹ\n" +
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
                val path = PathPool.acquire().apply {
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
                PathPool.release(path)
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
    // Delegates to ShipShapeColorMap so the accent here matches the ship
    // picker grid 1:1. Inline hex literals would drift over time.
    val color = Color(com.tranphuloi.neon.ui.game.ship.shape.ShipShapeColorMap.argbFor(shape))
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
            title = "Trinh Sát Sơ Cấp", subtitle = "Hành Tinh Băng · HP 126 · Tốc độ nhanh",
            description = "Hình trái tim cyan — variant chuyên trị melee. Thấy đầu tiên ở Hành Tinh Băng.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyHeart(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "Trinh Sát Cảnh Giới", subtitle = "Hành Tinh Băng · HP 126",
            description = "Tam giác đơn giản — alien glyph với center pip + side dots. Spawn formation Row.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyTriangle(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "Trinh Sát Tẩy Chuồn", subtitle = "Hành Tinh Băng · HP 126",
            description = "Bài chuồn — cluster 3 hình tròn + thân. Spawn formation ZigZag.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyClub(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "Trinh Sát Đĩa Tròn", subtitle = "Hành Tinh Băng · HP 126",
            description = "Hình tròn — đĩa cyan với ring viền + center pip. Spawn formation V.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemyCircle(c, scoutColor, scoutAccent) }),
        EnemyVariantSpec(
            title = "Trinh Sát Bóng Đêm", subtitle = "Hành Tinh Băng + Trạm + Lõi",
            description = "Bài bích — heart ngược + thân stem. Outro variant.",
            color = scoutColor, draw = { sc, c -> sc.drawEnemySpade(c, scoutColor, scoutAccent) }),
        // FIGHTER family — Chapter 2 (Mây Tinh Vân)
        EnemyVariantSpec(
            title = "Chiến Đấu Cơ Alpha", subtitle = "Mây Tinh Vân · HP 180",
            description = "Hexagon variant 0 — baseline cân bằng. Stats tham chiếu cho mọi family.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyHexagon(c, fighterColor, fighterAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Chiến Đấu Cơ Bravo", subtitle = "Mây Tinh Vân · HP 180",
            description = "Virus xanh — 8 gai radiating + nhân RNA. Biological/alien aesthetic.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyVirus(c, fighterColor, fighterAccent) }),
        EnemyVariantSpec(
            title = "Chiến Đấu Cơ Charlie", subtitle = "Mây Tinh Vân · HP 180",
            description = "Mắt nhìn — oval body + iris + pupil. Cảm giác bị quan sát.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyEye(c, fighterColor, fighterAccent) }),
        EnemyVariantSpec(
            title = "Chiến Đấu Cơ Delta", subtitle = "Mây Tinh Vân + Trạm + Lõi · HP 180",
            description = "Bài rô — kim cương dọc (vertical rhombus) xanh. Outro variant lặp Ch4+5.",
            color = fighterColor, draw = { sc, c -> sc.drawEnemyCardDiamond(c, fighterColor, fighterAccent) }),
        // HEAVY family — Chapter 1 (Vành Đai Tiểu Hành Tinh)
        EnemyVariantSpec(
            title = "Tăng Thiết Giáp", subtitle = "Vành Đai · HP 288 · Tốc độ chậm",
            description = "Diamond variant 0 — heavy hitter, ×1.3 damage. Bắt đầu Vành Đai.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyDiamond(c, heavyColor, heavyAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Tăng Cận Vệ", subtitle = "Vành Đai + Trạm Thù Địch · HP 288",
            description = "Trái tim đỏ — heart shape đỏ chính danh. Tank-class hạng nặng.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyHeart(c, heavyColor, heavyAccent) }),
        EnemyVariantSpec(
            title = "Tăng Phản Lực", subtitle = "Vành Đai + Trạm + Lõi · HP 288",
            description = "Hình tròn đỏ — đĩa đỏ với ring viền dày. Endgame anchor.",
            color = heavyColor, draw = { sc, c -> sc.drawEnemyCircle(c, heavyColor, heavyAccent) }),
        // ELITE family — Chapter 4 (Trạm Thù Địch)
        EnemyVariantSpec(
            title = "Tinh Nhuệ Thập Tự", subtitle = "Trạm Thù Địch · HP 225 · ×1.1 dmg",
            description = "Cross variant 0 — 4 cánh + tip glow nhỏ. Arm spinner spread.",
            color = eliteColor, draw = { sc, c -> sc.drawCrossPreview(c, eliteColor, eliteAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Đĩa Bay Alien", subtitle = "Trạm Thù Địch · HP 225",
            description = "Đĩa bay alien — saucer + dome cockpit + 5 underside lights. Tàu bay alien class.",
            color = eliteColor, draw = { sc, c -> sc.drawEnemyUFOPreview(c, eliteColor, eliteAccent) }),
        EnemyVariantSpec(
            title = "Quả Cầu Bay", subtitle = "Trạm Thù Địch · HP 225",
            description = "Orb variant 0 — single orbit ring. Floating attacker.",
            color = eliteColor, draw = { sc, c -> sc.drawOrbVariantPreview(c, eliteColor, eliteAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Drone Bốn Cánh", subtitle = "Trạm Thù Địch · HP 225",
            description = "Drone 4 cánh quạt + cannon dưới + sensor mắt đỏ. Mech-style alien.",
            color = eliteColor, draw = { sc, c -> sc.drawEnemyDronePreview(c, eliteColor, eliteAccent) }),
        // BERSERKER family — Chapter 5 (Lõi Thiên Hà)
        EnemyVariantSpec(
            title = "Cuồng Loạn Mũi Tên", subtitle = "Lõi Thiên Hà · HP 162 · ×1.4 dmg",
            description = "Chevron variant 0 — mũi tên đơn. Damage cao nhất game.",
            color = berserkerColor, draw = { sc, c -> sc.drawChevronPreview(c, berserkerColor, berserkerAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Cá Đuối Vũ Trụ", subtitle = "Lõi Thiên Hà · HP 162",
            description = "Manta ray alien — delta-wing + 2 mắt + spine ridge. Tàu bay sinh học.",
            color = berserkerColor, draw = { sc, c -> sc.drawEnemyMantaRayPreview(c, berserkerColor, berserkerAccent) }),
        EnemyVariantSpec(
            title = "Cuồng Loạn Gai", subtitle = "Lõi Thiên Hà · HP 162",
            description = "Spike variant 0 — sao gai 8 cánh, spike ngắn.",
            color = berserkerColor, draw = { sc, c -> sc.drawSpikePreviewV(c, berserkerColor, berserkerAccent, variant = 0) }),
        EnemyVariantSpec(
            title = "Robot Chiến Đấu", subtitle = "Lõi Thiên Hà · HP 162",
            description = "Mech chiến tranh — boxy hull + viewport + 2 cannon sides + sensor antenna. Tàu công nghiệp.",
            color = berserkerColor, draw = { sc, c -> sc.drawEnemyMechPreview(c, berserkerColor, berserkerAccent) }),
        // ── Round 81 — 10 new enemy roster (preview only; chapter wire R82) ──
        EnemyVariantSpec(
            title = "Lưỡi Cưa Quay Tròn", subtitle = "Trạm Thù Địch / Lõi · HP 200 · melee orbit",
            description = "Lưỡi cưa 12 răng quay tròn. Tấn công melee qua orbit pattern. Highest contact damage.",
            color = Color(0xFFCCCCCC),
            draw = { sc, c -> sc.drawEnemySpinningSawPreview(c, Color(0xFFCCCCCC), Color(0xFFFF2D55)) }),
        EnemyVariantSpec(
            title = "Bạch Tuộc Vũ Trụ", subtitle = "Trạm Thù Địch / Lõi · HP 350 · tracks player",
            description = "Đầu mực + 6 tentacles wavy. Slow nhưng tracking nguyện vọng player position. Multi-arm grab.",
            color = Color(0xFF60E0C0),
            draw = { sc, c -> sc.drawEnemyTentacleSquidPreview(c, Color(0xFF60E0C0), Color(0xFFFFD040)) }),
        EnemyVariantSpec(
            title = "Đặt Mìn", subtitle = "Trạm Thù Địch / Lõi · HP 280 · drops mines behind",
            description = "Tank vuông + turret + treads. Di chuyển chậm + drop mine khi pass. Area denial role.",
            color = Color(0xFF707080),
            draw = { sc, c -> sc.drawEnemyMineLayerPreview(c, Color(0xFF707080), Color(0xFFFF2D55)) }),
        EnemyVariantSpec(
            title = "Drone Khiên", subtitle = "Trạm Thù Địch / Lõi · HP 180 · front shield",
            description = "Hexagonal drone + shield arc phía trước. Shield block damage từ player bullets. Flank to kill.",
            color = Color(0xFF40A0FF),
            draw = { sc, c -> sc.drawEnemyShieldDronePreview(c, Color(0xFF40A0FF), Color(0xFF80E0FF)) }),
        EnemyVariantSpec(
            title = "Bắn Tỉa Xa", subtitle = "Trạm Thù Địch / Lõi · HP 220 · single high-damage shot",
            description = "Long rifle barrel + scope + red dot. Bắn 1 phát precise high-damage từ xa. Aim line warns 0.5s before shot.",
            color = Color(0xFF8B5C2E),
            draw = { sc, c -> sc.drawEnemySniperPreview(c, Color(0xFF8B5C2E), Color(0xFF404040)) }),
        EnemyVariantSpec(
            title = "Tăng Pháo Mortar", subtitle = "Trạm Thù Địch / Lõi · HP 400 · arc mortar",
            description = "Tank + mortar tube angled up + 4 wheels. Bắn shell với arc trajectory, AOE damage on landing.",
            color = Color(0xFF606040),
            draw = { sc, c -> sc.drawEnemyBomberCrawlerPreview(c, Color(0xFF606040), Color(0xFFFFE040)) }),
        EnemyVariantSpec(
            title = "Cặp Sinh Đôi", subtitle = "Trạm Thù Địch / Lõi · HP 150 each · spawn-pair link",
            description = "Spawn pair-link. Cả 2 phải die simultaneously, nếu kill 1 trước thì regenerate. Damage on both via shared HP pool.",
            color = Color(0xFFFF60A0),
            draw = { sc, c -> sc.drawEnemyMirrorTwinPreview(c, Color(0xFFFF60A0), Color.White) }),
        EnemyVariantSpec(
            title = "Bóng Ma Mờ Ảo", subtitle = "Trạm Thù Địch / Lõi · HP 300 · 50% damage reduction",
            description = "Ghost semi-transparent. 50% damage reduction (chỉ kill bằng ULTIMATE laser hoặc BURN status). Flicker every 2s.",
            color = Color(0xFFB14CFF),
            draw = { sc, c -> sc.drawEnemyPhantomPreview(c, Color(0xFFB14CFF), Color(0xFFE8E8F0)) }),
        EnemyVariantSpec(
            title = "Pháp Sư Hồi Phục", subtitle = "Trạm Thù Địch / Lõi · HP 250 · heals other enemies",
            description = "Healing orb + aura ring + + symbol + 4 healing particles. Heals 5hp/s cho enemies trong 100dp radius. Kill priority.",
            color = Color(0xFF60FFAA),
            draw = { sc, c -> sc.drawEnemyHealerPreview(c, Color(0xFF60FFAA), Color.White) }),
        EnemyVariantSpec(
            title = "Cảm Tử Quân", subtitle = "Trạm Thù Địch / Lõi · HP 100 · accelerates + explodes",
            description = "Warning triangle + ! mark + trail. Tăng tốc khi gần player, explode on contact for high damage. Risk-reward kill quickly.",
            color = Color(0xFFFFE040),
            draw = { sc, c -> sc.drawEnemyKamikazePreview(c, Color(0xFFFFE040), Color(0xFFFF6020)) }),
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
        val path = PathPool.acquire().apply {
            moveTo(cx - halfW, cy - h * 0.20f + yShift)
            lineTo(cx, cy + h * 0.18f + yShift)
            lineTo(cx + halfW, cy - h * 0.20f + yShift)
            lineTo(cx + halfW * 0.7f, cy - h * 0.27f + yShift)
            lineTo(cx, cy + h * 0.08f + yShift)
            lineTo(cx - halfW * 0.7f, cy - h * 0.27f + yShift)
            close()
        }
        drawPath(path, body)
        PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
    drawCircle(accent, innerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpikePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.45f
    val innerR = outerR * 0.40f
    val path = PathPool.acquire().apply {
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
        PathPool.release(path)
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
    val path = PathPool.acquire().apply {
        moveTo(cx, cy + halfH)                              // tip down
        lineTo(cx + halfW, cy - halfH)                      // top-right
        lineTo(cx, cy - halfH + notch)                      // back notch (chevron)
        lineTo(cx - halfW, cy - halfH)                      // top-left
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
    drawCircle(accent, w * 0.12f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDiamond(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color, variant: Int = 0,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.45f; val halfH = h * 0.50f
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f))
    PathPool.release(path)
    PathPool.release(path)
    PathPool.release(path)
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
                val mini = PathPool.acquire().apply {
                    moveTo(mx, my - miniR)
                    lineTo(mx + miniR, my)
                    lineTo(mx, my + miniR)
                    lineTo(mx - miniR, my)
                    close()
                }
                drawPath(mini, accent)
                PathPool.release(mini)
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
            title = "Lính Gác Mắt Sát Thủ",
            subtitle = "Giữa Vành Đai · HP 1200",
            description = "Mắt sát thủ khổng lồ — sclera + iris + pupil + 6 mi mắt eldritch. Spray 360° spread + radial barrage. " +
                "Phase 2 (HP<50%) tăng fire rate. Defeat reward: buff picker post-kill.",
            iconDraw = { c -> drawBossEyePreview(c, gold, goldAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = red,
            title = "Mặt Trời Đỏ Máu",
            subtitle = "Cuối Vành Đai · HP 3000",
            description = "Mặt trời đỏ rực — corona ring + 12 radial flares + hot disc center. Pattern Round 74 wired: " +
                "8-laser ring radial mỗi tick. Audio cue 1.4× pitch (sting cao chói tai). HP bar full-width.",
            iconDraw = { c -> drawBossStar(c, red, redAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 2 — Mây Tinh Vân
        InfoCard(
            color = violet,
            title = "Hộ Vệ Nguyên Tử",
            subtitle = "Giữa Mây Tinh Vân · HP 1200",
            description = "Mô hình nguyên tử — 3 quỹ đạo điện tử quay quanh hạt nhân tím. " +
                "Orbit + counter pattern. Phase 2 phá quỹ đạo thành 3 sub-pattern.",
            iconDraw = { c -> drawBossFractalPreview(c, violet, violetAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = green,
            title = "Thập Tự Ngọc Lục Bảo",
            subtitle = "Cuối Mây Tinh Vân · HP 4000",
            description = "Cross spinner 4 cánh xanh + 4 tip glow. Quay liên tục. " +
                "Pattern Round 74 wired: alternating axis sweep (vertical wall ↔ 4-diagonal). Audio 1.15× pitch.",
            iconDraw = { c -> drawBossCrossPreview(c, green, greenAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 3 — Hành Tinh Băng
        InfoCard(
            color = violet,
            title = "Hồn Ma Trẻ Em",
            subtitle = "Giữa Hành Tinh Băng · HP 1200",
            description = "Hồn ma trẻ em — hollow eyes + wavy bottom sheet. " +
                "Pattern SWARM — spawn 4 drone con khi Phase 2.",
            iconDraw = { c -> drawBossHauntedKidPreview(c, violet, violetAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = red,
            title = "Mặt Trăng Tử Thần",
            subtitle = "Cuối Hành Tinh Băng · HP 3000",
            description = "Mặt trăng tử thần — full moon disc + skull eye sockets + teeth row + crack fissures. " +
                "Arena ICE_PATCHES — tàu trượt " +
                "→ dodge ring barrage khó hơn. Audio cue 0.75× pitch trầm.",
            iconDraw = { c -> drawBossDeathMoonPreview(c, red, redAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 4 — Trạm Thù Địch
        InfoCard(
            color = gold,
            title = "Chúa Tể Địa Ngục",
            subtitle = "Giữa Trạm Thù Địch · HP 1200",
            description = "Chúa tể địa ngục — devil head với 2 curved horns + glowing eyes + fangs. " +
                "ELITE enemies bay xung quanh hỗ trợ. " +
                "Audio cue 0.55× pitch sâu nhất.",
            iconDraw = { c -> drawBossHellLordPreview(c, gold, goldAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = green,
            title = "Quỷ Satan",
            subtitle = "Cuối Trạm Thù Địch · HP 4000",
            description = "Quỷ Satan — inverted pentagram + all-seeing eye trung tâm + 5 candle dots. " +
                "ELITE enemy wave đồng hành " +
                "(cross/orb violet) bay xung quanh boss → tổng pressure cao hơn.",
            iconDraw = { c -> drawBossSatanGlyphPreview(c, green, greenAcc) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Ch 5 — Lõi Thiên Hà
        InfoCard(
            color = magenta,
            title = "Bá Vương Thiên Hà",
            subtitle = "Cuối Lõi Thiên Hà · HP 22500 · 3-pha",
            description = "Boss cuối game. 8 chân + thân + 2 mắt sáng. " +
                "Phase 1 (HP>15000): fire pattern thông thường. " +
                "Phase 2 (HP≤15000): tăng tốc độ + spawn BERSERKER wave. " +
                "Phase 3 (HP≤7500): ring barrage 360° + tracking lasers. " +
                "Audio cue 0.65× pitch (sting trầm sâu). Victory ending khác theo difficulty.",
            iconDraw = { c -> drawBossSpiderPreview(c, magenta, redAcc) },
        )
        Spacer(modifier = Modifier.height(20.dp))
        // 12 mid-bosses xuất hiện rải rác qua các chapter (game-stage 6/8/10/12).
        Text(
            text = "★ TRÙM MINI",
            color = magenta,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        InfoCard(
            color = Color(0xFFFFB048),
            title = "Gà Mái Dầu",
            subtitle = "Vành Đai · ném trứng loạn xà ngầu",
            description = "Mái dầu khổng lồ — thân oval + đầu + mỏ + 3 mào đỏ + lông cánh. Ném trứng cluster.",
            iconDraw = { c -> drawBossHenMotherPreview(c, Color(0xFFFFB048), gold) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = red,
            title = "Trâu Hung Hẵn",
            subtitle = "Vành Đai ·quăng sừng trâu",
            description = "Đầu trâu khổng lồ — 2 sừng cong + vòng mũi vàng + mắt đỏ giận dữ. Quăng sừng spear.",
            iconDraw = { c -> drawBossBuffaloPreview(c, red, redAcc) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFF888888),
            title = "Chuột Ngu Si",
            subtitle = "Mây Tinh Vân ·ném đạn laza",
            description = "Đầu chuột — 2 tai tròn + 2 răng cửa to + ria mép + biểu cảm ngơ ngác. Bắn laser bullets.",
            iconDraw = { c -> drawBossRatPreview(c, Color(0xFF888888), Color(0xFFFF80B0)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFFF9020),
            title = "Cọp Hung Tợn",
            subtitle = "Mây Tinh Vân ·gầm vang ra đạn lum la",
            description = "Đầu hổ — bờm 8 tia + sọc đen + nanh trắng + mắt vàng. Gầm shockwave 360°.",
            iconDraw = { c -> drawBossTigerPreview(c, Color(0xFFFF9020), Color(0xFF202020)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFFF60A0),
            title = "Cô Gái Sexy",
            subtitle = "Hành Tinh Băng ·ném tóc 4 phương 8 hướng",
            description = "Silhouette nữ thần — đầu + tóc dài bay + vương miện ngọc + thân đồng hồ cát. Tóc tóc tua 8 hướng.",
            iconDraw = { c -> drawBossDivaPreview(c, Color(0xFFFF60A0), Color(0xFFFFD700)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = violet,
            title = "Tháp Tinh Quỷ",
            subtitle = "Hành Tinh Băng · bắn pearl projectile",
            description = "Tháp obelisk cao — đỉnh sáng + vương miện ngọc + 3 vòng năng lượng quanh đỉnh. Bắn pearl projectile.",
            iconDraw = { c -> drawBossTrollTowerPreview(c, violet, gold) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFFFB0C0),
            title = "Đôi Đỉnh Sinh Hoa",
            subtitle = "Trạm Thù Địch · 2 tia sữa sát thương cao",
            description = "Twin dome boss — 2 đỉnh cong + đầu vòi sáng + halo. Bắn 2 milk ray song song high-damage.",
            iconDraw = { c -> drawBossTwinSummitsPreview(c, Color(0xFFFFB0C0), Color.White) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFF6B3010),
            title = "Đôi Cầu Hư Vô",
            subtitle = "Trạm Thù Địch · cluster sát thương chí mạng",
            description = "2 quả cầu đen lớn + tâm cleft + 3 spit blob. Bắn cluster bullet brown sát thương cao.",
            iconDraw = { c -> drawBossVoidGlobesPreview(c, Color(0xFF6B3010), Color(0xFF8B5C2E)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFE8E8F0),
            title = "Bạch Long Mắt Lam",
            subtitle = "Lõi Thiên Hà ·thét ra lửa",
            description = "Rồng trắng dài sinuous — 2 sừng + mắt xanh lam + móng vuốt + 3 lửa phun. Fire breath cone.",
            iconDraw = { c -> drawBossWhiteDragonPreview(c, Color(0xFFE8E8F0), Color(0xFF0080FF)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFCC0000),
            title = "Cộng Sản Bịp Bợm",
            subtitle = "Lõi Thiên Hà ·quăng búa liềm bịp bợm",
            description = "Vòng đỏ + búa + liềm vàng + sao 5 cánh ở đầu. Throws hammer + sickle projectile pair.",
            iconDraw = { c -> drawBossHammerSicklePreview(c, Color(0xFFCC0000), Color(0xFFFFD700)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFF208040),
            title = "Tư Bản Bóc Lột",
            subtitle = "Lõi Thiên Hà ·quăng money",
            description = "Tư bản béo + mũ chóp đen + monocle + ria mép + $ trên bụng + túi tiền. Throws money bills.",
            iconDraw = { c -> drawBossMoneyTycoonPreview(c, Color(0xFF208040), Color(0xFFFFD700)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        InfoCard(
            color = Color(0xFFFF9520),
            title = "Tycoon Vàng",
            subtitle = "Lõi Thiên Hà · quăng đô la",
            description = "Tóc cam đặc trưng + cà vạt vàng + complexion da + monocle. 3 dollar bills float quanh. " +
                "Throws dollar bills 360°.",
            iconDraw = { c -> drawBossGoldenTycoonPreview(c, Color(0xFFFFB890), Color(0xFFFFC020)) },
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
    val baseTri = PathPool.acquire().apply {
        moveTo(cx - w * 0.15f, cy + h * 0.45f)
        lineTo(cx + w * 0.15f, cy + h * 0.45f)
        lineTo(cx, cy + offsetD * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    PathPool.release(baseTri)
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
    val triPath = PathPool.acquire().apply {
        moveTo(cx - halfW, cy + halfH * 0.30f)
        lineTo(cx + halfW, cy + halfH * 0.30f)
        lineTo(cx, cy - halfH)
        close()
    }
    drawPath(triPath, body)
    PathPool.release(triPath)
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy + halfH * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.20f))
    val baseTri = PathPool.acquire().apply {
        moveTo(cx - w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx + w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx, cy + halfH * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    PathPool.release(baseTri)
    drawCircle(accent, w * 0.06f, androidx.compose.ui.geometry.Offset(cx, cy + halfH * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyCardDiamond(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val halfW = w * 0.32f; val halfH = h * 0.48f
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f))
    PathPool.release(path)
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
    val triPath = PathPool.acquire().apply {
        moveTo(cx - halfW, cy - halfH * 0.18f)
        lineTo(cx + halfW, cy - halfH * 0.18f)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(triPath, body)
    PathPool.release(triPath)
    PathPool.release(triPath)
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
    val path = PathPool.acquire().apply {
        moveTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    PathPool.release(path)
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
                glyph = boosterPreviewMapper.glyphFor(booster),
                iconDraw = { c -> drawBoosterPreview(c, booster) },
            )
        }
    }
}

// Stateless mapper hoisted to file scope so the 27 LazyColumn rows share a
// single instance instead of allocating per-recomposition. No fields means
// no leak surface.
private val boosterPreviewMapper = BoosterToBoosterUIMapper()

private fun boosterColor(b: BoosterType): Color =
    Color(boosterPreviewMapper.previewColorArgb(b))

/**
 * Bách Khoa Vật phẩm row icon. Delegates to the shared
 * [com.tranphuloi.neon.ui.game.world.drawBoosterShape] dispatcher so the
 * preview silhouette stays identical to the in-game pickup. The mapper
 * provides shape + color + glyph as a single source of truth — without it,
 * a dispatch on `drawableId` here would collapse 27 BoosterTypes onto the
 * 6 sprite shapes and ship visual duplicates.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBoosterPreview(
    canvasSize: androidx.compose.ui.geometry.Size,
    b: BoosterType,
) {
    val w = canvasSize.width
    val h = canvasSize.height
    val cx = w / 2
    val cy = h / 2
    val size = minOf(w, h) * 0.7f
    val color = boosterColor(b)
    // Soft halo background
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = size * 0.7f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
    val shape = boosterPreviewMapper.shapeFor(b)
    drawBoosterShape(shape, cx, cy, size, color)
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
    BoosterType.REGEN_BOOSTER -> "Hồi máu chậm"
    BoosterType.TIME_FREEZE_BOOSTER -> "Đóng băng thời gian"
    BoosterType.MINI_BOOSTER -> "Tàu nhỏ + nhanh"
    BoosterType.VAMPIRE_BOOSTER -> "Hút máu"
    BoosterType.GHOST_BOOSTER -> "Bóng ma"
    BoosterType.GRAVITY_BOOSTER -> "Hấp dẫn khoáng"
    BoosterType.REFLECT_BOOSTER -> "Phản xạ đạn"
    BoosterType.CHAIN_LIGHTNING_BOOSTER -> "Sét dây chuyền"
    BoosterType.CLONE_BOOSTER -> "Tàu phân thân"
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
    BoosterType.REGEN_BOOSTER ->
        "Hồi máu thụ động chậm: +1 HP/giây trong 30 giây.\nTốt cho người chơi cẩn thận."
    BoosterType.TIME_FREEZE_BOOSTER ->
        "Đóng băng toàn bộ enemy + đạn enemy trong 3 giây.\nCơ hội vàng để xả damage."
    BoosterType.MINI_BOOSTER ->
        "Tàu thu nhỏ 60% + tăng tốc 30%.\nHitbox bé hơn, né dễ hơn, kéo dài 12 giây."
    BoosterType.VAMPIRE_BOOSTER ->
        "Mỗi đòn đánh enemy hồi 50% damage thành máu.\nKéo dài 10 giây."
    BoosterType.GHOST_BOOSTER ->
        "Xuyên qua enemy (bỏ va chạm thân).\nVẫn nhận damage từ đạn enemy. Kéo dài 5 giây."
    BoosterType.GRAVITY_BOOSTER ->
        "Toàn bộ khoáng trên màn hình tự bay vào ship.\nNam châm × 100 lần, kéo dài 10 giây."
    BoosterType.REFLECT_BOOSTER ->
        "Hấp thụ đạn enemy + đánh trả 30 dmg vào enemy gần nhất.\nKéo dài 8 giây."
    BoosterType.CHAIN_LIGHTNING_BOOSTER ->
        "Mỗi đòn đánh tự lan sang 2 enemy gần nhất (50% damage).\nKéo dài 10 giây."
    BoosterType.CLONE_BOOSTER ->
        "Spawn tàu phân thân bên cạnh tàu chính, bắn cùng nhịp.\nDPS tăng gấp đôi. Kéo dài 8 giây."
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
    BoosterType.REGEN_BOOSTER -> "Nhặt sớm — hồi máu dài, không stack."
    BoosterType.TIME_FREEZE_BOOSTER -> "Hiếm — để dành cho boss hoặc combat dày."
    BoosterType.MINI_BOOSTER -> "Combat dày — hitbox nhỏ hơn dễ né."
    BoosterType.VAMPIRE_BOOSTER -> "Combo với damage cao — heal nhanh hơn."
    BoosterType.GHOST_BOOSTER -> "Nhặt khi bị enemy bao vây — drift xuyên qua."
    BoosterType.GRAVITY_BOOSTER -> "Stage có nhiều khoáng — bay vào trong tích tắc."
    BoosterType.REFLECT_BOOSTER -> "Combat dày đạn — chuyển phòng thủ thành tấn công."
    BoosterType.CHAIN_LIGHTNING_BOOSTER -> "Cụm enemy dày — clear nhanh bằng chain damage."
    BoosterType.CLONE_BOOSTER -> "Boss fight — combo với damage buffs để DPS tối đa."
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
    BoosterType.REGEN_BOOSTER -> "⏱ 30s · Refresh"
    BoosterType.TIME_FREEZE_BOOSTER -> "⏱ 3s · Refresh"
    BoosterType.MINI_BOOSTER -> "⏱ 12s · Refresh"
    BoosterType.VAMPIRE_BOOSTER -> "⏱ 10s · Refresh"
    BoosterType.GHOST_BOOSTER -> "⏱ 5s · Refresh"
    BoosterType.GRAVITY_BOOSTER -> "⏱ 10s · Refresh"
    BoosterType.REFLECT_BOOSTER -> "⏱ 8s · Refresh"
    BoosterType.CHAIN_LIGHTNING_BOOSTER -> "⏱ 10s · Refresh"
    BoosterType.CLONE_BOOSTER -> "⏱ 8s · Refresh"
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
    glyph: String?,
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
                // Mirror in-game's TopEnd glyph badge (GameWorld renders the
                // same overlay) so the preview matches gameplay 1:1.
                if (glyph != null) {
                    Text(
                        text = glyph,
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                    )
                }
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

// Round 79 (#1) — 4 boss preview helpers mirror in-game shapes for InfoScreen.

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossDeathMoonPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val r = minOf(w, h) * 0.42f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    val socketR = r * 0.18f
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        androidx.compose.ui.geometry.Offset(cx - r * 0.30f, cy - r * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        androidx.compose.ui.geometry.Offset(cx + r * 0.30f, cy - r * 0.15f))
    val teethCount = 4
    val teethY = cy + r * 0.40f
    val teethStartX = cx - r * 0.30f
    val teethWidth = r * 0.60f
    val teethStep = teethWidth / teethCount
    for (i in 0 until teethCount) {
        drawRect(Color.Black.copy(alpha = 0.70f),
            topLeft = androidx.compose.ui.geometry.Offset(teethStartX + i * teethStep + teethStep * 0.10f, teethY),
            size = androidx.compose.ui.geometry.Size(teethStep * 0.80f, r * 0.10f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossHauntedKidPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val headR = minOf(w, h) * 0.22f
    val bodyW = w * 0.50f
    val bodyH = h * 0.40f
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx - bodyW / 2f, cy)
        lineTo(cx - bodyW / 2f, cy + bodyH * 0.45f)
        val waveCount = 4
        val waveStep = bodyW / waveCount
        for (i in 0..waveCount) {
            val wx = cx - bodyW / 2f + i * waveStep
            val wy = if (i % 2 == 0) cy + bodyH * 0.55f else cy + bodyH * 0.40f
            lineTo(wx, wy)
        }
        lineTo(cx + bodyW / 2f, cy)
        close()
    }
    drawPath(bodyPath, body.copy(alpha = 0.85f))
    PathPool.release(bodyPath)
    drawCircle(body.copy(alpha = 0.85f), headR, androidx.compose.ui.geometry.Offset(cx, cy - bodyH * 0.20f))
    val eyeR = headR * 0.28f
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        androidx.compose.ui.geometry.Offset(cx - headR * 0.35f, cy - bodyH * 0.20f))
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        androidx.compose.ui.geometry.Offset(cx + headR * 0.35f, cy - bodyH * 0.20f))
    drawCircle(accent, eyeR * 0.40f,
        androidx.compose.ui.geometry.Offset(cx - headR * 0.35f, cy - bodyH * 0.20f))
    drawCircle(accent, eyeR * 0.40f,
        androidx.compose.ui.geometry.Offset(cx + headR * 0.35f, cy - bodyH * 0.20f))
    drawCircle(Color.Black.copy(alpha = 0.85f), headR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx, cy - bodyH * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossHellLordPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val headR = minOf(w, h) * 0.32f
    drawOval(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - headR, cy - headR * 0.80f),
        size = androidx.compose.ui.geometry.Size(headR * 2, headR * 1.80f))
    for (sign in intArrayOf(-1, 1)) {
        val baseX = cx + sign * headR * 0.65f
        val baseY = cy - headR * 0.60f
        val tipX = cx + sign * headR * 0.80f
        val tipY = cy - headR * 1.45f
        val hornPath = PathPool.acquire().apply {
            moveTo(baseX, baseY)
            cubicTo(cx + sign * headR * 1.0f, cy - headR * 1.20f, cx + sign * headR * 1.0f, cy - headR * 1.20f, tipX, tipY)
            cubicTo(cx + sign * headR * 0.85f, cy - headR * 1.10f, baseX + sign * headR * 0.10f, baseY - headR * 0.05f, baseX, baseY)
            close()
        }
        drawPath(hornPath, body)
        PathPool.release(hornPath)
    }
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - headR * 0.50f, cy - headR * 0.18f),
        size = androidx.compose.ui.geometry.Size(headR * 0.35f, headR * 0.16f))
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = androidx.compose.ui.geometry.Offset(cx + headR * 0.15f, cy - headR * 0.18f),
        size = androidx.compose.ui.geometry.Size(headR * 0.35f, headR * 0.16f))
    for (i in 0 until 3) {
        val fx = cx - headR * 0.20f + i * headR * 0.20f
        val mouthY = cy + headR * 0.35f
        val fpath = PathPool.acquire().apply {
            moveTo(fx - headR * 0.05f, mouthY)
            lineTo(fx + headR * 0.05f, mouthY)
            lineTo(fx, mouthY + headR * 0.15f)
            close()
        }
        drawPath(fpath, Color.White.copy(alpha = 0.85f))
        PathPool.release(fpath)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossSatanGlyphPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.42f
    val innerR = outerR * 0.40f
    drawCircle(body, outerR * 1.10f, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    val starPath = PathPool.acquire().apply {
        val rotation = Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, body)
    PathPool.release(starPath)
    val eyeRx = innerR * 0.85f
    val eyeRy = innerR * 0.55f
    drawOval(Color.White,
        topLeft = androidx.compose.ui.geometry.Offset(cx - eyeRx, cy - eyeRy),
        size = androidx.compose.ui.geometry.Size(eyeRx * 2, eyeRy * 2))
    drawCircle(accent, eyeRy * 0.65f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.Black, eyeRy * 0.35f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// Round 80 (#0) — 4 weird flying ship enemy preview helpers (mirror in-game).

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyUFOPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val saucerRx = w * 0.40f
    val saucerRy = h * 0.14f
    drawOval(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - saucerRx, cy + h * 0.08f - saucerRy),
        size = androidx.compose.ui.geometry.Size(saucerRx * 2, saucerRy * 2))
    val domeRx = saucerRx * 0.50f
    val domeRy = h * 0.20f
    drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = androidx.compose.ui.geometry.Offset(cx - domeRx, cy + h * 0.08f - saucerRy - domeRy),
        size = androidx.compose.ui.geometry.Size(domeRx * 2, domeRy * 2))
    drawCircle(Color.White.copy(alpha = 0.6f), domeRx * 0.40f,
        androidx.compose.ui.geometry.Offset(cx, cy + h * 0.08f - saucerRy - domeRy * 0.40f))
    val lightY = cy + h * 0.18f
    for (i in 0 until 5) {
        val lx = cx - saucerRx * 0.65f + i * saucerRx * 0.32f
        drawCircle(accent, w * 0.022f, androidx.compose.ui.geometry.Offset(lx, lightY))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyDronePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyW = w * 0.35f; val bodyH = h * 0.28f
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH))
    val armLen = w * 0.42f
    for (sign in intArrayOf(-1, 1)) for (vsign in intArrayOf(-1, 1)) {
        val ex = cx + sign * armLen * 0.85f
        val ey = cy + vsign * armLen * 0.65f
        drawLine(body,
            androidx.compose.ui.geometry.Offset(cx + sign * bodyW * 0.35f, cy + vsign * bodyH * 0.30f),
            androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = w * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(accent, w * 0.05f, androidx.compose.ui.geometry.Offset(ex, ey))
    }
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.035f, cy + bodyH / 2f),
        size = androidx.compose.ui.geometry.Size(w * 0.07f, h * 0.15f))
    drawCircle(Color(0xFFFF2D55), w * 0.045f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyMantaRayPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val wingPath = PathPool.acquire().apply {
        moveTo(cx, cy + h * 0.40f)
        cubicTo(cx + w * 0.18f, cy + h * 0.18f, cx + w * 0.40f, cy, cx + w * 0.45f, cy - h * 0.15f)
        cubicTo(cx + w * 0.28f, cy - h * 0.28f, cx + w * 0.10f, cy - h * 0.28f, cx, cy - h * 0.17f)
        cubicTo(cx - w * 0.10f, cy - h * 0.28f, cx - w * 0.28f, cy - h * 0.28f, cx - w * 0.45f, cy - h * 0.15f)
        cubicTo(cx - w * 0.40f, cy, cx - w * 0.18f, cy + h * 0.18f, cx, cy + h * 0.40f)
        close()
    }
    drawPath(wingPath, body)
    PathPool.release(wingPath)
    drawCircle(accent, w * 0.045f, androidx.compose.ui.geometry.Offset(cx - w * 0.07f, cy - h * 0.18f))
    drawCircle(accent, w * 0.045f, androidx.compose.ui.geometry.Offset(cx + w * 0.07f, cy - h * 0.18f))
    drawCircle(Color.White, w * 0.018f, androidx.compose.ui.geometry.Offset(cx - w * 0.07f, cy - h * 0.18f))
    drawCircle(Color.White, w * 0.018f, androidx.compose.ui.geometry.Offset(cx + w * 0.07f, cy - h * 0.18f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyMechPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyW = w * 0.45f; val bodyH = h * 0.50f
    val hullPath = PathPool.acquire().apply {
        moveTo(cx - bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW / 2f, cy + bodyH / 2f)
        lineTo(cx - bodyW / 2f, cy + bodyH / 2f)
        close()
    }
    drawPath(hullPath, body)
    PathPool.release(hullPath)
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - bodyW * 0.25f, cy - bodyH * 0.30f),
        size = androidx.compose.ui.geometry.Size(bodyW * 0.50f, bodyH * 0.15f))
    for (sign in intArrayOf(-1, 1)) {
        val cannonX = cx + sign * bodyW * 0.50f
        drawRect(body,
            topLeft = androidx.compose.ui.geometry.Offset(cannonX - w * 0.035f, cy - bodyH * 0.20f),
            size = androidx.compose.ui.geometry.Size(w * 0.07f, bodyH * 0.45f))
    }
    drawCircle(Color(0xFFFF2D55), w * 0.03f, androidx.compose.ui.geometry.Offset(cx, cy - bodyH * 0.70f))
}

// Round 81 — 12 new boss preview helpers (simplified InfoScreen icons).

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossHenMotherPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyRx = w * 0.30f; val bodyRy = h * 0.25f
    drawOval(body, topLeft = androidx.compose.ui.geometry.Offset(cx - bodyRx, cy - bodyRy * 0.5f),
        size = androidx.compose.ui.geometry.Size(bodyRx * 2, bodyRy * 2))
    val headR = w * 0.13f
    drawCircle(body, headR, androidx.compose.ui.geometry.Offset(cx - bodyRx * 0.85f, cy - bodyRy * 0.85f))
    for (i in 0 until 3) {
        drawCircle(Color(0xFFFF2D55), headR * 0.18f,
            androidx.compose.ui.geometry.Offset(cx - bodyRx * 0.95f + i * headR * 0.20f, cy - bodyRy * 1.05f - i * headR * 0.10f))
    }
    drawCircle(Color.Black, headR * 0.18f, androidx.compose.ui.geometry.Offset(cx - bodyRx * 0.95f, cy - bodyRy * 0.95f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossBuffaloPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val headRx = w * 0.30f; val headRy = h * 0.28f
    drawOval(body, topLeft = androidx.compose.ui.geometry.Offset(cx - headRx, cy - headRy),
        size = androidx.compose.ui.geometry.Size(headRx * 2, headRy * 2))
    for (side in intArrayOf(-1, 1)) {
        val baseX = cx + side * headRx * 0.85f
        val tipX = cx + side * headRx * 1.45f
        val hornPath = PathPool.acquire().apply {
            moveTo(baseX, cy - headRy * 0.50f)
            cubicTo(cx + side * headRx * 1.20f, cy - headRy * 0.80f,
                cx + side * headRx * 1.45f, cy - headRy * 0.90f, tipX, cy - headRy * 1.05f)
            cubicTo(cx + side * headRx * 1.25f, cy - headRy * 0.70f,
                cx + side * headRx * 1.05f, cy - headRy * 0.40f, baseX, cy - headRy * 0.50f)
            close()
        }
        drawPath(hornPath, body)
        PathPool.release(hornPath)
    }
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, androidx.compose.ui.geometry.Offset(cx - headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, androidx.compose.ui.geometry.Offset(cx + headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color(0xFFFFC020), headRx * 0.12f, androidx.compose.ui.geometry.Offset(cx, cy + headRy * 0.50f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.03f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossRatPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val headR = w * 0.28f
    drawCircle(body, headR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(body, headR * 0.55f, androidx.compose.ui.geometry.Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(body, headR * 0.55f, androidx.compose.ui.geometry.Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, androidx.compose.ui.geometry.Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, androidx.compose.ui.geometry.Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    drawCircle(Color(0xFFFF80B0), headR * 0.10f, androidx.compose.ui.geometry.Offset(cx, cy + headR * 0.30f))
    // Eyes white + black
    drawCircle(Color.White, headR * 0.18f, androidx.compose.ui.geometry.Offset(cx - headR * 0.28f, cy - headR * 0.10f))
    drawCircle(Color.White, headR * 0.18f, androidx.compose.ui.geometry.Offset(cx + headR * 0.28f, cy - headR * 0.10f))
    drawCircle(Color.Black, headR * 0.10f, androidx.compose.ui.geometry.Offset(cx - headR * 0.26f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.10f, androidx.compose.ui.geometry.Offset(cx + headR * 0.30f, cy - headR * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossTigerPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val headR = w * 0.32f
    // Mane
    for (i in 0 until 8) {
        val a = i * 2.0 * Math.PI / 8
        drawLine(body,
            androidx.compose.ui.geometry.Offset(cx + (headR * 0.95f * kotlin.math.cos(a)).toFloat(),
                cy + (headR * 0.95f * kotlin.math.sin(a)).toFloat()),
            androidx.compose.ui.geometry.Offset(cx + (headR * 1.20f * kotlin.math.cos(a)).toFloat(),
                cy + (headR * 1.20f * kotlin.math.sin(a)).toFloat()),
            strokeWidth = w * 0.035f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    drawCircle(body, headR, androidx.compose.ui.geometry.Offset(cx, cy))
    // Stripes
    for (i in 0 until 3) {
        drawArc(accent,
            startAngle = (50f + i * 30f), sweepAngle = 25f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - headR * 0.85f, cy - headR * 0.55f),
            size = androidx.compose.ui.geometry.Size(headR * 1.7f, headR * 1.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f))
    }
    drawCircle(Color(0xFFFFE040), headR * 0.16f, androidx.compose.ui.geometry.Offset(cx - headR * 0.30f, cy - headR * 0.05f))
    drawCircle(Color(0xFFFFE040), headR * 0.16f, androidx.compose.ui.geometry.Offset(cx + headR * 0.30f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, androidx.compose.ui.geometry.Offset(cx - headR * 0.30f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, androidx.compose.ui.geometry.Offset(cx + headR * 0.30f, cy - headR * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossDivaPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Hair sweeps
    for (side in intArrayOf(-1, 1)) {
        val hp = PathPool.acquire().apply {
            moveTo(cx + side * w * 0.10f, cy - h * 0.35f)
            cubicTo(cx + side * w * 0.35f, cy - h * 0.20f,
                cx + side * w * 0.40f, cy + h * 0.10f,
                cx + side * w * 0.35f, cy + h * 0.30f)
            lineTo(cx + side * w * 0.20f, cy)
            close()
        }
        drawPath(hp, body)
        PathPool.release(hp)
    }
    // Head
    drawCircle(body, w * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.20f))
    // Crown gem
    val gemPath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.42f)
        lineTo(cx + w * 0.06f, cy - h * 0.34f)
        lineTo(cx, cy - h * 0.28f)
        lineTo(cx - w * 0.06f, cy - h * 0.34f)
        close()
    }
    drawPath(gemPath, accent)
    PathPool.release(gemPath)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossTrollTowerPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val spirePath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.40f)
        lineTo(cx + w * 0.10f, cy - h * 0.25f)
        lineTo(cx + w * 0.15f, cy + h * 0.30f)
        lineTo(cx + w * 0.22f, cy + h * 0.42f)
        lineTo(cx - w * 0.22f, cy + h * 0.42f)
        lineTo(cx - w * 0.15f, cy + h * 0.30f)
        lineTo(cx - w * 0.10f, cy - h * 0.25f)
        close()
    }
    drawPath(spirePath, body)
    PathPool.release(spirePath)
    // Crown gems
    for (i in 0 until 4) {
        drawCircle(accent, w * 0.025f, androidx.compose.ui.geometry.Offset(cx - w * 0.07f + i * w * 0.045f, cy - h * 0.38f))
    }
    // Tip glow
    drawCircle(Color.White, w * 0.05f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.40f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossTwinSummitsPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val domeRx = w * 0.22f; val domeRy = h * 0.28f
    for (side in intArrayOf(-1, 1)) {
        val px = cx + side * w * 0.20f
        drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(px - domeRx, cy - domeRy * 0.5f),
            size = androidx.compose.ui.geometry.Size(domeRx * 2, domeRy * 2))
        drawCircle(accent, domeRx * 0.18f, androidx.compose.ui.geometry.Offset(px, cy - domeRy * 0.25f))
    }
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.40f, cy + domeRy * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.12f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossVoidGlobesPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val globeR = w * 0.24f
    drawCircle(body, globeR, androidx.compose.ui.geometry.Offset(cx - w * 0.18f, cy))
    drawCircle(body, globeR, androidx.compose.ui.geometry.Offset(cx + w * 0.18f, cy))
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f, androidx.compose.ui.geometry.Offset(cx - w * 0.25f, cy - h * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f, androidx.compose.ui.geometry.Offset(cx + w * 0.11f, cy - h * 0.05f))
    for (i in 0 until 3) {
        drawCircle(accent, w * 0.035f - i * w * 0.008f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.20f + i * h * 0.08f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossWhiteDragonPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx + w * 0.30f, cy + h * 0.40f)
        cubicTo(cx + w * 0.10f, cy + h * 0.25f, cx - w * 0.20f, cy + h * 0.10f, cx - w * 0.10f, cy - h * 0.10f)
        cubicTo(cx + w * 0.05f, cy - h * 0.25f, cx + w * 0.15f, cy - h * 0.30f, cx + w * 0.05f, cy - h * 0.38f)
        cubicTo(cx - w * 0.05f, cy - h * 0.28f, cx - w * 0.20f, cy - h * 0.18f, cx - w * 0.20f, cy + h * 0.05f)
        cubicTo(cx - w * 0.05f, cy + h * 0.20f, cx + w * 0.20f, cy + h * 0.35f, cx + w * 0.30f, cy + h * 0.40f)
        close()
    }
    drawPath(bodyPath, body)
    PathPool.release(bodyPath)
    drawCircle(accent, w * 0.025f, androidx.compose.ui.geometry.Offset(cx + w * 0.02f, cy - h * 0.34f))
    drawCircle(accent, w * 0.025f, androidx.compose.ui.geometry.Offset(cx + w * 0.10f, cy - h * 0.34f))
    // Fire breath
    for (i in 0 until 3) {
        val a = (-120.0 + i * 12) * Math.PI / 180.0
        drawLine(Color(0xFFFF6020),
            androidx.compose.ui.geometry.Offset(cx + w * 0.05f, cy - h * 0.28f),
            androidx.compose.ui.geometry.Offset(cx + w * 0.05f + (w * 0.20f * kotlin.math.cos(a)).toFloat(),
                cy - h * 0.28f + (w * 0.20f * kotlin.math.sin(a)).toFloat()),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossHammerSicklePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    drawCircle(body, w * 0.40f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, w * 0.40f, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f))
    // Sickle blade (simplified)
    drawArc(accent, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.25f, cy - h * 0.20f),
        size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.30f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.06f))
    // Hammer head
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx + w * 0.05f, cy - h * 0.05f),
        size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.10f))
    // Star top
    val starOuter = w * 0.08f; val starInner = starOuter * 0.42f
    val starCy = cy - h * 0.30f
    val starPath = PathPool.acquire().apply {
        val rot = -Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rot + i * Math.PI / 5
            val r = if (i % 2 == 0) starOuter else starInner
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = starCy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, accent)
    PathPool.release(starPath)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossMoneyTycoonPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    drawCircle(body, w * 0.32f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.05f))
    drawCircle(body, w * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.28f))
    // Top hat
    drawRect(Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.11f, cy - h * 0.48f),
        size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.14f))
    drawRect(Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.18f, cy - h * 0.38f),
        size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.04f))
    // $ on belly
    drawLine(accent,
        androidx.compose.ui.geometry.Offset(cx, cy - h * 0.05f),
        androidx.compose.ui.geometry.Offset(cx, cy + h * 0.18f),
        strokeWidth = w * 0.04f)
    drawArc(accent, startAngle = 60f, sweepAngle = 240f, useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.10f, cy - h * 0.02f),
        size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.10f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBossGoldenTycoonPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Head flesh
    drawCircle(body, w * 0.20f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.10f))
    // Orange hair sweep
    val hairPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.22f, cy - h * 0.20f)
        cubicTo(cx - w * 0.28f, cy - h * 0.38f, cx + w * 0.08f, cy - h * 0.48f, cx + w * 0.28f, cy - h * 0.30f)
        cubicTo(cx + w * 0.20f, cy - h * 0.26f, cx, cy - h * 0.25f, cx - w * 0.05f, cy - h * 0.22f)
        cubicTo(cx - w * 0.15f, cy - h * 0.25f, cx - w * 0.20f, cy - h * 0.22f, cx - w * 0.22f, cy - h * 0.20f)
        close()
    }
    drawPath(hairPath, Color(0xFFFF9520))
    PathPool.release(hairPath)
    // Golden tie
    val tiePath = PathPool.acquire().apply {
        moveTo(cx - w * 0.04f, cy + h * 0.10f)
        lineTo(cx + w * 0.04f, cy + h * 0.10f)
        lineTo(cx + w * 0.05f, cy + h * 0.32f)
        lineTo(cx, cy + h * 0.45f)
        lineTo(cx - w * 0.05f, cy + h * 0.32f)
        close()
    }
    drawPath(tiePath, accent)
    PathPool.release(tiePath)
    // Suit body
    val suitPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.15f, cy + h * 0.05f)
        lineTo(cx + w * 0.15f, cy + h * 0.05f)
        lineTo(cx + w * 0.30f, cy + h * 0.50f)
        lineTo(cx - w * 0.30f, cy + h * 0.50f)
        close()
    }
    drawPath(suitPath, Color(0xFF202040))
    PathPool.release(suitPath)
}

// ─────────────────────────────────────────────────────────────────────────
// Round 81 — 10 new enemy preview helpers (InfoScreen roster). Gameplay wire
// (new RegularEnemyType + drawable XMLs + Chapter spawn) defer R82.
// ─────────────────────────────────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemySpinningSawPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val outerR = minOf(w, h) * 0.42f
    val toothCount = 12
    val toothPath = PathPool.acquire().apply {
        for (i in 0 until toothCount * 2) {
            val a = -Math.PI / 2 + i * Math.PI / toothCount
            val r = if (i % 2 == 0) outerR else outerR * 0.78f
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(toothPath, body)
    PathPool.release(toothPath)
    drawCircle(accent, outerR * 0.40f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.Black, outerR * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyTentacleSquidPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2 - h * 0.10f
    val headR = w * 0.28f
    drawCircle(body, headR, androidx.compose.ui.geometry.Offset(cx, cy))
    // 6 tentacles wavy down
    for (i in 0 until 6) {
        val baseAngle = -90.0 + (i - 2.5) * 22.0
        val a = baseAngle * Math.PI / 180.0
        val bx = cx + (headR * kotlin.math.cos(a)).toFloat()
        val by = cy + (headR * kotlin.math.sin(a)).toFloat()
        val ex = bx + (headR * 0.85f * kotlin.math.cos(a)).toFloat()
        val ey = by + headR * 1.20f
        val tPath = PathPool.acquire().apply {
            moveTo(bx, by)
            cubicTo(bx + (i - 2.5).toFloat() * w * 0.05f, by + h * 0.10f,
                ex - (i - 2.5).toFloat() * w * 0.03f, ey - h * 0.05f, ex, ey)
        }
        drawPath(tPath, body, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.035f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round))
        PathPool.release(tPath)
    }
    drawCircle(accent, headR * 0.18f, androidx.compose.ui.geometry.Offset(cx - headR * 0.30f, cy))
    drawCircle(accent, headR * 0.18f, androidx.compose.ui.geometry.Offset(cx + headR * 0.30f, cy))
    drawCircle(Color.Black, headR * 0.08f, androidx.compose.ui.geometry.Offset(cx - headR * 0.30f, cy))
    drawCircle(Color.Black, headR * 0.08f, androidx.compose.ui.geometry.Offset(cx + headR * 0.30f, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyMineLayerPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    val bodyW = w * 0.60f; val bodyH = h * 0.35f
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH))
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.03f))
    // Tank turret
    drawCircle(body, w * 0.10f, androidx.compose.ui.geometry.Offset(cx, cy - bodyH * 0.40f))
    // Treads (2 rectangles below body)
    drawRect(Color(0xFF404040),
        topLeft = androidx.compose.ui.geometry.Offset(cx - bodyW * 0.55f, cy + bodyH * 0.45f),
        size = androidx.compose.ui.geometry.Size(bodyW * 1.10f, h * 0.10f))
    // Mines below (3 small circles)
    for (i in 0 until 3) {
        drawCircle(Color(0xFFFF2D55), w * 0.04f,
            androidx.compose.ui.geometry.Offset(cx - w * 0.20f + i * w * 0.20f, cy + h * 0.40f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyShieldDronePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Drone body (small hex)
    val bodyR = w * 0.18f
    val hexPath = PathPool.acquire().apply {
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            val x = cx + (bodyR * kotlin.math.cos(a)).toFloat()
            val y = cy + (bodyR * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(hexPath, body)
    PathPool.release(hexPath)
    // Front shield arc (large semicircle in front)
    drawArc(accent.copy(alpha = 0.55f),
        startAngle = 30f, sweepAngle = 120f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.40f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    drawArc(Color.White.copy(alpha = 0.30f),
        startAngle = 30f, sweepAngle = 120f, useCenter = true,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.40f))
    // Center eye
    drawCircle(Color(0xFFFF2D55), bodyR * 0.4f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemySniperPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Long rifle barrel (vertical, points down)
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy - h * 0.40f),
        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.80f))
    // Scope (rectangle attached to top)
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.10f, cy - h * 0.20f),
        size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.10f))
    drawCircle(Color.Red, w * 0.025f, androidx.compose.ui.geometry.Offset(cx + w * 0.07f, cy - h * 0.15f))
    // Stock (horizontal rectangle at top)
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.15f, cy + h * 0.30f),
        size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.10f))
    // Muzzle flash (bottom)
    drawCircle(Color(0xFFFFE040), w * 0.06f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.42f))
    drawCircle(Color.White, w * 0.025f, androidx.compose.ui.geometry.Offset(cx, cy - h * 0.42f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyBomberCrawlerPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Wide tank body
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.30f))
    drawRect(accent,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.30f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f))
    // Mortar tube (angled up-right)
    drawLine(body,
        androidx.compose.ui.geometry.Offset(cx, cy - h * 0.05f),
        androidx.compose.ui.geometry.Offset(cx + w * 0.20f, cy - h * 0.35f),
        strokeWidth = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Wheels (4 small circles)
    for (i in 0 until 4) {
        drawCircle(Color(0xFF404040), h * 0.06f,
            androidx.compose.ui.geometry.Offset(cx - w * 0.30f + i * w * 0.20f, cy + h * 0.18f))
    }
    // Shell trajectory dot
    drawCircle(Color(0xFFFFE040), w * 0.03f, androidx.compose.ui.geometry.Offset(cx + w * 0.22f, cy - h * 0.38f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyMirrorTwinPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // 2 identical small ships side-by-side
    for (sign in intArrayOf(-1, 1)) {
        val px = cx + sign * w * 0.18f
        val triPath = PathPool.acquire().apply {
            moveTo(px, cy - h * 0.20f)
            lineTo(px + w * 0.10f, cy + h * 0.20f)
            lineTo(px - w * 0.10f, cy + h * 0.20f)
            close()
        }
        drawPath(triPath, body)
        PathPool.release(triPath)
        drawCircle(accent, w * 0.04f, androidx.compose.ui.geometry.Offset(px, cy))
    }
    // Connecting energy line between
    drawLine(accent,
        androidx.compose.ui.geometry.Offset(cx - w * 0.10f, cy),
        androidx.compose.ui.geometry.Offset(cx + w * 0.10f, cy),
        strokeWidth = w * 0.015f)
    // Equals sign indicating linkage
    drawLine(accent, androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy - h * 0.05f),
        androidx.compose.ui.geometry.Offset(cx + w * 0.05f, cy - h * 0.05f), strokeWidth = w * 0.01f)
    drawLine(accent, androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy + h * 0.05f),
        androidx.compose.ui.geometry.Offset(cx + w * 0.05f, cy + h * 0.05f), strokeWidth = w * 0.01f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyPhantomPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Ghost body (bell shape with wavy bottom)
    val ghostPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.30f, cy - h * 0.20f)
        cubicTo(cx - w * 0.35f, cy - h * 0.45f, cx + w * 0.35f, cy - h * 0.45f, cx + w * 0.30f, cy - h * 0.20f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        // Wavy bottom
        val waveCount = 5
        val waveStep = w * 0.60f / waveCount
        for (i in 0..waveCount) {
            val wx = cx + w * 0.30f - i * waveStep
            val wy = if (i % 2 == 0) cy + h * 0.40f else cy + h * 0.30f
            lineTo(wx, wy)
        }
        close()
    }
    // Semi-transparent ghost
    drawPath(ghostPath, body.copy(alpha = 0.55f))
    drawPath(ghostPath, accent.copy(alpha = 0.85f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.025f))
    PathPool.release(ghostPath)
    // Eyes (2 dark sockets)
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, androidx.compose.ui.geometry.Offset(cx - w * 0.10f, cy - h * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, androidx.compose.ui.geometry.Offset(cx + w * 0.10f, cy - h * 0.15f))
    // O-shaped mouth
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.04f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.05f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.015f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyHealerPreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Healing aura ring (large)
    drawCircle(Color(0xFF60FFAA).copy(alpha = 0.25f), w * 0.42f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color(0xFF60FFAA), w * 0.42f, androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.02f))
    // Inner orb
    drawCircle(body, w * 0.18f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), w * 0.10f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Plus symbol (healing)
    val armW = w * 0.04f
    val armL = w * 0.20f
    drawRect(Color(0xFF60FFAA),
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2f, cy - armL / 2f),
        size = androidx.compose.ui.geometry.Size(armW, armL))
    drawRect(Color(0xFF60FFAA),
        topLeft = androidx.compose.ui.geometry.Offset(cx - armL / 2f, cy - armW / 2f),
        size = androidx.compose.ui.geometry.Size(armL, armW))
    // 4 small healing particles around
    for (i in 0 until 4) {
        val a = i * Math.PI / 2
        drawCircle(Color(0xFF60FFAA), w * 0.025f,
            androidx.compose.ui.geometry.Offset(
                cx + (w * 0.32f * kotlin.math.cos(a)).toFloat(),
                cy + (w * 0.32f * kotlin.math.sin(a)).toFloat()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnemyKamikazePreview(
    canvasSize: androidx.compose.ui.geometry.Size, body: Color, accent: Color,
) {
    val w = canvasSize.width; val h = canvasSize.height
    val cx = w / 2; val cy = h / 2
    // Triangular body (warning pointed)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.30f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        lineTo(cx - w * 0.30f, cy + h * 0.30f)
        close()
    }
    drawPath(bodyPath, Color(0xFFFFE040))
    drawPath(bodyPath, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.04f))
    PathPool.release(bodyPath)
    // Warning exclamation in center
    drawRect(Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.02f, cy - h * 0.10f),
        size = androidx.compose.ui.geometry.Size(w * 0.04f, h * 0.20f))
    drawCircle(Color.Black, w * 0.025f, androidx.compose.ui.geometry.Offset(cx, cy + h * 0.18f))
    // Trail behind (explosion warning lines)
    for (i in 0 until 3) {
        val sy = cy + h * 0.35f + i * h * 0.05f
        drawLine(Color(0xFFFF6020),
            androidx.compose.ui.geometry.Offset(cx - w * 0.20f + i * w * 0.05f, sy),
            androidx.compose.ui.geometry.Offset(cx + w * 0.20f - i * w * 0.05f, sy),
            strokeWidth = w * 0.02f)
    }
}
