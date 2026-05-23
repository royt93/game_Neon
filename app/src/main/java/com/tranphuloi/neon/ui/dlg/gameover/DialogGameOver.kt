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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonDialog
import com.tranphuloi.neon.common.NeonDialogButton
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
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
                Logger.d("DialogGameOver: banking $parsed lifetime minerals (meta)")
                meta.addMinerals(parsed)
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
    val voiceNewBest = androidx.compose.ui.res.stringResource(com.tranphuloi.neon.R.string.voice_new_best)
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
            voiceAnnouncer.announce(
                voiceNewBest,
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
        accentColor = NeonRedAlert,
        titleSize = 32.sp,
        dismissible = false,
        onDismiss = {
            Logger.d("DialogGameOver: ✕ tapped → back to Menu (checkpoint preserved)")
            onBackToMenu()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            ScoreRow(
                label = "KHOÁNG VẬT",
                value = score,
                accentColor = NeonGold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ScoreRow(
                label = "KỶ LỤC",
                value = highestSoFar.toString(),
                accentColor = NeonCyan,
            )
            if (isNewBest && currentScore > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonGold.copy(alpha = 0.25f))
                        .border(
                            BorderStroke(1.5.dp, NeonGold),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "★ KỶ LỤC MỚI ★",
                        color = NeonGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        style = TextStyle(letterSpacing = 3.sp),
                    )
                }
            }
            // 34d Wave 4 — victory ending text shown only when player defeated FinalBoss.
            // Difficulty-aware: hard difficulty gets a stronger congratulations message.
            if (runStatsState?.victoryAchieved == true) {
                Spacer(modifier = Modifier.height(14.dp))
                VictoryPanel()
            }
            // 15c: stats breakdown panel — only when stats snapshot exists.
            if (runStatsState != null) {
                Spacer(modifier = Modifier.height(14.dp))
                StatsPanel(stats = runStatsState)
            }
            // 23x Endless — only when current run was endless. `isEndless` already
            // implies runStatsState != null (since isEndless = state?.key == "endless").
            if (isEndless) {
                Spacer(modifier = Modifier.height(14.dp))
                EndlessPanel(
                    currentSeconds = runStatsState.timeSec.toInt(),
                    endlessEntries = endlessEntries,
                )
            }
            // 17c Daily challenge — daily best panel above the all-time list.
            Spacer(modifier = Modifier.height(14.dp))
            DailyPanel(
                dayKey = todayKey,
                dailyEntries = dailyEntries,
                currentScore = currentScore,
            )
            Spacer(modifier = Modifier.height(14.dp))
            LeaderboardList(
                entries = entries,
                currentScore = currentScore,
                playerRank = playerRank,
            )
            Spacer(modifier = Modifier.height(18.dp))
            // ─── Action buttons (round 28: inline since sheet has no actions slot) ───
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonDialogButton(
                    text = "CHƠI LẠI",
                    color = NeonCyan,
                    leadingGlyph = "▶",
                    onClick = {
                        Logger.d("DialogGameOver: Restart pressed — clearing checkpoint then navigate Game")
                        val modeKey = runStatsState?.gameModeKey ?: "campaign"
                        scope.launch { runPersistence.clearCheckpoint(modeKey) }
                        onRestartGame()
                    },
                )
                NeonDialogButton(
                    text = "VỀ MENU",
                    color = NeonMagenta,
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
    val difficulty by com.tranphuloi.neon.data.LocalSettings.current.difficulty
        .collectAsState(initial = com.tranphuloi.neon.data.Difficulty.NORMAL)
    val (heading, subtitle) = when (difficulty) {
        com.tranphuloi.neon.data.Difficulty.EASY -> "THIÊN HÀ ĐƯỢC CỨU" to
            "Đã hạ Bá Vương trên độ DỄ. Thử độ VỪA lần sau nhé!"
        com.tranphuloi.neon.data.Difficulty.NORMAL -> "BÁ VƯƠNG BỊ HẠ" to
            "Lái tàu xuất sắc. Thiên hà nợ anh sự bình yên, Đội trưởng."
        com.tranphuloi.neon.data.Difficulty.HARD -> "CHIẾN THẮNG HUYỀN THOẠI" to
            "Chinh phục độ KHÓ. Anh là huyền thoại Sky Force U*S*A."
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonGold.copy(alpha = 0.15f))
            .border(
                BorderStroke(2.dp, NeonGold),
                RoundedCornerShape(8.dp),
            )
            .neonGlow(NeonGold, intensity = 0.55f, radiusFactor = 1.4f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "★ $heading ★",
            color = NeonGold,
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
    }
}

@Composable
private fun EndlessPanel(
    currentSeconds: Int,
    endlessEntries: List<LeaderboardEntry>,
) {
    val bestSeconds = endlessEntries.maxByOrNull { it.score }?.score ?: currentSeconds
    val currentStr = String.format(Locale.US, "%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val bestStr = String.format(Locale.US, "%02d:%02d", bestSeconds / 60, bestSeconds % 60)
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(com.tranphuloi.neon.common.NeonViolet.copy(alpha = 0.12f))
            .border(
                BorderStroke(1.dp, com.tranphuloi.neon.common.NeonViolet.copy(alpha = 0.55f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "VÔ TẬN",
            color = com.tranphuloi.neon.common.NeonViolet,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(label = "SỐNG SÓT", value = currentStr)
        StatLine(label = "TỐT NHẤT", value = bestStr)
        if (currentSeconds >= bestSeconds && currentSeconds > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "★ KỶ LỤC MỚI VÔ TẬN ★",
                color = com.tranphuloi.neon.common.NeonViolet,
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
) {
    val bestToday = dailyEntries.maxByOrNull { it.score }?.score ?: currentScore
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(NeonMagenta.copy(alpha = 0.10f))
            .border(
                BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.45f)),
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
                color = NeonMagenta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(letterSpacing = 4.sp),
            )
            Text(
                text = stringResource(id = R.string.daily_seed_label, dayKey.toString()),
                color = NeonMagenta.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(
            label = "TỐT NHẤT HÔM NAY",
            value = bestToday.toString(),
        )
        if (dailyEntries.size >= 2) {
            StatLine(
                label = "SỐ LẦN CHƠI",
                value = dailyEntries.size.toString(),
            )
        }
        if (currentScore > 0 && currentScore == bestToday && dailyEntries.size >= 2) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "★ KỶ LỤC HÔM NAY ★",
                color = NeonMagenta,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                style = TextStyle(letterSpacing = 2.sp),
            )
        }
    }
}

@Composable
private fun StatsPanel(stats: com.tranphuloi.neon.data.RunStats) {
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
                BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "THỐNG KÊ",
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        StatLine(label = "THỜI GIAN", value = timeStr)
        StatLine(label = "MÀN ĐẠT", value = stats.stagesReached.toString())
        StatLine(label = "DIỆT ĐỊCH", value = stats.enemiesKilled.toString())
        if (stats.bossesDefeated > 0) {
            StatLine(label = "HẠ BOSS", value = stats.bossesDefeated.toString())
        }
        StatLine(label = "COMBO TỐI ĐA", value = "×${stats.maxCombo}")
    }
}

@Composable
private fun StatLine(label: String, value: String) {
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
            color = NeonCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
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
                BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = "TOP CAO ĐIỂM",
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(letterSpacing = 4.sp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        entries.take(5).forEachIndexed { idx, e ->
            val isCurrent = playerRank == (idx + 1) && e.score == currentScore
            val rankColor = when {
                isCurrent -> NeonGold
                idx == 0 -> NeonGold
                idx == 1 -> NeonCyan
                idx == 2 -> NeonMagenta
                else -> Color.White.copy(alpha = 0.6f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isCurrent) {
                    Modifier
                        .fillMaxWidth()
                        .background(NeonGold.copy(alpha = 0.18f))
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
                    color = if (isCurrent) NeonGold else Color.White,
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
