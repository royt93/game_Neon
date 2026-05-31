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
     * Brief hit-stop on laser→enemy hit. 30ms (was 80ms) + rate cap so rapid-fire
     * triple-laser hits don't cumulatively pause the game (cause of perceived lag).
     */
    private var lastHitFreezeAtMillis: Long = 0L
    fun freezeForHit() {
        val now = System.currentTimeMillis()
        if (now - lastHitFreezeAtMillis < HIT_FREEZE_RATE_CAP_MS) return
        if (now >= unfrozenAtMillis) {
            unfrozenAtMillis = now + HIT_FREEZE_MS
            lastHitFreezeAtMillis = now
        }
    }

    /**
     * Wave 16 — long freeze for the full-screen boss-intro cinematic. Reuses
     * the existing loop gate (`isFrozen()` at the top of the game loop) so the
     * whole simulation truly pauses for the cinematic — no separate freeze path.
     * Unlike the micro hit-stops this is a hard SET (not max-with-existing) so a
     * stray prior hit-stop can't shorten it.
     */
    fun freezeForBossIntro(durationMillis: Long) {
        unfrozenAtMillis = System.currentTimeMillis() + durationMillis
        Logger.d("HitStopController: boss-intro freeze ${durationMillis}ms")
    }

    /** Wave 16 — end the boss-intro freeze early (player tapped "skip"). */
    fun endBossIntroFreeze() {
        unfrozenAtMillis = 0L
        Logger.d("HitStopController: boss-intro freeze ended (skip)")
    }

    fun isFrozen(now: Long = System.currentTimeMillis()): Boolean = now < unfrozenAtMillis

    companion object {
        const val ENEMY_FREEZE_MS: Long = 60L
        const val BOSS_FREEZE_MS: Long = 120L
        const val HIT_FREEZE_MS: Long = 30L
        const val HIT_FREEZE_RATE_CAP_MS: Long = 250L

        /** Wave 16 — boss-intro cinematic hold (matches BossIntroOverlay duration). */
        const val BOSS_INTRO_FREEZE_MS: Long = 2400L
    }
}
