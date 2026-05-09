package com.tranphuloi.neon.ui.game.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.utils.Logger

@Composable
fun ButtonsMovement(
    onMoveLeft: (Boolean) -> Unit,
    onMoveRight: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {

    val buttonSize = dimensionResource(id = R.dimen.button_size)
    val buttonPadding = dimensionResource(id = R.dimen.button_padding)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .padding(buttonPadding)
            .fillMaxWidth()
    ) {
        MovementButton(
            painterId = R.drawable.button_move_left_purple,
            contentDescription = stringResource(id = R.string.game_left_button),
            glowColor = NeonCyan,
            buttonSize = buttonSize,
            label = "LEFT",
            onPressedChange = onMoveLeft,
        )
        MovementButton(
            painterId = R.drawable.button_move_right_purple,
            contentDescription = stringResource(id = R.string.game_right_button),
            glowColor = NeonMagenta,
            buttonSize = buttonSize,
            label = "RIGHT",
            onPressedChange = onMoveRight,
        )
    }
}

@Composable
private fun MovementButton(
    painterId: Int,
    contentDescription: String,
    glowColor: Color,
    buttonSize: androidx.compose.ui.unit.Dp,
    label: String,
    onPressedChange: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "btnScale"
    )
    // Image alpha: semi-transparent at rest, more opaque when pressed so user feedback is clear.
    val imageAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 0.45f,
        animationSpec = tween(durationMillis = 150),
        label = "btnImageAlpha"
    )
    val ringExpand by animateFloatAsState(
        targetValue = if (pressed) 1.55f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "btnRingExpand"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "btnRingAlpha"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.55f else 0.18f,    // always-on subtle halo at rest
        animationSpec = tween(durationMillis = 150),
        label = "btnHaloAlpha"
    )

    Box(
        modifier = Modifier
            .size(buttonSize)
            .drawBehind {
                // Always-on subtle halo: makes the transparent image visible against
                // any background while preserving see-through neon aesthetic.
                if (haloAlpha > 0f) {
                    val haloRadius = size.minDimension / 2 * 1.4f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = haloAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = haloRadius,
                        ),
                        radius = haloRadius,
                        center = center
                    )
                }
                // Always-on outer ring stroke — neon outline so user sees the tap zone.
                drawCircle(
                    color = glowColor.copy(alpha = 0.55f),
                    radius = size.minDimension / 2 * 0.95f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Press feedback: expanding ring on top.
                if (ringAlpha > 0f) {
                    drawCircle(
                        color = glowColor.copy(alpha = ringAlpha),
                        radius = size.minDimension / 2 * ringExpand,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(id = painterId),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = imageAlpha
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            Logger.d("MovementButton[$label] PRESS")
                            pressed = true
                            onPressedChange(true)
                            this.awaitRelease()
                            Logger.d("MovementButton[$label] RELEASE")
                            onPressedChange(false)
                            pressed = false
                        }
                    )
                }
        )
    }
}
