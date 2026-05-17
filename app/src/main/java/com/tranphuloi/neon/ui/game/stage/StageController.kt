package com.tranphuloi.neon.ui.game.stage

import androidx.compose.runtime.saveable.Saver
import com.tranphuloi.neon.utils.DateUtils
import com.tranphuloi.neon.utils.Logger

class StageController(
    private val dateUtils: DateUtils = DateUtils(),
    private var stageIndex: Int = 0,
    private var stageStartSnapshotMillis: Long = dateUtils.currentTimeMillis(),
    /** Wave 5 — pluggable stage source. Default = static buildStageScript() list. */
    private val provider: StageProvider = StaticListProvider(),
    private val onStageAdvance: (newIndex: Int, newStage: Stage) -> Unit = { _, _ -> },
) {

    fun currentIndex(): Int = stageIndex

    /** Wave 5 — total script size from provider; -1 for infinite (procedural) providers. */
    fun scriptSize(): Int = provider.size()

    /**
     * 32d Wave 4 — current chapter id (1..5) derived from active stage. O(1) since
     * every stage carries a chapterId (StageMessage default 0 → fallback walk).
     * Round-20 hotfix: previously walked backward only, which made chapter intro
     * messages ("CHAPTER 2 / NEBULA CLOUD / GO!") inherit the PREVIOUS chapter's id
     * → stageTint stuck on chapter 1's gold during chapter 2's intro for ~7s.
     * Now StageMessage entries built in `buildStageScript` carry chapterId directly.
     *
     * Wave 5 — reads through StageProvider so non-campaign modes (endless, boss
     * rush) can supply chapter context without relying on the global `stages` list.
     */
    fun currentChapterId(): Int {
        if (!provider.hasAt(stageIndex)) return 1
        val direct = provider.chapterAt(stageIndex)
        if (direct > 0) return direct
        // Fallback walk-back — also via provider so non-static providers behave.
        for (i in stageIndex downTo 0) {
            if (!provider.hasAt(i)) continue
            val st = provider.getAt(i)
            when (st) {
                is StageGame -> return st.chapterId
                is StageBoss -> return st.chapterId
                is StageMessage -> if (st.chapterId > 0) return st.chapterId
                else -> Unit
            }
        }
        return 1
    }

    /** 32d active hazard — only StageGame carries hazard tag, null otherwise. */
    fun currentHazard(): HazardType? {
        if (!provider.hasAt(stageIndex)) return null
        val s = provider.getAt(stageIndex)
        return if (s is StageGame) s.hazard else null
    }

    fun getGameStage(readyForNextStage: Boolean = false): Stage {
        val stageTimeExpired =
            stageStartSnapshotMillis + getStage().durationSec * 1000 < dateUtils.currentTimeMillis()
        val hasNextStage = provider.hasAt(stageIndex + 1)
        if (stageTimeExpired && hasNextStage) {
            if (!readyForNextStage) return StageBreak
            val from = stageIndex
            stageIndex++
            stageStartSnapshotMillis = dateUtils.currentTimeMillis()
            Logger.d("StageController advance: index $from → $stageIndex (${provider.getAt(stageIndex)::class.simpleName})")
            onStageAdvance(stageIndex, provider.getAt(stageIndex))
        }
        return getStage()
    }

    private fun getStage(): Stage {
        return provider.getAt(stageIndex)
    }

    companion object {
        /**
         * Builds a Saver bound to a specific [provider]. Restore reuses the live
         * provider (not Serializable) instead of the default static script, so
         * non-CAMPAIGN modes (Survival / BossRush / TimeAttack / Endless) keep
         * working after process-death restore. Round 23 fix.
         *
         * If the restored stageIndex is out-of-range for the current provider
         * (e.g. campaign script shortened, or static StageBoss layout changed
         * between app versions), it's coerced to 0 — the run effectively restarts
         * at the first stage rather than crashing on `provider.getAt(badIndex)`.
         */
        fun saver(provider: StageProvider): Saver<StageController, *> = Saver(
            save = {
                Pair(it.stageIndex, it.stageStartSnapshotMillis)
            },
            restore = { (savedIndex, savedMillis) ->
                val safeIndex = if (provider.hasAt(savedIndex)) savedIndex else {
                    Logger.d("StageController.saver: restored index $savedIndex OOB for provider → reset to 0")
                    0
                }
                StageController(
                    stageIndex = safeIndex,
                    stageStartSnapshotMillis = savedMillis,
                    provider = provider,
                )
            }
        )
    }
}
