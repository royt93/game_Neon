package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.Keep
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.laser.Laser

@Keep
data class UltimateLaser(
    override val id: String,
    override var xOffset: Float,
    override var yOffset: Float = -10f,
    private val yRange: Float,
) : Laser {

    override val xOffsetMovementSpeed: Float = 0f
    // Pixel-3 round 4 — slow beam from 7 → 5 px/tick to dwell longer at
    // bottom band (~1.2s visible instead of ~0.7s) at FAR camera zoom.
    // Total sweep ~10s instead of 7.2s. User reported "splash xanh không
    // phủ full screen" partly because beam moved through bottom too fast.
    override val yOffsetMovementSpeed: Float = 5f
    override var width: Float = 30f
    // Pixel-3 round 4 — taller beam (30 → 60 dp) so visual "vùng effect"
    // feels heavier + covers more vertical area per frame. Combined with
    // slower speed, bottom band sees beam-coverage for ~1.5s post-fire.
    override var height: Float = 60f
    override var rotation: Float = 0f
    override var impactPower: Float = 1000f
    override val drawableId: Int = R.drawable.ic_laser_blue_11
    override var destroyed: Boolean = false

    /**
     * Wave 14b — boss ids this beam has already damaged.
     *
     * The ultimate beam is NOT destroyed on hit (it sweeps through the whole
     * screen), so [LasersController.monitorLaserCollision] re-overlaps every
     * `Millis(1)` tick. Against a regular mob that's harmless (it dies on the
     * first hit), but against a boss it re-applied [impactPower] every frame
     * of overlap → the boss evaporated in a fraction of a second (user bug
     * report: "quét sạch toàn bộ enemy và boss"). We record each boss here so
     * a single beam chips a boss exactly once, for [BOSS_DAMAGE_PER_HIT].
     */
    val hitBossIds: MutableSet<String> = mutableSetOf()

    override fun moveLaser() {
        rotation += 7f
        if (rotation > 360f) rotation = 0f

        yOffset -= yOffsetMovementSpeed
    }

    companion object {
        /**
         * Wave 14b — flat damage a single ultimate beam deals to a boss
         * (multiplied by the run's damage multiplier at the call site), used
         * as the UPPER bound of [bossChipDamage].
         *
         * Sized so a full ChargeShot sweep (≈2–3 of the 9 beams overlap a
         * boss hitbox) chips a meaningful slice without one-shotting. Regular
         * mobs are unaffected — they still take the full
         * [UltimateLaser.impactPower] and die instantly.
         */
        const val BOSS_DAMAGE_PER_HIT = 250f

        /**
         * Wave 14b audit — hard cap on a single beam's boss chip as a fraction
         * of the boss's SPAWN HP ([Enemy.initialHp]).
         *
         * Without this, a heavily damage-upgraded run (high `dmgMul`) scaled
         * [BOSS_DAMAGE_PER_HIT] back up enough to again approach one-shotting
         * the weakest boss. Capping each beam at 8% of spawn HP makes a full
         * sweep (≈2–3 beams ≈ ≤24%) mathematically unable to one-shot ANY
         * boss, regardless of upgrades — while keeping the chip a consistent
         * percentage across the whole boss roster.
         */
        const val BOSS_MAX_HP_FRACTION = 0.08f

        /**
         * Per-beam chip a boss takes, = min(flat × dmgMul, 8% of spawn HP).
         * Pure + side-effect-free so it can be unit-tested directly; the
         * `≤ 8% of initialHp` term is the no-one-shot guarantee.
         */
        fun bossChipDamage(dmgMul: Float, bossInitialHp: Float): Float =
            minOf(BOSS_DAMAGE_PER_HIT * dmgMul, bossInitialHp * BOSS_MAX_HP_FRACTION)
    }
}
