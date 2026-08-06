package com.streamix.core.analytics

import android.os.Bundle
import com.streamix.core.model.SearchResult

fun AnalyticsManager.logContentOpened(item: SearchResult) {
    val bundle = Bundle().apply {
        putString(AnalyticsEvent.Param.ID, item.id)
        putString(AnalyticsEvent.Param.TITLE, item.title)
        putString(AnalyticsEvent.Param.MEDIA_TYPE, item.mediaType)
    }
    logEvent(AnalyticsEvent.CONTENT_OPENED, bundle)
}

fun AnalyticsManager.logSearch(query: String, count: Int) {
    val bundle = Bundle().apply {
        putString(AnalyticsEvent.Param.QUERY, query)
        putInt(AnalyticsEvent.Param.SEARCH_RESULT_COUNT, count)
    }
    logEvent(AnalyticsEvent.SEARCH_QUERY, bundle)
}

fun AnalyticsManager.logVideoEvent(event: String, title: String, id: String, quality: String? = null) {
    val bundle = Bundle().apply {
        putString(AnalyticsEvent.Param.TITLE, title)
        putString(AnalyticsEvent.Param.ID, id)
        quality?.let { putString(AnalyticsEvent.Param.QUALITY, it) }
    }
    logEvent(event, bundle)
}

fun AnalyticsManager.logError(type: String, message: String) {
    val bundle = Bundle().apply {
        putString(AnalyticsEvent.Param.ERROR_MESSAGE, message)
    }
    logEvent(type, bundle)
}
