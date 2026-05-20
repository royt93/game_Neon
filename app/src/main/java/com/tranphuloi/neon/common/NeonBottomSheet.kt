package com.tranphuloi.neon.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

/**
 * Round 28 — NeonBottomSheet: shared bottom-sheet component for all dialog
 * routes in the app. Replaces NeonDialog's centered-card pattern with a
 * sheet pinned to the bottom edge.
 *
 * Visuals: rounded top corners (24dp), neon-themed border + glow in
 * [accentColor], NeonBgMid background, full-width, wraps content height.
 *
 * Header row: drag handle (top center) + title + ✕ close button (top right).
 * Body: caller-provided ColumnScope content.
 *
 * Animations: slide-up enter (260ms tween) + slide-down + fade exit.
 *
 * Dismiss interactions (gated by [dismissible]):
 *   - Tap scrim (outside sheet) → onDismiss
 *   - Swipe-down on sheet > 80dp → onDismiss
 *   - Tap ✕ button → onDismiss
 *
 * When [dismissible] = false (modal-final scenarios like GameOver), scrim
 * tap + swipe-down are disabled; only ✕ button works. Back-press handling
 * lives on the navigation host side via DialogProperties.
 *
 * Usage:
 * ```
 * dialog(
 *     route = "settings",
 *     dialogProperties = DialogProperties(
 *         usePlatformDefaultWidth = false,
 *         decorFitsSystemWindows = false,
 *     ),
 * ) {
 *     NeonBottomSheet(
 *         title = "CÀI ĐẶT",
 *         accentColor = NeonCyan,
 *         onDismiss = { navController.popBackStack() },
 *     ) {
 *         // content goes here as ColumnScope.() -> Unit
 *     }
 * }
 * ```
 */
@Composable
fun NeonBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    accentColor: Color = NeonCyan,
    titleSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Logger.d("NeonBottomSheet \"$title\" mounting")
        // Tiny delay so initial state is hidden → triggers slide-in.
        delay(16L)
        visible = true
    }

    // Round 29 — Dialog has its own Window separate from Activity. By default the
    // Dialog window shows status bar even when Activity hides it. Walk up the
    // composition tree to find the DialogWindowProvider and force-hide system bars
    // + flip decorFitsSystemWindows = false so content goes edge-to-edge.
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(view) {
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        if (dialogWindow != null) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            androidx.core.view.WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            Logger.d("NeonBottomSheet \"$title\" → status bar + nav bar hidden on dialog window")
        } else {
            Logger.d("NeonBottomSheet \"$title\" → no DialogWindowProvider found, falling through")
        }
    }

    /** Wrap onDismiss so we play exit animation before propagating. */
    val dismissWithAnim: () -> Unit = remember(onDismiss) {
        {
            Logger.d("NeonBottomSheet \"$title\" dismiss requested")
            visible = false
            // Caller's onDismiss runs immediately — the sheet animates out
            // while NavHost pops; visually OK because pop is also animated.
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Round 30 — scrim dim REMOVED per user spec. Background is transparent
        // (alpha = 0) but Box still captures tap events to handle dismiss.
        val scrimAlpha = 0f
        val noopInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .let { mod ->
                    if (dismissible) {
                        mod.clickable(
                            interactionSource = noopInteraction,
                            indication = null,
                            onClick = {
                                Logger.d("NeonBottomSheet \"$title\" scrim tapped → dismiss")
                                dismissWithAnim()
                            },
                        )
                    } else mod
                },
        )

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 260),
                initialOffsetY = { it },
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 200),
                targetOffsetY = { it },
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SheetContent(
                title = title,
                accentColor = accentColor,
                titleSize = titleSize,
                dismissible = dismissible,
                onCloseClick = {
                    Logger.d("NeonBottomSheet \"$title\" ✕ tapped → dismiss")
                    dismissWithAnim()
                },
                onDragDismiss = {
                    Logger.d("NeonBottomSheet \"$title\" swipe-down → dismiss")
                    dismissWithAnim()
                },
                content = content,
            )
        }
    }
}

@Composable
private fun SheetContent(
    title: String,
    accentColor: Color,
    titleSize: androidx.compose.ui.unit.TextUnit,
    dismissible: Boolean,
    onCloseClick: () -> Unit,
    onDragDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    // Round 31 — cap sheet height at 90% of screen so content can't overflow above
    // the visible area on tall content (e.g. MetaUpgrade skill tree). Inner content
    // with verticalScroll / LazyColumn still scrolls past this cap.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * 0.9f).dp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .clip(cornerShape)
            .background(NeonBgMid)
            .border(BorderStroke(2.dp, accentColor), cornerShape)
            .neonGlow(color = accentColor, intensity = 0.5f, radiusFactor = 1.3f)
            // Swipe-down to dismiss — only on the sheet itself, only if dismissible.
            // 80dp threshold (≈80px on mdpi) — matches Material 3 sheet defaults.
            .let { mod ->
                if (dismissible) {
                    mod.pointerInput(Unit) {
                        var totalDragY = 0f
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (totalDragY > 80f) onDragDismiss()
                                totalDragY = 0f
                            },
                            onDragCancel = { totalDragY = 0f },
                            onVerticalDrag = { _, drag ->
                                if (drag > 0f) totalDragY += drag
                            },
                        )
                    }
                } else mod
            }
            // Round 29 — bottom padding 56dp so content clears system nav bar /
            // gesture inset area when edge-to-edge is enabled.
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 56.dp),
    ) {
        // ─── Drag handle ───
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 2.dp, bottom = 8.dp)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor.copy(alpha = 0.55f)),
        )

        // ─── Header: title left, ✕ button right ───
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                color = accentColor,
                fontSize = titleSize,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 2.sp),
                modifier = Modifier
                    .weight(1f)
                    .neonGlow(accentColor, intensity = 0.4f, radiusFactor = 1.2f),
            )
            CloseButton(accentColor = accentColor, onClick = onCloseClick)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Body content ───
        content()
    }
}

@Composable
private fun CloseButton(accentColor: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NeonRedAlert.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, NeonRedAlert.copy(alpha = 0.75f)), RoundedCornerShape(18.dp)),
    ) {
        Text(
            text = "✕",
            color = NeonRedAlert,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * Helper: holder for the dialog properties most NavHost.dialog() routes need
 * when wrapping content in a NeonBottomSheet. Use as:
 *   `dialog(route, dialogProperties = NeonBottomSheetDialogProps())`
 */
fun bottomSheetDialogProperties(
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,                // scrim handled by sheet itself
): androidx.compose.ui.window.DialogProperties =
    androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        decorFitsSystemWindows = false,
    )

