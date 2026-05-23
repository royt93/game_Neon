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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBgDeep)
            // Round 67.6 — statusBars padding only (not systemBars) so the
            // bottom is flush + the top respects status bar when visible.
            .windowInsetsPadding(WindowInsets.statusBars),
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
            // Content with bottom-padded so last item not flush against nav bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
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
}

private fun bulletDescription(b: BulletType): String = when (b) {
    BulletType.NORMAL -> "Đạn cơ bản. Tốc độ 7 px/tick. Không buff khởi đầu."
    BulletType.PIERCING -> "Xuyên qua tối đa 3 enemy trước khi tiêu hủy. Common 3 / Rare 4 / Epic 5."
    BulletType.PLASMA -> "Đạn lớn với AoE explosion 80px khi trúng. Common 80 / Rare 110 / Epic 140."
    BulletType.FIRE -> "Áp dụng BURN status 100% trên mỗi hit. Enemy nhận 5HP/sec damage trong 3 giây sau."
    BulletType.HOMING -> "Tự đuổi theo enemy gần nhất mỗi tick (MissileLaser pattern)."
    BulletType.BOUNCE -> "Phản xạ off cạnh màn hình. Mỗi viên đạn hit tối đa 3 enemy trước khi tiêu hủy."
    BulletType.GIANT -> "Kích thước ×2 + damage ×2. Không cần charge-up."
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBulletCapsule(
    canvasSize: androidx.compose.ui.geometry.Size,
    bullet: BulletType,
) {
    val color = when (bullet) {
        BulletType.NORMAL -> NeonCyan
        BulletType.PIERCING -> Color(0xFFFF2DE0)
        BulletType.PLASMA -> Color(0xFF00F0FF)
        BulletType.FIRE -> Color(0xFFFF6020)
        BulletType.HOMING -> Color(0xFFFF40A0)
        BulletType.BOUNCE -> Color(0xFF40FFD0)
        BulletType.GIANT -> Color(0xFFFFD040)
    }
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val w = if (bullet == BulletType.GIANT) canvasSize.width * 0.5f
        else if (bullet == BulletType.PLASMA) canvasSize.width * 0.65f
        else canvasSize.width * 0.25f
    val h = canvasSize.height * 0.7f
    // Glow halo
    drawCircle(
        color = color.copy(alpha = 0.4f),
        radius = canvasSize.width * 0.4f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
    // Capsule body
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 2, cy - h / 2),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2),
    )
    // White-hot core
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = androidx.compose.ui.geometry.Offset(cx - w / 4, cy - h / 2 + h * 0.1f),
        size = androidx.compose.ui.geometry.Size(w / 2, h * 0.8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 4),
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Ship tab — uses drawShipVector preview.
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ShipTab() {
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        InfoCard(
            color = NeonCyan,
            title = "Fighter (mặc định)",
            subtitle = "HP base 1000 · speed base 2.0 px/tick",
            description = "Phi thuyền chính. Vector arrow body + wings (rộng hơn khi LASER_BOOSTER) + " +
                "cockpit + engine glow. Color = ship-skin setting.",
            iconDraw = { c -> drawShipPreview(c, NeonCyan, laserBoosted = false) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonGold,
            title = "Boosted ship (LASER active)",
            subtitle = "Wings rộng hơn khi LASER_BOOSTER pickup",
            description = "Hình dạng wings ×1.2 khi laser-booster active 15s. Visual cue.",
            iconDraw = { c -> drawShipPreview(c, NeonGold, laserBoosted = true) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonMagenta,
            title = "5 Ship Skins",
            subtitle = "Settings → Skin tàu",
            description = "Cyan / Gold / Magenta / Violet / Đỏ. Đổi màu aura + glow của ship + lasers.",
            iconDraw = { c ->
                // Show 5 mini-rings of skin colors.
                val colors = listOf(Color(0xFF00F0FF), Color(0xFFFFCB47), Color(0xFFFF2DE0),
                    Color(0xFFB14CFF), Color(0xFFFF2D55))
                val r = c.width * 0.10f
                val gap = c.width * 0.06f
                val totalW = colors.size * (2 * r) + (colors.size - 1) * gap
                val startX = (c.width - totalW) / 2 + r
                colors.forEachIndexed { i, col ->
                    drawCircle(
                        color = col,
                        radius = r,
                        center = androidx.compose.ui.geometry.Offset(
                            startX + i * (2 * r + gap), c.height / 2,
                        ),
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonViolet,
            title = "Stat upgrades (Wave 8)",
            subtitle = "Chưa implement",
            description = "Roadmap: 5 ship shape variants (Fighter/Bomber/Stealth/Tank/Interceptor) " +
                "+ 5 stat tracks (HP/Damage/Speed/Magnet/Crit) × 5 levels. Mua bằng lifetime minerals.",
            iconDraw = { c ->
                // Placeholder — neutral diamond.
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.width / 2, c.height * 0.1f)
                    lineTo(c.width * 0.9f, c.height / 2)
                    lineTo(c.width / 2, c.height * 0.9f)
                    lineTo(c.width * 0.1f, c.height / 2)
                    close()
                }
                drawPath(path, NeonViolet.copy(alpha = 0.7f))
            },
        )
    }
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
            title = "Status Effects",
            subtitle = "BURN / SLOW / STUN",
            description = "BURN — 5HP/sec DoT (cam). SLOW — movement ×0.5 (cyan). " +
                "STUN — stop firing 2s (vàng). 10% per hit (5% on boss). FIRE bullet luôn apply BURN 100%.",
            iconDraw = { c ->
                // 3 colored circles representing the 3 status effects.
                val cy = c.height / 2
                val r = c.height * 0.13f
                drawCircle(Color(0xFFFF6020), r, androidx.compose.ui.geometry.Offset(c.width * 0.25f, cy))
                drawCircle(Color(0xFF00F0FF), r, androidx.compose.ui.geometry.Offset(c.width * 0.50f, cy))
                drawCircle(Color(0xFFFFD040), r, androidx.compose.ui.geometry.Offset(c.width * 0.75f, cy))
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
    Column(modifier = Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        InfoCard(
            color = NeonRedAlert,
            title = "Mid Bosses (3 variants)",
            subtitle = "Giữa Chapter 1-4 · HP 1200",
            description = "OFFENSIVE — 360° spread (Ch 1+4). DEFENSIVE — orbit + counter (Ch 2). " +
                "SWARM — spawn 4 drones (Ch 3). Phase 2 kích hoạt khi HP < 50%.",
            iconDraw = { c -> drawMidBoss(c, NeonRedAlert, Color(0xFFCC1144)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonRedAlert,
            title = "LevelOneBoss",
            subtitle = "End Ch 1 (Vành Đai Tiểu HT) + Ch 3 (HT Băng) · HP 3000",
            description = "Star + inner hex core. Có HP bar riêng + intro cinematic + buff picker " +
                "post-kill. Ch 3 reuse cùng model với palette change.",
            iconDraw = { c -> drawBossStar(c, Color(0xFFFF5555), Color(0xFFCC1144)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonRedAlert,
            title = "LevelTwoBoss",
            subtitle = "End Ch 2 (Mây Tinh Vân) + Ch 4 (Trạm Thù Địch) · HP 4000",
            description = "Mid-game boss. Pattern attacks phức tạp hơn LevelOne.",
            iconDraw = { c -> drawBossStar(c, Color(0xFF6EFFAA), Color(0xFF24B86E)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard(
            color = NeonRedAlert,
            title = "FinalBoss",
            subtitle = "End Ch 5 (Lõi Thiên Hà) · HP 22500 · 3-phase",
            description = "Phase 1 — fire pattern thông thường. Phase 2 (HP ≤ 15000) — tăng tốc độ. " +
                "Phase 3 (HP ≤ 7500) — ring barrage 360°. Victory ending khác theo difficulty.",
            iconDraw = { c -> drawBossStar(c, NeonMagenta, Color(0xFFCC1144)) },
        )
    }
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(BoosterType.entries) { booster ->
            InfoCard(
                color = boosterColor(booster),
                title = booster.name,
                subtitle = "Weight ${booster.weight}",
                description = boosterDescription(booster),
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

private fun boosterDescription(b: BoosterType): String = when (b) {
    BoosterType.HEALTH_BOOSTER -> "+100 HP × rarity (1.0/1.5/2.0)."
    BoosterType.SHIELD_BOOSTER -> "Khiên 10s × rarity. Chặn mọi damage."
    BoosterType.LASER_BOOSTER -> "Boosted laser 15s × rarity."
    BoosterType.TRIPLE_LASER_BOOSTER -> "Bắn 3 đạn cùng lúc 20s × rarity."
    BoosterType.ULTIMATE_WEAPON_BOOSTER -> "Quét 9 beams (one-shot)."
    BoosterType.REVIVE_TOKEN -> "Auto-revive 1 lần khi HP=0. Restore 300 HP + 1.5s i-frames."
    BoosterType.PIERCING_BOOSTER -> "Bullet PIERCING 10s. Xuyên 3/4/5 enemy theo rarity."
    BoosterType.PLASMA_BOOSTER -> "Bullet PLASMA 10s. AoE 80/110/140px theo rarity."
    BoosterType.MAGNET_BOOST -> "Magnet radius ×2 trong 15s."
    BoosterType.CRIT_SURGE -> "Damage ×3 trong 8s."
    BoosterType.SPREAD_SHOT -> "Bắn 5-way fan trong 10s."
    BoosterType.BERSERK -> "Damage ×2 + take ×1.5 damage trong 12s."
    BoosterType.PHASE_SHIELD -> "Extended i-frames + ghost visual ring 5s."
    BoosterType.SCORE_X3 -> "Minerals earned ×3 trong 15s."
    BoosterType.QUICK_HEAL -> "+250 HP × rarity (one-shot)."
    BoosterType.MINERAL_SUPERCHARGE -> "Instant collect tất cả minerals + bonus +5/each."
    BoosterType.HEALING_AURA -> "+5 HP/sec regen trong 10s."
    BoosterType.DOUBLE_FIRE -> "Fire 2 lasers per shot stacked trong 10s."
    BoosterType.FIRE_BOOSTER -> "Bullet FIRE 10s. Apply BURN status 100% per hit."
    BoosterType.HOMING_BOOSTER -> "Bullet HOMING 10s. Đuổi theo enemy gần nhất."
    BoosterType.BOUNCE_BOOSTER -> "Bullet BOUNCE 12s. Ricochet off edges, 3 hits per laser."
    BoosterType.GIANT_BOOSTER -> "Bullet GIANT 10s. ×2 size + ×2 damage."
}

// ─────────────────────────────────────────────────────────────────────────
// Shared card
// ─────────────────────────────────────────────────────────────────────────

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
