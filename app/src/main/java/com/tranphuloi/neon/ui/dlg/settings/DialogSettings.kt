package com.tranphuloi.neon.ui.dlg.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Round 24/27 — clean DialogSettings.
 *
 * Round 27 simplified to 3 sections (was 4): mode/buff/upgrade rows moved to
 * MenuScreen's primary navigation grid to eliminate duplication.
 *
 *   ÂM THANH    — Music slider + SFX slider
 *   CHƠI         — Vibration + Reduce motion checkboxes + Difficulty pills + Ship skin pills
 *   ỨNG DỤNG     — Rate / Share / Privacy links
 *
 * Compact spacing (10dp section gap, 4dp inter-row gap), small section headers
 * with subtle horizontal divider above each, vertical scroll on small screens.
 */
@Composable
fun DialogSettings(
    onDismiss: () -> Unit,
) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Round 39 — accent colors here come from LocalNeonPalette so toggling
    // "Chế độ màu" inside this dialog gives immediate visual feedback.
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current

    val musicVolume by settings.musicVolume.collectAsState(initial = 80)
    val sfxVolume by settings.sfxVolume.collectAsState(initial = 90)
    val vibrationEnabled by settings.vibrationEnabled.collectAsState(initial = true)
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val voiceAnnouncerEnabled by settings.voiceAnnouncerEnabled.collectAsState(initial = true)
    val autoSkipLoadout by settings.autoSkipLoadout.collectAsState(initial = true)
    val difficulty by settings.difficulty.collectAsState(initial = Difficulty.NORMAL)
    // Round 70 (Issue 2) — SecondaryWeapon picker đã chuyển sang LoadoutPicker
    // duy nhất. Settings không còn read/write SECONDARY_WEAPON key.
    // Round 27 — lastMode / lastModifier reads removed; that info now lives in MenuScreen.

    LaunchedEffect(Unit) { Logger.d("DialogSettings shown") }

    com.tranphuloi.neon.common.NeonBottomSheet(
        title = "CÀI ĐẶT",
        accentColor = NeonCyan,
        onDismiss = {
            Logger.d("DialogSettings: dismissed")
            onDismiss()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            // Round 30 — 18dp section gap for breathing room (was 10dp).
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ────── Section 1: Âm thanh ──────
            SectionPanel(headerLabel = "ÂM THANH", glyph = "♪", color = NeonCyan) {
                SettingSlider(
                    label = "Nhạc",
                    value = musicVolume,
                    color = NeonCyan,
                    onChange = { scope.launch { settings.setMusicVolume(it) } },
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingSlider(
                    label = "Hiệu ứng",
                    value = sfxVolume,
                    color = NeonGold,
                    onChange = { scope.launch { settings.setSfxVolume(it) } },
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Round 62 — TTS announcer toggle in Audio section since it's
                // gameplay sound. Uses sfxVolume slider (no separate TTS volume).
                SettingCheck(
                    label = stringResource(com.tranphuloi.neon.R.string.settings_voice_announcer),
                    value = voiceAnnouncerEnabled,
                    onChange = { scope.launch { settings.setVoiceAnnouncerEnabled(it) } },
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Round 68 — auto-skip LoadoutPicker toggle (default ON).
                SettingCheck(
                    label = "Tự động bỏ qua Trang Bị",
                    value = autoSkipLoadout,
                    onChange = { scope.launch { settings.setAutoSkipLoadout(it) } },
                )
            }

            // ────── Section 2: Chơi ──────
            // Round 32 — each control group lives in its own subtle sub-panel
            // so Rung / Giảm chuyển động / Độ khó / Skin tàu feel separated.
            SectionPanel(headerLabel = "CHƠI", glyph = "⊞", color = NeonMagenta) {
                ControlGroup {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingCheck(
                            label = "Rung",
                            value = vibrationEnabled,
                            onChange = { scope.launch { settings.setVibrationEnabled(it) } },
                            modifier = Modifier.weight(1f),
                        )
                        SettingCheck(
                            label = "Giảm chuyển động",
                            value = reduceMotion,
                            onChange = { scope.launch { settings.setReduceMotion(it) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                ControlGroup {
                    LabelledPillRow(label = "Độ khó", color = palette.magenta) {
                        Difficulty.values().forEach { d ->
                            val labelText = when (d) {
                                Difficulty.EASY -> "Dễ"
                                Difficulty.NORMAL -> "Vừa"
                                Difficulty.HARD -> "Khó"
                            }
                            Pill(
                                label = labelText,
                                selected = d == difficulty,
                                color = NeonMagenta,
                                onClick = { scope.launch { settings.setDifficulty(d) } }
                            )
                        }
                    }
                }
                // Wave 13 (#1) — "Hào quang tàu" (skin mua + chọn) đã chuyển hẳn
                // sang Cửa hàng → tab Skin (mua + chọn cùng chỗ, nhất quán với tab
                // Tàu). Gỡ khỏi Cài đặt để tránh tách đôi luồng.
                // Round 70 (Issue 2) — SecondaryWeapon picker removed. Loadout
                // bottom sheet (TRANG BỊ button trong menu) là duy nhất quản lý
                // bullet + secondary weapon per-run.
                // Wave 13a (slice E) — "Chế độ màu" (ColorBlindMode) đã chuyển sang
                // Cửa hàng → tab Hiển thị (gom tuỳ chỉnh hiển thị về 1 chỗ).
                // Round 77 (R77g) — Camera zoom picker.
                Spacer(modifier = Modifier.height(10.dp))
                ControlGroup {
                    val cameraZoom by settings.cameraZoom.collectAsState(
                        initial = com.tranphuloi.neon.data.CameraZoom.MEDIUM,
                    )
                    LabelledPillRow(label = "Tầm nhìn", color = palette.violet) {
                        com.tranphuloi.neon.data.CameraZoom.entries.forEach { z ->
                            Pill(
                                label = z.displayName,
                                selected = z == cameraZoom,
                                color = palette.violet,
                                onClick = { scope.launch { settings.setCameraZoom(z) } }
                            )
                        }
                    }
                }
            }

            // ────── Section 3: Ứng dụng ──────
            SectionPanel(headerLabel = "ỨNG DỤNG", glyph = "✦", color = Color.White.copy(alpha = 0.7f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FlatLink(
                        label = "Đánh giá",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                    ) {
                        Logger.d("Settings: Rate clicked")
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }.onFailure {
                            Logger.w("Settings: Play Store not available", it)
                        }
                    }
                    FlatLink(
                        label = "Chia sẻ",
                        color = NeonMagenta,
                        modifier = Modifier.weight(1f),
                    ) {
                        Logger.d("Settings: Share clicked")
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Sky Force U*S*A — bắn phi thuyền vũ trụ synthwave. " +
                                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                            )
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
                        }.onFailure { Logger.w("Settings: Share intent failed", it) }
                    }
                    FlatLink(
                        label = "Riêng tư",
                        color = NeonGold,
                        modifier = Modifier.weight(1f),
                    ) {
                        Logger.d("Settings: Privacy clicked")
                        // Round 76 (R76a) — Use LegalLinks constant thay placeholder.
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(com.tranphuloi.neon.common.LegalLinks.PRIVACY_POLICY_URL)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }.onFailure {
                            Logger.w("Settings: Privacy URL open failed", it)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Round 30 — Section panel: each settings group lives in its own subtle card
 * with a glyph + header + grouped content. Replaces the flat SectionHeader.
 */
@Composable
private fun SectionPanel(
    headerLabel: String,
    glyph: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(com.tranphuloi.neon.common.NeonBgEdge.copy(alpha = 0.55f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = glyph,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = headerLabel,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 3.sp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

/**
 * Round 32 — sub-panel inside a SectionPanel: groups related controls with a
 * subtle dark background so adjacent groups don't bleed visually into each other.
 */
@Composable
private fun ControlGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    color: Color,
    onChange: (Int) -> Unit,
) {
    // Local state lets the thumb track smoothly while we only commit to DataStore on release,
    // avoiding ~50 disk writes per drag.
    var localValue by remember(value) { mutableStateOf(value.toFloat()) }
    Column {
        Row {
            Text(
                label,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${localValue.toInt()}",
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = localValue,
            valueRange = 0f..100f,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onChange(localValue.toInt()) },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f),
            )
        )
    }
}

@Composable
private fun SettingCheck(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
        )
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun LabelledPillRow(
    label: String,
    color: Color,
    content: @Composable () -> Unit,
) {
    // Round 39 — was a single Row with fixed-width label + non-wrapping inner Row.
    // 5-item Pill rows (ship aura picker) overflowed horizontally on narrow screens
    // and the last Pill rendered visually clipped/distorted. FlowRow wraps Pills
    // onto a second line when they don't fit; label stays above content.
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Pill(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    locked: Boolean = false,
) {
    // Wave 12 round 3 — locked pills dim their border + label so the 🔒 prefix
    // reads as disabled-but-tappable (tap logs a hint rather than selecting).
    val borderColor = if (locked) color.copy(alpha = 0.35f) else color
    val labelColor = when {
        selected -> Color.White
        locked -> color.copy(alpha = 0.45f)
        else -> color
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.5f) else Color.Transparent)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** Compact text-button used in the ỨNG DỤNG row. */
@Composable
private fun FlatLink(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.55f)), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp),
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
