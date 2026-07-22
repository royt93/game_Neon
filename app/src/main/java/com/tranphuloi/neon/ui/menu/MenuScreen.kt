package com.tranphuloi.neon.ui.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalAchievements
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.LocalRunPersistence
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.mode.GameMode
import com.tranphuloi.neon.utils.Logger
import kotlin.random.Random

/**
 * Round 27 — game-like menu screen.
 *
 * Layers (back to front):
 *   1) Vertical gradient background (matches game palette)
 *   2) Animated starfield Canvas (60 stars, 3 layers parallax drift)
 *   3) Content column: title pulse → splash logo → info card → PLAY → 2x2 icon grid
 *
 * UX rules:
 *   - PLAY button is the primary CTA (huge, pulsing border, ▶ glyph).
 *   - 4 secondary actions each have a distinct neon-colored icon (⊞ Mode / ⚡ Buff /
 *     ⬆ Upgrade / ⚙ Settings) + label below.
 *   - Mode + buff state visible in an info card so user knows what they'll get
 *     when they press PLAY.
 *   - All buttons use a single `MenuButton`/`PrimaryButton` helper with consistent
 *     ripple-style click semantics — `Modifier.clickable` BEFORE padding so the
 *     entire visible area is tappable.
 *
 * Click verification: every onClick fires Logger.d so adb logcat confirms taps
 * register.
 */
@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onOpenModePicker: () -> Unit,
    onOpenModifierPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit = {},
    onOpenPractice: () -> Unit = {},
    onOpenLoadout: () -> Unit = {},
    /** Wave 11c — open Statistics screen. */
    onOpenStats: () -> Unit = {},
    /** Wave 12 — open Shop screen. */
    onOpenShop: () -> Unit = {},
    /** Task 30 (2026-07-19) — open Boss Rush (gated by Achievement.FINAL_BOSS_KILL). */
    onOpenBossRush: () -> Unit = {},
) {
    val settings = LocalSettings.current
    val meta = LocalMetaProgression.current
    val runPersist = LocalRunPersistence.current
    val achievements = LocalAchievements.current
    val unlockedAchievements by achievements.unlockedFlow.collectAsState(initial = emptySet())
    val bossRushUnlocked = "final_boss_kill" in unlockedAchievements
    var bossRushLockedHintAt by remember { mutableLongStateOf(0L) }

    val lastModeKey by settings.lastMode.collectAsState(initial = "campaign")
    val mode = GameMode.fromKey(lastModeKey)
    val modifierKey by settings.lastModifier.collectAsState(initial = "none")
    val runModifier = com.tranphuloi.neon.ui.game.modifier.RunModifier.fromKey(modifierKey)
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val checkpoint by runPersist.checkpointFor(lastModeKey).collectAsState(initial = 0)

    // Wave 21 (#4) — điểm danh hằng ngày.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val todayKey = remember { com.tranphuloi.neon.data.LeaderboardRepository.todayUtcDayKey() }
    val dailyAvailable by meta.dailyClaimAvailable(todayKey).collectAsState(initial = false)
    val dailyStreak by meta.dailyStreak.collectAsState(initial = 0)
    var dailyClaimedAmount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Logger.d("MenuScreen entered (mode=$mode, modifier=$runModifier, checkpoint=$checkpoint, balance=$balance)")
    }

    // Round 76 (R76f user audit issue 6) — Menu nhạc nền. Trước fix MenuScreen
    // không wire AudioPlayer → bkg.mp3 không play khi vào menu.
    // AudioPlayer(GameStatus.RUNNING) → holder.play() activates background music.
    com.tranphuloi.neon.ui.game.audio.AudioPlayer(
        gameStatus = com.tranphuloi.neon.ui.game.settings.GameStatus.RUNNING,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonBgDeep, NeonBgMid, NeonBgEdge),
                ),
            ),
    ) {
        // Layer 1: animated starfield (decorative, no pointer events)
        StarfieldBackground(modifier = Modifier.fillMaxSize())

        // Layer 1b: comet streak (round 28) — parabolic trajectory, 8-15s gap
        CometStreak(modifier = Modifier.fillMaxSize())

        // Layer 2: content — adaptive *scaling* layout via SubcomposeLayout.
        //   Yêu cầu sản phẩm: menu KHÔNG scroll, mọi thứ phải vừa đúng 1 màn.
        //   Trước đây tính `s` từ ước lượng chiều cao cứng (neededH = 810dp +
        //   offset theo showDaily/checkpoint) → sai lệch theo device/density/
        //   độ dài chuỗi dịch → phải verticalScroll dự phòng khi ước lượng sai
        //   (chính là nguồn gốc bug "menu bị scroll"). Giờ ĐO THẬT chiều cao
        //   nội dung ở scale 1 bằng 1 pass "probe" vô hình (maxHeight =
        //   Infinity), tính `s` chính xác từ số đo thật đó, rồi compose lại
        //   đúng 1 lần ở `s` đã tính (pass "content", cái duy nhất được đặt
        //   lên màn hình) — luôn vừa khít theo đúng nghĩa, không còn cần
        //   scroll fallback.
        SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
            val minScale = 0.72f
            val onClaimDaily: () -> Unit = {
                scope.launch {
                    val granted = meta.claimDaily(todayKey)
                    if (granted > 0) dailyClaimedAmount = granted
                    Logger.d("MenuScreen: điểm danh +$granted◇")
                }
            }

            val probeHeightPx = subcompose("probe") {
                MenuContent(
                    s = 1f,
                    showBreathingSpacer = false,
                    mode = mode,
                    runModifier = runModifier,
                    balance = balance,
                    checkpoint = checkpoint,
                    dailyAvailable = dailyAvailable,
                    dailyClaimedAmount = dailyClaimedAmount,
                    dailyStreak = dailyStreak,
                    bossRushUnlocked = bossRushUnlocked,
                    onPlay = onPlay,
                    onOpenModePicker = onOpenModePicker,
                    onOpenModifierPicker = onOpenModifierPicker,
                    onOpenLoadout = onOpenLoadout,
                    onOpenShop = onOpenShop,
                    onOpenStats = onOpenStats,
                    onOpenBossRush = onOpenBossRush,
                    onBossRushLockedTap = { bossRushLockedHintAt = System.currentTimeMillis() },
                    onOpenPractice = onOpenPractice,
                    onOpenInfo = onOpenInfo,
                    onOpenSettings = onOpenSettings,
                    onClaimDaily = onClaimDaily,
                )
            }.first()
                .measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = Constraints.Infinity))
                .height

            // *0.97f: chừa ~3% lề đáy để phần tử cuối không sát viền do làm tròn.
            val s = if (probeHeightPx > 0) {
                (constraints.maxHeight * 0.97f / probeHeightPx).coerceIn(minScale, 1f)
            } else {
                1f
            }

            val contentPlaceable = subcompose("content") {
                MenuContent(
                    s = s,
                    showBreathingSpacer = true,
                    mode = mode,
                    runModifier = runModifier,
                    balance = balance,
                    checkpoint = checkpoint,
                    dailyAvailable = dailyAvailable,
                    dailyClaimedAmount = dailyClaimedAmount,
                    dailyStreak = dailyStreak,
                    bossRushUnlocked = bossRushUnlocked,
                    onPlay = onPlay,
                    onOpenModePicker = onOpenModePicker,
                    onOpenModifierPicker = onOpenModifierPicker,
                    onOpenLoadout = onOpenLoadout,
                    onOpenShop = onOpenShop,
                    onOpenStats = onOpenStats,
                    onOpenBossRush = onOpenBossRush,
                    onBossRushLockedTap = { bossRushLockedHintAt = System.currentTimeMillis() },
                    onOpenPractice = onOpenPractice,
                    onOpenInfo = onOpenInfo,
                    onOpenSettings = onOpenSettings,
                    onClaimDaily = onClaimDaily,
                )
            }.first().measure(constraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                contentPlaceable.placeRelative(0, 0)
            }
        }

        // Layer 3: locked-Boss-Rush tap hint — custom Neon-styled toast (không
        // dùng Toast.makeText mặc định của Android theo yêu cầu), phong cách
        // banner đồng bộ với AchievementBanner.
        LockedFeatureToast(
            message = stringResource(id = R.string.boss_rush_locked_hint),
            shownAtMillis = bossRushLockedHintAt,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Toast báo "tính năng đang khóa" khi bấm nút bị khóa (ví dụ Chiến Boss chưa
 * mở). Auto-dismiss sau ~2.2s, style theo [AchievementBanner]: nền NeonBgMid,
 * viền + glow màu NeonMagenta (khớp màu nút Chiến Boss).
 */
@Composable
private fun LockedFeatureToast(
    message: String,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        nowMillis = System.currentTimeMillis()
        repeat(66) {                                                   // ~2.2s @ 33ms
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > 2200L) return

    val t = elapsed.toFloat() / 2200f
    val slideOffset = when {
        t < 0.15f -> 100f * (1f - t / 0.15f)
        t < 0.85f -> 0f
        else -> 100f * ((t - 0.85f) / 0.15f)
    }
    val alpha = when {
        t < 0.15f -> t / 0.15f
        t < 0.85f -> 1f
        else -> 1f - (t - 0.85f) / 0.15f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .graphicsLayer {
                translationY = slideOffset
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(8.dp))
            .background(NeonBgMid)
            .border(BorderStroke(2.dp, NeonMagenta), RoundedCornerShape(8.dp))
            .neonGlow(NeonMagenta, intensity = 0.45f, radiusFactor = 1.2f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Extracted content Column cho [MenuScreen] — dùng bởi SubcomposeLayout ở 2
 * pass đo: "probe" vô hình ở `s = 1f` (không breathing spacer — Modifier.weight()
 * cần bounded constraints, mà probe đo dưới maxHeight = Infinity) để lấy chiều
 * cao nội tại thật, rồi "content" ở `s` đã tính từ số đo đó — pass này mới thật
 * sự được đặt lên màn hình.
 */
@Composable
private fun MenuContent(
    s: Float,
    showBreathingSpacer: Boolean,
    mode: GameMode,
    runModifier: com.tranphuloi.neon.ui.game.modifier.RunModifier,
    balance: Int,
    checkpoint: Int,
    dailyAvailable: Boolean,
    dailyClaimedAmount: Int,
    dailyStreak: Int,
    bossRushUnlocked: Boolean,
    onPlay: () -> Unit,
    onOpenModePicker: () -> Unit,
    onOpenModifierPicker: () -> Unit,
    onOpenLoadout: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBossRush: () -> Unit,
    onBossRushLockedTap: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onClaimDaily: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Round 33 — windowInsetsPadding clears notch / status-bar cutout area
            // so title isn't masked. Activity hides status bar (round 25)
            // but display cutout still occupies layout space → must reserve.
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 22.dp, vertical = 24f.sdp(s)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12f.sdp(s)),
    ) {
        // ─── Title (stagger 0ms) ───
        EntryAnim(stepIndex = 0) { TitleBlock(s = s) }

        // Breathing spacer after title — chỉ khi có dư chỗ (s == 1f, màn
        // cao hơn nội dung) VÀ đang ở pass "content" thật (không phải probe —
        // Modifier.weight() cần bounded constraints, probe đo dưới
        // maxHeight = Infinity nên không thể đặt spacer weight ở đó).
        if (showBreathingSpacer && s >= 1f) Spacer(modifier = Modifier.weight(0.3f))

        // ─── Splash ship logo (stagger 120ms) ───
        EntryAnim(stepIndex = 1) { ShipLogo(s = s) }

        // ─── Info card (stagger 240ms) ───
        EntryAnim(stepIndex = 2) {
            InfoCard(
                mode = mode,
                runModifier = runModifier,
                balance = balance,
                checkpoint = checkpoint,
                s = s,
            )
        }

        // ─── PLAY button (stagger 360ms) ───
        EntryAnim(stepIndex = 3) {
            PlayButton(
                hasCheckpoint = checkpoint > 0,
                s = s,
                onClick = {
                    Logger.d("MenuScreen: PLAY tapped (checkpoint=$checkpoint)")
                    onPlay()
                },
            )
        }

        // Wave 21 (#4) — nút ĐIỂM DANH: hiện khi còn quà hôm nay; sau khi
        // claim đổi thành thông báo "+X◇" (streak ngày liên tiếp).
        if (dailyAvailable || dailyClaimedAmount > 0) {
            EntryAnim(stepIndex = 3) {
                DailyCheckInButton(
                    claimed = dailyClaimedAmount > 0,
                    claimedAmount = dailyClaimedAmount,
                    streak = dailyStreak,
                    s = s,
                    onClaim = onClaimDaily,
                )
            }
            // Wave 25 fix — tách nút ĐIỂM DANH (CTA thưởng, thuộc cụm hero/PLAY)
            // khỏi label nhóm "TRƯỚC TRẬN" bên dưới (user: "bị khít"). +~28dp tổng.
            Spacer(modifier = Modifier.height(8f.sdp(s)))
        }

            // Round 70 fix (Issue 1) — KHÔNG thêm Spacer riêng. Column outer
            // đã có `verticalArrangement = Arrangement.spacedBy(12.dp)` → gap
            // 12dp tự động giữa PLAY và grid. Trước fix tôi sai khi thêm
            // Spacer(height=12) → gap thực tế = 12+12 = 24dp, vẫn không uniform
            // với grid inter-row 12dp.

            // ─── Grouped action buttons (stagger 480ms) ───
            // Wave 13b — 8 nút gộp 3 nhóm có header để bớt quá tải:
            //   • TRƯỚC TRẬN: Chế độ / Thử thách / Trang bị (lựa chọn per-run)
            //   • TIẾN TRÌNH: Cửa hàng / Nâng cấp / Thống kê (kinh tế + tiến độ)
            //   • KHÁC: Luyện tập / Bách khoa / Cài đặt
            // 3-item groups render 3-wide (compact) — gồm cả nhóm KHÁC (thêm Luyện tập).
            // "BUFF" → "THỬ THÁCH" lộ rõ tính đánh-đổi per-run (RunModifier), hết
            // nhầm với Nâng cấp (skill-tree vĩnh viễn).
            EntryAnim(stepIndex = 4) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8f.sdp(s)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GroupHeader(label = "Trước trận", color = NeonViolet, s = s)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10f.sdp(s)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "Chế độ",
                            glyph = "⊞",
                            color = NeonViolet,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: MODE tapped")
                                onOpenModePicker()
                            },
                        )
                        MenuButton(
                            label = "Thử thách",
                            glyph = "⚡",
                            color = NeonGold,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: CHALLENGE (modifier) tapped")
                                onOpenModifierPicker()
                            },
                        )
                        MenuButton(
                            label = "Trang bị",
                            glyph = "◈",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: LOADOUT tapped")
                                onOpenLoadout()
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(2f.sdp(s)))
                    GroupHeader(label = "Tiến trình", color = NeonGold, s = s)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12f.sdp(s)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Wave 13a (slice C) — NÂNG CẤP (skill-tree) đã gộp vào Cửa hàng
                        // → tab Nâng cấp. Hàng này còn Cửa hàng + Thống kê.
                        // Task 30 (2026-07-19) — thêm "Chiến Boss" → hàng 3 nút, đổi cả
                        // 3 sang compact (khớp 2 hàng 3-nút còn lại).
                        MenuButton(
                            label = "Cửa hàng",
                            glyph = "◇",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: SHOP tapped")
                                onOpenShop()
                            },
                        )
                        MenuButton(
                            label = "Thống kê",
                            glyph = "▦",
                            color = NeonGold,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: STATS tapped")
                                onOpenStats()
                            },
                        )
                        MenuButton(
                            label = "Chiến Boss",
                            glyph = "☠",
                            color = if (bossRushUnlocked) NeonMagenta else NeonMagenta.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                if (bossRushUnlocked) {
                                    Logger.d("MenuScreen: BOSS RUSH tapped")
                                    onOpenBossRush()
                                } else {
                                    Logger.d("MenuScreen: BOSS RUSH locked (chưa clear campaign)")
                                    onBossRushLockedTap()
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(2f.sdp(s)))
                    GroupHeader(label = "Khác", color = NeonViolet, s = s)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12f.sdp(s)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "Luyện tập",
                            glyph = "◎",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f).testTag("menu_practice"),
                            compact = true,   // hàng "Khác" giờ 3 nút → compact tránh cắt "Bách khoa"
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: PRACTICE tapped")
                                onOpenPractice()
                            },
                        )
                        MenuButton(
                            label = "Bách khoa",
                            glyph = "❡",
                            color = NeonViolet,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: INFO tapped")
                                onOpenInfo()
                            },
                        )
                        MenuButton(
                            label = "Cài đặt",
                            glyph = "⚙",
                            color = NeonMagenta,
                            modifier = Modifier.weight(1f).testTag("menu_settings"),
                            compact = true,
                            s = s,
                            onClick = {
                                Logger.d("MenuScreen: SETTINGS tapped")
                                onOpenSettings()
                            },
                        )
                    }
                }
            }

            // Wave 13 (#5) — build version label, sourced from Gradle
            // `versionName` (app/build.gradle) via PackageManager at runtime.
            EntryAnim(stepIndex = 5) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val versionName = remember {
                    runCatching {
                        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                    }.getOrNull().orEmpty()
                }
                if (versionName.isNotEmpty()) {
                    Text(
                        text = "Phiên bản $versionName",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11f.ssp(s),
                        style = TextStyle(letterSpacing = 1.sp),
                    )
                }
            }

        }
    }

/**
 * Stagger entry animation wrapper. Each child fades in + slides up 20px on
 * mount, with delay = stepIndex × 120ms. Once visible, never re-animates.
 */
@Composable
private fun EntryAnim(stepIndex: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((stepIndex * 120L).coerceAtLeast(0L))
        visible = true
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 420),
        label = "entryAlpha$stepIndex",
    )
    val translateY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 420),
        label = "entryTranslate$stepIndex",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = translateY
        },
    ) {
        content()
    }
}

// ─────────────────────────── sub-components ───────────────────────────

@Composable
private fun TitleBlock(s: Float) {
    val pulse = rememberInfiniteTransition(label = "titlePulse")
    val glow by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SKY FORCE",
            color = NeonCyan,
            // Round 33 — title 36 → 44sp for stronger hero presence.
            fontSize = 44f.ssp(s),
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 5.sp),
            modifier = Modifier
                .graphicsLayer { alpha = glow }
                .neonGlow(NeonCyan, intensity = 0.75f, radiusFactor = 1.8f),
        )
        Text(
            text = "U*S*A",
            color = NeonMagenta,
            // Round 33 — subtitle 22 → 28sp, scales with main title.
            fontSize = 28f.ssp(s),
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 7.sp),
            modifier = Modifier.neonGlow(NeonMagenta, intensity = 0.5f, radiusFactor = 1.4f),
        )
    }
}

@Composable
private fun ShipLogo(s: Float) {
    val pulse = rememberInfiniteTransition(label = "shipPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shipScale",
    )
    val haloAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloAlpha",
    )
    // Round 28 — idle parallax: gentle ±3° rotation over 4.2s
    val tilt by pulse.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shipTilt",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180f.sdp(s)),
    ) {
        // Radial halo behind the ship
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = haloAlpha),
                        NeonMagenta.copy(alpha = haloAlpha * 0.5f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension / 2,
                ),
                radius = size.minDimension / 2,
                center = center,
            )
        }
        Image(
            painter = painterResource(id = R.drawable.splash_image),
            contentDescription = stringResource(id = R.string.splash),
            modifier = Modifier
                .size(140f.sdp(s))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt           // round 28 idle tilt
                },
        )
    }
}

@Composable
private fun InfoCard(
    mode: GameMode,
    runModifier: com.tranphuloi.neon.ui.game.modifier.RunModifier,
    balance: Int,
    checkpoint: Int,
    s: Float,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6f.sdp(s)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeonBgMid.copy(alpha = 0.65f))
            .border(BorderStroke(1.5.dp, NeonViolet.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12f.sdp(s)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Chế độ",
                    color = NeonViolet.copy(alpha = 0.7f),
                    fontSize = 11f.ssp(s),
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(letterSpacing = 3.sp),
                )
                Text(
                    text = mode.displayName,
                    color = NeonViolet,
                    fontSize = 18f.ssp(s),
                    fontWeight = FontWeight.Black,
                )
            }
            // Lifetime minerals badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "♦",
                    color = NeonGold,
                    fontSize = 20f.ssp(s),
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "$balance",
                    color = NeonGold,
                    fontSize = 18f.ssp(s),
                    fontWeight = FontWeight.Black,
                )
            }
        }
        if (runModifier != com.tranphuloi.neon.ui.game.modifier.RunModifier.NONE) {
            Text(
                text = "⚡ Thử thách: ${runModifier.displayName} (×${runModifier.scoreMul})",
                color = NeonGold,
                fontSize = 14f.ssp(s),
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (checkpoint > 0) {
            Text(
                text = "▸ Đang ở màn $checkpoint",
                color = NeonCyan,
                fontSize = 14f.ssp(s),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlayButton(
    hasCheckpoint: Boolean,
    s: Float,
    onClick: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "playPulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderAlpha",
    )
    val label = if (hasCheckpoint) "Tiếp tục" else "Bắt đầu"
    // Round 70 (Issue 1) — Same height + corner radius as MenuButton để đồng nhất.
    // Pulse animation + gradient bg retained để PLAY vẫn nổi bật là hero action.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("menu_play")
            .fillMaxWidth()
            .height(UNIFIED_BUTTON_HEIGHT * s)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.4f),
                        NeonMagenta.copy(alpha = 0.25f),
                        NeonCyan.copy(alpha = 0.4f),
                    ),
                ),
            )
            .border(
                BorderStroke(2.dp, NeonCyan.copy(alpha = borderAlpha)),
                RoundedCornerShape(14.dp),
            )
            .neonGlow(NeonCyan, intensity = 0.7f * borderAlpha, radiusFactor = 1.6f)
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "▶",
                color = NeonCyan,
                fontSize = 22f.ssp(s),
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 18f.ssp(s),
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 3.sp),
                modifier = Modifier.neonGlow(Color.White, intensity = 0.35f, radiusFactor = 1.3f),
            )
        }
    }
}

/** Wave 21 (#4) — nút điểm danh hằng ngày (vàng). Trước claim: bấm để nhận;
 *  sau claim: hiện "+X◇" + chuỗi ngày, hết bấm được. */
@Composable
private fun DailyCheckInButton(
    claimed: Boolean,
    claimedAmount: Int,
    streak: Int,
    s: Float,
    onClaim: () -> Unit,
) {
    val label = if (claimed) {
        "✓ Đã nhận +$claimedAmount◇" + if (streak > 1) "  ·  chuỗi $streak ngày" else ""
    } else {
        "🎁 Điểm danh hôm nay"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(UNIFIED_BUTTON_HEIGHT * s)
            .then(if (claimed) Modifier else Modifier.clickable(onClick = onClaim))
            .clip(RoundedCornerShape(14.dp))
            .background(NeonGold.copy(alpha = if (claimed) 0.12f else 0.2f))
            .border(
                BorderStroke(1.5.dp, NeonGold.copy(alpha = if (claimed) 0.5f else 1f)),
                RoundedCornerShape(14.dp),
            )
            .neonGlow(NeonGold, intensity = if (claimed) 0.2f else 0.55f, radiusFactor = 1.4f)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            color = if (claimed) NeonGold else Color.White,
            fontSize = 15f.ssp(s),
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 2.sp),
        )
    }
}

/**
 * Round 70 (Issue 1) — Unified button style:
 *  - Tất cả button có cùng height (UNIFIED_BUTTON_HEIGHT) + cùng style (corner
 *    14dp + border 1.5dp + glow + bg alpha 0.12). Khác biệt duy nhất là MÀU
 *    accent per-button + glyph/label.
 *  - Layout: Row(glyph + label) centered — replaces Column(glyph/label stack)
 *    để khớp layout PlayButton (Row arrow + label).
 *  - Trước Round 70, MenuButton là Column → cao hơn PLAY ~10dp. Sau Round 70,
 *    height đồng nhất → grid 2×3 cảm giác như 1 hệ thống.
 */
@Composable
private fun MenuButton(
    label: String,
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    // Wave 13b — compact = 3-wide row variant: nhỏ font/glyph + maxLines=1 để
    // label dài ("THỬ THÁCH", "BÁCH KHOA") không tràn ở 1/3 chiều rộng.
    compact: Boolean = false,
    s: Float,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(UNIFIED_BUTTON_HEIGHT * s)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
            .neonGlow(color, intensity = 0.25f, radiusFactor = 1.2f)
            .padding(horizontal = if (compact) 6.dp else 10.dp),
    ) {
        Text(
            text = glyph,
            color = color,
            fontSize = (if (compact) 18f else 22f).ssp(s),
            fontWeight = FontWeight.Black,
            modifier = Modifier.neonGlow(color, intensity = 0.5f, radiusFactor = 1.2f),
        )
        Spacer(modifier = Modifier.size(if (compact) 5.dp else 8.dp))
        Text(
            text = label,
            color = color,
            fontSize = (if (compact) 11f else 13f).ssp(s),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            style = TextStyle(letterSpacing = if (compact) 0.5.sp else 1.5.sp),
        )
    }
}

/**
 * Wave 13b — small uppercase section header above each menu button group.
 * Accent bar + dim label, matches LoadoutPicker.SectionHeader vibe.
 */
@Composable
private fun GroupHeader(label: String, color: Color, s: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12f.sdp(s))
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.size(7.dp))
        Text(
            text = label,
            color = color.copy(alpha = 0.85f),
            fontSize = 11f.ssp(s),
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 2.sp),
        )
    }
}

/** Round 70 (Issue 1) — single source of truth cho button height. */
private val UNIFIED_BUTTON_HEIGHT = 64.dp

/**
 * Adaptive scaling helpers — nhân giá trị thiết kế (ở scale 1.0) với hệ số `s`
 * tính từ maxHeight trong MenuScreen, để toàn bộ menu co tỉ lệ vừa đúng 1 màn
 * mà không scroll/clip. `s` luôn trong [0.72f, 1f].
 */
private fun Float.sdp(s: Float): Dp = (this * s).dp
private fun Float.ssp(s: Float): TextUnit = (this * s).sp

// ─────────────────────────── starfield background ───────────────────────────

private data class MenuStar(
    val xFrac: Float,                                      // 0..1 of canvas width
    val baseY: Float,                                      // initial y in px
    val sizePx: Float,
    val baseAlpha: Float,
    val twinklePhase: Float,                               // 0..2π
    val layer: Int,                                        // 0=far, 1=mid, 2=near (drift speed)
)

/**
 * 60 stars in 3 layers. Drift downward at layer-scaled speed via an
 * infinite transition. Each star also twinkles via sin(phase + time).
 * Pure decorative, doesn't consume pointer events.
 */
@Composable
private fun StarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val rng = Random(424242L)
        List(60) {
            val layer = it % 3
            MenuStar(
                xFrac = rng.nextFloat(),
                baseY = rng.nextFloat() * 2000f,           // pre-scatter across virtual canvas
                sizePx = when (layer) {
                    0 -> rng.nextFloat() * 1.2f + 0.8f     // far: small
                    1 -> rng.nextFloat() * 1.8f + 1.4f     // mid
                    else -> rng.nextFloat() * 2.4f + 2.0f  // near: bigger
                },
                baseAlpha = when (layer) {
                    0 -> 0.4f
                    1 -> 0.6f
                    else -> 0.85f
                },
                twinklePhase = rng.nextFloat() * 6.283f,
                layer = layer,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "starfield")
    val timeMs by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
        ),
        label = "starTime",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        stars.forEach { star ->
            // Drift speed per layer (px/sec): far=20, mid=40, near=80
            val speed = when (star.layer) {
                0 -> 20f
                1 -> 40f
                else -> 80f
            }
            // Position cycles every (h + 100) / speed seconds
            val cycleSec = (h + 100f) / speed
            val phase = (timeMs / 1000f) % cycleSec
            val y = (star.baseY + phase * speed) % (h + 100f) - 50f
            // Twinkle: alpha = base * (0.6 + 0.4 * sin(phase + twinkle))
            val twinkle = kotlin.math.sin(timeMs / 600f + star.twinklePhase)
            val alpha = (star.baseAlpha * (0.6f + 0.4f * twinkle)).coerceIn(0f, 1f)
            val color = when (star.layer) {
                0 -> NeonCyan.copy(alpha = alpha * 0.5f)   // far cyan tint
                1 -> Color.White.copy(alpha = alpha)
                else -> NeonGold.copy(alpha = alpha)       // near gold tint for warmth
            }
            drawCircle(
                color = color,
                radius = star.sizePx,
                center = Offset(star.xFrac * w, y),
            )
        }
    }
}

// ─────────────────────────── comet streak ───────────────────────────

/**
 * Round 28 — occasional comet streak across the menu background.
 * Parabolic trajectory left→right (or right→left, randomized per cycle).
 * 8-15s gap between streaks, 1.5s active duration. Sparkle trail behind.
 */
@Composable
private fun CometStreak(modifier: Modifier = Modifier) {
    // Cycle state: when does the comet start, in what direction, what's the height?
    var cometCycle by remember { mutableStateOf<Triple<Long, Int, Float>?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            val gapMs = (8_000L..15_000L).random()
            kotlinx.coroutines.delay(gapMs)
            val rng = Random.Default
            // direction: -1 = right-to-left, +1 = left-to-right
            val dir = if (rng.nextBoolean()) 1 else -1
            val heightFrac = 0.15f + rng.nextFloat() * 0.35f       // upper-third of screen
            cometCycle = Triple(System.currentTimeMillis(), dir, heightFrac)
            kotlinx.coroutines.delay(1500L)                        // active duration
            cometCycle = null
        }
    }

    val timeTick = rememberInfiniteTransition(label = "cometTick")
    val tickFrame by timeTick.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32, easing = LinearEasing),
        ),
        label = "tick",
    )

    Canvas(modifier = modifier) {
        val cycle = cometCycle ?: return@Canvas
        val (startMs, dir, heightFrac) = cycle
        // Suppress lint for unused tickFrame — it just drives recomposition.
        @Suppress("UNUSED_EXPRESSION") tickFrame
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
        if (elapsed > 1500L) return@Canvas
        val t = elapsed.toFloat() / 1500f                          // 0..1
        val w = size.width
        val h = size.height
        // Start off-screen, end off-screen.
        val x = if (dir > 0) -50f + t * (w + 100f) else w + 50f - t * (w + 100f)
        // Parabolic vertical: dip down slightly mid-trajectory.
        val baseY = h * heightFrac
        val dip = 60f * kotlin.math.sin((t * Math.PI).toFloat())
        val y = baseY + dip
        // Comet head: bright gold circle with cyan halo.
        val headAlpha = (1f - kotlin.math.abs(t - 0.5f) * 2f).coerceIn(0f, 1f)
        drawCircle(
            color = NeonGold.copy(alpha = headAlpha * 0.4f),
            radius = 14f,
            center = Offset(x, y),
        )
        drawCircle(
            color = NeonGold.copy(alpha = headAlpha),
            radius = 5f,
            center = Offset(x, y),
        )
        // Trail: 8 fading dots behind the head, opposite of dir.
        for (i in 1..8) {
            val trailT = (t - i * 0.018f).coerceAtLeast(0f)
            if (trailT == 0f) continue
            val trailX = if (dir > 0) -50f + trailT * (w + 100f) else w + 50f - trailT * (w + 100f)
            val trailY = baseY + 60f * kotlin.math.sin((trailT * Math.PI).toFloat())
            val trailAlpha = headAlpha * (1f - i / 9f) * 0.6f
            drawCircle(
                color = Color.White.copy(alpha = trailAlpha),
                radius = (5f - i * 0.4f).coerceAtLeast(1f),
                center = Offset(trailX, trailY),
            )
        }
    }
}
