package com.tranphuloi.neon.ui.game.controls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

/**
 * 1c: Compact top-screen boss HP bar.
 * Compacted per user feedback — single tight row 200dp wide × ~22dp tall.
 */
@Composable
fun BossHpBar(
    enemies: List<EnemyUI>,
    modifier: Modifier = Modifier,
) {
    val boss = enemies.firstOrNull { it.isBoss } ?: return
    val ratio = (boss.currentHp / boss.initialHp).coerceIn(0f, 1f)

    val pulse = rememberInfiniteTransition(label = "bossPulse")
    val warningAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bossWarningAlpha",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(200.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = boss.displayName,
                color = NeonRedAlert.copy(alpha = warningAlpha),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
            Box(modifier = Modifier.width(6.dp))
            Text(
                text = "${boss.currentHp.toInt()}/${boss.initialHp.toInt()}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // Wave 17m — hiện CHIÊU THỨC dưới tên để người chơi nhận ra từng boss khác
        // nhau (user: "không thấy boss riêng biệt"). Đọc theo bossKind.
        val skill = bossSkillLabel(boss.bossKind)
        if (skill.isNotEmpty()) {
            Text(
                text = "⚔ $skill",
                color = Color(0xFFFFC400).copy(alpha = 0.9f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.55f))
                .neonGlow(
                    color = NeonRedAlert,
                    intensity = 0.25f + (1f - ratio) * 0.35f,
                    radiusFactor = 1.2f,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = ratio)
                    .clip(MaterialTheme.shapes.small)
                    .background(NeonRedAlert),
            )
        }
    }
}

/** Wave 17m — chiêu thức ngắn theo BossKind (hiện dưới tên trên HP bar) để
 *  người chơi nhận ra từng boss khác nhau. Phủ cả 33 kind (mid + final). */
private fun bossSkillLabel(kind: BossKind?): String = when (kind) {
    null -> ""
    BossKind.STAR -> "Vòng tia 8 hướng"
    BossKind.CROSS -> "Quét trục luân phiên"
    BossKind.ORB -> "Tia mắt săn đuổi"
    BossKind.FRACTAL -> "Quỹ đạo nguyên tử"
    BossKind.SPIDER -> "Tơ nhện đa hướng"
    BossKind.DEATH_MOON -> "Lực hấp dẫn kéo"
    BossKind.HAUNTED_KID -> "Ám khắp đỉnh màn"
    BossKind.HELL_LORD -> "Tia mắt địa ngục"
    BossKind.SATAN_GLYPH -> "Ấn quỷ tỏa"
    BossKind.HEN_MOTHER -> "Ổ trứng rơi chậm"
    BossKind.BUFFALO_RAGE -> "Húc sừng gia tốc"
    BossKind.DUMB_RAT -> "Gặm nhấm thất thường"
    BossKind.FIERCE_TIGER -> "Gầm: tường có khe"
    BossKind.SEXY_DIVA -> "Quất tóc bay cong"
    BossKind.TROLL_TOWER -> "Tia dọc từ đỉnh"
    BossKind.TWIN_SUMMITS -> "Tia kép song song"
    BossKind.VOID_GLOBES -> "Xé hư vô hình X"
    BossKind.WHITE_DRAGON -> "Thét luồng lửa"
    BossKind.HAMMER_SICKLE -> "Quăng búa & liềm"
    BossKind.MONEY_TYCOON -> "Mưa tiền khắp màn"
    BossKind.GOLDEN_TYCOON -> "Đô la xoáy ốc"
    BossKind.SKULL_CROSSBONES -> "Quạt xương xoay"
    BossKind.VAMPIRE -> "Bầy dơi hút máu"
    BossKind.COSMIC_CENTIPEDE -> "Phun độc cung rộng"
    BossKind.GIANT_CONDOM -> "Sóng xung kích"
    BossKind.VENOM_SPIDER -> "Lưới tơ độc 8 hướng"
    BossKind.CORRUPTION -> "Tường tiền đè, khe quét"
    BossKind.TRAFFIC_JAM -> "Lấp làn trái/phải"
    BossKind.KPI_BOSS -> "Cột chỉ tiêu + deadline"
    BossKind.TIKTOKER -> "Spam tim bay cong"
    BossKind.ATM_BANKRUPT -> "Nhả tiền dồn rồi kẹt"
    BossKind.NOKIA_BRICK -> "Ném gạch nặng chậm"
    BossKind.INFLATION_STORM -> "Đạn nhiều dần + tăng tốc"
}
