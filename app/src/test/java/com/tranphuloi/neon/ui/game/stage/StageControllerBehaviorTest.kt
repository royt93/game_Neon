package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [StageController] — chapter-id fallback walk-back,
 * script size via [StageProvider], hazard lookup, and stage-expiry advance.
 */
class StageControllerBehaviorTest {

    private fun stageGame(durationSec: Int = 100, chapterId: Int = 1, hazard: HazardType? = null) =
        StageGame(enemyType = LevelOneBossType, durationTimeSec = durationSec, hazard = hazard, chapterId = chapterId)

    // ── scriptSize() qua StageProvider ──

    @Test
    fun `scriptSize reflects provider list size`() {
        val provider = StaticListProvider(list = listOf(stageGame(), stageGame(), StageBreak))
        val ctrl = StageController(provider = provider)
        assertEquals(3, ctrl.scriptSize())
    }

    // ── currentChapterId() ──

    @Test
    fun `currentChapterId returns direct chapterId when stage carries one`() {
        val provider = StaticListProvider(
            list = listOf(StageMessage("intro", 1000, chapterId = 0), stageGame(chapterId = 2)),
        )
        val ctrl = StageController(stageIndex = 1, provider = provider)
        assertEquals("StageGame ở index 1 có chapterId=2 trực tiếp", 2, ctrl.currentChapterId())
    }

    @Test
    fun `currentChapterId falls back to nearest prior chapter when current stage has none`() {
        // index 1 là StageMessage chapterId mặc định 0 (chưa gán) → phải walk-back
        // về index 0 (StageGame chapterId=2) thay vì trả về 0/1 sai.
        val provider = StaticListProvider(
            list = listOf(stageGame(chapterId = 2), StageMessage("chapter 2 intro", 1000)),
        )
        val ctrl = StageController(stageIndex = 1, provider = provider)
        assertEquals("fallback walk-back phải tìm ra chapterId=2 từ stage trước đó", 2, ctrl.currentChapterId())
    }

    @Test
    fun `currentChapterId defaults to 1 when no prior stage has a chapter`() {
        val provider = StaticListProvider(list = listOf(StageMessage("intro", 1000, chapterId = 0)))
        val ctrl = StageController(stageIndex = 0, provider = provider)
        assertEquals("không có chapter nào trước đó → mặc định 1", 1, ctrl.currentChapterId())
    }

    @Test
    fun `currentChapterId returns 1 when index out of range`() {
        val provider = StaticListProvider(list = listOf(stageGame()))
        val ctrl = StageController(stageIndex = 5, provider = provider)
        assertEquals(1, ctrl.currentChapterId())
    }

    // ── currentHazard() — chỉ StageGame mang hazard ──

    @Test
    fun `currentHazard returns hazard only for StageGame`() {
        val provider = StaticListProvider(list = listOf(stageGame(hazard = HazardType.ASTEROID_STORM)))
        val ctrl = StageController(stageIndex = 0, provider = provider)
        assertEquals(HazardType.ASTEROID_STORM, ctrl.currentHazard())
    }

    @Test
    fun `currentHazard is null for non-StageGame stages`() {
        val provider = StaticListProvider(list = listOf(StageMessage("intro", 1000)))
        val ctrl = StageController(stageIndex = 0, provider = provider)
        assertNull(ctrl.currentHazard())
    }

    // ── getGameStage() — expiry + advance ──

    @Test
    fun `getGameStage returns StageBreak when expired but not ready to advance`() {
        val provider = StaticListProvider(list = listOf(stageGame(durationSec = 1), stageGame(durationSec = 100)))
        val ctrl = StageController(
            stageStartSnapshotMillis = DateUtils().currentTimeMillis() - 5_000,
            provider = provider,
        )
        assertEquals("hết giờ nhưng chưa readyForNextStage → StageBreak", StageBreak, ctrl.getGameStage(readyForNextStage = false))
    }

    @Test
    fun `getGameStage advances index and fires callback when ready`() {
        val provider = StaticListProvider(list = listOf(stageGame(durationSec = 1, chapterId = 1), stageGame(durationSec = 100, chapterId = 3)))
        var advancedIndex = -1
        var advancedStage: Stage? = null
        val ctrl = StageController(
            stageStartSnapshotMillis = DateUtils().currentTimeMillis() - 5_000,
            provider = provider,
            onStageAdvance = { idx, stage -> advancedIndex = idx; advancedStage = stage },
        )
        val result = ctrl.getGameStage(readyForNextStage = true)
        assertEquals(1, ctrl.currentIndex())
        assertEquals(1, advancedIndex)
        assertTrue("stage mới phải là StageGame chapterId=3", advancedStage is StageGame && (advancedStage as StageGame).chapterId == 3)
        assertTrue("kết quả trả về đúng stage mới", result is StageGame && result.chapterId == 3)
    }

    @Test
    fun `getGameStage does not advance past the last stage (campaign end)`() {
        val provider = StaticListProvider(list = listOf(stageGame(durationSec = 1, chapterId = 5)))
        var advanceCalled = false
        val ctrl = StageController(
            stageStartSnapshotMillis = DateUtils().currentTimeMillis() - 5_000,
            provider = provider,
            onStageAdvance = { _, _ -> advanceCalled = true },
        )
        val result = ctrl.getGameStage(readyForNextStage = true)
        assertEquals(0, ctrl.currentIndex())
        assertTrue("không advance khi hết campaign", !advanceCalled)
        assertTrue(result is StageGame && result.chapterId == 5)
    }

    @Test
    fun `getGameStage returns current stage unchanged when not expired`() {
        val provider = StaticListProvider(list = listOf(stageGame(durationSec = 100, chapterId = 1), stageGame(durationSec = 100, chapterId = 2)))
        val ctrl = StageController(provider = provider)
        val result = ctrl.getGameStage(readyForNextStage = true)
        assertEquals(0, ctrl.currentIndex())
        assertTrue(result is StageGame && result.chapterId == 1)
    }
}
