package com.streamix.core.analytics

import android.os.Bundle

interface AnalyticsManager {
    fun logEvent(event: String, params: Bundle? = null)
    fun logScreen(screenName: String, screenClass: String? = null)
    fun setUserProperty(name: String, value: String)
}
