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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.tranphuloi.neon.data.ShipSkin
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

    val musicVolume by settings.musicVolume.collectAsState(initial = 80)
    val sfxVolume by settings.sfxVolume.collectAsState(initial = 90)
    val vibrationEnabled by settings.vibrationEnabled.collectAsState(initial = true)
    val reduceMotion by settings.reduceMotion.collectAsState(initial = false)
    val difficulty by settings.difficulty.collectAsState(initial = Difficulty.NORMAL)
    val shipSkin by settings.shipSkin.collectAsState(initial = ShipSkin.REGULAR)
    // Round 27 — lastMode / lastModifier reads removed; that info now lives in MenuScreen.

    LaunchedEffect(Unit) { Logger.d("DialogSettings shown") }

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonCyan),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.neonGlow(color = NeonCyan, intensity = 0.35f, radiusFactor = 1.2f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ────── Header ──────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CÀI ĐẶT",
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        Logger.d("DialogSettings: Close pressed")
                        onDismiss()
                    },
                ) {
                    Text("ĐÓNG", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }

            // ────── Section 1: Âm thanh ──────
            SectionHeader(label = "ÂM THANH", color = NeonCyan)
            SettingSlider(
                label = "Nhạc",
                value = musicVolume,
                color = NeonCyan,
                onChange = { scope.launch { settings.setMusicVolume(it) } },
            )
            SettingSlider(
                label = "Hiệu ứng",
                value = sfxVolume,
                color = NeonGold,
                onChange = { scope.launch { settings.setSfxVolume(it) } },
            )

            // ────── Section 2: Chơi ──────
            SectionHeader(label = "CHƠI", color = NeonMagenta)
            // Two checkboxes side by side to compact.
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
            LabelledPillRow(label = "Độ khó", color = NeonMagenta) {
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
            LabelledPillRow(label = "Skin tàu", color = NeonGold) {
                ShipSkin.values().forEach { s ->
                    val labelText = when (s) {
                        ShipSkin.REGULAR -> "Thường"
                        ShipSkin.BOOSTED -> "Cường hóa"
                    }
                    Pill(
                        label = labelText,
                        selected = s == shipSkin,
                        color = NeonGold,
                        onClick = { scope.launch { settings.setShipSkin(s) } }
                    )
                }
            }

            // Round 27 — "CHUẨN BỊ RUN" section removed (duplicate with MenuScreen
            // 2x2 grid). Settings dialog now focuses on audio + gameplay toggles +
            // app links only. Mode / Buff / Upgrade access lives in MenuScreen.

            // ────── Section 3: Ứng dụng ──────
            SectionHeader(label = "ỨNG DỤNG", color = Color.White.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://example.com/privacy")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }.onFailure {
                        Logger.w("Settings: Privacy URL open failed", it)
                    }
                }
            }
        }
    }
}

/** Section header: bolder caps label with thin divider line above. Round 25 — bumped from 10sp → 13sp. */
@Composable
private fun SectionHeader(label: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color.copy(alpha = 0.3f)),
        )
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(letterSpacing = 2.5.sp),
        )
    }
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(92.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.5f) else Color.Transparent)
            .border(BorderStroke(1.dp, color), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else color,
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
