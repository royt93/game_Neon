package com.tranphuloi.neon.ui.game.modifier

/**
 * Task 29 — Sự kiện tuần: chọn 1 [RunModifier] DETERMINISTIC theo tuần (UTC
 * week-key) để mọi người chơi cùng modifier trong tuần, đua trên weekly leaderboard.
 *
 * THUẦN (không Compose/Android) → test JVM. `weekKey` lấy từ
 * [com.tranphuloi.neon.data.LeaderboardRepository.todayUtcWeekKey].
 */
object WeeklyEvent {
    /** Minerals thưởng khi hoàn thành sự kiện tuần (1 lần/tuần). */
    const val REWARD_MINERALS: Int = 300

    /** Pool = mọi modifier trừ NONE (sự kiện luôn có đánh đổi). */
    private val POOL: List<RunModifier> = RunModifier.entries.filter { it != RunModifier.NONE }

    /** Modifier của tuần [weekKey] — cùng weekKey luôn ra cùng kết quả. */
    fun modifierFor(weekKey: Long): RunModifier {
        if (POOL.isEmpty()) return RunModifier.NONE
        val idx = Math.floorMod(weekKey, POOL.size.toLong()).toInt()
        return POOL[idx]
    }
}
