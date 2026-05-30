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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.tranphuloi.neon.common.neonGlow
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
    onOpenMetaUpgrade: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit = {},
    onOpenLoadout: () -> Unit = {},
    /** Wave 11c — open Statistics screen. */
    onOpenStats: () -> Unit = {},
    /** Wave 12 — open Shop screen. */
    onOpenShop: () -> Unit = {},
) {
    val settings = LocalSettings.current
    val meta = LocalMetaProgression.current
    val runPersist = LocalRunPersistence.current

    val lastModeKey by settings.lastMode.collectAsState(initial = "campaign")
    val mode = GameMode.fromKey(lastModeKey)
    val modifierKey by settings.lastModifier.collectAsState(initial = "none")
    val runModifier = com.tranphuloi.neon.ui.game.modifier.RunModifier.fromKey(modifierKey)
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val checkpoint by runPersist.checkpointFor(lastModeKey).collectAsState(initial = 0)

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

        // Layer 2: content — round 31 adaptive layout via BoxWithConstraints.
        //   - Tall screens (≥ 720dp): fillMaxSize + Spacer(weight=1f) push grid
        //     to bottom edge, removing dead-space.
        //   - Short screens (< 720dp, landscape, small phones): switch to
        //     verticalScroll + spacedBy(12dp) so content can't clip.
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val tallEnough = maxHeight >= 720.dp
            val baseColumnMod = Modifier
                .fillMaxSize()
                // Round 33 — windowInsetsPadding clears notch / status-bar cutout area
                // so title isn't masked. Activity hides status bar (round 25)
                // but display cutout still occupies layout space → must reserve.
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 22.dp, vertical = 24.dp)
            val columnMod = if (tallEnough) baseColumnMod
            else baseColumnMod.verticalScroll(rememberScrollState())
        Column(
            modifier = columnMod,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ─── Title (stagger 0ms) ───
            EntryAnim(stepIndex = 0) { TitleBlock() }

            // Round 32 — small breathing spacer after title pushes logo into
            // visual center area (was: title flush, logo touching).
            if (tallEnough) Spacer(modifier = Modifier.weight(0.3f))

            // ─── Splash ship logo (stagger 120ms) ───
            EntryAnim(stepIndex = 1) { ShipLogo() }

            // ─── Info card (stagger 240ms) ───
            EntryAnim(stepIndex = 2) {
                InfoCard(
                    mode = mode,
                    runModifier = runModifier,
                    balance = balance,
                    checkpoint = checkpoint,
                )
            }

            // ─── PLAY button (stagger 360ms) ───
            EntryAnim(stepIndex = 3) {
                PlayButton(
                    hasCheckpoint = checkpoint > 0,
                    onClick = {
                        Logger.d("MenuScreen: PLAY tapped (checkpoint=$checkpoint)")
                        onPlay()
                    },
                )
            }

            // Round 70 fix (Issue 1) — KHÔNG thêm Spacer riêng. Column outer
            // đã có `verticalArrangement = Arrangement.spacedBy(12.dp)` → gap
            // 12dp tự động giữa PLAY và grid. Trước fix tôi sai khi thêm
            // Spacer(height=12) → gap thực tế = 12+12 = 24dp, vẫn không uniform
            // với grid inter-row 12dp.

            // ─── 5-button grid (stagger 480ms) ───
            // Round 67.7 — fix "buttons không cách đều":
            // (a) Unified gap: Column spacedBy(12.dp) + Row spacedBy(12.dp) — was
            //     vertical 12dp + horizontal 14dp (inconsistent).
            // (b) BÁCH KHOA now in Row 3 với invisible spacer placeholder ở slot
            //     trái — giữ width đúng bằng các button khác (~50% width thay vì
            //     100% fillMaxWidth gây cảm giác "to gấp đôi").
            EntryAnim(stepIndex = 4) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "CHẾ ĐỘ",
                            glyph = "⊞",
                            color = NeonViolet,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: MODE tapped")
                                onOpenModePicker()
                            },
                        )
                        MenuButton(
                            label = "BUFF",
                            glyph = "⚡",
                            color = NeonGold,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: BUFF tapped")
                                onOpenModifierPicker()
                            },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "NÂNG CẤP",
                            glyph = "⬆",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: UPGRADE tapped")
                                onOpenMetaUpgrade()
                            },
                        )
                        MenuButton(
                            label = "CÀI ĐẶT",
                            glyph = "⚙",
                            color = NeonMagenta,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: SETTINGS tapped")
                                onOpenSettings()
                            },
                        )
                    }
                    // Round 68 — Row 3: TRANG BỊ + BÁCH KHOA paired (full row).
                    // Trang Bị mở LoadoutPicker manually (auto-skip Settings default true).
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "TRANG BỊ",
                            glyph = "◈",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: LOADOUT tapped")
                                onOpenLoadout()
                            },
                        )
                        MenuButton(
                            label = "BÁCH KHOA",
                            glyph = "❡",
                            color = NeonViolet,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: INFO tapped")
                                onOpenInfo()
                            },
                        )
                    }
                    // Wave 11c + 12 — Row 4: THỐNG KÊ + CỬA HÀNG paired.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MenuButton(
                            label = "THỐNG KÊ",
                            glyph = "▦",
                            color = NeonGold,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: STATS tapped")
                                onOpenStats()
                            },
                        )
                        MenuButton(
                            label = "CỬA HÀNG",
                            glyph = "◇",
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Logger.d("MenuScreen: SHOP tapped")
                                onOpenShop()
                            },
                        )
                    }
                }
            }

        }
        }       // end BoxWithConstraints (round 31)
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
private fun TitleBlock() {
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
            fontSize = 44.sp,
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
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 7.sp),
            modifier = Modifier.neonGlow(NeonMagenta, intensity = 0.5f, radiusFactor = 1.4f),
        )
    }
}

@Composable
private fun ShipLogo() {
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
        modifier = Modifier.size(180.dp),
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
                .size(140.dp)
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
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NeonBgMid.copy(alpha = 0.65f))
            .border(BorderStroke(1.5.dp, NeonViolet.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "CHẾ ĐỘ",
                    color = NeonViolet.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(letterSpacing = 3.sp),
                )
                Text(
                    text = mode.displayName,
                    color = NeonViolet,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            // Lifetime minerals badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "♦",
                    color = NeonGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "$balance",
                    color = NeonGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        if (runModifier != com.tranphuloi.neon.ui.game.modifier.RunModifier.NONE) {
            Text(
                text = "⚡ Buff: ${runModifier.displayName} (×${runModifier.scoreMul})",
                color = NeonGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (checkpoint > 0) {
            Text(
                text = "▸ Đang ở màn $checkpoint",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlayButton(
    hasCheckpoint: Boolean,
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
    val label = if (hasCheckpoint) "TIẾP TỤC" else "BẮT ĐẦU"
    // Round 70 (Issue 1) — Same height + corner radius as MenuButton để đồng nhất.
    // Pulse animation + gradient bg retained để PLAY vẫn nổi bật là hero action.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(UNIFIED_BUTTON_HEIGHT)
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 3.sp),
                modifier = Modifier.neonGlow(Color.White, intensity = 0.35f, radiusFactor = 1.3f),
            )
        }
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
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(UNIFIED_BUTTON_HEIGHT)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
            .neonGlow(color, intensity = 0.25f, radiusFactor = 1.2f)
            .padding(horizontal = 10.dp),
    ) {
        Text(
            text = glyph,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.neonGlow(color, intensity = 0.5f, radiusFactor = 1.2f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 1.5.sp),
        )
    }
}

/** Round 70 (Issue 1) — single source of truth cho button height. */
private val UNIFIED_BUTTON_HEIGHT = 64.dp

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
