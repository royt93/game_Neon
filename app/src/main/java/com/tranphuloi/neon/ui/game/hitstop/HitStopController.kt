package com.tranphuloi.neon.ui.game.hitstop

import com.tranphuloi.neon.utils.Logger

/**
 * 18c: Hit-stop / hit-pause — freeze game tick briefly on enemy kill for "juice" feedback.
 *
 *  - Regular enemy kill → 60ms freeze
 *  - Boss kill → 120ms freeze + screen flash trigger
 *
 * Controlled by setting an `unfrozenAtMillis` deadline; the game loop checks
 * `isFrozen()` before processing tinker work. Lightweight — no Composable.
 */
class HitStopController(
    private val onBossKillFlash: () -> Unit = {},
) {

    @Volatile
    private var unfrozenAtMillis: Long = 0L

    init {
        Logger.d("HitStopController init")
    }

    fun freezeForEnemyKill() {
        val now = System.currentTimeMillis()
        unfrozenAtMillis = maxOf(unfrozenAtMillis, now + ENEMY_FREEZE_MS)
    }

    fun freezeForBossKill() {
        val now = System.currentTimeMillis()
        unfrozenAtMillis = maxOf(unfrozenAtMillis, now + BOSS_FREEZE_MS)
        Logger.d("HitStopController: boss kill freeze ${BOSS_FREEZE_MS}ms + flash trigger")
        onBossKillFlash()
    }

    /**
     * 80ms hit-stop on every laser→enemy hit. Cooldown ensures rapid-fire hits
     * don't compound (e.g., 5 hits in 200ms still result in only ~80ms freeze).
     */
    fun freezeForHit() {
        val now = System.currentTimeMillis()
        if (now >= unfrozenAtMillis) {
            unfrozenAtMillis = now + HIT_FREEZE_MS
        }
    }

    fun isFrozen(now: Long = System.currentTimeMillis()): Boolean = now < unfrozenAtMillis

    companion object {
        const val ENEMY_FREEZE_MS: Long = 60L
        const val BOSS_FREEZE_MS: Long = 120L
        const val HIT_FREEZE_MS: Long = 80L
    }
}
