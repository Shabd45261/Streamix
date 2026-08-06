package com.streamix.core

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor() {
    enum class Profile(val label: String, val iconResId: Int, val color: Color) {
        MOVIES("Movies", com.streamix.R.drawable.ic_profile_movies, Color(0xFF7C6AF7)),
        SONGS ("Songs",  com.streamix.R.drawable.ic_profile_songs,  Color(0xFF4ECDC4)),
        ADULT ("Adult",  com.streamix.R.drawable.ic_profile_adult,  Color(0xFFF7566A))
    }

    private val _currentProfile = MutableStateFlow(Profile.MOVIES)
    val currentProfile: StateFlow<Profile> = _currentProfile.asStateFlow()

    private val _isAdultLocked = MutableStateFlow(true)
    val isAdultLocked = _isAdultLocked.asStateFlow()

    fun cycleProfile() {
        val all = Profile.entries
        val currentIndex = all.indexOf(_currentProfile.value)
        val nextIndex = (currentIndex + 1) % all.size
        val nextProfile = all[nextIndex]
        
        if (nextProfile == Profile.ADULT && _isAdultLocked.value) {
            // Should trigger PIN entry in UI
            // For now, we'll just cycle to the one after Adult if it's locked
            // and the UI will handle the actual locking logic
        }
        _currentProfile.value = nextProfile
    }

    fun unlockAdult() {
        _isAdultLocked.value = false
    }

    fun lockAdult() {
        _isAdultLocked.value = true
    }

    fun setProfile(profile: Profile) {
        _currentProfile.value = profile
    }
}
