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
    FIRST_BLOOD("first_blood", "VẾT MÁU ĐẦU TIÊN", "Diệt địch đầu tiên", AchievementTier.BRONZE),
    FIRST_BOSS("first_boss", "SÁT BOSS", "Hạ boss đầu tiên", AchievementTier.SILVER),
    COMBO_5("combo_5", "BÙNG NỔ", "Đạt combo ×5", AchievementTier.BRONZE),
    COMBO_10("combo_10", "BẤT KHẢ CHIẾN", "Đạt combo ×10", AchievementTier.SILVER),
    MINERALS_100("minerals_100", "THỢ MỎ", "Thu 100 khoáng vật tổng", AchievementTier.BRONZE),
    MINERALS_1000("minerals_1000", "ĐẠI GIA", "Thu 1000 khoáng vật tổng", AchievementTier.GOLD),
    SURVIVE_5MIN("survive_5min", "MARATHON", "Sống sót 5 phút trong 1 game", AchievementTier.SILVER),
    NO_DAMAGE_STAGE("no_damage_stage", "HOÀN HẢO", "Clear 1 màn không bị damage", AchievementTier.GOLD),
    SHIELD_PICKUP_10("shield_10", "ĐƯỢC BẢO VỆ", "Nhặt khiên 10 lần", AchievementTier.BRONZE),
    BOSS_RUSH_S("boss_rush_s", "TIA CHỚP", "Hạ boss với rank S", AchievementTier.GOLD),

    // --- 46x Wave 5 expansion: +20 thành tựu (kill/time/combo) ---
    KILL_50("kill_50", "PHI CÔNG ÁCE", "Diệt 50 địch trong 1 game", AchievementTier.BRONZE),
    KILL_200("kill_200", "CỰU BINH", "Diệt 200 địch trong 1 game", AchievementTier.SILVER),
    KILL_500("kill_500", "TÀN SÁT", "Diệt 500 địch trong 1 game", AchievementTier.GOLD),
    COMBO_20("combo_20", "VÔ ĐỊCH", "Đạt combo ×20", AchievementTier.GOLD),
    SURVIVE_10MIN("survive_10min", "TIM SẮT", "Sống sót 10 phút trong 1 game", AchievementTier.GOLD),
    STAGE_30("stage_30", "NHÀ THÁM HIỂM", "Đạt màn 30+", AchievementTier.BRONZE),
    STAGE_60("stage_60", "NGƯỜI DẪN ĐƯỜNG", "Đạt màn 60+", AchievementTier.SILVER),
    STAGE_100("stage_100", "BẢN ĐỒ THIÊN HÀ", "Đạt màn 100+", AchievementTier.GOLD),
    BOSS_3("boss_3", "SÁT THỦ", "Hạ 3 boss trong 1 game", AchievementTier.SILVER),
    BOSS_5("boss_5", "THỢ SĂN BOSS", "Hạ 5 boss trong 1 game", AchievementTier.GOLD),
    REVIVE_ONCE("revive_once", "PHƯỢNG HOÀNG", "Được hồi sinh 1 lần", AchievementTier.SILVER),
    SMART_BOMB_5("smart_bomb_5", "PHÁ HỦY", "Dùng smart bomb 5 lần trong 1 game", AchievementTier.BRONZE),
    ENDLESS_60S("endless_60s", "TRỤ VỮNG", "Sống sót 60s trong chế độ Vô Tận", AchievementTier.BRONZE),
    ENDLESS_180S("endless_180s", "KIÊN CƯỜNG", "Sống sót 3 phút trong chế độ Vô Tận", AchievementTier.SILVER),
    ENDLESS_300S("endless_300s", "BẤT TỬ", "Sống sót 5 phút trong chế độ Vô Tận", AchievementTier.GOLD),
    FINAL_BOSS_KILL("final_boss_kill", "ANH HÙNG THIÊN HÀ", "Hạ Bá Vương Thiên Hà", AchievementTier.GOLD),
    HARD_VICTORY("hard_victory", "HUYỀN THOẠI", "Thắng Chiến Dịch trên Hard", AchievementTier.GOLD),
    TIME_ATTACK_HIGH("time_attack_high", "TỐC ĐỘ", "Đạt 50+ kill trong Đua Thời Gian", AchievementTier.SILVER),
    BOSS_RUSH_CLEAR("boss_rush_clear", "VƯỢT ẢI", "Clear chế độ Chiến Boss", AchievementTier.GOLD),
    MODIFIER_RUN("modifier_run", "ƯA MẠO HIỂM", "Hoàn thành 1 run với Buff", AchievementTier.BRONZE),
    ;
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
