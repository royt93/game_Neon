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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

        // Layer 2: content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ─── Title ───
            TitleBlock()

            // ─── Splash ship logo ───
            ShipLogo()

            // ─── Info card: mode + buff + checkpoint ───
            InfoCard(
                mode = mode,
                runModifier = runModifier,
                balance = balance,
                checkpoint = checkpoint,
            )

            // ─── PLAY button ───
            PlayButton(
                hasCheckpoint = checkpoint > 0,
                onClick = {
                    Logger.d("MenuScreen: PLAY tapped (checkpoint=$checkpoint)")
                    onPlay()
                },
            )

            // ─── 2x2 icon grid ───
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
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

            // Tail spacer so last button isn't flush with bottom edge on tall screens
            Spacer(modifier = Modifier.height(8.dp))
        }
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
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 4.sp),
            modifier = Modifier
                .graphicsLayer { alpha = glow }
                .neonGlow(NeonCyan, intensity = 0.7f, radiusFactor = 1.7f),
        )
        Text(
            text = "U*S*A",
            color = NeonMagenta,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 6.sp),
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)               // clickable FIRST → covers padding
            .clip(RoundedCornerShape(18.dp))
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
                BorderStroke(2.5.dp, NeonCyan.copy(alpha = borderAlpha)),
                RoundedCornerShape(18.dp),
            )
            .neonGlow(NeonCyan, intensity = 0.7f * borderAlpha, radiusFactor = 1.8f)
            .padding(vertical = 22.dp, horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "▶",
                color = NeonCyan,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 4.sp),
                modifier = Modifier.neonGlow(Color.White, intensity = 0.35f, radiusFactor = 1.3f),
            )
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)               // clickable FIRST → covers padding
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
            .neonGlow(color, intensity = 0.25f, radiusFactor = 1.2f)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            text = glyph,
            color = color,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.neonGlow(color, intensity = 0.5f, radiusFactor = 1.2f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 2.sp),
        )
    }
}

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

