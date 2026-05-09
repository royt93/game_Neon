package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "neon_settings")

object SettingsKeys {
    val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    val MUSIC_VOLUME = intPreferencesKey("music_volume")
    val SFX_VOLUME = intPreferencesKey("sfx_volume")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val DIFFICULTY = stringPreferencesKey("difficulty")
    val SHIP_SKIN = stringPreferencesKey("ship_skin")
    val TUTORIAL_SHOWN = booleanPreferencesKey("tutorial_shown")
    val DIFFICULTY_PICKED = booleanPreferencesKey("difficulty_picked")
}

enum class Difficulty(val key: String, val multiplier: Float) {
    EASY("easy", 0.7f),
    NORMAL("normal", 1.0f),
    HARD("hard", 1.4f);

    companion object {
        fun fromKey(key: String?): Difficulty =
            values().firstOrNull { it.key == key } ?: NORMAL
    }
}

enum class ShipSkin(val key: String) {
    REGULAR("regular"),
    BOOSTED("boosted");

    companion object {
        fun fromKey(key: String?): ShipSkin =
            values().firstOrNull { it.key == key } ?: REGULAR
    }
}

class SettingsRepository(private val appContext: Context) {

    init {
        Logger.d("SettingsRepository init")
    }

    val reduceMotion: Flow<Boolean> = appContext.dataStore.data.map {
        it[SettingsKeys.REDUCE_MOTION] ?: false
    }
    val musicVolume: Flow<Int> = appContext.dataStore.data.map {
        it[SettingsKeys.MUSIC_VOLUME] ?: 80
    }
    val sfxVolume: Flow<Int> = appContext.dataStore.data.map {
        it[SettingsKeys.SFX_VOLUME] ?: 90
    }
    val vibrationEnabled: Flow<Boolean> = appContext.dataStore.data.map {
        it[SettingsKeys.VIBRATION_ENABLED] ?: true
    }
    val difficulty: Flow<Difficulty> = appContext.dataStore.data.map {
        Difficulty.fromKey(it[SettingsKeys.DIFFICULTY])
    }
    val shipSkin: Flow<ShipSkin> = appContext.dataStore.data.map {
        ShipSkin.fromKey(it[SettingsKeys.SHIP_SKIN])
    }
    val tutorialShown: Flow<Boolean> = appContext.dataStore.data.map {
        it[SettingsKeys.TUTORIAL_SHOWN] ?: false
    }
    val difficultyPicked: Flow<Boolean> = appContext.dataStore.data.map {
        it[SettingsKeys.DIFFICULTY_PICKED] ?: false
    }

    suspend fun setReduceMotion(value: Boolean) {
        Logger.d("SettingsRepository.setReduceMotion=$value")
        appContext.dataStore.edit { it[SettingsKeys.REDUCE_MOTION] = value }
    }

    suspend fun setMusicVolume(value: Int) {
        Logger.d("SettingsRepository.setMusicVolume=$value")
        appContext.dataStore.edit { it[SettingsKeys.MUSIC_VOLUME] = value.coerceIn(0, 100) }
    }

    suspend fun setSfxVolume(value: Int) {
        Logger.d("SettingsRepository.setSfxVolume=$value")
        appContext.dataStore.edit { it[SettingsKeys.SFX_VOLUME] = value.coerceIn(0, 100) }
    }

    suspend fun setVibrationEnabled(value: Boolean) {
        Logger.d("SettingsRepository.setVibrationEnabled=$value")
        appContext.dataStore.edit { it[SettingsKeys.VIBRATION_ENABLED] = value }
    }

    suspend fun setDifficulty(value: Difficulty) {
        Logger.d("SettingsRepository.setDifficulty=${value.key}")
        appContext.dataStore.edit {
            it[SettingsKeys.DIFFICULTY] = value.key
            it[SettingsKeys.DIFFICULTY_PICKED] = true
        }
    }

    suspend fun setShipSkin(value: ShipSkin) {
        Logger.d("SettingsRepository.setShipSkin=${value.key}")
        appContext.dataStore.edit { it[SettingsKeys.SHIP_SKIN] = value.key }
    }

    suspend fun markTutorialShown() {
        Logger.d("SettingsRepository.markTutorialShown")
        appContext.dataStore.edit { it[SettingsKeys.TUTORIAL_SHOWN] = true }
    }
}
