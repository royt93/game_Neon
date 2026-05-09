package com.tranphuloi.neon.common

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val neonPalette = darkColors(
    primary = NeonCyan,
    primaryVariant = NeonViolet,
    secondary = NeonMagenta,
    background = NeonBgDeep,
    surface = NeonBgMid,
    onPrimary = NeonBgDeep,
    onSecondary = NeonBgDeep,
    onBackground = NeonCyan,
    onSurface = NeonCyan,
)

@Composable
fun NeonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = neonPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
