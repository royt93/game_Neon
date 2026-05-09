package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.pickup.PickupPopup
import kotlinx.coroutines.delay

/**
 * 4c: Floating "+1" / "+1 ×3 = +3" popup overlay.
 * Float upward 30dp + alpha fade over 500ms lifetime.
 */
@Composable
fun PickupPopupOverlay(popups: List<PickupPopup>) {
    if (popups.isEmpty()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasPopups = popups.isNotEmpty()
    LaunchedEffect(hasPopups) {
        if (!hasPopups) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(50L)
        }
    }

    popups.forEach { p ->
        val t = p.progress(nowMillis)
        if (t >= 1f) return@forEach
        val yOffset = p.initialY - PickupPopup.FLOAT_DISTANCE * t
        // Always gold (was magenta-on-combo which confused users); larger size for combo
        // emphasizes the bonus amount without changing color.
        Text(
            text = p.text,
            color = NeonGold,
            fontSize = if (p.isComboBonus) 14.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = p.xOffset.dp, y = yOffset.dp)
                .graphicsLayer { alpha = 1f - t },
        )
    }
}
