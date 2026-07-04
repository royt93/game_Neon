package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.R

/**
 * Round 74 (R73d) — Wave 9a: 5 enemy family với stat profile riêng.
 * Mỗi family bao gồm 4 drawable variant → tổng 20 regular enemy.
 *
 *   SCOUT     — light_blue family (5 variant): nhanh, máu thấp, dart shape
 *   FIGHTER   — green family (4 variant): cân bằng, hexagon shape
 *   HEAVY     — red family (3 variant + 1 spike): chậm, máu cao, diamond/spike
 *   ELITE     — cross + orb family (4 variant): trung bình, shape khác biệt
 *   BERSERKER — chevron (2 variant) + spike (2 variant): tấn công nhanh, máu vừa
 *
 * Stat multipliers apply trên top base values trong Stage.buildRegularEnemyType.
 */
enum class EnemyFamily(
    val displayName: String,
    val hpMul: Float,
    val speedMul: Float,
    val impactMul: Float,
) {
    SCOUT(displayName = "Trinh sát", hpMul = 0.7f, speedMul = 1.3f, impactMul = 0.8f),
    FIGHTER(displayName = "Chiến đấu", hpMul = 1.0f, speedMul = 1.0f, impactMul = 1.0f),
    HEAVY(displayName = "Nặng", hpMul = 1.6f, speedMul = 0.7f, impactMul = 1.3f),
    ELITE(displayName = "Tinh nhuệ", hpMul = 1.25f, speedMul = 1.1f, impactMul = 1.1f),
    BERSERKER(displayName = "Cuồng sát", hpMul = 0.9f, speedMul = 1.2f, impactMul = 1.4f),
    ;

    companion object {
        /** Map drawableId → family. Used by Stage spawn + EnemyCanvas color/shape. */
        fun fromDrawableId(drawableId: Int): EnemyFamily = when (drawableId) {
            R.drawable.enemy_light_blue_1, R.drawable.enemy_light_blue_2,
            R.drawable.enemy_light_blue_3, R.drawable.enemy_light_blue_4,
            R.drawable.enemy_light_blue_5 -> SCOUT
            R.drawable.enemy_green_1, R.drawable.enemy_green_2,
            R.drawable.enemy_green_3, R.drawable.enemy_green_4 -> FIGHTER
            R.drawable.enemy_red_1, R.drawable.enemy_red_2,
            R.drawable.enemy_red_3 -> HEAVY
            R.drawable.enemy_cross_1, R.drawable.enemy_cross_2,
            R.drawable.enemy_orb_1, R.drawable.enemy_orb_2 -> ELITE
            R.drawable.enemy_chevron_1, R.drawable.enemy_chevron_2,
            R.drawable.enemy_spike_1, R.drawable.enemy_spike_2 -> BERSERKER
            // Task 09 (đợt 3) — 5 địch chủ đề mới: gán family theo tính cách đòn.
            R.drawable.enemy_splitter -> ELITE       // phân thân, cân bằng
            R.drawable.enemy_repulsor -> HEAVY       // tank đẩy lùi, chậm & trâu
            R.drawable.enemy_jammer -> SCOUT         // nhiễu sóng, nhanh & mỏng
            R.drawable.enemy_missileer -> BERSERKER  // pháo thủ, đòn nặng
            R.drawable.enemy_predator -> FIGHTER     // săn mồi, cân bằng
            else -> FIGHTER
        }
    }
}
