package com.streamix.scrapers.adult

import android.util.Log
import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class RedtubeScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "redtube",
        displayName = "Redtube",
        baseUrl = "https://www.redtube.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/mostviewed?page=$page" else "$baseUrl/mostviewed"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        val url = "$baseUrl/?search=$encoded&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    private fun parseVideoList(html: String): List<ScrapeResult> {
        val results = mutableListOf<ScrapeResult>()
        try {
            val doc = Jsoup.parse(html, baseUrl)
            doc.select(".video-list li, .video_item").forEach { el ->
                val linkEl = el.select("a").firstOrNull() ?: return@forEach
                val href = linkEl.attr("href")
                if (!href.contains("/video/")) return@forEach
                
                val title = el.select(".video-title, .title").text().ifEmpty { linkEl.attr("title") }.trim()
                val thumb = el.select("img").attr("data-src").ifEmpty { el.select("img").attr("src") }
                val duration = el.select(".duration").text()
                
                val fullUrl = if (href.startsWith("http")) href else "$baseUrl${if (href.startsWith("/")) "" else "/"}$href"

                results.add(ScrapeResult(
                    id = fullUrl,
                    title = title,
                    posterUrl = thumb.takeIf { it.isNotEmpty() }?.let {
                        if (it.startsWith("//")) "https:$it"
                        else if (it.startsWith("/")) "$baseUrl$it"
                        else if (!it.startsWith("http")) "$baseUrl/$it"
                        else it
                    },
                    pageUrl = fullUrl,
                    duration = duration,
                    sourceId = info.id
                ))
            }
        } catch (e: Exception) { 
            Log.e("RedtubeScraper", "Parse failed", e)
        }
        return results.distinctBy { it.pageUrl }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val results = mutableListOf<StreamResult>()
        
        try {
            // Redtube uses a JSON blob in the page for video configurations
            // Look for "mediaDefinition" or similar in scripts
            val mediaRegex = Regex(""""mediaDefinition":\s*(\[.*\])""")
            val match = mediaRegex.find(html)
            if (match != null) {
                val jsonArr = org.json.JSONArray(match.groupValues[1])
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val videoUrl = obj.optString("videoUrl")
                    val quality = obj.optString("quality")
                    val format = obj.optString("format") // "hls" or "mp4"
                    
                    if (videoUrl.isNotEmpty()) {
                        results.add(StreamResult(
                            url = videoUrl.replace("\\/", "/"),
                            quality = quality,
                            format = if (format.lowercase() == "hls") "hls" else "mp4"
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        results
    }
}
