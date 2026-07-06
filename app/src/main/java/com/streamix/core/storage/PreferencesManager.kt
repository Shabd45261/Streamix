package com.streamix.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        .map { it[TERTIARY_COLOR] ?: "#FF0000" } // Default red

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
