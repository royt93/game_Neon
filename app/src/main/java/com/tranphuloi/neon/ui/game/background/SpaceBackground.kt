package com.tranphuloi.neon.ui.game.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Compose entry point for the space background — delegates to a native
 * [SpaceBackgroundView] under `AndroidView` for ~3-5× faster rendering than
 * Compose Canvas. State updates flow Compose → View via the `update` callback.
 */
@Composable
fun SpaceBackground(
    state: BackgroundState,
    running: Boolean = true,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx -> SpaceBackgroundView(ctx) },
        update = { view ->
            view.state = state
            view.running = running
        },
        modifier = modifier,
    )
}
