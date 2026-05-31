package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.common.Once

/**
 * 33c+d Wave 4 — Mid-boss variants spawned every ~6 stages within a chapter.
 * Each variant has distinct stats + behavior. Phase transition triggers at HP < 50%
 * (handled inside `MidBoss` class — increases fire rate / enables secondary attack).
 *
 * Round 82 — added `defaultBossKind` field + 12 new variants for R81 boss roster
 * wire. Each variant carries its target BossKind so MidBoss.bossKind dispatch is
 * data-driven. Existing 3 (OFFENSIVE/DEFENSIVE/SWARM) keep their BossKind but
 * also expose it via the new field. EnemyFactory chapter-aware override still
 * takes precedence (for Ch4 OFFENSIVE reuse → HELL_LORD, etc.).
 */
sealed class MidBossType(
    val drawableId: Int,
    val baseHp: Float,
    val displayName: String,
    val defaultBossKind: BossKind,
) : EnemyType(spawnRate = Once) {

    /** Aggressive shooter — moderate HP, fast lasers. Phase 2: triple-shot spread. */
    object OFFENSIVE : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1500f,
        displayName = "TIỂU BOSS TẤN CÔNG",
        defaultBossKind = BossKind.ORB,
    )

    /** Tanky — high HP, slow movement. Phase 2: laser barrage. */
    object DEFENSIVE : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2500f,
        displayName = "TIỂU BOSS PHÒNG THỦ",
        defaultBossKind = BossKind.FRACTAL,
    )

    /**
     * Swarm — low HP, fast erratic figure-8 movement. Phase 2: aggressive triple-spread.
     * Reuses green_boss drawable (only 2 boss webps available); distinguished from
     * DEFENSIVE by behavior (figure-8 motion vs slow patrol) and lower HP.
     */
    object SWARM : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1200f,
        displayName = "TIỂU BOSS BẦY ĐÀN",
        defaultBossKind = BossKind.FRACTAL,
    )

    // ── Round 82 (boss wire) — 12 new variants per R81 roster ──

    object HEN_MOTHER : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1800f,
        displayName = "GÀ MÁI DẦU",
        defaultBossKind = BossKind.HEN_MOTHER,
    )

    object BUFFALO_RAGE : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2800f,
        displayName = "TRÂU HUNG HẴN",
        defaultBossKind = BossKind.BUFFALO_RAGE,
    )

    object DUMB_RAT : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1400f,
        displayName = "CHUỘT NGU SI",
        defaultBossKind = BossKind.DUMB_RAT,
    )

    object FIERCE_TIGER : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2200f,
        displayName = "CỌP HUNG TỢN",
        defaultBossKind = BossKind.FIERCE_TIGER,
    )

    object SEXY_DIVA : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2000f,
        displayName = "CÔ GÁI SEXY",
        defaultBossKind = BossKind.SEXY_DIVA,
    )

    object TROLL_TOWER : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2400f,
        displayName = "THÁP TINH QUỶ",
        defaultBossKind = BossKind.TROLL_TOWER,
    )

    object TWIN_SUMMITS : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2100f,
        displayName = "ĐÔI ĐỈNH SINH HOA",
        defaultBossKind = BossKind.TWIN_SUMMITS,
    )

    object VOID_GLOBES : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2600f,
        displayName = "ĐÔI CẦU HƯ VÔ",
        defaultBossKind = BossKind.VOID_GLOBES,
    )

    object WHITE_DRAGON : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 3200f,
        displayName = "BẠCH LONG MẮT LAM",
        defaultBossKind = BossKind.WHITE_DRAGON,
    )

    object HAMMER_SICKLE : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2700f,
        // Wave 15 — đồng bộ với BossKind.HAMMER_SICKLE sau lần đổi tên
        // "Cộng Sản Bịp Bợm" → "Cộng Sản Lên Ngôi" (trước chỉ đổi ở BossKind).
        displayName = "CỘNG SẢN LÊN NGÔI",
        defaultBossKind = BossKind.HAMMER_SICKLE,
    )

    object MONEY_TYCOON : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2300f,
        displayName = "TƯ BẢN BÓC LỘT",
        defaultBossKind = BossKind.MONEY_TYCOON,
    )

    object GOLDEN_TYCOON : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2500f,
        displayName = "TYCOON VÀNG",
        defaultBossKind = BossKind.GOLDEN_TYCOON,
    )

    // ── Wave 15 batch 1 — 3 boss user nêu đích danh ──

    /** Đầu lâu + xương chéo — bắn xương xoay. HP trung bình. */
    object SKULL_CROSSBONES : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2000f,
        displayName = "ĐẦU LÂU XƯƠNG CHÉO",
        defaultBossKind = BossKind.SKULL_CROSSBONES,
    )

    /** Ma cà rồng — HP cao + dai (tự hồi máu khi gây sát thương ở MidBoss phase 2). */
    object VAMPIRE : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2900f,
        displayName = "MA CÀ RỒNG",
        defaultBossKind = BossKind.VAMPIRE,
    )

    /** Con rết vũ trụ — rất dài, HP cao nhất batch, đòn độc. */
    object COSMIC_CENTIPEDE : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 3100f,
        displayName = "CON RẾT VŨ TRỤ",
        defaultBossKind = BossKind.COSMIC_CENTIPEDE,
    )

    // ── Wave 16 batch 2 — 3 boss user nêu đích danh (nốt) ──

    /** Bao cao su khổng lồ — phình rồi nổ ra vòng đạn. */
    object GIANT_CONDOM : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2400f,
        displayName = "BAO CAO SU KHỔNG LỒ",
        defaultBossKind = BossKind.GIANT_CONDOM,
    )

    /** Nhện Venom — cực nguy hiểm: tơ độc 8 hướng. */
    object VENOM_SPIDER : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2800f,
        displayName = "NHỆN VENOM",
        defaultBossKind = BossKind.VENOM_SPIDER,
    )

    /** Tham Nhũng — HP cao nhất, phun "tiền" dày đặc đè người. */
    object CORRUPTION : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 3300f,
        displayName = "THAM NHŨNG",
        defaultBossKind = BossKind.CORRUPTION,
    )
}
