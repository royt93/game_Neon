package com.tranphuloi.neon.ui.game.stage

/**
 * Wave 5 foundation — abstracts where stages come from. StageController holds a
 * provider; the default is [StaticListProvider] wrapping the campaign
 * [stages] list (back-compat, zero-behavior-change).
 *
 * Future providers:
 *  - 43x SurvivalProvider: cycle chapter-1 stages indefinitely.
 *  - 23x EndlessProvider: post-campaign procedural scaling.
 *  - 43x BossRushProvider: filter to StageBoss entries only.
 *  - 43x TimeAttackProvider: chapter-1 with strict 60s clock at the outer layer.
 *
 * Providers MUST be deterministic per-index OR generate-on-demand idempotently.
 * `getAt(i)` is called once per stage advance; later calls for the same index
 * may or may not happen, depending on saver restoration timing.
 */
sealed interface StageProvider {
    /** True when there is a stage at [index]; false → campaign ended. */
    fun hasAt(index: Int): Boolean

    /** Returns the stage at [index]. Caller must check [hasAt] first. */
    fun getAt(index: Int): Stage

    /** Optional chapter id at [index]. -1 = unknown / not chapter-based. */
    fun chapterAt(index: Int): Int = -1

    /**
     * Total stage count if known; -1 for infinite/procedural providers.
     * Used by StageController.getGameStage to detect campaign-end transitions.
     */
    fun size(): Int
}

/** Default — wraps the static [stages] list built by `buildStageScript()`. */
class StaticListProvider(private val list: List<Stage> = stages) : StageProvider {
    override fun hasAt(index: Int): Boolean = index in list.indices
    override fun getAt(index: Int): Stage = list[index]
    override fun chapterAt(index: Int): Int {
        if (index !in list.indices) return -1
        return when (val s = list[index]) {
            is StageGame -> s.chapterId
            is StageBoss -> s.chapterId
            is StageMessage -> s.chapterId
            else -> 0
        }
    }
    override fun size(): Int = list.size
}
