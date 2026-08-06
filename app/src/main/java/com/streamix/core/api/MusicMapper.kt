package com.streamix.core.api

import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.SearchResult
import org.json.JSONObject

object MusicMapper {
    fun fromTrackJson(json: JSONObject): SearchResult {
        return SearchResult(
            id = json.optString("video_id"),
            title = json.optString("title"),
            posterUrl = json.optString("thumbnail"),
            year = null,
            type = ContentType.MUSIC,
            sourceId = "ytmusic"
        )
    }
}
