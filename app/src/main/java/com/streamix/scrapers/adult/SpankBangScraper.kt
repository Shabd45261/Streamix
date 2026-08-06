package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class SpankBangScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "spankbang",
        displayName = "SpankBang",
        baseUrl = "https://spankbang.party", // Updated from .com
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/trending/videos/$page/"
        val html = ScraperHttpClient.get(url, mapOf("Referer" to baseUrl)) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/s/$encoded/videos/$page/"
        val html = ScraperHttpClient.get(url, mapOf("Referer" to baseUrl)) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    private fun parseVideoList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select(".video-item").forEach { el ->
                val linkEl = el.select("a.n").firstOrNull() ?: el.select("a").firstOrNull() ?: return@forEach
                val title = linkEl.attr("title").ifEmpty { linkEl.text() }.trim()
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img").firstOrNull()
                val thumb = img?.attr("data-src") ?: img?.attr("src") ?: ""
                val duration = el.select(".l").text()

                results.add(ScrapeResult(
                    id = link, title = title,
                    posterUrl = thumb.takeIf { it.startsWith("http") },
                    pageUrl = link, duration = duration, sourceId = info.id
                ))
            }
            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl, mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()

        val results = mutableListOf<StreamResult>()

        val streamDataRegex = Regex("""stream_data\s*=\s*(\{[^}]+\})""")
        val streamStr = streamDataRegex.find(html)?.groupValues?.get(1)

        if (streamStr != null) {
            try {
                val json = org.json.JSONObject(streamStr)
                json.keys().forEach { quality ->
                    val url = json.optString(quality)
                    if (url.isNotEmpty() && url.startsWith("http")) {
                        val format = if (url.contains(".m3u8")) "hls" else "mp4"
                        results.add(StreamResult(url = url, quality = quality, format = format))
                    }
                }
            } catch (_: Exception) {}
        }

        if (results.isEmpty()) {
            Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""").find(html)?.groupValues?.get(1)?.let { url ->
                results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
            }
        }

        results
    }
}
