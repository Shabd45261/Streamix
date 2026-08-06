package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject

class JavseenScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "javseen",
        displayName = "Javseen",
        baseUrl = "https://javseen.tv",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        // Javseen uses an AJAX call for the home page
        val url = if (page <= 1) "$baseUrl/recent/?ajax=browse_videos" else "$baseUrl/recent/$page/?ajax=browse_videos"
        val json = ScraperHttpClient.get(url, mapOf("Accept" to "*/*", "Referer" to "$baseUrl/recent/")) ?: return@withContext emptyList()
        try {
            val html = JSONObject(json).optString("html", "")
            if (html.isEmpty()) return@withContext emptyList()
            parseList(html)
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) "$baseUrl/search/video/?s=$query" else "$baseUrl/search/video/?s=$query&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("li[id^=video-]").forEach { el ->
                val linkEl = el.selectFirst("a.thumbnail") ?: return@forEach
                val title = linkEl.selectFirst("span.video-title")?.text()?.trim() ?: linkEl.attr("title")
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = linkEl.selectFirst("img")
                val thumb = img?.attr("src") ?: ""

                results.add(ScrapeResult(
                    id = link, title = title,
                    posterUrl = thumb.takeIf { it.startsWith("http") },
                    pageUrl = link, sourceId = info.id
                ))
            }
            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        val results = mutableListOf<StreamResult>()

        doc.select("button.button_choice_server").forEach { btn ->
            val encodedEmbed = btn.attr("data-embed") ?: ""
            if (encodedEmbed.isNotEmpty()) {
                val decodedUrl = String(android.util.Base64.decode(encodedEmbed, android.util.Base64.DEFAULT))
                if (decodedUrl.startsWith("http")) {
                    results.add(StreamResult(url = decodedUrl, quality = btn.text().trim(), format = "mp4"))
                }
            }
        }
        
        results
    }
}
