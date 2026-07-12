package com.tranphuloi.neon.ui.dlg.gameover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonDialog
import com.tranphuloi.neon.common.NeonDialogButton
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LeaderboardEntry
import com.tranphuloi.neon.data.LocalLeaderboard
import com.tranphuloi.neon.data.LocalRunStats
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DialogGameOver(
    score: String,
    onRestartGame: () -> Unit,
    onBackToMenu: () -> Unit = {},
) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val leaderboard = LocalLeaderboard.current
    val meta = com.tranphuloi.neon.data.LocalMetaProgression.current
    val runPersistence = com.tranphuloi.neon.data.LocalRunPersistence.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val entries by leaderboard.topEntries.collectAsState(initial = emptyList())
    // 17c Daily challenge — separate top-N for today's UTC day key, displayed
    // alongside the all-time list.
    val todayKey = remember { com.tranphuloi.neon.data.LeaderboardRepository.todayUtcDayKey() }
    val dailyEntries by leaderboard.dailyEntries(todayKey).collectAsState(initial = emptyList())
    // 23x Endless — top-10 by survival seconds. Shown only when current run was endless.
    val endlessEntries by leaderboard.endlessEntries.collectAsState(initial = emptyList())
    var submitted by remember { mutableStateOf(false) }
    val runStatsState = LocalRunStats.current.value
    val isEndless = runStatsState?.gameModeKey == "endless"

    // Round 77 (R77f) — staggered reveal animation. Mỗi step = delay.
    // Round 78 (#5 fix) — was 120ms × 8 = 960ms tổng stagger + 280ms tween cho
    // mỗi section. User feedback: animation laggy. Giảm xuống 50ms × 8 = 400ms
    // stagger + tween 180ms để dialog feel snappier. Cũng giảm số AnimatedVisibility
    // transition đang chạy đồng thời (8 transitions over 960ms → over 400ms +
    // shorter individual duration → ít overlap, less Compose transition overhead).
    var revealStep by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..8) {
            kotlinx.coroutines.delay(50L)
            revealStep = i
        }
    }

    LaunchedEffect(Unit) {
        Logger.d("DialogGameOver shown (score=$score, mode=${runStatsState?.gameModeKey ?: "?"})")
        if (!submitted) {
            val parsed = score.toIntOrNull()
            if (parsed != null) {
                Logger.d("DialogGameOver: submitting score=$parsed to leaderboard + daily(day=$todayKey)")
                leaderboard.submit(parsed)
                leaderboard.submitDaily(parsed, todayKey)
                if (isEndless) {
                    val sec = runStatsState.timeSec.toInt().coerceAtLeast(0)
                    Logger.d("DialogGameOver: submitting endless survival=${sec}s")
                    leaderboard.submitEndless(sec)
                }
                // 48x — bank earned minerals into lifetime balance.
                // Wave 22 (#4) — + thưởng mốc màn (mỗi 5 màn +30◇, trần 300).
                val stageBonus = com.tranphuloi.neon.data.stageMilestoneBonus(
                    runStatsState?.stagesReached ?: 0,
                )
                Logger.d("DialogGameOver: banking $parsed + mốc-màn $stageBonus lifetime minerals (meta)")
                meta.addMinerals(parsed + stageBonus)
            }
            submitted = true
        }
    }

    val highestSoFar = entries.maxByOrNull { it.score }?.score ?: score.toIntOrNull() ?: 0
    val currentScore = score.toIntOrNull() ?: 0
    val isNewBest = currentScore >= highestSoFar && currentScore > 0
    val playerRank = entries
        .indexOfFirst { it.score == currentScore }
        .let { if (it == -1) null else it + 1 }

    // Round 62 → Round 63 (audit fix) — TTS callout when player breaks their record.
    //
    // Round 62 bug: at initial composition `entries` is emptyList() (collectAsState
    // initial) so `highestSoFar = entries.max ?: score` evaluated to the score
    // itself → `isNewBest = score >= score = TRUE` for ANY submission. LaunchedEffect
    // fired immediately and announced "Kỷ lục mới!" even for score=47 << best=255.
    //
    // Round 63 fix: gate by (a) submitted=true (leaderboard.submit returned),
    // (b) entries.isNotEmpty() (Flow has settled with real data), (c) one-shot
    // guard via announcedNewBest so future entries updates don't re-fire.
    val voiceAnnouncer = com.tranphuloi.neon.ui.game.audio.LocalVoiceAnnouncer.current
    // Wave 11d Bug #3 audit-bonus fix — wire new-best variants. Prior code
    // resolved only the singular `voice_new_best` string while the variant
    // pair `_2`/`_3` shipped as dead resources. Now uses announceVariants
    // for true rotation.
    val voiceNewBestList = listOf(
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best_2),
        androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best_3),
    )
    var announcedNewBest by remember { mutableStateOf(false) }
    LaunchedEffect(submitted, entries) {
        if (!submitted || currentScore <= 0 || announcedNewBest) {
            return@LaunchedEffect
        }
        // Round 63 audit — require entries.size > 1 so the FIRST-EVER submission
        // for a mode doesn't trigger "Kỷ lục mới!" (first = best is technically
        // true but feels wrong — "new record" implies beating an OLD record).
        // Post-submit entries always contains the just-submitted score; size>1
        // means there's a prior entry to compare against.
        if (entries.size <= 1) return@LaunchedEffect
        // Exclude the just-submitted score from the comparison: the prior
        // record is the max EXCLUDING this run's score. If multiple entries
        // share currentScore (player tied an existing high), we still want to
        // count it as new best.
        val priorHighest = entries
            .filter { it.score != currentScore }
            .maxByOrNull { it.score }?.score ?: 0
        if (currentScore > priorHighest) {
            voiceAnnouncer.announceVariants(
                eventKey = "new_best",
                phrases = voiceNewBestList,
                personality = com.tranphuloi.neon.ui.game.audio.VoicePersonality.TRIUMPH,
            )
            announcedNewBest = true
        }
    }

    // Round 28 — migrated from NeonDialog to NeonBottomSheet.
    // dismissible = false: only ✕ / explicit button can dismiss (modal-final).
    // The ✕ acts like "VỀ MENU" since GameOver doesn't have a passive close.
    com.tranphuloi.neon.common.NeonBottomSheet(
        title = stringResource(id = R.string.game_over_dialog_title),
        accentColor = palette.redAlert,
        titleSize = 32.sp,
        dismissible = false,
        onDismiss = {
            Logger.d("DialogGameOver: ✕ tapped → back to Menu (checkpoint preserved)")
            onBackToMenu()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            // Round 77 (R77f) — staggered reveal: step 1 = score, 2 = highest, 3 = badge,
            // 4 = victory, 5 = stats, 6 = endless, 7 = daily, 8 = leaderboard.
            RevealWrap(visible = revealStep >= 1) {
                ScoreRow(
                    label = "Khoáng vật",
                    value = score,
                    accentColor = palette.gold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RevealWrap(visible = revealStep >= 2) {
                ScoreRow(
                    label = "Kỷ lục",
                    value = highestSoFar.toString(),
                    accentColor = palette.cyan,
                )
            }
            if (isNewBest && currentScore > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                // Round 77 (R77f) — pulse animation cho "KỶ LỤC MỚI" badge.
                RevealWrap(visible = revealStep >= 3) {
                    val pulseTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "newBestPulse")
                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.08f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                        ),
                        label = "newBestPulseScale",
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.gold.copy(alpha = 0.25f))
                            .border(
                                BorderStroke(1.5.dp, palette.gold),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "★ Kỷ lục mới ★",
                            color = palette.gold,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            style = TextStyle(letterSpacing = 3.sp),
                        )
                    }
                }
            }
            // 34d Wave 4 — victory ending text shown only when player defeated FinalBoss.
            // Difficulty-aware: hard difficulty gets a stronger congratulations message.
            if (runStatsState?.victoryAchieved == true) {
                Spacer(modifier = Modifier.height(14.dp))
                RevealWrap(visible = revealStep >= 4) { VictoryPanel() }
            }
            if (runStatsState != null) {
                Spacer(modifier = Modifier.height(14.dp))
                RevealWrap(visible = revealStep >= 5) { StatsPanel(stats = runStatsState) }
            }
            if (isEndless) {
                Spacer(modifier = Modifier.height(14.dp))
                RevealWrap(visible = revealStep >= 6) {
                    EndlessPanel(
                        currentSeconds = runStatsState.timeSec.toInt(),
                        endlessEntries = endlessEntries,
                        violet = palette.violet,
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            RevealWrap(visible = revealStep >= 7) {
                DailyPanel(
                    dayKey = todayKey,
                    dailyEntries = dailyEntries,
                    currentScore = currentScore,
                    magenta = palette.magenta,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            RevealWrap(visible = revealStep >= 8) {
                LeaderboardList(
                    entries = entries,
                    currentScore = currentScore,
                    playerRank = playerRank,
                    palette = palette,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            // ─── Action buttons (round 28: inline since sheet has no actions slot) ───
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonDialogButton(
                    text = "Chơi lại",
                    color = palette.cyan,
                    leadingGlyph = "▶",
                    onClick = {
                        Logger.d("DialogGameOver: Restart pressed — clearing checkpoint then navigate Game")
                        val modeKey = runStatsState?.gameModeKey ?: "campaign"
                        scope.launch { runPersistence.clearCheckpoint(modeKey) }
                        onRestartGame()
                    },
                )
                NeonDialogButton(
                    text = "Về menu",
                    color = palette.magenta,
                    leadingGlyph = "◀",
                    onClick = {
                        Logger.d("DialogGameOver: Back to Menu pressed (checkpoint preserved)")
                        onBackToMenu()
                    },
                )
            }
        }
    }
}

@Composable
private fun VictoryPanel() {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val difficulty by com.tranphuloi.neon.data.LocalSettings.current.difficulty
        .collectAsState(initial = com.tranphuloi.neon.data.Difficulty.NORMAL)
    val (heading, subtitle) = when (difficulty) {
        com.tranphuloi.neon.data.Difficulty.EASY -> "Thiên hà được cứu" to
            "Đã hạ Bá Vương trên độ DỄ. Thử độ VỪA lần sau nhé!"
        com.tranphuloi.neon.data.Difficulty.NORMAL -> "Bá vương bị hạ" to
            "Lái tàu xuất sắc. Thiên hà nợ anh sự bình yên, Đội trưởng."
        com.tranphuloi.neon.data.Difficulty.HARD -> "Chiến thắng huyền thoại" to
            "Chinh phục độ KHÓ. Anh là huyền thoại Sky Force U*S*A."
    }
    // Task 04 — epilogue kết truyện theo độ khó (song ngữ, gắn ngữ cảnh Lõi Thiên Hà).
    val epilogue = androidx.compose.ui.res.stringResource(
        when (difficulty) {
            com.tranphuloi.neon.data.Difficulty.EASY -> com.tranphuloi.neon.R.string.story_epilogue_easy
            com.tranphuloi.neon.data.Difficulty.NORMAL -> com.tranphuloi.neon.R.string.story_epilogue_normal
            com.tranphuloi.neon.data.Difficulty.HARD -> com.tranphuloi.neon.R.string.story_epilogue_hard
        },
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.gold.copy(alpha = 0.15f))
            .border(
                BorderStroke(2.dp, palette.gold),
                RoundedCornerShape(8.dp),
            )
            .neonGlow(palette.gold, intensity = 0.55f, radiusFactor = 1.4f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "★ $heading ★",
            color = palette.gold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 2.sp),
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // Task 04 — epilogue kết truyện (in nghiêng, mờ hơn subtitle để phân cấp).
        Text(
            text = epilogue,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EndlessPanel(
    currentSeconds: Int,
    endlessEntries: List<LeaderboardEntry>,
    violet: Color,
) {
    val bestSeconds = endlessEntries.maxByOrNull { it.score }?.score ?: currentSeconds
    val currentStr = String.format(Locale.US, "%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val bestStr = String.format(Locale.US, "%02d:%02d", bestSeconds / 60, bestSeconds % 60)
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(violet.copy(alpha = 0.12f))
            .border(
                BorderStroke(1.dp, violet.copy(alpha = 0.55f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Vô tận",
            color = violet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(label = "Sống sót", value = currentStr, color = violet)
        StatLine(label = "Tốt nhất", value = bestStr, color = violet)
        if (currentSeconds >= bestSeconds && currentSeconds > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "★ Kỷ lục mới vô tận ★",
                color = violet,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                style = TextStyle(letterSpacing = 2.sp),
            )
        }
    }
}

@Composable
private fun DailyPanel(
    dayKey: Long,
    dailyEntries: List<LeaderboardEntry>,
    currentScore: Int,
    magenta: Color,
) {
    val bestToday = dailyEntries.maxByOrNull { it.score }?.score ?: currentScore
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(magenta.copy(alpha = 0.10f))
            .border(
                BorderStroke(1.dp, magenta.copy(alpha = 0.45f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.daily_challenge_label).uppercase(),
                color = magenta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(letterSpacing = 4.sp),
            )
            Text(
                text = stringResource(id = R.string.daily_seed_label, dayKey.toString()),
                color = magenta.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(
            label = "Tốt nhất hôm nay",
            value = bestToday.toString(),
            color = magenta,
        )
        if (dailyEntries.size >= 2) {
            StatLine(
                label = "Số lần chơi",
                value = dailyEntries.size.toString(),
                color = magenta,
            )
        }
        if (currentScore > 0 && currentScore == bestToday && dailyEntries.size >= 2) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "★ Kỷ lục hôm nay ★",
                color = magenta,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                style = TextStyle(letterSpacing = 2.sp),
            )
        }
    }
}

@Composable
private fun StatsPanel(stats: com.tranphuloi.neon.data.RunStats) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val timeStr = String.format(
        java.util.Locale.US,
        "%02d:%02d",
        stats.timeSec / 60,
        stats.timeSec % 60,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(
                BorderStroke(1.dp, palette.cyan.copy(alpha = 0.3f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Thống kê",
            color = palette.cyan.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(label = "Thời gian", value = timeStr, color = palette.cyan)
        StatLine(label = "Màn đạt", value = stats.stagesReached.toString(), color = palette.cyan)
        // Wave 22 (#4) — thưởng mốc màn (hiện khi có).
        val stageBonus = com.tranphuloi.neon.data.stageMilestoneBonus(stats.stagesReached)
        if (stageBonus > 0) {
            StatLine(label = "Thưởng mốc màn", value = "+$stageBonus ◇", color = palette.cyan)
        }
        StatLine(label = "Diệt địch", value = stats.enemiesKilled.toString(), color = palette.cyan)
        if (stats.bossesDefeated > 0) {
            StatLine(label = "Hạ boss", value = stats.bossesDefeated.toString(), color = palette.cyan)
        }
        StatLine(label = "Combo tối đa", value = "×${stats.maxCombo}", color = palette.cyan)
    }
}

@Composable
private fun StatLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(letterSpacing = 2.sp),
        )
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Round 77 (R77f) — Staggered reveal wrapper. AnimatedVisibility với slide-up
 * + fade-in. Gate visible bằng revealStep counter.
 */
@Composable
private fun RevealWrap(visible: Boolean, content: @Composable () -> Unit) {
    // Round 78 (#5 fix) — Tween 280 → 180. Smaller slide offset (it/4 vs it/3)
    // less overdraw. Combined with shorter stagger (50ms vs 120ms) GameOver
    // dialog feels snappier without losing the "cascading reveal" effect.
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(180),
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = androidx.compose.animation.core.tween(180,
                easing = androidx.compose.animation.core.FastOutSlowInEasing),
            initialOffsetY = { it / 4 },
        ),
        exit = androidx.compose.animation.fadeOut(),
    ) {
        content()
    }
}

@Composable
private fun ScoreRow(label: String, value: String, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(letterSpacing = 3.sp),
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Text(
            text = value,
            color = accentColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.neonGlow(
                color = accentColor,
                intensity = 0.55f,
                radiusFactor = 1.3f,
            ),
        )
    }
}

@Composable
private fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    currentScore: Int,
    playerRank: Int?,
    palette: com.tranphuloi.neon.common.NeonPalette,
) {
    if (entries.isEmpty()) return
    val df = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(
                BorderStroke(1.dp, palette.cyan.copy(alpha = 0.3f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Top cao điểm",
            color = palette.cyan.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        entries.take(5).forEachIndexed { idx, e ->
            val isCurrent = playerRank == (idx + 1) && e.score == currentScore
            val rankColor = when {
                isCurrent -> palette.gold
                idx == 0 -> palette.gold
                idx == 1 -> palette.cyan
                idx == 2 -> palette.magenta
                else -> Color.White.copy(alpha = 0.6f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isCurrent) {
                    Modifier
                        .fillMaxWidth()
                        .background(palette.gold.copy(alpha = 0.18f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                },
            ) {
                Text(
                    text = "#${idx + 1}",
                    color = rankColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = "${e.score}",
                    color = if (isCurrent) palette.gold else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = df.format(Date(e.timestampMillis)),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}
