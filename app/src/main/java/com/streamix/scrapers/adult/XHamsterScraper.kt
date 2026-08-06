package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class XHamsterScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "xhamster",
        displayName = "xHamster",
        baseUrl = "https://xhamster.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/newest?page=$page"
        val html = ScraperHttpClient.get(url, mapOf("Referer" to baseUrl)) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search/$encoded?page=$page"
        val html = ScraperHttpClient.get(url, mapOf("Referer" to baseUrl)) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    private fun parseVideoList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            // Update selectors for current xHamster layout
            doc.select(".video-thumb, .thumb-list__item, [data-video-url]").forEach { el ->
                val titleEl = el.select(".video-thumb__title a, .thumb-list__item-title a").firstOrNull()
                    ?: el.select("a[title]").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim().ifEmpty { titleEl.attr("title") }
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else {
                    if (href.startsWith("/")) "$baseUrl$href" else "$baseUrl/$href"
                }
                
                val img = el.select("img").firstOrNull()
                val thumb = img?.attr("data-src") ?: img?.attr("src") ?: ""
                val duration = el.select(".video-thumb__duration, .duration").text()

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

        // xHamster stores sources in window.initials JSON
        val jsonRegex = Regex("""window\.initials\s*=\s*(\{.+?\});""", RegexOption.DOT_MATCHES_ALL)
        val jsonStr = jsonRegex.find(html)?.groupValues?.get(1)

        if (jsonStr != null) {
            try {
                val json = org.json.JSONObject(jsonStr)
                val xplayerSettings = json.optJSONObject("xplayerSettings")
                val sources = xplayerSettings?.optJSONObject("sources")
                sources?.keys()?.forEach { quality ->
                    val urlOrArray = sources.opt(quality)
                    val url = when (urlOrArray) {
                        is String -> urlOrArray
                        is org.json.JSONArray -> if (urlOrArray.length() > 0) urlOrArray.getJSONObject(0).optString("url") else null
                        else -> null
                    }
                    if (!url.isNullOrEmpty()) {
                        val format = if (url.contains(".m3u8")) "hls" else "mp4"
                        results.add(StreamResult(url = url, quality = quality, format = format))
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback: grep for m3u8
        if (results.isEmpty()) {
            Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""").find(html)?.groupValues?.get(1)?.let { url ->
                results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
            }
        }

        results
    }
}
