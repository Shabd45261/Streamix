package com.streamix.scrapers

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourcePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)

    // Get selected scraper ID for a profile. null = all sources
    fun getSelectedSource(profile: String): String? =
        prefs.getString("selected_$profile", if (profile == "MOVIES") "moviebox" else null)

    // Save selected scraper ID. null = all sources
    fun setSelectedSource(profile: String, scraperId: String?) =
        prefs.edit { 
            if (scraperId == null) remove("selected_$profile")
            else putString("selected_$profile", scraperId)
        }
}
