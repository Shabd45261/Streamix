package com.streamix.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.storage.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.streamix.core.analytics.AnalyticsEvent
import android.os.Bundle
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val analyticsManager: com.streamix.core.analytics.AnalyticsManager
) : ViewModel() {

    val primaryColor = prefs.primaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Black)

    val secondaryColor = prefs.secondaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.White)

    val tertiaryColor = prefs.tertiaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.White)
        
    val isAdultVerified = prefs.isAdultVerified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val youtubeAccountName = prefs.youtubeAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoScrollShorts = prefs.autoScrollShorts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val backgroundPlaybackYoutube = prefs.backgroundPlaybackYoutube
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val sponsorBlockEnabled = prefs.sponsorBlockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val floatingDockEnabled = prefs.floatingDockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoScrollShorts(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoScrollShorts(enabled)
            logSettingChanged("auto_scroll_shorts", enabled.toString())
        }
    }

    fun setBackgroundPlaybackYoutube(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackgroundPlaybackYoutube(enabled)
            logSettingChanged("background_playback_youtube", enabled.toString())
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setSponsorBlockEnabled(enabled)
            logSettingChanged("sponsor_block_enabled", enabled.toString())
        }
    }

    fun setFloatingDockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setFloatingDockEnabled(enabled)
            logSettingChanged("floating_dock", enabled.toString())
        }
    }

    fun updateTheme(primary: Color, secondary: Color, tertiary: Color) {
        viewModelScope.launch {
            prefs.setThemeColors(primary.toHex(), secondary.toHex(), tertiary.toHex())
            logSettingChanged("theme_primary", primary.toHex())
            logSettingChanged("theme_tertiary", tertiary.toHex())
        }
    }

    private fun logSettingChanged(name: String, value: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsEvent.Param.SETTING_NAME, name)
            putString(AnalyticsEvent.Param.SETTING_VALUE, value)
        }
        analyticsManager.logEvent(AnalyticsEvent.SETTING_CHANGED, bundle)
    }

    private fun Color.toHex(): String {
        return String.format("#%06X", (0xFFFFFF and this.toArgb()))
    }
    
    fun setAdultVerified(verified: Boolean) {
        viewModelScope.launch {
            prefs.setAdultVerified(verified)
        }
    }

    fun logoutYoutube() {
        viewModelScope.launch {
            prefs.setYoutubeAccount(null, null)
            analyticsManager.logEvent("youtube_logout")
        }
    }
}
