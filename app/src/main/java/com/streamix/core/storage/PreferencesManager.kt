package com.streamix.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.streamix.core.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val ADULT_PASSCODE_KEY = stringPreferencesKey("adult_passcode")
        val ADULT_PASSCODE_SET = booleanPreferencesKey("adult_passcode_set")
        val CURRENT_PROFILE    = stringPreferencesKey("current_profile")
        val IS_ADULT_VERIFIED  = booleanPreferencesKey("is_adult_verified")
        
        val YOUTUBE_COOKIES      = stringPreferencesKey("youtube_cookies")
        val YOUTUBE_ACCOUNT_NAME = stringPreferencesKey("youtube_account_name")
        
        val PRIMARY_COLOR      = stringPreferencesKey("primary_color")
        val SECONDARY_COLOR    = stringPreferencesKey("secondary_color")
        val TERTIARY_COLOR     = stringPreferencesKey("tertiary_color")
        
        val SUBSCRIBED_CHANNELS = stringPreferencesKey("subscribed_channels")
        val USER_INTERESTS      = stringPreferencesKey("user_interests")
        val SHORTS_SEARCH_HISTORY = stringPreferencesKey("shorts_search_history")
        val ADULT_SHORTS_SEARCH_HISTORY = stringPreferencesKey("adult_shorts_search_history")
        val HOME_YT_SEARCH_HISTORY = stringPreferencesKey("home_yt_search_history")
        val HOME_MOVIES_SEARCH_HISTORY = stringPreferencesKey("home_movies_search_history")
        val HOME_ADULT_SEARCH_HISTORY = stringPreferencesKey("home_adult_search_history")
        val GLOBAL_SEARCH_HISTORY = stringPreferencesKey("global_search_history")
        val AUTO_SCROLL_SHORTS  = booleanPreferencesKey("auto_scroll_shorts")
        val BACKGROUND_PLAYBACK_YOUTUBE = booleanPreferencesKey("background_playback_youtube")
        val SPONSOR_BLOCK_ENABLED = booleanPreferencesKey("sponsor_block_enabled")
        val FLOATING_DOCK_ENABLED = booleanPreferencesKey("floating_dock_enabled")
        val IGNORED_VERSION = intPreferencesKey("ignored_version")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    }

    val sponsorBlockEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[SPONSOR_BLOCK_ENABLED] ?: true }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SPONSOR_BLOCK_ENABLED] = enabled }
    }

    val floatingDockEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[FLOATING_DOCK_ENABLED] ?: false }

    suspend fun setFloatingDockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[FLOATING_DOCK_ENABLED] = enabled }
    }

    val autoScrollShorts: Flow<Boolean> = context.dataStore.data
        .map { it[AUTO_SCROLL_SHORTS] ?: false }

    suspend fun setAutoScrollShorts(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_SCROLL_SHORTS] = enabled }
    }

    val backgroundPlaybackYoutube: Flow<Boolean> = context.dataStore.data
        .map { it[BACKGROUND_PLAYBACK_YOUTUBE] ?: false }

    suspend fun setBackgroundPlaybackYoutube(enabled: Boolean) {
        context.dataStore.edit { it[BACKGROUND_PLAYBACK_YOUTUBE] = enabled }
    }

    val userInterests: Flow<Set<String>> = context.dataStore.data
        .map { it[USER_INTERESTS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet() }

    suspend fun addInterest(interest: String) {
        if (interest.isBlank()) return
        context.dataStore.edit {
            val current = it[USER_INTERESTS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(interest.lowercase())
            // Keep only last 20 interests to avoid bloating
            it[USER_INTERESTS] = current.toList().takeLast(20).joinToString(",")
        }
    }

    val globalSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[GLOBAL_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }

    suspend fun addGlobalSearch(query: String) {
        addSearchHistory(GLOBAL_SEARCH_HISTORY, query)
    }

    suspend fun removeGlobalSearch(query: String) {
        removeSearchHistory(GLOBAL_SEARCH_HISTORY, query)
    }

    val homeYtSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[HOME_YT_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }
    suspend fun addHomeYtSearch(query: String) = addSearchHistory(HOME_YT_SEARCH_HISTORY, query)
    suspend fun removeHomeYtSearch(query: String) = removeSearchHistory(HOME_YT_SEARCH_HISTORY, query)

    val homeMoviesSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[HOME_MOVIES_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }
    suspend fun addHomeMoviesSearch(query: String) = addSearchHistory(HOME_MOVIES_SEARCH_HISTORY, query)
    suspend fun removeHomeMoviesSearch(query: String) = removeSearchHistory(HOME_MOVIES_SEARCH_HISTORY, query)

    val homeAdultSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[HOME_ADULT_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }
    suspend fun addHomeAdultSearch(query: String) = addSearchHistory(HOME_ADULT_SEARCH_HISTORY, query)
    suspend fun removeHomeAdultSearch(query: String) = removeSearchHistory(HOME_ADULT_SEARCH_HISTORY, query)

    val ytShortsSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[SHORTS_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }
    suspend fun addYtShortsSearch(query: String) = addSearchHistory(SHORTS_SEARCH_HISTORY, query)
    suspend fun removeYtShortsSearch(query: String) = removeSearchHistory(SHORTS_SEARCH_HISTORY, query)

    val adultShortsSearchHistory: Flow<List<String>> = context.dataStore.data
        .map { it[ADULT_SHORTS_SEARCH_HISTORY]?.split("|")?.filter { it.isNotBlank() } ?: emptyList() }
    suspend fun addAdultShortsSearch(query: String) = addSearchHistory(ADULT_SHORTS_SEARCH_HISTORY, query)
    suspend fun removeAdultShortsSearch(query: String) = removeSearchHistory(ADULT_SHORTS_SEARCH_HISTORY, query)

    private suspend fun addSearchHistory(key: androidx.datastore.preferences.core.Preferences.Key<String>, query: String) {
        if (query.isBlank()) return
        context.dataStore.edit {
            val current = it[key]?.split("|")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.removeAll { it.equals(query, ignoreCase = true) }
            current.add(0, query)
            it[key] = current.take(30).joinToString("|")
        }
    }

    private suspend fun removeSearchHistory(key: androidx.datastore.preferences.core.Preferences.Key<String>, query: String) {
        context.dataStore.edit {
            val current = it[key]?.split("|")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            current.remove(query)
            it[key] = current.joinToString("|")
        }
    }

    val shortsSearchHistory: Flow<Set<String>> = context.dataStore.data
        .map { it[SHORTS_SEARCH_HISTORY]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet() }

    suspend fun addShortsSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit {
            val current = it[SHORTS_SEARCH_HISTORY]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(query.lowercase())
            // Keep only last 10 search queries
            it[SHORTS_SEARCH_HISTORY] = current.toList().takeLast(10).joinToString(",")
        }
    }

    val subscribedChannels: Flow<Set<String>> = context.dataStore.data
        .map { it[SUBSCRIBED_CHANNELS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet() }

    suspend fun toggleSubscription(id: String) {
        if (id.isBlank()) return
        context.dataStore.edit {
            val current = it[SUBSCRIBED_CHANNELS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(id)) {
                current.remove(id)
            } else {
                current.add(id)
            }
            it[SUBSCRIBED_CHANNELS] = current.filter { it.isNotBlank() }.joinToString(",")
        }
    }

    val isAdultVerified: Flow<Boolean> = context.dataStore.data
        .map { it[IS_ADULT_VERIFIED] ?: false }

    val primaryColor: Flow<String> = context.dataStore.data
        .map { it[PRIMARY_COLOR] ?: "#000000" } // Default black

    val secondaryColor: Flow<String> = context.dataStore.data
        .map { it[SECONDARY_COLOR] ?: "#FFFFFF" } // Default white

    val tertiaryColor: Flow<String> = context.dataStore.data
        .map { it[TERTIARY_COLOR] ?: "#FFFFFF" } // Default white

    val youtubeCookies: Flow<String?> = context.dataStore.data
        .map { it[YOUTUBE_COOKIES] }

    val youtubeAccountName: Flow<String?> = context.dataStore.data
        .map { it[YOUTUBE_ACCOUNT_NAME] }

    suspend fun setYoutubeAccount(cookies: String?, name: String?) {
        context.dataStore.edit {
            if (cookies != null) it[YOUTUBE_COOKIES] = cookies else it.remove(YOUTUBE_COOKIES)
            if (name != null) it[YOUTUBE_ACCOUNT_NAME] = name else it.remove(YOUTUBE_ACCOUNT_NAME)
        }
    }

    suspend fun setAdultVerified(verified: Boolean) {
        context.dataStore.edit { it[IS_ADULT_VERIFIED] = verified }
    }

    suspend fun setThemeColors(primary: String, secondary: String, tertiary: String) {
        context.dataStore.edit {
            it[PRIMARY_COLOR] = primary
            it[SECONDARY_COLOR] = secondary
            it[TERTIARY_COLOR] = tertiary
        }
    }

    val adultPasscode: Flow<String?> = context.dataStore.data
        .map { it[ADULT_PASSCODE_KEY] }

    val isPasscodeSet: Flow<Boolean> = context.dataStore.data
        .map { it[ADULT_PASSCODE_SET] ?: false }

    val currentProfile: Flow<Profile> = context.dataStore.data
        .map { 
            val label = it[CURRENT_PROFILE] ?: Profile.MOVIES.name
            Profile.valueOf(label)
        }

    val ignoredVersion: Flow<Int> = context.dataStore.data
        .map { it[IGNORED_VERSION] ?: -1 }

    val lastUpdateCheck: Flow<Long> = context.dataStore.data
        .map { it[LAST_UPDATE_CHECK] ?: 0L }

    suspend fun setIgnoredVersion(version: Int) {
        context.dataStore.edit { it[IGNORED_VERSION] = version }
    }

    suspend fun setLastUpdateCheck(time: Long) {
        context.dataStore.edit { it[LAST_UPDATE_CHECK] = time }
    }

    suspend fun setAdultPasscode(passcode: String) {
        context.dataStore.edit {
            it[ADULT_PASSCODE_KEY] = passcode
            it[ADULT_PASSCODE_SET] = true
        }
    }

    suspend fun setProfile(profile: Profile) {
        context.dataStore.edit {
            it[CURRENT_PROFILE] = profile.name
        }
    }
}
