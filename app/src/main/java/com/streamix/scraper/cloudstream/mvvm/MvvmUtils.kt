package com.streamix.scraper.cloudstream.mvvm

import android.util.Log

fun logError(t: Throwable) {
    Log.e("Cloudstream", t.message ?: "Unknown error", t)
}

inline fun <T> safe(call: () -> T): T? {
    return try {
        call()
    } catch (e: Exception) {
        logError(e)
        null
    }
}
