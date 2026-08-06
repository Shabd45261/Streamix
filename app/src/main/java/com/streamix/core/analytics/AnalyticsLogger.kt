package com.streamix.core.analytics

import android.util.Log
import com.streamix.BuildConfig

object AnalyticsLogger {
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("StreamixAnalytics", message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e("StreamixAnalytics", message, throwable)
        }
    }
}
