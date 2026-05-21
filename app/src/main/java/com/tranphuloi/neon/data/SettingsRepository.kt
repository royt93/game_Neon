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
    /** Wave 5 (43x) — last picked GameMode, applied to next run. */
    val LAST_MODE = stringPreferencesKey("last_mode")
    /** Wave 5 (25x) — last picked RunModifier, applied to next run. "none" by default. */
    val LAST_MODIFIER = stringPreferencesKey("last_modifier")
    /** Wave 6 (27x) round 39 — color blind mode key; values from [ColorBlindMode.key]. */
    val COLOR_BLIND_MODE = stringPreferencesKey("color_blind_mode")
}

/**
 * Wave 6 (27x) round 39 — accessibility mode for UI palette. Game entity
 * bitmaps (ship/enemy/laser sprites) are NOT recolored; this only affects
 * Compose-rendered surfaces that read [com.tranphuloi.neon.common.LocalNeonPalette].
 *
 * - NORMAL: original cyan/magenta/gold/violet/red neon palette.
 * - COLORBLIND_SAFE: Wong-derived palette (blue / orange / yellow / pink /
 *   vermillion) chosen to remain distinguishable under deuteranopia /
 *   protanopia / tritanopia.
 */
enum class ColorBlindMode(val key: String, val displayName: String) {
    NORMAL(key = "normal", displayName = "Tiêu chuẩn"),
    COLORBLIND_SAFE(key = "cb_safe", displayName = "Mù màu");

    companion object {
        fun fromKey(key: String?): ColorBlindMode =
            entries.firstOrNull { it.key == key } ?: NORMAL
    }
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

/**
 * Wave 6 (45x) round 38 — ship aura color customization. The picker in
 * DialogSettings stores the user choice; GameWorld reads it via
 * `LocalSettings.current.shipSkin.collectAsState()` to color the ship's
 * neon-glow modifier.
 *
 * Old keys `regular` / `boosted` are silently migrated to AURA_CYAN by
 * [fromKey]'s fallback (the previous values were a dead setting — picker
 * existed but nothing read it in-game, so no save data depends on them).
 */
enum class ShipSkin(
    val key: String,
    val displayName: String,
    val glowColorHex: Long,
) {
    AURA_CYAN(key = "aura_cyan", displayName = "Cyan", glowColorHex = 0xFF00F0FF),
    AURA_GOLD(key = "aura_gold", displayName = "Vàng", glowColorHex = 0xFFFFCB47),
    AURA_MAGENTA(key = "aura_magenta", displayName = "Hồng", glowColorHex = 0xFFFF2DE0),
    AURA_VIOLET(key = "aura_violet", displayName = "Tím", glowColorHex = 0xFFB14CFF),
    AURA_REDALERT(key = "aura_red", displayName = "Đỏ", glowColorHex = 0xFFFF2D55);

    companion object {
        fun fromKey(key: String?): ShipSkin =
            entries.firstOrNull { it.key == key } ?: AURA_CYAN
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
    /** Wave 5 (43x) — current game mode key. Defaults to "campaign". */
    val lastMode: Flow<String> = appContext.dataStore.data.map {
        it[SettingsKeys.LAST_MODE] ?: "campaign"
    }
    /** Wave 5 (25x) — current run modifier key. "none" = no modifier picked. */
    val lastModifier: Flow<String> = appContext.dataStore.data.map {
        it[SettingsKeys.LAST_MODIFIER] ?: "none"
    }
    /** Wave 6 (27x) round 39 — color blind mode. Defaults to NORMAL. */
    val colorBlindMode: Flow<ColorBlindMode> = appContext.dataStore.data.map {
        ColorBlindMode.fromKey(it[SettingsKeys.COLOR_BLIND_MODE])
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

    suspend fun setLastMode(key: String) {
        Logger.d("SettingsRepository.setLastMode=$key")
        appContext.dataStore.edit { it[SettingsKeys.LAST_MODE] = key }
    }

    suspend fun setLastModifier(key: String) {
        Logger.d("SettingsRepository.setLastModifier=$key")
        appContext.dataStore.edit { it[SettingsKeys.LAST_MODIFIER] = key }
    }

    suspend fun setColorBlindMode(value: ColorBlindMode) {
        Logger.d("SettingsRepository.setColorBlindMode=${value.key}")
        appContext.dataStore.edit { it[SettingsKeys.COLOR_BLIND_MODE] = value.key }
    }
}
