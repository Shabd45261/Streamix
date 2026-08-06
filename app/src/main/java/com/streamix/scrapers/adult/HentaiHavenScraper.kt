package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject

class HentaiHavenScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "hentaihaven",
        displayName = "Hentai Haven",
        baseUrl = "https://hentaihaven.xxx",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(baseUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html, baseUrl)
        val results = mutableListOf<ScrapeResult>()

        doc.select("div.item.vraven_item").forEach { el ->
            val linkEl = el.selectFirst("a") ?: return@forEach
            val href = linkEl.attr("href")
            val title = linkEl.attr("title").ifEmpty { el.select("div.home_slider_header").text() }
            if (href.isEmpty()) return@forEach

            val img = el.selectFirst("img")
            val thumb = img?.attr("src")?.ifEmpty { img.attr("data-src") } ?: ""

            results.add(ScrapeResult(
                id = href, title = title,
                posterUrl = thumb.takeIf { it.startsWith("http") },
                pageUrl = href, sourceId = info.id
            ))
        }
        results.distinctBy { it.pageUrl }.take(30)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/?s=$query&post_type=wp-manga"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html, baseUrl)
        val results = mutableListOf<ScrapeResult>()

        doc.select("div.c-tabs-item__content").forEach { el ->
            val linkEl = el.selectFirst("a") ?: return@forEach
            val href = linkEl.attr("href")
            val title = linkEl.attr("title")
            if (href.isEmpty()) return@forEach

            val img = el.selectFirst("img")
            val thumb = img?.attr("src") ?: img?.attr("data-src") ?: ""

            results.add(ScrapeResult(
                id = href, title = title,
                posterUrl = thumb.takeIf { it.startsWith("http") },
                pageUrl = href, sourceId = info.id
            ))
        }
        results
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        val results = mutableListOf<StreamResult>()

        val iframe = doc.selectFirst("div.player_logic_item iframe")?.attr("src") ?: ""
        if (iframe.isNotEmpty()) {
            val iframeHtml = ScraperHttpClient.get(iframe, mapOf("Referer" to pageUrl)) ?: ""
            
            // HentaiHaven uses specific encrypted vars 'en' and 'iv'
            val en = Regex("var en = '([^']*)';").find(iframeHtml)?.groupValues?.get(1) ?: ""
            val iv = Regex("var iv = '([^']*)';").find(iframeHtml)?.groupValues?.get(1) ?: ""
            
            if (en.isNotEmpty()) {
                val apiUrl = "$baseUrl/wp-content/plugins/player-logic/api.php"
                // This requires a POST with 'action', 'a', 'b'
                // For simplicity, let's look for direct m3u8 as fallback
            }
        }
        
        // Fallback for m3u8
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        results
    }
}
