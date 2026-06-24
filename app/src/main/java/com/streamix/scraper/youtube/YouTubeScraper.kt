package com.streamix.scraper.youtube

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import com.streamix.ui.youtube.YoutubeVideoItem
import org.jsoup.nodes.Document
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeScraper @Inject constructor() : BaseScraper() {

    override val name = "YouTube"
    override val mainUrl = "https://www.youtube.com"

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val html = get("$mainUrl/results?search_query=$encoded")
            val doc = org.jsoup.Jsoup.parse(html)
            
            // Mobile/Web parsing fallback
            val results = mutableListOf<SearchResult>()
            
            // Search for video IDs in the script (since modern YT is heavily JS based)
            val idRegex = Regex("""/watch\?v=([a-zA-Z0-9_-]{11})""")
            idRegex.findAll(html).distinctBy { it.groupValues[1] }.take(20).forEach { match ->
                val id = match.groupValues[1]
                results.add(SearchResult(
                    id = id,
                    title = "YouTube Video", // Hard to parse title from raw HTML script
                    posterPath = "https://img.youtube.com/vi/$id/mqdefault.jpg",
                    mediaType = "youtube"
                ))
            }
            
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        // Since we don't have a built-in extractor, we'll try to find a direct link if possible,
        // otherwise we'll use a placeholder and rely on VLC or a future extractor implementation.
        // For now, return the watch URL and mark it as YouTube so the player can handle it.
        return listOf(VideoLink("https://www.youtube.com/watch?v=$mediaId", "Auto", name))
    }

    suspend fun getTrending(): List<YoutubeVideoItem> {
        return searchShorts("trending").map { 
            YoutubeVideoItem(it.id, it.title, it.thumbnailUrl, it.channelName, it.viewCount)
        }
    }

    suspend fun searchShorts(query: String): List<YoutubeVideoItem> {
        return try {
            val q = if (query.isBlank()) "shorts" else "$query shorts"
            val encoded = URLEncoder.encode(q, "UTF-8")
            val html = get("$mainUrl/results?search_query=$encoded")
            
            val items = mutableListOf<YoutubeVideoItem>()
            val idRegex = Regex("""/watch\?v=([a-zA-Z0-9_-]{11})""")
            idRegex.findAll(html).distinctBy { it.groupValues[1] }.take(15).forEach { match ->
                val id = match.groupValues[1]
                items.add(YoutubeVideoItem(
                    id = id,
                    title = "Short Video",
                    thumbnailUrl = "https://img.youtube.com/vi/$id/mqdefault.jpg",
                    channelName = "YouTube",
                    viewCount = ""
                ))
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }
}
