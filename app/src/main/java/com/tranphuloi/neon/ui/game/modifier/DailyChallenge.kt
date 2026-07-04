package com.tranphuloi.neon.ui.game.modifier

/**
 * Task 08 — Thử thách hằng ngày: chọn 1 [RunModifier] DETERMINISTIC theo ngày (UTC
 * day-key) để mọi người chơi cùng modifier trong ngày, đua trên daily leaderboard.
 *
 * THUẦN (không Compose/Android) → test JVM. `dayKey` lấy từ
 * [com.tranphuloi.neon.data.LeaderboardRepository.todayUtcDayKey].
 * Deterministic-spawn (cùng địch) chưa làm — chỉ modifier là cố định theo ngày.
 */
object DailyChallenge {
    /** Minerals thưởng khi hoàn thành thử thách ngày (1 lần/ngày). */
    const val REWARD_MINERALS: Int = 100

    /** Pool = mọi modifier trừ NONE (thử thách luôn có đánh đổi). */
    private val POOL: List<RunModifier> = RunModifier.entries.filter { it != RunModifier.NONE }

    /** Modifier của ngày [dayKey] — cùng dayKey luôn ra cùng kết quả. */
    fun modifierFor(dayKey: Long): RunModifier {
        if (POOL.isEmpty()) return RunModifier.NONE
        val idx = Math.floorMod(dayKey, POOL.size.toLong()).toInt()
        return POOL[idx]
    }
}
