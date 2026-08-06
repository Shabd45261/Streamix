package com.streamix.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.streamix.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AnalyticsManager {

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context).apply {
            setUserProperty("app_version", BuildConfig.VERSION_NAME)
            setUserProperty("android_version", android.os.Build.VERSION.RELEASE)
            setUserProperty("device_model", android.os.Build.MODEL)
            setUserProperty("device_manufacturer", android.os.Build.MANUFACTURER)
        }
    }

    override fun logEvent(event: String, params: Bundle?) {
        if (BuildConfig.DEBUG) {
            Log.d("Analytics", "Event: $event, Params: ${params?.let { bundleToMap(it) }}")
        }
        try {
            firebaseAnalytics.logEvent(event, params)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("Analytics", "Error logging event: $event", e)
            }
        }
    }

    override fun logScreen(screenName: String, screenClass: String?) {
        if (BuildConfig.DEBUG) {
            Log.d("Analytics", "ScreenView: $screenName, Class: $screenClass")
        }
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: "ComposeScreen")
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun setUserProperty(name: String, value: String) {
        if (BuildConfig.DEBUG) {
            Log.d("Analytics", "UserProperty: $name = $value")
        }
        try {
            firebaseAnalytics.setUserProperty(name, value)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("Analytics", "Error setting user property: $name", e)
            }
        }
    }

    private fun bundleToMap(bundle: Bundle): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        for (key in bundle.keySet()) {
            map[key] = bundle.get(key)
        }
        return map
    }
}
