package com.tranphuloi.neon.ui.game.story

import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType

/**
 * Wave 5 (47x) — story / lore content registry.
 *
 * Keep lines short (1-2 sentences). Long monologues during gameplay are
 * annoying. Chapter intros: ~4 narration lines played at first entry.
 * Boss intros: 1 line per boss (taunt). Player wins are not narrated to
 * avoid stalling the post-kill flow; we already have BossRankOverlay for that.
 */
object StoryRegistry {

    /** Chapter id (1..5) → ordered list of intro lines (NARRATOR). */
    fun chapterIntro(chapterId: Int): List<StoryLine> = when (chapterId) {
        1 -> listOf(
            StoryLine("ĐỘI TRƯỞNG", "Phi công, ta đã xác định vị trí xác tàu trong Vành đai Tiểu hành tinh."),
            StoryLine("ĐỘI TRƯỞNG", "Coi chừng đá — chúng mạnh hơn nhìn đấy."),
        )
        2 -> listOf(
            StoryLine("ĐỘI TRƯỞNG", "Mây Tinh Vân dày đặc — tầm nhìn sẽ giảm."),
            StoryLine("ĐỘI TRƯỞNG", "Tin vào máy đo. Đừng đứng yên."),
        )
        3 -> listOf(
            StoryLine("ĐỘI TRƯỞNG", "Chào mừng đến Hành Tinh Băng. Mặt đất rất hiểm."),
            StoryLine("ĐỘI TRƯỞNG", "Lạnh không làm chúng chậm lại. Chuẩn bị đi."),
        )
        4 -> listOf(
            StoryLine("ĐỘI TRƯỞNG", "Trạm Thù Địch phía trước — vũ trang đầy đủ."),
            StoryLine("ĐỘI TRƯỞNG", "Mô hình tấn công phi tự nhiên. Thích nghi nhanh."),
        )
        5 -> listOf(
            StoryLine("ĐỘI TRƯỞNG", "Đây rồi — Lõi Thiên Hà."),
            StoryLine("ĐỘI TRƯỞNG", "Hạ Bá Vương. Cả thiên hà trông cậy vào anh."),
        )
        else -> emptyList()
    }

    /** Boss type → taunt shown at boss-spawn. */
    fun bossTaunt(type: EnemyType): StoryLine? = when (type) {
        LevelOneBossType -> StoryLine(
            speaker = "BOSS CẤP 1",
            text = "Tiểu tốt vô danh. Không trụ nổi 1 phút đâu.",
        )
        LevelTwoBossType -> StoryLine(
            speaker = "BOSS CẤP 2",
            text = "Đã nghiền nát phi công mạnh hơn ngươi nhiều.",
        )
        FinalBossType -> StoryLine(
            speaker = "BÁ VƯƠNG THIÊN HÀ",
            text = "Ngươi không nên đi xa đến vậy. Kết thúc ở đây.",
            durationMs = 4500,
        )
        is MidBossType -> StoryLine(
            speaker = type.displayName,
            text = when (type) {
                MidBossType.OFFENSIVE -> "Hàng hay là chết."
                MidBossType.DEFENSIVE -> "Phá khiên ta đi. Thách đó."
                MidBossType.SWARM -> "Không bắt được, không bắn được."
                // Round 82 — 12 R81 boss taunts (Vietnamese, in-character).
                MidBossType.HEN_MOTHER -> "Cục tác! Ta đẻ trứng cho ngươi đó!"
                MidBossType.BUFFALO_RAGE -> "Sừng ta sẽ xuyên qua tàu ngươi!"
                MidBossType.DUMB_RAT -> "Phô-mai... à không, laser! Ta bắn đây!"
                MidBossType.FIERCE_TIGER -> "Gầm! Hú vang khắp dải Ngân Hà!"
                MidBossType.SEXY_DIVA -> "Tóc ta lấp lánh, đẹp lắm. Cẩn thận nhé."
                MidBossType.TROLL_TOWER -> "Cao chót vót, ánh sáng từ đỉnh thần thánh."
                MidBossType.TWIN_SUMMITS -> "Hai đỉnh kép, hai tia sữa song hành."
                MidBossType.VOID_GLOBES -> "Vô tận hư vô, hứng đòn của ta đi!"
                MidBossType.WHITE_DRAGON -> "Bạch Long Mắt Lam — thét ra lửa!"
                MidBossType.HAMMER_SICKLE -> "Búa liềm bịp bợm — vinh quang giai cấp!"
                MidBossType.MONEY_TYCOON -> "Money makes the world go round, tàu trẻ con."
                MidBossType.GOLDEN_TYCOON -> "Believe me, tàu ngươi sẽ chết tuyệt vời nhất."
            },
        )
        else -> null
    }
}
