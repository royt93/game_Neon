package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.R

/**
 * Task 09 (đợt 3) — kiểu tấn công RIÊNG cho 5 địch chủ đề mới (Wave 9a hoàn tất).
 *
 * Tách khỏi `drawableId` để [com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy]
 * dispatch attack qua field enum (TESTABLE thuần trên JVM, không phụ thuộc R —
 * test dựng `RegularEnemyType(attackKind = ...)` trực tiếp). Production map
 * drawable → attackKind qua [forDrawable] ở Stage.buildGameStage.
 *
 * Mỗi kind = 1 hình dạng đòn khác biệt (không tăng DPS quá tay — điểm nhấn là
 * pattern né được):
 *  - [SINGLE]  1 tia giữa (mặc định — mọi địch cũ giữ nguyên).
 *  - [SPLIT]   Splitter: 2 tia từ 2 mép toả ngoài ("kéo mở").
 *  - [CONE3]   Repulsor: nón 3 tia (giữa + 2 bên) dồn ép.
 *  - [CURVE2]  Jammer: 2 tia bay CONG thất thường (LaserMotion.CURVE).
 *  - [VOLLEY3] Missileer: loạt 3 tia thẳng xuống lệch ngang ("volley").
 *  - [HOMING1] Predator: 1 tia HOMING bám tàu.
 */
enum class EnemyAttackKind {
    SINGLE, SPLIT, CONE3, CURVE2, VOLLEY3, HOMING1 ;

    companion object {
        /** Map drawableId → attack. Chỉ 5 địch mới có attack riêng; còn lại SINGLE. */
        fun forDrawable(drawableId: Int): EnemyAttackKind = when (drawableId) {
            R.drawable.enemy_splitter -> SPLIT
            R.drawable.enemy_repulsor -> CONE3
            R.drawable.enemy_jammer -> CURVE2
            R.drawable.enemy_missileer -> VOLLEY3
            R.drawable.enemy_predator -> HOMING1
            else -> SINGLE
        }
    }
}
