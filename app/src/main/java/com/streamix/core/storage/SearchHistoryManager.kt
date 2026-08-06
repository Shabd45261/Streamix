package com.streamix.core.storage

import com.streamix.core.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryManager @Inject constructor(
    private val prefs: PreferencesManager
) {
    /**
     * Returns a Flow of search history for the specified profile.
     */
    fun getHistory(profile: Profile): Flow<List<String>> {
        return when (profile) {
            Profile.YOUTUBE -> prefs.homeYtSearchHistory
            Profile.MOVIES -> prefs.homeMoviesSearchHistory
            Profile.ADULT -> prefs.homeAdultSearchHistory
        }
    }

    /**
     * Returns a Flow of search history for shorts.
     */
    fun getShortsHistory(isAdult: Boolean): Flow<List<String>> {
        return if (isAdult) prefs.adultShortsSearchHistory else prefs.ytShortsSearchHistory
    }

    /**
     * Adds a query to the search history for the specified profile.
     * Implements the following logic:
     * - Trims leading/trailing spaces.
     * - Ignores empty or whitespace-only searches.
     * - Removes duplicates (case-insensitive, but keeps latest capitalization).
     * - Inserts at the top.
     * - Limits history to 30 items.
     */
    suspend fun addSearch(profile: Profile, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        when (profile) {
            Profile.YOUTUBE -> prefs.addHomeYtSearch(trimmed)
            Profile.MOVIES -> prefs.addHomeMoviesSearch(trimmed)
            Profile.ADULT -> prefs.addHomeAdultSearch(trimmed)
        }
    }

    /**
     * Adds a query to the shorts search history.
     */
    suspend fun addShortsSearch(isAdult: Boolean, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        if (isAdult) prefs.addAdultShortsSearch(trimmed) else prefs.addYtShortsSearch(trimmed)
    }

    /**
     * Removes a query from the search history for the specified profile.
     */
    suspend fun removeSearch(profile: Profile, query: String) {
        when (profile) {
            Profile.YOUTUBE -> prefs.removeHomeYtSearch(query)
            Profile.MOVIES -> prefs.removeHomeMoviesSearch(query)
            Profile.ADULT -> prefs.removeHomeAdultSearch(query)
        }
    }

    /**
     * Removes a query from the shorts search history.
     */
    suspend fun removeShortsSearch(isAdult: Boolean, query: String) {
        if (isAdult) prefs.removeAdultShortsSearch(query) else prefs.removeYtShortsSearch(query)
    }
}
