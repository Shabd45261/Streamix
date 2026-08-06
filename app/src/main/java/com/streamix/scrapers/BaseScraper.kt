package com.streamix.scrapers

import com.streamix.core.plugin.ContentCategory

data class ScrapeResult(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val pageUrl: String,
    val quality: String? = null,
    val duration: String? = null,
    val sourceId: String = "",
    val description: String? = null
)

data class StreamResult(
    val url: String,
    val quality: String,
    val format: String, // "hls" | "mp4" | "dash"
    val headers: Map<String, String> = emptyMap()
)

data class ScraperInfo(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val iconUrl: String = "",
    val categories: List<ContentCategory>,
    val isEnabled: Boolean = true,
    var isWorking: Boolean = true // Added status indicator
)

interface BaseScraper {
    val info: ScraperInfo
    suspend fun getTrending(page: Int = 1): List<ScrapeResult>
    suspend fun search(query: String, page: Int = 1): List<ScrapeResult>
    suspend fun getStreamLinks(pageUrl: String): List<StreamResult>
    suspend fun getCategories(): List<String> = emptyList()
    suspend fun browseCategory(category: String, page: Int = 1): List<ScrapeResult> = emptyList()
}
