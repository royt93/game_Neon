package com.tranphuloi.neon.data

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSettings = staticCompositionLocalOf<SettingsRepository> {
    error("SettingsRepository not provided. Wrap in CompositionLocalProvider(LocalSettings provides ...).")
}

val LocalLeaderboard = staticCompositionLocalOf<LeaderboardRepository> {
    error("LeaderboardRepository not provided. Wrap in CompositionLocalProvider(LocalLeaderboard provides ...).")
}
