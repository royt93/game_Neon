package com.tranphuloi.neon.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.ShipSkin
import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import kotlinx.coroutines.delay

/**
 * Wave 11c — Statistics screen ("THỐNG KÊ").
 * Wave 11d Bug #6 polish — edge-to-edge insets, (✕) close icon, staggered
 * animations for section reveal + bar fill.
 *
 * Reads MetaProgressionRepository's 4 aggregate snapshot Flows + lifetime
 * counters. Renders 5 sections:
 *   1) Lifetime totals header
 *   2) Bullet kill ranking (bar chart, animated fill)
 *   3) Boss kill grid (21 BossKind, 3-column)
 *   4) Ship time breakdown
 *   5) Rank distribution
 *
 * Animations:
 *   - Sections fade + slide-in staggered by 80ms each
 *   - Bar widths animate from 0 → target on appear (tween 800ms FastOutSlowIn)
 *   - Title has infinite pulse glow (radius oscillates, 2.5s period)
 *   - (✕) close button has subtle rotation on press (handled by clickable ripple)
 *
 * Bullet attribution disclaimer notes that the last-hit bullet is used (precise)
 * or ship.activeBulletType for kill sources that bypass onLaserHit
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

    // Bug #6 — staggered reveal. Each section flips visible at increasing
    // delays so the screen "builds up" instead of dumping everything at once.
    var revealStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..6) {
            delay(80L)
            revealStep = i
        }
    }

    // Audit-Pixel-2 #1 fix — title pulse moved into NeonActionBar (shared).

    val insetsPad = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NeonBgEdge, NeonBgMid, NeonBgDeep))
            ),
    ) {
        // Pixel-3 #3 — shared decorative starfield background. Animated 60-star
        // 3-layer parallax + twinkle (extracted from MenuScreen). Sits below
        // gradient, above device background. Doesn't consume pointer events.
        com.tranphuloi.neon.common.NeonStarfieldBackground(
            modifier = Modifier.fillMaxSize(),
        )
        // Pixel-3 #2 fix — sticky action bar. Outer Column holds bar (fixed)
        // + scrollable content (verticalScroll only on inner column). Pre-fix
        // the action bar scrolled with content; user had to scroll back up to
        // tap (✕).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetsPad),
        ) {
            com.tranphuloi.neon.common.NeonActionBar(
                title = "Thống kê",
                titleColor = NeonGold,
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StaggeredSection(visible = revealStep >= 1) {
                StatsCard(title = "Tổng cộng", accentColor = NeonGold) {
                    StatRow("Quái thường tiêu diệt", lifetimeEnemies.toString())
                    StatRow("Boss đã hạ", bossTotal.toString())
                    StatRow("Khoáng sản tích lũy", lifetimeMinerals.toString())
                    StatRow("Tổng thời gian chơi", formatMillis(timeTotal))
                }
            }

            StaggeredSection(visible = revealStep >= 2) {
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
            }

            StaggeredSection(visible = revealStep >= 3) {
                StatsCard(title = "Boss đã hạ — phân loại", accentColor = NeonMagenta) {
                    if (bossTotal == 0) {
                        EmptyHint()
                    } else {
                        BossKindGrid(bossKills)
                    }
                }
            }

            StaggeredSection(visible = revealStep >= 4) {
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
            }

            StaggeredSection(visible = revealStep >= 5) {
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
            }

            StaggeredSection(visible = revealStep >= 6) {
                Text(
                    text = "Ghi chú: thuộc tính đạn dùng cú đánh gây sát thương cuối; vũ khí phụ / REFLECT / CHAIN_LIGHTNING tính theo đạn đang kích hoạt.",
                    style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
                Spacer(Modifier.height(8.dp))
            }       // inner scrollable Column close
        }           // outer Column close (sticky bar + content)
    }               // Box close
}

@Composable
private fun StaggeredSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 4 },
            ),
    ) {
        content()
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
                fontSize = 15.sp,
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
private fun AnimatedBar(targetPct: Float, color: Color) {
    // Bug #6 animation — bar fills from 0% to actual pct on appear.
    val animPct by animateFloatAsState(
        targetValue = targetPct,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar-fill",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF1A2030)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animPct)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.7f)),
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
            // Wave 18 — tên Việt (displayName) thay tên enum thô (user: "thô kệch").
            text = type.displayName,
            style = TextStyle(color = Color.White, fontSize = 12.sp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            AnimatedBar(targetPct = pct, color = NeonCyan)
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
                BossKindCell(kind = kind, count = count, modifier = Modifier.weight(1f))
            }
            repeat(3 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BossKindCell(kind: BossKind, count: Int, modifier: Modifier) {
    // Bug #6 animation — defeated bosses pulse subtly so the eye is drawn to
    // milestones. Undefeated cells stay flat.
    //
    // Audit P1 fix — rememberInfiniteTransition is now hoisted UNCONDITIONALLY
    // (was inside an `if (count > 0) { ... }` block, which violated Compose
    // rules-of-hooks: when a boss kind is killed for the first time during a
    // live session, the cell flips count 0→1 and the remember count grows by
    // one, risking IllegalStateException / animation glitch on slot-table
    // diff). Animation is always running; we just suppress its visible effect
    // by gating the alpha consumption below.
    //
    // Audit-2 Risk #3 note — battery cost: up to 21 cells × 1 infinite
    // animation each = 21 always-running InfiniteTransitions when Stats is
    // foregrounded. Compose batches its frame callbacks so the dispatcher
    // cost is one Choreographer tick that updates all 21 floats in lockstep
    // (AndroidUiDispatcher.MonotonicFrameClock single postFrameCallback).
    // HOWEVER each cell still RECOMPOSES per frame because each reads its
    // own `animatedPulse` State<Float> — at 60Hz × 21 cells = ~1260 cell
    // recompositions/sec while Stats is foregrounded.
    //
    // Audit-3 #3 note — how to profile if you want to verify before
    // optimizing:
    //   adb shell dumpsys gfxinfo com.tranphuloi.neon framestats
    //   # foreground game, navigate Menu → THỐNG KÊ, leave open 30s
    //   # check "Frame Stats since: ..." for jank rate.
    //   # Or use Android Studio Profiler > CPU > Trace System Calls
    //   # while the Stats screen is foregrounded.
    //
    // If measured cost is non-trivial (e.g., >5% CPU sustained when
    // foregrounded on Pixel 3a / mid-tier device): switch to a single
    // shared InfiniteTransition at the StatsScreen level + cell-level
    // `derivedStateOf` read — that keeps hooks count stable AND moves
    // 21 transitions to 1 (cells still recompose, but only when the
    // derived value crosses a threshold).
    //
    // Decision today: accept cost. Stats is a transient screen (user taps
    // "✕" to leave); per-session foreground time is bounded.
    val it = rememberInfiniteTransition(label = "boss-pulse-${kind.name}")
    val animatedPulse by it.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha-pulse",
    )
    val pulse = if (count > 0) animatedPulse else 0f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (count > 0) NeonMagenta.copy(alpha = pulse)
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
        Box(modifier = Modifier.weight(1f)) {
            AnimatedBar(targetPct = pct, color = skinColor)
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
            modifier = Modifier
                .width(28.dp)
                .graphicsLayer { alpha = if (count > 0) 1f else 0.4f },
        )
        Box(modifier = Modifier.weight(1f)) {
            AnimatedBar(targetPct = pct, color = rankColor)
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
