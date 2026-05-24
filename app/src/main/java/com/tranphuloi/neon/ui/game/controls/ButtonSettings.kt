package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.neonGlow

/**
 * Round 70 (Issue 6) — Pause icon (‖) replacing gear vector. Button thực tế
 * mở pause/settings dialog → ‖ rõ mục đích hơn gear. Hai vertical bars
 * rounded + cyan neon glow.
 *
 * Round 67.6 đã migrate từ bitmap `button_settings.webp` sang Canvas vector.
 * Round 70 đổi glyph từ gear (8-tooth) sang pause bars để khớp vai trò.
 */
@Composable
fun ButtonSettings(modifier: Modifier = Modifier, onSettings: () -> Unit) {
    val buttonPaddingEnd = dimensionResource(id = R.dimen.button_padding)
    val buttonPaddingTop = 14.dp
    // Round 70 fix (Issue 6) — size 60→68dp + brighter glow để "rõ ràng hơn"
    // theo user feedback. Pause bars + border circle riêng đã clear; size bump
    // tăng tap target + visibility.
    val buttonSize = 68.dp

    Canvas(
        modifier = modifier
            .padding(top = buttonPaddingTop, end = buttonPaddingEnd)
            .size(buttonSize)
            .neonGlow(color = NeonCyan, intensity = 0.55f, radiusFactor = 1.35f)
            .clickable { onSettings() },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Two rounded vertical bars (‖). Bar width ~16%, height ~56% of icon.
        val barW = size.width * 0.16f
        val barH = size.height * 0.56f
        val gap = size.width * 0.12f
        val topY = cy - barH / 2f
        val corner = CornerRadius(barW / 2f)
        // Left bar
        drawRoundRect(
            color = NeonCyan,
            topLeft = Offset(cx - gap / 2f - barW, topY),
            size = Size(barW, barH),
            cornerRadius = corner,
        )
        // Right bar
        drawRoundRect(
            color = NeonCyan,
            topLeft = Offset(cx + gap / 2f, topY),
            size = Size(barW, barH),
            cornerRadius = corner,
        )
        // Subtle circular border for affordance — outlines the tappable region.
        val borderR = minOf(size.width, size.height) * 0.46f
        drawCircle(
            color = NeonCyan.copy(alpha = 0.45f),
            radius = borderR,
            center = Offset(cx, cy),
            style = Stroke(width = size.width * 0.035f),
        )
    }
}
