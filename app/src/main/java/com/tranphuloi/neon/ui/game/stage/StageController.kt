package com.tranphuloi.neon.ui.game.stage

import androidx.compose.runtime.saveable.Saver
import com.tranphuloi.neon.utils.DateUtils
import com.tranphuloi.neon.utils.Logger

class StageController(
    private val dateUtils: DateUtils = DateUtils(),
    private var stageIndex: Int = 0,
    private var stageStartSnapshotMillis: Long = dateUtils.currentTimeMillis(),
    private val onStageAdvance: (newIndex: Int, newStage: Stage) -> Unit = { _, _ -> },
) {

    fun currentIndex(): Int = stageIndex

    /**
     * 32d Wave 4 — current chapter id (1..5) derived from active stage. O(1) since
     * every stage carries a chapterId (StageMessage default 0 → fallback walk).
     * Round-20 hotfix: previously walked backward only, which made chapter intro
     * messages ("CHAPTER 2 / NEBULA CLOUD / GO!") inherit the PREVIOUS chapter's id
     * → stageTint stuck on chapter 1's gold during chapter 2's intro for ~7s.
     * Now StageMessage entries built in `buildStageScript` carry chapterId directly.
     */
    fun currentChapterId(): Int {
        val s = stages[stageIndex]
        val direct = when (s) {
            is StageGame -> s.chapterId
            is StageBoss -> s.chapterId
            is StageMessage -> s.chapterId
            else -> 0
        }
        if (direct > 0) return direct
        // Fallback for any StageMessage that wasn't tagged (defensive — shouldn't happen
        // with current buildStageScript, but kept so legacy / hand-edited scripts work).
        for (i in stageIndex downTo 0) {
            val st = stages[i]
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
        val s = stages[stageIndex]
        return if (s is StageGame) s.hazard else null
    }

    fun getGameStage(readyForNextStage: Boolean = false): Stage {
        val stageTimeExpired =
            stageStartSnapshotMillis + getStage().durationSec * 1000 < dateUtils.currentTimeMillis()
        val hasNextStage = stageIndex < stages.lastIndex
        if (stageTimeExpired && hasNextStage) {
            if (!readyForNextStage) return StageBreak
            val from = stageIndex
            stageIndex++
            stageStartSnapshotMillis = dateUtils.currentTimeMillis()
            Logger.d("StageController advance: index $from → $stageIndex (${stages[stageIndex]::class.simpleName})")
            onStageAdvance(stageIndex, stages[stageIndex])
        }
        return getStage()
    }

    private fun getStage(): Stage {
        return stages[stageIndex]
    }

    companion object {
        fun saver(): Saver<StageController, *> = Saver(
            save = {
                Pair(it.stageIndex, it.stageStartSnapshotMillis)
            },
            restore = {
                StageController(stageIndex = it.first, stageStartSnapshotMillis = it.second)
            }
        )
    }
}
