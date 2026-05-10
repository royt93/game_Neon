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
private val DAILY_KEY = stringPreferencesKey("daily_csv")            // 17c Daily challenge — separate top-10 list per UTC day. Format: `dateKey|score|timestamp` lines. dateKey = days-since-epoch UTC.

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

    /**
     * 17c Daily challenge — submit a score tagged with today's UTC day key.
     * Returns the top-10 entries for [dayKey] (today by default), best-first.
     * Older days remain in storage so users can browse history if we add a UI later.
     */
    suspend fun submitDaily(score: Int, dayKey: Long = todayUtcDayKey()): List<LeaderboardEntry> {
        Logger.d("LeaderboardRepository.submitDaily score=$score day=$dayKey")
        var dayList: List<LeaderboardEntry> = emptyList()
        appContext.leaderboardDataStore.edit { prefs ->
            val all = prefs[DAILY_KEY].orEmpty()
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 3) return@mapNotNull null
                    val d = parts[0].toLongOrNull() ?: return@mapNotNull null
                    val s = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val t = parts[2].toLongOrNull() ?: return@mapNotNull null
                    Triple(d, s, t)
                }
                .toMutableList()
            all.add(Triple(dayKey, score, System.currentTimeMillis()))
            // Cap per-day at MAX_ENTRIES top entries; keep history of older days.
            val byDay = all.groupBy { it.first }
            val rebuilt = byDay.flatMap { (_, list) ->
                list.sortedByDescending { it.second }.take(MAX_ENTRIES)
            }
            prefs[DAILY_KEY] = rebuilt.joinToString(separator = "\n") {
                "${it.first}|${it.second}|${it.third}"
            }
            dayList = rebuilt
                .filter { it.first == dayKey }
                .sortedByDescending { it.second }
                .map { LeaderboardEntry(it.second, it.third) }
            Logger.d("LeaderboardRepository.submitDaily: day=$dayKey size=${dayList.size} best=${dayList.firstOrNull()?.score}")
        }
        return dayList
    }

    /** Top-10 of [dayKey] (today by default), best-first. */
    fun dailyEntries(dayKey: Long = todayUtcDayKey()): Flow<List<LeaderboardEntry>> =
        appContext.leaderboardDataStore.data.map { prefs ->
            prefs[DAILY_KEY].orEmpty()
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 3) return@mapNotNull null
                    val d = parts[0].toLongOrNull() ?: return@mapNotNull null
                    if (d != dayKey) return@mapNotNull null
                    val s = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val t = parts[2].toLongOrNull() ?: return@mapNotNull null
                    LeaderboardEntry(s, t)
                }
                .sortedByDescending { it.score }
                .toList()
        }

    companion object {
        const val MAX_ENTRIES = 10
        private const val MILLIS_PER_DAY: Long = 86_400_000L

        /** Days since UTC epoch — stable per calendar day regardless of device time zone. */
        fun todayUtcDayKey(): Long = System.currentTimeMillis() / MILLIS_PER_DAY
    }
}
