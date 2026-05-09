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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.data.ShipSkin
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

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

    LaunchedEffect(Unit) { Logger.d("DialogSettings shown") }

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonCyan),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonCyan, intensity = 0.35f, radiusFactor = 1.2f)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "SETTINGS",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5,
            )

            SettingSlider(
                label = "Music",
                value = musicVolume,
                color = NeonCyan,
                onChange = {
                    scope.launch { settings.setMusicVolume(it) }
                },
            )
            SettingSlider(
                label = "SFX",
                value = sfxVolume,
                color = NeonGold,
                onChange = {
                    scope.launch { settings.setSfxVolume(it) }
                },
            )
            SettingCheck(
                label = "Vibration",
                value = vibrationEnabled,
                onChange = { scope.launch { settings.setVibrationEnabled(it) } }
            )
            SettingCheck(
                label = "Reduce motion",
                value = reduceMotion,
                onChange = { scope.launch { settings.setReduceMotion(it) } }
            )

            Text("Difficulty", color = NeonMagenta, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Difficulty.values().forEach { d ->
                    Pill(
                        label = d.key.replaceFirstChar { it.uppercase() },
                        selected = d == difficulty,
                        color = NeonMagenta,
                        onClick = { scope.launch { settings.setDifficulty(d) } }
                    )
                }
            }

            Text("Ship skin", color = NeonGold, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ShipSkin.values().forEach { s ->
                    Pill(
                        label = s.key.replaceFirstChar { it.uppercase() },
                        selected = s == shipSkin,
                        color = NeonGold,
                        onClick = { scope.launch { settings.setShipSkin(s) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Qb: Rate / Share / Privacy.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    Logger.d("Settings: Rate clicked")
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }.onFailure {
                        Logger.w("Settings: Play Store not available", it)
                    }
                }) {
                    Text("Rate", color = NeonCyan)
                }
                TextButton(onClick = {
                    Logger.d("Settings: Share clicked")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out Neon — a synthwave shoot-'em-up. " +
                                "https://play.google.com/store/apps/details?id=${context.packageName}"
                        )
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    }.onFailure { Logger.w("Settings: Share intent failed", it) }
                }) {
                    Text("Share", color = NeonMagenta)
                }
                TextButton(onClick = {
                    Logger.d("Settings: Privacy clicked")
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://example.com/privacy")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }.onFailure {
                        Logger.w("Settings: Privacy URL open failed", it)
                    }
                }) {
                    Text("Privacy", color = NeonGold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = {
                    Logger.d("DialogSettings: Close pressed")
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("CLOSE", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
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
            Text(label, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${localValue.toInt()}", color = color)
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
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
        )
        Text(label, color = Color.White)
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
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color.copy(alpha = 0.5f) else Color.Transparent)
            .border(BorderStroke(1.dp, color), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) Color.White else color, fontWeight = FontWeight.Bold)
    }
}
