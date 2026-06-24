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
        
        val PRIMARY_COLOR      = stringPreferencesKey("primary_color")
        val SECONDARY_COLOR    = stringPreferencesKey("secondary_color")
        val TERTIARY_COLOR     = stringPreferencesKey("tertiary_color")
    }

    val isAdultVerified: Flow<Boolean> = context.dataStore.data
        .map { it[IS_ADULT_VERIFIED] ?: false }

    val primaryColor: Flow<String> = context.dataStore.data
        .map { it[PRIMARY_COLOR] ?: "#000000" } // Default black

    val secondaryColor: Flow<String> = context.dataStore.data
        .map { it[SECONDARY_COLOR] ?: "#FFFFFF" } // Default white

    val tertiaryColor: Flow<String> = context.dataStore.data
        .map { it[TERTIARY_COLOR] ?: "#FF0000" } // Default red

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
