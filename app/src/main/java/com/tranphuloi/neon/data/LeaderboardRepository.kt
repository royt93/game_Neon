package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.leaderboardDataStore by preferencesDataStore(name = "neon_leaderboard")

private val ENTRIES_KEY = stringPreferencesKey("entries_csv")

/**
 * Top-10 local leaderboard. Entries serialized as CSV: `score|timestamp` lines, newline-separated.
 * Lightweight, no JSON parsing dependency. Top-10 cap means storage stays trivial.
 */
data class LeaderboardEntry(
    val score: Int,
    val timestampMillis: Long,
)

class LeaderboardRepository(private val appContext: Context) {

    init {
        Logger.d("LeaderboardRepository init")
    }

    val topEntries: Flow<List<LeaderboardEntry>> = appContext.leaderboardDataStore.data.map { prefs ->
        prefs[ENTRIES_KEY].orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 2) return@mapNotNull null
                val score = parts[0].toIntOrNull() ?: return@mapNotNull null
                val ts = parts[1].toLongOrNull() ?: return@mapNotNull null
                LeaderboardEntry(score = score, timestampMillis = ts)
            }
            .toList()
    }

    suspend fun submit(score: Int): List<LeaderboardEntry> {
        Logger.d("LeaderboardRepository.submit score=$score")
        var newList: List<LeaderboardEntry> = emptyList()
        appContext.leaderboardDataStore.edit { prefs ->
            val existing = prefs[ENTRIES_KEY].orEmpty()
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 2) return@mapNotNull null
                    val s = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val t = parts[1].toLongOrNull() ?: return@mapNotNull null
                    LeaderboardEntry(s, t)
                }
                .toMutableList()
            existing.add(LeaderboardEntry(score, System.currentTimeMillis()))
            existing.sortByDescending { it.score }
            val capped = existing.take(MAX_ENTRIES)
            newList = capped
            prefs[ENTRIES_KEY] = capped.joinToString(separator = "\n") {
                "${it.score}|${it.timestampMillis}"
            }
            Logger.d("LeaderboardRepository.submit: top size=${capped.size} best=${capped.firstOrNull()?.score}")
        }
        return newList
    }

    companion object {
        const val MAX_ENTRIES = 10
    }
}
