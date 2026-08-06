package com.streamix.core

enum class TvType {
    Movie, TvSeries, Anime, Music, Adult
}

data class SearchResponse(
    val name: String,
    val url: String,
    val posterUrl: String?,
    val type: TvType? = null,
    val quality: String? = null
)

data class LoadResponse(
    val name: String,
    val url: String,
    val posterUrl: String?,
    val description: String?,
    val rating: String?,
    val year: Int?,
    val actors: List<String>?,
    val episodes: List<Episode>? = null
)

data class Episode(
    val name: String?,
    val number: Int?,
    val season: Int?,
    val url: String,
    val posterUrl: String? = null,
    val description: String? = null
)

data class VideoLink(
    val name: String,
    val url: String,
    val quality: String? = null,
    val isHls: Boolean = false
)

interface MainAPI {
    val name: String
    val mainUrl: String
    val supportedTypes: Set<TvType>

    /** Search for content */
    suspend fun search(query: String): List<SearchResponse>

    /** Get latest content for home screen */
    suspend fun getLatest(): List<SearchResponse>

    /** Load detailed info for a specific entry */
    suspend fun load(url: String): LoadResponse

    /** Extract video links/streams */
    suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        callback: (VideoLink) -> Unit
    ): Boolean
}
