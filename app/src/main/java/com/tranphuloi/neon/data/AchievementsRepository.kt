package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.achievementsDataStore by preferencesDataStore(name = "neon_achievements")

private val UNLOCKED_KEY = stringPreferencesKey("unlocked_csv")

/**
 * Wave 5 (46x) — achievement tier. Drives AchievementBanner border color
 * and is shown in any future achievements browser. Higher tier = harder to
 * unlock + more prestigious badge.
 */
enum class AchievementTier {
    BRONZE,
    SILVER,
    GOLD,
}

/**
 * 9b / 46x: Persistent achievement tracking. Each achievement has stable
 * [Achievement.id]; unlocked set serialized as comma-separated IDs.
 * Tier added in 46x for visual badge differentiation.
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val tier: AchievementTier = AchievementTier.BRONZE,
) {
    // --- Wave 1 originals (id giữ nguyên để tương thích save cũ) ---
    FIRST_BLOOD("first_blood", "Vết máu đầu tiên", "Diệt địch đầu tiên", AchievementTier.BRONZE),
    FIRST_BOSS("first_boss", "Sát boss", "Hạ boss đầu tiên", AchievementTier.SILVER),
    COMBO_5("combo_5", "Bùng nổ", "Đạt combo ×5", AchievementTier.BRONZE),
    COMBO_10("combo_10", "Bất khả chiến", "Đạt combo ×10", AchievementTier.SILVER),
    MINERALS_100("minerals_100", "Thợ mỏ", "Thu 100 khoáng vật tổng", AchievementTier.BRONZE),
    MINERALS_1000("minerals_1000", "Đại gia", "Thu 1000 khoáng vật tổng", AchievementTier.GOLD),
    SURVIVE_5MIN("survive_5min", "Marathon", "Sống sót 5 phút trong 1 game", AchievementTier.SILVER),
    NO_DAMAGE_STAGE("no_damage_stage", "Hoàn hảo", "Clear 1 màn không bị damage", AchievementTier.GOLD),
    SHIELD_PICKUP_10("shield_10", "Được bảo vệ", "Nhặt khiên 10 lần", AchievementTier.BRONZE),
    BOSS_RUSH_S("boss_rush_s", "Tia chớp", "Hạ boss với rank S", AchievementTier.GOLD),

    // --- 46x Wave 5 expansion: +20 thành tựu (kill/time/combo) ---
    KILL_50("kill_50", "Phi công áce", "Diệt 50 địch trong 1 game", AchievementTier.BRONZE),
    KILL_200("kill_200", "Cựu binh", "Diệt 200 địch trong 1 game", AchievementTier.SILVER),
    KILL_500("kill_500", "Tàn sát", "Diệt 500 địch trong 1 game", AchievementTier.GOLD),
    COMBO_20("combo_20", "Vô địch", "Đạt combo ×20", AchievementTier.GOLD),
    SURVIVE_10MIN("survive_10min", "Tim sắt", "Sống sót 10 phút trong 1 game", AchievementTier.GOLD),
    STAGE_30("stage_30", "Nhà thám hiểm", "Đạt màn 30+", AchievementTier.BRONZE),
    STAGE_60("stage_60", "Người dẫn đường", "Đạt màn 60+", AchievementTier.SILVER),
    STAGE_100("stage_100", "Bản đồ thiên hà", "Đạt màn 100+", AchievementTier.GOLD),
    BOSS_3("boss_3", "Sát thủ", "Hạ 3 boss trong 1 game", AchievementTier.SILVER),
    BOSS_5("boss_5", "Thợ săn boss", "Hạ 5 boss trong 1 game", AchievementTier.GOLD),
    REVIVE_ONCE("revive_once", "Phượng hoàng", "Được hồi sinh 1 lần", AchievementTier.SILVER),
    SMART_BOMB_5("smart_bomb_5", "Phá hủy", "Dùng smart bomb 5 lần trong 1 game", AchievementTier.BRONZE),
    ENDLESS_60S("endless_60s", "Trụ vững", "Sống sót 60s trong chế độ Vô Tận", AchievementTier.BRONZE),
    ENDLESS_180S("endless_180s", "Kiên cường", "Sống sót 3 phút trong chế độ Vô Tận", AchievementTier.SILVER),
    ENDLESS_300S("endless_300s", "Bất tử", "Sống sót 5 phút trong chế độ Vô Tận", AchievementTier.GOLD),
    FINAL_BOSS_KILL("final_boss_kill", "Anh hùng thiên hà", "Hạ bá vương thiên hà", AchievementTier.GOLD),
    HARD_VICTORY("hard_victory", "Huyền thoại", "Thắng Chiến Dịch trên Hard", AchievementTier.GOLD),
    TIME_ATTACK_HIGH("time_attack_high", "Tốc độ", "Đạt 50+ kill trong Đua Thời Gian", AchievementTier.SILVER),
    BOSS_RUSH_CLEAR("boss_rush_clear", "Vượt ải", "Clear chế độ Chiến Boss", AchievementTier.GOLD),
    MODIFIER_RUN("modifier_run", "Ưa mạo hiểm", "Hoàn thành 1 run với Buff", AchievementTier.BRONZE),

    // --- Wave 11c expansion: telemetry-driven lifetime achievements ---
    // Derive from MetaProgressionRepository aggregate Flows. Unlock check
    // runs once per GAME_OVER (after recordRunMetrics writes), so the new
    // total is visible. Pure lifetime milestones — survive the data drift
    // mentioned in Wave 11c disclaimer (non-bullet kills attributed to
    // current loadout, regular enemies aggregated lifetime-only).
    PLASMA_MASTER("plasma_master", "Plasma thiên tài",
        "Tích lũy 100 kill bằng PLASMA", AchievementTier.SILVER),
    HOMING_VETERAN("homing_veteran", "Tên lửa thuần thục",
        "Tích lũy 100 kill bằng HOMING", AchievementTier.SILVER),
    BOSS_ALL_KINDS("boss_all_kinds", "Toàn bộ boss",
        "Đã hạ ít nhất 1 con cho mỗi 21 loại Boss", AchievementTier.GOLD),
    S_RANK_10("s_rank_10", "Perfect killer",
        "Đạt hạng S 10 lần (lifetime)", AchievementTier.GOLD),
    CYAN_HOUR("cyan_hour", "Cyan kỳ cựu",
        "Chơi tàu Cyan ≥ 1 giờ lifetime", AchievementTier.BRONZE),
    LIFETIME_KILLS_1000("lifetime_kills_1000",
        "Thiên địch",
        "Diệt 1000 quái thường lifetime", AchievementTier.SILVER),

    // --- Task 07 — gắn kết feature đợt 1 (drone / sét chain / ship XP) ---
    DRONE_DUO("drone_duo", "Phi đội đôi",
        "Nuôi 2 drone cùng lúc trong 1 game", AchievementTier.BRONZE),
    CHAIN_TRIPLE("chain_triple", "Sét dây chuyền",
        "Một phát Sét Chain lan trúng 3 địch", AchievementTier.SILVER),
    LIGHTNING_MASTER("lightning_master", "Thiên lôi",
        "Tích lũy 100 kill bằng Sét Chain", AchievementTier.SILVER),
    SHIP_MAX_LEVEL("ship_max_level", "Tàu tối thượng",
        "Đưa 1 tàu lên cấp tối đa (Lv5)", AchievementTier.SILVER),
    SHIP_COLLECTOR("ship_collector", "Nhà sưu tầm",
        "Sở hữu 10 tàu", AchievementTier.GOLD),
    ;
}

/**
 * Wave 22 (#4) — thưởng khoáng khi MỞ MỚI 1 thành tựu, theo bậc: Đồng 30◇,
 * Bạc 60◇, Vàng 120◇. Hàm thuần để test + gọi ở nơi unlock trả true.
 */
internal fun achievementReward(tier: AchievementTier): Int = when (tier) {
    AchievementTier.BRONZE -> 30
    AchievementTier.SILVER -> 60
    AchievementTier.GOLD -> 120
}

class AchievementsRepository(private val appContext: Context) {

    init {
        Logger.d("AchievementsRepository init")
    }

    val unlockedFlow: Flow<Set<String>> = appContext.achievementsDataStore.data.map { prefs ->
        prefs[UNLOCKED_KEY].orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Returns true if newly unlocked, false if already unlocked.
     */
    suspend fun unlock(achievement: Achievement): Boolean {
        var newlyUnlocked = false
        appContext.achievementsDataStore.edit { prefs ->
            val current = prefs[UNLOCKED_KEY].orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            if (current.add(achievement.id)) {
                newlyUnlocked = true
                prefs[UNLOCKED_KEY] = current.joinToString(",")
                Logger.d("AchievementsRepository.unlock: NEW ${achievement.id} (\"${achievement.title}\")")
            }
        }
        return newlyUnlocked
    }
}

val LocalAchievements = androidx.compose.runtime.staticCompositionLocalOf<AchievementsRepository> {
    error("AchievementsRepository not provided.")
}
