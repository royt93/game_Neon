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
 * 9b: Persistent achievement tracking. Each achievement has stable [Achievement.id];
 * unlocked set serialized as comma-separated IDs. Lightweight, no JSON dep.
 */
enum class Achievement(
    val id: String,
    val title: String,
    val description: String,
) {
    FIRST_BLOOD("first_blood", "FIRST BLOOD", "Diệt enemy đầu tiên"),
    FIRST_BOSS("first_boss", "BOSS SLAYER", "Hạ boss đầu tiên"),
    COMBO_5("combo_5", "RAMPAGE", "Đạt combo 5×"),
    COMBO_10("combo_10", "UNSTOPPABLE", "Đạt combo 10×"),
    MINERALS_100("minerals_100", "PROSPECTOR", "Thu 100 minerals tổng"),
    MINERALS_1000("minerals_1000", "TYCOON", "Thu 1000 minerals tổng"),
    SURVIVE_5MIN("survive_5min", "MARATHON", "Sống sót 5 phút trong 1 game"),
    NO_DAMAGE_STAGE("no_damage_stage", "FLAWLESS", "Clear 1 stage không bị damage"),
    SHIELD_PICKUP_10("shield_10", "PROTECTED", "Pick up shield 10 lần"),
    BOSS_RUSH_S("boss_rush_s", "BLITZ", "Hạ boss với rank S"),
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
