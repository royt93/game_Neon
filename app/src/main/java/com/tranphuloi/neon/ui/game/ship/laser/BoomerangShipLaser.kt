package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Wave 18 — Dép Lào (SANDAL) boomerang laser. Bay LÊN tới đỉnh (~28% chiều cao
 * màn từ trên xuống) rồi QUAY VỀ phía tàu, nên đánh trúng địch ở cả 2 chiều
 * (lên + về). Tự huỷ khi rơi về lại mức phóng ban đầu (khỏi cần side-cull).
 *
 * Xoay liên tục (rotation += SPIN) cho cảm giác "dép xoay tít" như boomerang.
 * Collision tái dùng cơ chế hitsRemaining (giống [BounceShipLaser]): mỗi lần
 * trúng trừ 1, hết thì huỷ — để không vô hạn xuyên khi bay 2 chiều.
 */
@Keep
data class BoomerangShipLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float,
    private val yRange: Float,
    override var width: Float = 16f,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    override val yOffsetMovementSpeed: Float = BOOMERANG_SPEED
    override var height: Float = 20f
    override var rotation: Float = 0f
    override var impactPower: Float = 25f
    override val drawableId: Int = R.drawable.ic_laser_blue_7
    override var destroyed: Boolean = false
    override val bulletType: BulletType = BulletType.SANDAL

    /** Mức y lúc phóng — boomerang tự huỷ khi rơi về lại đây. */
    private val launchY: Float = yOffset

    /**
     * Đỉnh quỹ đạo (toạ độ y nhỏ = cao) — TƯƠNG ĐỐI với điểm phóng: luôn bay
     * lên một quãng [APEX_RISE] rồi quay về, kẹp trong mép trên ([yRange] *
     * 0.06). Trước đây cố định ở 28% màn → nếu tàu ở nửa TRÊN (launchY <
     * 0.28*yRange) thì đạn quay về tức thì (phí). Nay luôn lên một quãng có nghĩa.
     */
    private val apexY: Float = (launchY - APEX_RISE).coerceAtLeast(yRange * 0.06f)

    /** false = đang bay lên; true = đang quay về tàu. */
    var returning: Boolean = false
        private set

    /** Số lần còn trúng trước khi huỷ (đánh được cả lượt lên + về). */
    var hitsRemaining: Int = 4

    /**
     * Wave 18 — chống cắm-hết-hit-vào-1-địch. collision chạy Millis(1) nên nếu
     * không có cooldown, đạn chồng lên 1 địch vài ms sẽ tiêu hết hitsRemaining
     * tại chỗ → không kịp quay về. Mỗi địch chỉ tính 1 hit mỗi [REHIT_COOLDOWN_MS]
     * → cùng 1 địch ăn 1 đòn lượt LÊN + 1 đòn lượt VỀ.
     */
    private val lastHitAtByEnemy: MutableMap<String, Long> = mutableMapOf()

    /** True nếu được phép trúng [enemyId] tại [now] (ngoài cooldown). */
    fun canHit(enemyId: String, now: Long): Boolean {
        val last = lastHitAtByEnemy[enemyId] ?: return true
        return now - last >= REHIT_COOLDOWN_MS
    }

    /** Ghi nhận vừa trúng [enemyId] tại [now] để bắt đầu cooldown. */
    fun registerHit(enemyId: String, now: Long) {
        lastHitAtByEnemy[enemyId] = now
    }

    override fun moveLaser() {
        rotation += BOOMERANG_SPIN
        if (!returning) {
            yOffset -= yOffsetMovementSpeed
            if (yOffset <= apexY) returning = true
        } else {
            yOffset += yOffsetMovementSpeed
            // Rơi về lại mức phóng (hoặc xuống dưới đáy) → huỷ.
            if (yOffset >= launchY || yOffset >= yRange) destroyed = true
        }
    }

    companion object {
        const val BOOMERANG_SPEED: Float = 8f
        const val BOOMERANG_SPIN: Float = 14f

        /** Quãng bay lên (px) trước khi quay về — tương đối với điểm phóng. */
        const val APEX_RISE: Float = 440f

        /** Cùng 1 địch chỉ ăn lại đòn sau quãng này (ms). */
        const val REHIT_COOLDOWN_MS: Long = 180L
    }
}
