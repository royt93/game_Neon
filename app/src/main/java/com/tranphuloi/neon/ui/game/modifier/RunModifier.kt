package com.tranphuloi.neon.ui.game.modifier

import androidx.compose.runtime.Immutable

/**
 * Wave 5 (25x) — pre-game run modifier. Player picks 1 of 3 random before each run.
 * Multipliers combine with difficulty + meta upgrades; see EffectiveStats for capping rules.
 *
 * NONE is the default sentinel (no modifier picked / skipped).
 */
@Immutable
enum class RunModifier(
    val key: String,
    val displayName: String,
    val description: String,
    val hpMul: Float = 1f,
    val damageMul: Float = 1f,
    val speedMul: Float = 1f,
    val magnetMul: Float = 1f,
    val scoreMul: Float = 1f,
    val noShieldDrops: Boolean = false,
    val bossesOnly: Boolean = false,
) {
    NONE(
        key = "none",
        displayName = "TIÊU CHUẨN",
        description = "Không có buff — chơi mặc định.",
    ),
    TRIPLE_SPEED(
        key = "triple_speed",
        displayName = "TỐC HÀNH",
        description = "Tàu di chuyển nhanh gấp 3. Điểm ×1.5.",
        speedMul = 3f,
        scoreMul = 1.5f,
    ),
    GLASS_CANNON(
        key = "glass_cannon",
        displayName = "MỎNG NHƯ KÍNH",
        description = "HP còn nửa, sát thương ×2. Điểm ×2.",
        hpMul = 0.5f,
        damageMul = 2f,
        scoreMul = 2f,
    ),
    NO_SHIELDS(
        key = "no_shields",
        displayName = "KHÔNG KHIÊN",
        description = "Không có khiên rơi. Điểm ×1.5.",
        noShieldDrops = true,
        scoreMul = 1.5f,
    ),
    SUPER_MAGNET(
        key = "super_magnet",
        displayName = "NAM CHÂM MẠNH",
        description = "Phạm vi hút khoáng vật ×2.",
        magnetMul = 2f,
        scoreMul = 1.1f,
    ),
    BERSERKER(
        key = "berserker",
        displayName = "CUỒNG BẠO",
        description = "Sát thương ×1.5 nhưng HP ×0.75.",
        hpMul = 0.75f,
        damageMul = 1.5f,
        scoreMul = 1.4f,
    ),
    TANK(
        key = "tank",
        displayName = "GIÁP DÀY",
        description = "HP ×1.5 nhưng tốc độ ×0.7.",
        hpMul = 1.5f,
        speedMul = 0.7f,
        scoreMul = 1.2f,
    ),
    BOSSES_ONLY(
        key = "bosses_only",
        displayName = "CHỈ BOSS",
        description = "Bỏ qua tiểu yêu — chỉ đánh boss. Điểm ×2.",
        bossesOnly = true,
        scoreMul = 2f,
    ),
    DOUBLE_OR_NOTHING(
        key = "double_or_nothing",
        displayName = "ĐƯỢC ĂN CẢ",
        description = "Sát thương ×2 nhưng HP ×0.5. Điểm ×2.5.",
        hpMul = 0.5f,
        damageMul = 2f,
        scoreMul = 2.5f,
    );

    companion object {
        fun fromKey(key: String?): RunModifier =
            values().firstOrNull { it.key == key } ?: NONE

        /** 3 random modifiers (excluding NONE) for pre-game pick. */
        fun pickThree(): List<RunModifier> =
            values().filter { it != NONE }.shuffled().take(3)
    }
}
