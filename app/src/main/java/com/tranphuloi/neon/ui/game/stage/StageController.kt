package com.tranphuloi.neon.ui.game.stage

import androidx.compose.runtime.saveable.Saver
import com.tranphuloi.neon.utils.DateUtils
import com.tranphuloi.neon.utils.Logger

class StageController(
    private val dateUtils: DateUtils = DateUtils(),
    private var stageIndex: Int = 0,
    private var stageStartSnapshotMillis: Long = dateUtils.currentTimeMillis(),
) {

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
