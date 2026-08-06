package com.streamix.core.api

import com.streamix.core.plugin.ContentType
import com.streamix.core.plugin.SearchResult
import org.json.JSONObject

object TmdbMapper {
    private const val IMG_BASE = "https://image.tmdb.org/t/p/w342"

    fun fromMovieJson(json: JSONObject): SearchResult {
        return SearchResult(
            id = json.optInt("id").toString(),
            title = json.optString("title").ifEmpty { json.optString("original_title") },
            posterUrl = json.optString("poster_path").let { if (it.isNotEmpty()) "$IMG_BASE$it" else null },
            year = json.optString("release_date").take(4).toIntOrNull(),
            type = ContentType.MOVIE,
            sourceId = "tmdb"
        )
    }

    fun fromShowJson(json: JSONObject): SearchResult {
        return SearchResult(
            id = json.optInt("id").toString(),
            title = json.optString("name").ifEmpty { json.optString("original_name") },
            posterUrl = json.optString("poster_path").let { if (it.isNotEmpty()) "$IMG_BASE$it" else null },
            year = json.optString("first_air_date").take(4).toIntOrNull(),
            type = ContentType.SERIES,
            sourceId = "tmdb"
        )
    }
}
