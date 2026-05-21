package com.tranphuloi.neon.ui.game.buff

import androidx.compose.runtime.Immutable

/**
 * Wave 4 (42x) round 34 — roguelike buffs. After every boss kill, player picks
 * 1 of 3 random buffs. Buff stacks persist for rest of run (cleared on death
 * via checkpoint clear, but since round 26 death keeps checkpoint, buffs DO
 * persist into retry. This is intentional — death feels less punishing).
 *
 * Each buff has displayName, description, glyph icon, and multiplicative
 * effects on hp/damage/speed/magnet/score. ActiveBuffs holder in GameState
 * sums multipliers when feeding EffectiveStats.
 */
@Immutable
enum class RunBuff(
    val key: String,
    val displayName: String,
    val description: String,
    val glyph: String,
    val hpMul: Float = 1f,
    val damageMul: Float = 1f,
    val speedMul: Float = 1f,
    val magnetMul: Float = 1f,
    val scoreMul: Float = 1f,
) {
    HP_BOOST(
        key = "buff_hp",
        displayName = "GIA CỐ THÂN",
        description = "+25% HP tối đa",
        glyph = "♥",
        hpMul = 1.25f,
    ),
    DAMAGE_BOOST(
        key = "buff_dmg",
        displayName = "HỎA LỰC TĂNG",
        description = "+30% sát thương",
        glyph = "⚔",
        damageMul = 1.30f,
    ),
    SPEED_BOOST(
        key = "buff_spd",
        displayName = "TĂNG TỐC",
        description = "+25% tốc độ di chuyển",
        glyph = "⚡",
        speedMul = 1.25f,
    ),
    MAGNET_BOOST(
        key = "buff_mag",
        displayName = "TỪ TRƯỜNG",
        description = "+40% bán kính nam châm",
        glyph = "◉",
        magnetMul = 1.40f,
    ),
    SCORE_BOOST(
        key = "buff_score",
        displayName = "NHÂN ĐIỂM",
        description = "+25% điểm thưởng",
        glyph = "★",
        scoreMul = 1.25f,
    ),
    BALANCED(
        key = "buff_balanced",
        displayName = "CÂN BẰNG",
        description = "+10% tất cả chỉ số",
        glyph = "✦",
        hpMul = 1.10f,
        damageMul = 1.10f,
        speedMul = 1.10f,
        magnetMul = 1.10f,
        scoreMul = 1.10f,
    ),
    BERSERKER(
        key = "buff_berserker",
        displayName = "CUỒNG BẠO",
        description = "+50% sát thương, -10% HP",
        glyph = "☠",
        hpMul = 0.90f,
        damageMul = 1.50f,
    ),
    FORTRESS(
        key = "buff_fortress",
        displayName = "PHÁO ĐÀI",
        description = "+50% HP, -10% tốc độ",
        glyph = "⊞",
        hpMul = 1.50f,
        speedMul = 0.90f,
    ),
    GAMBLER(
        key = "buff_gambler",
        displayName = "ĐÁNH BẠC",
        description = "Điểm ×2, HP ×0.7",
        glyph = "✪",
        hpMul = 0.70f,
        scoreMul = 2.0f,
    );

    companion object {
        /** 3 random buffs for post-boss picker — no repeats. */
        fun pickThree(): List<RunBuff> = values().toList().shuffled().take(3)

        fun fromKey(key: String): RunBuff? = values().firstOrNull { it.key == key }
    }
}
