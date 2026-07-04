package com.tranphuloi.neon.ui.game.story

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKindResolver
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
        val kind = BossKindResolver.resolve(type, chapterId) ?: return null
        val text = tauntText(kind)
        val duration = if (kind == BossKind.SPIDER) 4500 else 3500
        return StoryLine(speaker = kind.displayName, text = text, durationMs = duration)
    }

    /** Wave 25c — taunt đọc từ bảng chung `bossMetaFor` (gom 4 when → 1 bảng). */
    private fun tauntText(kind: BossKind): String =
        com.tranphuloi.neon.ui.game.enemy.ship.model.bossMetaFor(kind).taunt

    // ── Task 04 — Boss dialogue / narrative / epilogue ──
    // Trả @StringRes (song ngữ vi+en) thay vì text hardcode; caller resolve qua
    // Context (GameState/StoryOverlay). Speaker vẫn là tên (NARRATOR / boss name).

    /** Beat narrative NARRATOR ở giữa chương (1..5). null nếu chương không có. */
    @androidx.annotation.StringRes
    fun chapterBeatRes(chapterId: Int): Int? = when (chapterId) {
        1 -> R.string.story_beat_ch1
        2 -> R.string.story_beat_ch2
        3 -> R.string.story_beat_ch3
        4 -> R.string.story_beat_ch4
        5 -> R.string.story_beat_ch5
        else -> null
    }

    /** Thoại FinalBoss (Bá Vương / SPIDER) khi vào phase 2/3. null cho phase 1. */
    @androidx.annotation.StringRes
    fun finalBossPhaseRes(phase: Int): Int? = when (phase) {
        2 -> R.string.story_final_phase2
        3 -> R.string.story_final_phase3
        else -> null
    }

    /**
     * Logic THUẦN (testable) quyết định thoại phase khi phase TĂNG từ [prevPhase]
     * → [currentPhase]. Trả @StringRes để fire, hoặc null nếu không tăng / không có
     * thoại cho phase đó (kể cả nhảy cấp, vd 1→3 lấy thoại phase 3). Tách khỏi
     * GameState để unit-test được quyết định "fire 1 lần mỗi lần lên phase".
     */
    @androidx.annotation.StringRes
    fun phaseLineOnAdvance(prevPhase: Int, currentPhase: Int): Int? =
        if (currentPhase > prevPhase) finalBossPhaseRes(currentPhase) else null

    /**
     * Thoại khi HẠ boss. FinalBoss (SPIDER) + vài mid-boss nổi bật có câu riêng;
     * còn lại dùng câu generic (Slice 0: FinalBoss + mid-boss + fallback).
     * Không bao giờ trả 0 → luôn có thoại defeat.
     */
    @androidx.annotation.StringRes
    fun bossDefeatRes(kind: BossKind?): Int = when (kind) {
        BossKind.SPIDER -> R.string.story_defeat_final
        BossKind.HELL_LORD -> R.string.story_defeat_hell_lord
        BossKind.WHITE_DRAGON -> R.string.story_defeat_white_dragon
        BossKind.HAMMER_SICKLE -> R.string.story_defeat_hammer_sickle
        BossKind.CRYPTO_BRO -> R.string.story_defeat_crypto_bro
        BossKind.KITCHEN_GOD -> R.string.story_defeat_kitchen_god
        else -> R.string.story_defeat_generic
    }
}
