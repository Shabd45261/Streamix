package com.streamix.scrapers.music

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YTMusicScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "ytmusic",
        displayName = "YouTube Music",
        baseUrl = "https://music.youtube.com",
        categories = listOf(ContentCategory.MUSIC)
    )

    // Uses Invidious public API as proxy (no auth needed)
    private val invidiousBase = "https://inv.tux.pizza/api/v1"

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$invidiousBase/trending?type=music&region=US"
        val json = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseTrending(json)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$invidiousBase/search?q=$encoded&type=video&page=$page&sort_by=relevance"
        val json = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseSearch(json)
    }

    private fun parseTrending(json: String): List<ScrapeResult> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until minOf(arr.length(), 30)).map { i ->
                val obj = arr.getJSONObject(i)
                val videoId = obj.optString("videoId")
                val title = obj.optString("title")
                val thumbs = obj.optJSONArray("videoThumbnails")
                val thumb = thumbs?.getJSONObject(0)?.optString("url") ?: ""
                val duration = obj.optInt("lengthSeconds", 0).let { s ->
                    if (s > 0) "${s / 60}:${(s % 60).toString().padStart(2, '0')}" else ""
                }
                ScrapeResult(
                    id = videoId,
                    title = title,
                    posterUrl = thumb.takeIf { it.startsWith("http") },
                    pageUrl = "https://www.youtube.com/watch?v=$videoId",
                    duration = duration,
                    sourceId = info.id
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseSearch(json: String): List<ScrapeResult> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until minOf(arr.length(), 30)).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                if (obj.optString("type") != "video") return@mapNotNull null
                val videoId = obj.optString("videoId")
                val title = obj.optString("title")
                val thumbs = obj.optJSONArray("videoThumbnails")
                val thumb = thumbs?.getJSONObject(0)?.optString("url") ?: ""
                ScrapeResult(
                    id = videoId, title = title,
                    posterUrl = thumb.takeIf { it.startsWith("http") },
                    pageUrl = "https://www.youtube.com/watch?v=$videoId",
                    sourceId = info.id
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val videoId = Regex("""v=([A-Za-z0-9_-]{11})""").find(pageUrl)?.groupValues?.get(1)
            ?: return@withContext emptyList()

        try {
            val url = "$invidiousBase/videos/$videoId"
            val json = ScraperHttpClient.get(url) ?: return@withContext emptyList()
            val obj = JSONObject(json)
            val results = mutableListOf<StreamResult>()

            // HLS adaptive stream
            val hlsUrl = obj.optString("hlsUrl")
            if (hlsUrl.isNotEmpty()) {
                results.add(StreamResult(url = hlsUrl, quality = "Auto", format = "hls"))
            }

            // Adaptive formats (dash/mp4)
            val adaptiveFormats = obj.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val fmt = adaptiveFormats.getJSONObject(i)
                    val mimeType = fmt.optString("type")
                    if (!mimeType.contains("video")) continue
                    val streamUrl = fmt.optString("url")
                    val quality = fmt.optString("qualityLabel").ifEmpty { fmt.optString("quality") }
                    if (streamUrl.isNotEmpty()) {
                        results.add(StreamResult(url = streamUrl, quality = quality, format = "mp4"))
                    }
                }
            }

            // Format streams (direct mp4)
            val formatStreams = obj.optJSONArray("formatStreams")
            if (formatStreams != null) {
                for (i in 0 until formatStreams.length()) {
                    val fmt = formatStreams.getJSONObject(i)
                    val streamUrl = fmt.optString("url")
                    val quality = fmt.optString("qualityLabel").ifEmpty { fmt.optString("quality") }
                    if (streamUrl.isNotEmpty()) {
                        results.add(StreamResult(url = streamUrl, quality = quality, format = "mp4"))
                    }
                }
            }

            results
        } catch (e: Exception) { emptyList() }
    }
}
