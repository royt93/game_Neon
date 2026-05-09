package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Nb: First-time tutorial overlay. Shows arrows over movement buttons + brief copy.
 * Auto-dismiss after 3.5s OR on tap. Marks tutorial-shown flag in DataStore.
 */
@Composable
fun TutorialOverlay(modifier: Modifier = Modifier) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Logger.d("TutorialOverlay shown")
        delay(3500L)
        if (!dismissed) {
            Logger.d("TutorialOverlay auto-dismiss after 3.5s")
            scope.launch { settings.markTutorialShown() }
            dismissed = true
        }
    }

    if (dismissed) return

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable {
                Logger.d("TutorialOverlay tap-dismiss")
                scope.launch { settings.markTutorialShown() }
                dismissed = true
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 160.dp)
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            Text(
                text = "TAP & HOLD",
                color = NeonCyan,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "the arrows below to move",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxSize()) {
                Text("◀", color = NeonCyan, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                Text("▶", color = NeonMagenta, fontSize = 80.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap anywhere to dismiss",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
