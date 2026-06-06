package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.common.Once

/** Wave 17j — kỹ năng đặc biệt của boss (ngoài bắn đạn). Khai báo ở model. */
enum class BossAbility { NONE, SHIELD, TELEPORT, LIFESTEAL }

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
        // Wave 17 — trước là FRACTAL (trùng shape DEFENSIVE) nhưng giá trị này
        // CHẾT: BossKindResolver luôn map SWARM→HAUNTED_KID nên render đã khác.
        // Đặt default đúng = HAUNTED_KID để code nói thật, không gây hiểu lầm.
        defaultBossKind = BossKind.HAUNTED_KID,
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
        baseHp = 2550f,                                 // Wave 17 — né trùng size DEFENSIVE(2500)
        displayName = "TYCOON VÀNG",
        defaultBossKind = BossKind.GOLDEN_TYCOON,
    )

    // ── Wave 15 batch 1 — 3 boss user nêu đích danh ──

    /** Đầu lâu + xương chéo — bắn xương xoay. HP trung bình. */
    object SKULL_CROSSBONES : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2050f,                                 // Wave 17 — né trùng size SEXY_DIVA(2000)
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
        baseHp = 2450f,                                 // Wave 17 — né trùng size TROLL_TOWER(2400)
        displayName = "BAO CAO SU KHỔNG LỒ",
        defaultBossKind = BossKind.GIANT_CONDOM,
    )

    /** Nhện Venom — cực nguy hiểm: tơ độc 8 hướng. */
    object VENOM_SPIDER : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2850f,                                 // Wave 17 — né trùng size BUFFALO_RAGE(2800)
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

    // ── Wave 18 batch 1 — 3 boss trào phúng đời sống VN ──

    /** Trùm Kẹt Xe — tường xe lấp một làn, luân phiên trái/phải ép né. */
    object TRAFFIC_JAM : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1700f,                                 // size riêng (né trùng mọi baseHp khác)
        displayName = "TRÙM KẸT XE",
        defaultBossKind = BossKind.TRAFFIC_JAM,
    )

    /** Sếp KPI — cột chỉ tiêu tăng tốc (ACCEL) + deadline đuổi (HOMING). */
    object KPI_BOSS : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2150f,
        displayName = "SẾP KPI",
        defaultBossKind = BossKind.KPI_BOSS,
    )

    /** Hot TikToker — spam tim bay cong (CURVE) + livestream. */
    object TIKTOKER : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1950f,
        displayName = "HOT TIKTOKER",
        defaultBossKind = BossKind.TIKTOKER,
    )

    // ── Wave 19 batch 2 — 3 boss trào phúng (nốt) ──

    /** ATM Hết Tiền — nhả luồng "tiền" dồn dập rồi kẹt (ngưng) theo nhịp. */
    object ATM_BANKRUPT : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1600f,
        displayName = "ATM HẾT TIỀN",
        defaultBossKind = BossKind.ATM_BANKRUPT,
    )

    /** Cục Gạch Nokia — ném vài "cục gạch" CỰC TO + chậm (nặng, ít khe). */
    object NOKIA_BRICK : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1850f,
        displayName = "CỤC GẠCH NOKIA",
        defaultBossKind = BossKind.NOKIA_BRICK,
    )

    /** Bão Giá Lạm Phát — mỗi loạt SỐ đạn tăng dần (lạm phát) + tăng tốc (ACCEL). */
    object INFLATION_STORM : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2250f,
        displayName = "BÃO GIÁ LẠM PHÁT",
        defaultBossKind = BossKind.INFLATION_STORM,
    )

    // ── Wave 20 batch 3 — 4 boss trào phúng (hết batch 1) ──

    /** Drama MXH — "ném đá" nhiều cụm lệch hướng theo fireTick (hùa nhau). */
    object SOCIAL_DRAMA : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1550f,
        displayName = "DRAMA MẠNG XÃ HỘI",
        defaultBossKind = BossKind.SOCIAL_DRAMA,
    )

    /** Trùm Đa Cấp — spread hình KIM TỰ THÁP: hàng dưới rộng dần (tuyến dưới). */
    object PYRAMID_SCHEME : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2350f,
        displayName = "TRÙM ĐA CẤP",
        defaultBossKind = BossKind.PYRAMID_SCHEME,
    )

    /** Thầy Bói Online — quạt đối xứng + 1 tia "tiên tri" HOMING đoán vị trí. */
    object FORTUNE_TELLER : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1650f,
        displayName = "THẦY BÓI ONLINE",
        defaultBossKind = BossKind.FORTUNE_TELLER,
    )

    /** Ông Táo Cưỡi Cá Chép — cá nhảy vòng cung (CURVE) + luồng lửa giữa. */
    object KITCHEN_GOD : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2750f,
        displayName = "ÔNG TÁO CƯỠI CÁ CHÉP",
        defaultBossKind = BossKind.KITCHEN_GOD,
    )

    // ── Wave 21 batch 4 — 2 boss trào phúng (mở màn batch 2) ──

    /** Ông Chú Crypto — "pump & dump": loạt dồn dày rồi loạt tản loạn xen kẽ. */
    object CRYPTO_BRO : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2480f,
        displayName = "ÔNG CHÚ CRYPTO",
        defaultBossKind = BossKind.CRYPTO_BRO,
    )

    /** Trẻ Trâu Toxic — "khẩu nghiệp": spam đạn nhỏ nhanh hướng loạn theo fireTick. */
    object TOXIC_KID : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1450f,
        displayName = "TRẺ TRÂU TOXIC",
        defaultBossKind = BossKind.TOXIC_KID,
    )

    // ── Wave 22 batch 5 — 2 boss trào phúng ──

    /** Trùm Karaoke Lạc Tông — "sóng âm": vòng cung đồng tâm lan ra như tiếng hát. */
    object KARAOKE_BOSS : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1880f,
        displayName = "TRÙM KARAOKE LẠC TÔNG",
        defaultBossKind = BossKind.KARAOKE_BOSS,
    )

    /** Đại Gia Phông Bạt — "flex": loạt đạn rộng chói loè phô trương, nhịp phô diễn. */
    object FLASHY_TYCOON : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2680f,
        displayName = "ĐẠI GIA PHÔNG BẠT",
        defaultBossKind = BossKind.FLASHY_TYCOON,
    )

    // ── Wave 23 batch 6 — 2 boss trào phúng ──

    /** Bác Sĩ Google — "chẩn đoán bừa": 4 luồng đạn chéo hình X (kết quả lung tung). */
    object DR_GOOGLE : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 1750f,
        displayName = "BÁC SĨ GOOGLE",
        defaultBossKind = BossKind.DR_GOOGLE,
    )

    /** Hoàng Thượng Mèo — "vuốt mèo": 2 cụm vuốt 3 tia lệch, luân phiên 2 bên. */
    object CAT_EMPEROR : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 2330f,
        displayName = "HOÀNG THƯỢNG MÈO",
        defaultBossKind = BossKind.CAT_EMPEROR,
    )

    // ── Wave 24 batch 7 — 2 boss trào phúng (HẾT batch 2 = đủ 18) ──

    /** Thánh Cuồng Sale — "flash sale": loạt dồn dày bùng 1 nhịp rồi nghỉ (đổ xô mua). */
    object SALE_FANATIC : MidBossType(
        drawableId = R.drawable.enemy_red_boss,
        baseHp = 1980f,
        displayName = "THÁNH CUỒNG SALE",
        defaultBossKind = BossKind.SALE_FANATIC,
    )

    /** Cô Hồn Tháng 7 — "hồn lang thang": đạn CHẬM bay cong vật vờ, thưa mà khó đoán. */
    object GHOST_MONTH : MidBossType(
        drawableId = R.drawable.enemy_green_boss,
        baseHp = 2230f,
        displayName = "CÔ HỒN THÁNG 7",
        defaultBossKind = BossKind.GHOST_MONTH,
    )

    // ════════════════════════════════════════════════════════════════════════
    // Wave 17j — MODEL NHẬN DIỆN gom 1 chỗ. Mỗi boss khai báo ĐẦY ĐỦ: hp
    // (baseHp) · name (displayName) · shape (defaultBossKind→drawBoss) · size
    // (sizeScale) · skill (attackName) · special (specialAbility). MidBoss +
    // EnemyCanvas ĐỌC từ đây → 1 nguồn sự thật, có test chốt khác biệt.
    // ════════════════════════════════════════════════════════════════════════

    /** SIZE — tỉ lệ ×(130×90). Suy từ baseHp (máu cao = to); 0.5 (~65px) → 1.9 (~247px). */
    val sizeScale: Float
        get() = (0.5f + (baseHp - 1200f) / 2100f * 1.4f).coerceIn(0.5f, 1.9f)

    /** SKILL — tên chiêu thức (đọc được, khớp hàm trong MidBoss.generateLasers). */
    val attackName: String
        get() = when (this) {
            OFFENSIVE -> "Tia mắt săn đuổi (homing)"
            DEFENSIVE -> "Quỹ đạo nguyên tử"
            SWARM -> "Ám khắp đỉnh màn"
            HEN_MOTHER -> "Ổ trứng rơi chậm"
            BUFFALO_RAGE -> "Húc sừng gia tốc"
            DUMB_RAT -> "Gặm nhấm thất thường"
            FIERCE_TIGER -> "Gầm: tường ngang có khe"
            SEXY_DIVA -> "Quất tóc bay cong"
            TROLL_TOWER -> "Tia dọc từ đỉnh tháp"
            TWIN_SUMMITS -> "Tia kép song song"
            VOID_GLOBES -> "Xé hư vô hình X"
            WHITE_DRAGON -> "Thét luồng lửa"
            HAMMER_SICKLE -> "Quăng búa & liềm"
            MONEY_TYCOON -> "Mưa tiền khắp màn"
            GOLDEN_TYCOON -> "Đô la xoáy ốc"
            SKULL_CROSSBONES -> "Quạt xương xoay"
            VAMPIRE -> "Bầy dơi bay cong hút máu"
            COSMIC_CENTIPEDE -> "Phun độc vòng cung rộng"
            GIANT_CONDOM -> "Sóng xung kích 2 vòng"
            VENOM_SPIDER -> "Lưới tơ độc 8 hướng"
            CORRUPTION -> "Tường tiền đè, khe quét"
            TRAFFIC_JAM -> "Tắc đường: lấp làn trái/phải"
            KPI_BOSS -> "Cột chỉ tiêu tăng tốc + deadline đuổi"
            TIKTOKER -> "Spam tim bay cong + livestream"
            ATM_BANKRUPT -> "Nhả tiền dồn dập rồi kẹt"
            NOKIA_BRICK -> "Ném cục gạch nặng & chậm"
            INFLATION_STORM -> "Giá tăng: đạn nhiều dần + tăng tốc"
            SOCIAL_DRAMA -> "Ném đá hội đồng nhiều hướng"
            PYRAMID_SCHEME -> "Spread hình tháp tăng tầng"
            FORTUNE_TELLER -> "Quạt bài + tia tiên tri đuổi"
            KITCHEN_GOD -> "Cá nhảy vòng cung + luồng lửa"
            CRYPTO_BRO -> "Pump & dump: dồn rồi tản"
            TOXIC_KID -> "Khẩu nghiệp: spam hướng loạn"
            KARAOKE_BOSS -> "Sóng âm vòng cung lan ra"
            FLASHY_TYCOON -> "Flex: loạt rộng chói loè"
            DR_GOOGLE -> "Chẩn đoán bừa: 4 luồng chéo X"
            CAT_EMPEROR -> "Vuốt mèo: 2 cụm 3 tia"
            SALE_FANATIC -> "Flash sale: dồn dày rồi nghỉ"
            GHOST_MONTH -> "Hồn lang thang: đạn cong chậm"
        }

    /** SPECIAL — kỹ năng đặc biệt ngoài bắn đạn. */
    val specialAbility: BossAbility
        get() = when (this) {
            WHITE_DRAGON, GIANT_CONDOM -> BossAbility.SHIELD
            VENOM_SPIDER, CORRUPTION, OFFENSIVE -> BossAbility.TELEPORT
            VAMPIRE -> BossAbility.LIFESTEAL
            else -> BossAbility.NONE
        }

    companion object {
        /**
         * Toàn bộ mid-boss variant (Wave 24: 39). Dùng cho Boss Rush (roster đầy
         * đủ thay vì chỉ vài StageBoss cuối chương) + pin test đếm số variant.
         */
        // NB: phải là `get()` — KHÔNG phải `val` khởi tạo sớm. Companion <clinit>
        // build list này lại trigger init các object con (extends MidBossType),
        // mà init object con cần class MidBossType đã load → vòng khởi tạo tĩnh
        // khiến một số object null (HEN_MOTHER null tại idx 3). Tính tại call-time
        // né hẳn vòng này; object là singleton nên identity vẫn ổn định.
        val ALL: List<MidBossType>
            get() = listOf(
                OFFENSIVE, DEFENSIVE, SWARM,
                HEN_MOTHER, BUFFALO_RAGE, DUMB_RAT, FIERCE_TIGER, SEXY_DIVA, TROLL_TOWER,
                TWIN_SUMMITS, VOID_GLOBES, WHITE_DRAGON, HAMMER_SICKLE, MONEY_TYCOON, GOLDEN_TYCOON,
                SKULL_CROSSBONES, VAMPIRE, COSMIC_CENTIPEDE,
                GIANT_CONDOM, VENOM_SPIDER, CORRUPTION,
                TRAFFIC_JAM, KPI_BOSS, TIKTOKER,
                ATM_BANKRUPT, NOKIA_BRICK, INFLATION_STORM,
                SOCIAL_DRAMA, PYRAMID_SCHEME, FORTUNE_TELLER, KITCHEN_GOD,
                CRYPTO_BRO, TOXIC_KID,
                KARAOKE_BOSS, FLASHY_TYCOON,
                DR_GOOGLE, CAT_EMPEROR,
                SALE_FANATIC, GHOST_MONTH,
            )
    }
}
