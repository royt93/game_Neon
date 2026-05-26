package com.tranphuloi.neon.ui.game.story

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
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

    /**
     * Boss type + chapter → taunt shown at boss-spawn.
     * Round 85 audit — chapter-aware. Resolves the actual BossKind for the
     * (type, chapter) combo so speaker name khớp với boss visual + banner.
     * Trước fix: Ch4 OFFENSIVE reuse → banner "Chúa Tể Địa Ngục" nhưng story
     * speaker = "TIỂU BOSS TẤN CÔNG" (mismatch). Sau fix: cả 2 đều "Chúa Tể
     * Địa Ngục".
     */
    fun bossTaunt(type: EnemyType, chapterId: Int = 1): StoryLine? {
        val kind = resolveBossKind(type, chapterId) ?: return null
        val text = tauntText(kind)
        val duration = if (kind == BossKind.SPIDER) 4500 else 3500
        return StoryLine(speaker = kind.displayName, text = text, durationMs = duration)
    }

    /** Mirror of EnemyFactory.resolveBossKindForChapter — keep in sync. */
    private fun resolveBossKind(type: EnemyType, chapterId: Int): BossKind? = when {
        type is LevelOneBossType && chapterId == 3 -> BossKind.DEATH_MOON
        type is LevelOneBossType -> BossKind.STAR
        type is LevelTwoBossType && chapterId == 4 -> BossKind.SATAN_GLYPH
        type is LevelTwoBossType -> BossKind.CROSS
        type is FinalBossType -> BossKind.SPIDER
        type == MidBossType.OFFENSIVE && chapterId == 4 -> BossKind.HELL_LORD
        type == MidBossType.SWARM -> BossKind.HAUNTED_KID
        type is MidBossType -> type.defaultBossKind
        else -> null
    }

    /** BossKind-specific taunt (in-character voice, Vietnamese). */
    private fun tauntText(kind: BossKind): String = when (kind) {
        BossKind.STAR -> "Tiểu tốt vô danh. Không trụ nổi 1 phút đâu."
        BossKind.CROSS -> "Đã nghiền nát phi công mạnh hơn ngươi nhiều."
        BossKind.ORB -> "Mắt ta dõi theo mọi cử động của ngươi."
        BossKind.FRACTAL -> "Phá khiên ta đi. Thách đó."
        BossKind.SPIDER -> "Ngươi không nên đi xa đến vậy. Kết thúc ở đây."
        BossKind.DEATH_MOON -> "Mặt trăng tử thần đã đến. Hết đường rồi."
        BossKind.HAUNTED_KID -> "Hi hi... chơi với em không?"
        BossKind.HELL_LORD -> "Địa ngục chào đón linh hồn ngươi."
        BossKind.SATAN_GLYPH -> "Ngũ giác đã được vẽ. Linh hồn ngươi là vật tế."
        BossKind.HEN_MOTHER -> "Cục tác! Ta đẻ trứng cho ngươi đó!"
        BossKind.BUFFALO_RAGE -> "Sừng ta sẽ xuyên qua tàu ngươi!"
        BossKind.DUMB_RAT -> "Phô-mai... à không, laser! Ta bắn đây!"
        BossKind.FIERCE_TIGER -> "Gầm! Hú vang khắp dải Ngân Hà!"
        BossKind.SEXY_DIVA -> "Tóc ta lấp lánh, đẹp lắm. Cẩn thận nhé."
        BossKind.TROLL_TOWER -> "Cao chót vót, ánh sáng từ đỉnh thần thánh."
        BossKind.TWIN_SUMMITS -> "Hai đỉnh kép, hai tia sữa song hành."
        BossKind.VOID_GLOBES -> "Vô tận hư vô, hứng đòn của ta đi!"
        BossKind.WHITE_DRAGON -> "Bạch Long Mắt Lam — thét ra lửa!"
        BossKind.HAMMER_SICKLE -> "Búa liềm bịp bợm — vinh quang giai cấp!"
        BossKind.MONEY_TYCOON -> "Tiền là sức mạnh, tàu trẻ con."
        BossKind.GOLDEN_TYCOON -> "Tin ta đi, tàu ngươi sẽ chết tuyệt vời nhất."
    }
}
