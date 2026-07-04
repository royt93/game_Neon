package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType

/**
 * 31d Wave 4 — 5 chapters × ~20 stages = 100-stage campaign script.
 * Each chapter exposes its visual palette, drawable palette, hazard mechanic,
 * mid-boss variant, and final-stage boss type.
 *
 * Chapter ARGB colors are alpha-blended onto stageTint in GameScreen — the
 * 0xRRGGBB component is the color, alpha is applied at render time.
 */
enum class Chapter(
    val id: Int,
    val displayName: String,
    val tintArgb: Long,
    val regularEnemyDrawables: List<Int>,
    val hazard: HazardType?,
    /**
     * Round 82 — was `midBossType: MidBossType?` (single). Now List để insert
     * multiple mid-bosses per chapter, wiring 12 R81 new bosses into actual
     * gameplay. Empty list = no mid-bosses (Ch5 default before FinalBoss).
     */
    val midBossTypes: List<MidBossType>,
    val finalBossType: EnemyType,
) {
    ASTEROID_BELT(
        id = 1,
        displayName = "Vành đai tiểu hành tinh",
        tintArgb = 0xFFFFB048,                        // warm gold-orange
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_1,
            R.drawable.enemy_red_2,
            R.drawable.enemy_red_3,
            // Round 82 — Ch1 gets Spinning Saw + Mine Layer as introduction.
            R.drawable.enemy_spinning_saw,
            R.drawable.enemy_mine_layer,
        ),
        hazard = HazardType.ASTEROID_STORM,
        // Round 82 — Ch1: OFFENSIVE (existing) + HEN_MOTHER + BUFFALO_RAGE (R81 new)
        // Wave 15 — + SKULL_CROSSBONES (chủ đề cướp biển vũ trụ hợp Vành đai).
        midBossTypes = listOf(
            MidBossType.OFFENSIVE,
            MidBossType.HEN_MOTHER,
            MidBossType.BUFFALO_RAGE,
            MidBossType.SKULL_CROSSBONES,
            // Wave 18 — Trùm Kẹt Xe (vành đai đông như "kẹt xe thiên thạch").
            MidBossType.TRAFFIC_JAM,
            // Wave 22 — Trùm Karaoke Lạc Tông (hát vang cả vành đai).
            MidBossType.KARAOKE_BOSS,
        ),
        finalBossType = LevelOneBossType,
    ),
    NEBULA_CLOUD(
        id = 2,
        displayName = "Mây tinh vân",
        tintArgb = 0xFFB14CFF,                        // violet
        regularEnemyDrawables = listOf(
            R.drawable.enemy_green_1,
            R.drawable.enemy_green_2,
            R.drawable.enemy_green_3,
            R.drawable.enemy_green_4,
            // Round 82 — Ch2: Tentacle Squid (foggy nebula = creatures hide).
            R.drawable.enemy_tentacle_squid,
            R.drawable.enemy_phantom,
        ),
        hazard = HazardType.NEBULA_FOG,
        // Round 82 — Ch2: DEFENSIVE (existing) + DUMB_RAT + FIERCE_TIGER
        // Wave 15 — + VAMPIRE (mây tinh vân tối tăm hợp ma cà rồng).
        midBossTypes = listOf(
            MidBossType.DEFENSIVE,
            MidBossType.DUMB_RAT,
            MidBossType.FIERCE_TIGER,
            MidBossType.VAMPIRE,
            // Wave 18 — Sếp KPI (mây mù = "không thấy đường về, chỉ thấy deadline").
            MidBossType.KPI_BOSS,
            // Wave 20 — Thầy Bói Online (mây mù huyền bí hợp bói toán).
            MidBossType.FORTUNE_TELLER,
            // Wave 23 — Bác Sĩ Google (mây mù = tra cứu mịt mờ).
            MidBossType.DR_GOOGLE,
            // Wave 24 — Cô Hồn Tháng 7 (mây tinh vân âm u hợp cô hồn).
            MidBossType.GHOST_MONTH,
        ),
        finalBossType = LevelTwoBossType,
    ),
    ICE_PLANET(
        id = 3,
        displayName = "Hành tinh băng",
        tintArgb = 0xFF00F0FF,                        // cyan ice
        regularEnemyDrawables = listOf(
            R.drawable.enemy_light_blue_1,
            R.drawable.enemy_light_blue_2,
            R.drawable.enemy_light_blue_3,
            R.drawable.enemy_light_blue_4,
            R.drawable.enemy_light_blue_5,
            // Round 82 — Ch3 Ice: Shield Drone + Sniper (high-tech defensive).
            R.drawable.enemy_shield_drone,
            R.drawable.enemy_sniper,
            // Task 09 (đợt 3) — Jammer (nhiễu sóng, SCOUT) + Predator (săn mồi HOMING).
            R.drawable.enemy_jammer,
            R.drawable.enemy_predator,
        ),
        hazard = HazardType.ICE_PATCHES,
        // Round 82 — Ch3: SWARM (existing) + SEXY_DIVA + TROLL_TOWER
        // Wave 15 — + COSMIC_CENTIPEDE (con rết dài bò trên băng).
        midBossTypes = listOf(
            MidBossType.SWARM,
            MidBossType.SEXY_DIVA,
            MidBossType.TROLL_TOWER,
            MidBossType.COSMIC_CENTIPEDE,
            // Wave 18 — Hot TikToker (lên sóng giữa băng giá, "trend lạnh người").
            MidBossType.TIKTOKER,
            // Wave 20 — Drama MXH (băng giá = "drama lạnh sống lưng").
            MidBossType.SOCIAL_DRAMA,
            // Wave 21 — Trẻ Trâu Toxic (băng giá = "lạnh lùng cà khịa").
            MidBossType.TOXIC_KID,
            // Wave 23 — Hoàng Thượng Mèo (mèo băng lãnh cung).
            MidBossType.CAT_EMPEROR,
        ),
        finalBossType = LevelOneBossType,             // reuse — palette change handled visually
    ),
    HOSTILE_STATION(
        id = 4,
        displayName = "Trạm thù địch",
        tintArgb = 0xFFFF2D55,                        // red alert
        // Round 74 (R73d) — Chapter 4 introduces ELITE family (cross/orb).
        // Round 82 — adds Mirror Twin + Healer + Bomber Crawler (advanced tactics).
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_2,
            R.drawable.enemy_red_3,
            R.drawable.enemy_green_3,
            R.drawable.enemy_light_blue_4,
            R.drawable.enemy_cross_1,
            R.drawable.enemy_cross_2,
            R.drawable.enemy_orb_1,
            R.drawable.enemy_orb_2,
            R.drawable.enemy_mirror_twin,
            R.drawable.enemy_healer,
            R.drawable.enemy_bomber_crawler,
            // Task 09 (đợt 3) — Splitter (phân thân, ELITE) + Repulsor (đẩy lùi, HEAVY).
            R.drawable.enemy_splitter,
            R.drawable.enemy_repulsor,
        ),
        hazard = null,                                // station = open zone
        // Round 82 — Ch4: OFFENSIVE reuse (→HELL_LORD via factory) + TWIN_SUMMITS + VOID_GLOBES
        // Wave 16 — + Bao Cao Su Khổng Lồ + Nhện Venom (trạm thù địch hợp 2 boss này).
        midBossTypes = listOf(
            MidBossType.OFFENSIVE,
            MidBossType.TWIN_SUMMITS,
            MidBossType.VOID_GLOBES,
            MidBossType.GIANT_CONDOM,
            MidBossType.VENOM_SPIDER,
            // Wave 19 — ATM Hết Tiền + Bão Giá Lạm Phát (trạm = kinh tế sụp đổ).
            MidBossType.ATM_BANKRUPT,
            MidBossType.INFLATION_STORM,
            // Wave 20 — Trùm Đa Cấp (trạm thù địch = ổ lừa đảo tuyến dưới).
            MidBossType.PYRAMID_SCHEME,
            // Wave 21 — Ông Chú Crypto (trạm = sàn giao dịch ảo).
            MidBossType.CRYPTO_BRO,
            // Wave 24 — Thánh Cuồng Sale (trạm = trung tâm thương mại vũ trụ).
            MidBossType.SALE_FANATIC,
        ),
        finalBossType = LevelTwoBossType,
    ),
    GALAXY_CORE(
        id = 5,
        displayName = "Lõi thiên hà",
        tintArgb = 0xFFFF2DE0,                        // deep magenta
        // Round 74 (R73d) — Chapter 5 introduces BERSERKER family (chevron/spike).
        // Round 82 — climactic chapter gets Kamikaze (suicide rush).
        regularEnemyDrawables = listOf(
            R.drawable.enemy_red_3,
            R.drawable.enemy_green_4,
            R.drawable.enemy_light_blue_5,
            R.drawable.enemy_chevron_1,
            R.drawable.enemy_chevron_2,
            R.drawable.enemy_spike_1,
            R.drawable.enemy_spike_2,
            R.drawable.enemy_kamikaze,
            // Task 09 (đợt 3) — Missileer (pháo thủ volley, BERSERKER) ở chương cuối.
            R.drawable.enemy_missileer,
        ),
        hazard = null,
        // Round 82 — Ch5: 4 mini-bosses before FinalBoss (was null = no mid-boss).
        // Climactic chapter — 4 themed bosses để culminate journey.
        midBossTypes = listOf(
            MidBossType.WHITE_DRAGON,
            MidBossType.HAMMER_SICKLE,
            MidBossType.MONEY_TYCOON,
            MidBossType.GOLDEN_TYCOON,
            // Wave 16 — Tham Nhũng ngự ở Lõi Thiên Hà (climax).
            MidBossType.CORRUPTION,
            // Wave 19 — Cục Gạch Nokia (bất tử như pin Nokia) ở lõi.
            MidBossType.NOKIA_BRICK,
            // Wave 20 — Ông Táo Cưỡi Cá Chép (về trời qua Lõi Thiên Hà).
            MidBossType.KITCHEN_GOD,
            // Wave 22 — Đại Gia Phông Bạt (phô trương ở lõi thiên hà).
            MidBossType.FLASHY_TYCOON,
        ),
        finalBossType = FinalBossType,                // 34d 3-phase final boss
    );

    /** Round 82 — back-compat convenience: first midBossType (or null). */
    val midBossType: MidBossType? get() = midBossTypes.firstOrNull()
}

/**
 * 32d Stage hazards — per-chapter environmental modifier baked into StageGame.
 * Mechanic implemented in GameState loop tinker callbacks; visual cues in GameScreen.
 */
enum class HazardType {
    /** Frequent space rock spawns + larger rocks. */
    ASTEROID_STORM,
    /** Reduced star/dust visibility (background dimmed) + occasional fog cloud overlay. */
    NEBULA_FOG,
    /** Ship slip — movement keeps gliding briefly after release (inertia). */
    ICE_PATCHES,
}
