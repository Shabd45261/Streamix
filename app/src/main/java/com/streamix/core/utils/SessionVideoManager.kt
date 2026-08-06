package com.streamix.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages temporarily hidden videos for the current app session.
 * These are not persisted and will be cleared when the app process is killed.
 */
object SessionVideoManager {
    private val _hiddenVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenVideoIds = _hiddenVideoIds.asStateFlow()

    fun hideVideo(id: String) {
        _hiddenVideoIds.update { it + id }
    }

    fun isVideoHidden(id: String): Boolean {
        return _hiddenVideoIds.value.contains(id)
    }

    /**
     * Helper extension to filter a list of SearchResults based on hidden state
     */
    fun <T : Any> List<T>.filterHidden(idSelector: (T) -> String): List<T> {
        val hiddenIds = _hiddenVideoIds.value
        return if (hiddenIds.isEmpty()) this else filter { idSelector(it) !in hiddenIds }
    }
}
