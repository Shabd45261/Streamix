package com.streamix.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.storage.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    val primaryColor = prefs.primaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Black)

    val secondaryColor = prefs.secondaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.White)

    val tertiaryColor = prefs.tertiaryColor.map { Color(android.graphics.Color.parseColor(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Red)
        
    val isAdultVerified = prefs.isAdultVerified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val youtubeAccountName = prefs.youtubeAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoScrollShorts = prefs.autoScrollShorts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val floatingDockEnabled = prefs.floatingDockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAutoScrollShorts(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoScrollShorts(enabled)
        }
    }

    fun setFloatingDockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setFloatingDockEnabled(enabled)
        }
    }

    fun updateTheme(primary: Color, secondary: Color, tertiary: Color) {
        viewModelScope.launch {
            prefs.setThemeColors(primary.toHex(), secondary.toHex(), tertiary.toHex())
        }
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
        }
    }
}
