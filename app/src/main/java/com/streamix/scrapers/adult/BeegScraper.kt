package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BeegScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "beeg",
        displayName = "Beeg",
        baseUrl = "https://beeg.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val apiBase = "https://store.externulls.com"

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * 48
        val url = "$apiBase/facts/tag?slug=index&limit=48&offset=$offset"
        val json = ScraperHttpClient.get(url, mapOf("Referer" to info.baseUrl)) ?: return@withContext emptyList()
        parseJson(json)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        // Beeg API search is more complex, fallback to actor search for now
        val url = "$apiBase/tag/recommends?type=person&slug=index"
        val json = ScraperHttpClient.get(url, mapOf("Referer" to info.baseUrl)) ?: return@withContext emptyList()
        try {
            val arr = JSONArray(json)
            val results = mutableListOf<ScrapeResult>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("tg_name")
                if (name.contains(query, ignoreCase = true)) {
                    val slug = obj.getString("tg_slug")
                    results.add(ScrapeResult(
                        id = "${info.baseUrl}/$slug",
                        title = name,
                        posterUrl = "", // Beeg actor thumbs are nested, skipping for now
                        pageUrl = "${info.baseUrl}/$slug",
                        sourceId = info.id
                    ))
                }
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    private fun parseJson(json: String): List<ScrapeResult> {
        return try {
            val root = JSONArray(json)
            val results = mutableListOf<ScrapeResult>()
            for (i in 0 until root.length()) {
                val group = root.getJSONObject(i)
                val file = group.getJSONObject("file")
                val data = file.getJSONArray("data")
                val first = data.getJSONObject(0)
                
                val title = first.getString("cd_value")
                val videoId = first.getString("cd_file")
                val thumb = "https://thumbs.externulls.com/videos/$videoId/49.webp?size=480x270"
                
                results.add(ScrapeResult(
                    id = videoId,
                    title = title,
                    posterUrl = thumb,
                    pageUrl = videoId, // using videoId as data
                    sourceId = info.id
                ))
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val id = if (pageUrl.startsWith("http")) pageUrl.substringAfterLast("/") else pageUrl
        val url = "$apiBase/facts/file/$id"
        val json = ScraperHttpClient.get(url, mapOf("Referer" to info.baseUrl)) ?: return@withContext emptyList()
        
        val results = mutableListOf<StreamResult>()
        try {
            val obj = JSONObject(json)
            val file = obj.optJSONObject("file") ?: obj.getJSONArray("fc_facts").getJSONObject(0).getJSONObject("file")
            val hls = file.getJSONObject("hls_resources").getString("fl_cdn_multi")
            if (hls.isNotEmpty()) {
                results.add(StreamResult(url = "https://video.beeg.com/$hls", quality = "Auto", format = "hls"))
            }
        } catch (_: Exception) {}
        
        results
    }
}
