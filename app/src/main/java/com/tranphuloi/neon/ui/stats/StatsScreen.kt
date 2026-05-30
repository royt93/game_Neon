package com.tranphuloi.neon.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.ShipSkin
import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType

/**
 * Wave 11c — Statistics screen ("THỐNG KÊ").
 *
 * Reads MetaProgressionRepository's 4 aggregate snapshot Flows + lifetime
 * counters. Renders 5 sections:
 *   1) Lifetime totals header (enemy kills + bosses defeated rollup + minerals)
 *   2) Bullet kill ranking (12 BulletType, bar chart by frequency)
 *   3) Boss kill grid (21 BossKind, 3-column display name + count)
 *   4) Ship time breakdown (5 ShipSkin, percentage of total time-played)
 *   5) Rank distribution (S/A/B/C/D bar chart, color-coded per BossRank)
 *
 * All data is read-only — no writes. Idempotent across re-entries.
 *
 * Disclaimer banner notes that bullet attribution uses the last-hit bullet
 * (precise) OR ship.activeBulletType for kill sources that bypass onLaserHit
 * (SmartBomb/REFLECT/CHAIN/secondary). Pre-Wave-11b users see 0 counts.
 */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val meta = LocalMetaProgression.current
    val lifetimeEnemies by meta.lifetimeEnemyKills.collectAsState(initial = 0L)
    val lifetimeMinerals by meta.lifetimeMinerals.collectAsState(initial = 0)
    val bulletKills by meta.allBulletKills.collectAsState(initial = emptyMap())
    val bossKills by meta.allBossKills.collectAsState(initial = emptyMap())
    val shipTime by meta.allShipTimeMillis.collectAsState(initial = emptyMap())
    val rankCounts by meta.allRankCounts.collectAsState(initial = emptyMap())

    val bossTotal = bossKills.values.sum()
    val bulletTotal = bulletKills.values.sum()
    val timeTotal = shipTime.values.sum()
    val rankTotal = rankCounts.values.sum()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NeonBgEdge, NeonBgMid, NeonBgDeep))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Text(
                text = "THỐNG KÊ",
                style = TextStyle(
                    color = NeonGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                ),
                modifier = Modifier.padding(top = 8.dp),
            )

            // Section 1 — Lifetime totals
            StatsCard(title = "Tổng cộng", accentColor = NeonGold) {
                StatRow("Quái thường tiêu diệt", lifetimeEnemies.toString())
                StatRow("Boss đã hạ", bossTotal.toString())
                StatRow("Khoáng sản tích lũy", lifetimeMinerals.toString())
                StatRow("Tổng thời gian chơi", formatMillis(timeTotal))
            }

            // Section 2 — Bullet kills (sorted desc)
            StatsCard(title = "Đạn — số quái diệt", accentColor = NeonCyan) {
                if (bulletTotal == 0) {
                    EmptyHint()
                } else {
                    val sorted = bulletKills.entries.sortedByDescending { it.value }
                    sorted.forEach { (type, count) ->
                        BulletBar(type, count, bulletTotal)
                    }
                }
            }

            // Section 3 — Boss kills (3-col grid by enum order)
            StatsCard(title = "Boss đã hạ — phân loại", accentColor = NeonMagenta) {
                if (bossTotal == 0) {
                    EmptyHint()
                } else {
                    BossKindGrid(bossKills)
                }
            }

            // Section 4 — Ship time per skin
            StatsCard(title = "Thời gian theo tàu", accentColor = NeonViolet) {
                if (timeTotal == 0L) {
                    EmptyHint()
                } else {
                    ShipSkin.entries.forEach { skin ->
                        val ms = shipTime[skin] ?: 0L
                        ShipTimeBar(skin, ms, timeTotal)
                    }
                }
            }

            // Section 5 — Rank distribution
            StatsCard(title = "Phân bố hạng boss", accentColor = NeonGold) {
                if (rankTotal == 0) {
                    EmptyHint()
                } else {
                    BossRank.entries.forEach { rank ->
                        val count = rankCounts[rank] ?: 0
                        RankBar(rank, count, rankTotal)
                    }
                }
            }

            // Disclaimer + back
            Text(
                text = "Ghi chú: thuộc tính đạn dùng cú đánh gây sát thương cuối; vũ khí phụ / REFLECT / CHAIN_LIGHTNING tính theo đạn đang kích hoạt.",
                style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .clickable { onBack() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "← QUAY LẠI",
                    style = TextStyle(color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1320))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        content()
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(color = Color(0xFFB0C0D0), fontSize = 13.sp),
        )
        Text(
            text = value,
            style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun BulletBar(type: BulletType, count: Int, total: Int) {
    val pct = if (total == 0) 0f else count.toFloat() / total.toFloat()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type.name,
            style = TextStyle(color = Color.White, fontSize = 12.sp),
            modifier = Modifier.width(96.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF1A2030)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(NeonCyan.copy(alpha = 0.7f)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = TextStyle(color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
private fun BossKindGrid(kills: Map<BossKind, Int>) {
    val rows = BossKind.entries.chunked(3)
    rows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { kind ->
                val count = kills[kind] ?: 0
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (count > 0) NeonMagenta.copy(alpha = 0.20f)
                            else Color(0xFF161C28)
                        )
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = kind.displayName,
                        style = TextStyle(
                            color = if (count > 0) Color.White else Color(0xFF606878),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2,
                    )
                    Text(
                        text = count.toString(),
                        style = TextStyle(
                            color = if (count > 0) NeonMagenta else Color(0xFF505868),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                }
            }
            // Pad incomplete row to 3 cells
            repeat(3 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ShipTimeBar(skin: ShipSkin, millis: Long, total: Long) {
    val pct = if (total == 0L) 0f else millis.toFloat() / total.toFloat()
    val skinColor = Color(skin.glowColorHex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = skin.displayName,
            style = TextStyle(color = Color.White, fontSize = 12.sp),
            modifier = Modifier.width(72.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF1A2030)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(skinColor.copy(alpha = 0.7f)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatMillis(millis),
            style = TextStyle(color = skinColor, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(64.dp),
        )
    }
}

@Composable
private fun RankBar(rank: BossRank, count: Int, total: Int) {
    val pct = if (total == 0) 0f else count.toFloat() / total.toFloat()
    val rankColor = rank.color()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.letter,
            style = TextStyle(
                color = rankColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            ),
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF1A2030)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(rankColor.copy(alpha = 0.7f)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = TextStyle(color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = "Chưa có dữ liệu — hãy chơi vài lượt!",
        style = TextStyle(color = Color(0xFF707888), fontSize = 12.sp),
    )
}

private fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
