package com.tranphuloi.neon.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onStartGame: () -> Unit) {

    LaunchedEffect(Unit) {
        Logger.d("SplashScreen entered, delay 1200ms before start")
        delay(1200)
        Logger.d("SplashScreen delay done → onStartGame()")
        onStartGame()
    }

    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashPulseScale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashPulseAlpha"
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonBgDeep, NeonBgMid, NeonBgEdge)
                )
            )
    ) {

        val textXOffset = remember { Animatable(-350f) }
        LaunchedEffect(Unit) {
            textXOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(360.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = pulseAlpha),
                            NeonMagenta.copy(alpha = pulseAlpha * 0.6f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2 * pulseScale,
                    ),
                    radius = size.minDimension / 2 * pulseScale,
                    center = center,
                )
            }
            Image(
                painter = painterResource(id = R.drawable.splash_image),
                contentDescription = stringResource(id = R.string.splash),
                modifier = Modifier
                    .size(250.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val density = LocalDensity.current
        Box(modifier = Modifier.clip(RectangleShape)) {
            Text(
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.h2,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.offset {
                    IntOffset(x = with(density) { textXOffset.value.dp.roundToPx() }, y = 0)
                }
            )
        }
    }
}
