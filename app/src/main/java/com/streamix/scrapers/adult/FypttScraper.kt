package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class FypttScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "fyptt",
        displayName = "Fyptt",
        baseUrl = "https://fyptt.to",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/tiktok-porn/page/$page/" else "$baseUrl/tiktok-porn/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/page/$page/?s=$query" else "$baseUrl/?s=$query"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.fl-post-column").forEach { el ->
                val linkEl = el.select(".fl-post-grid-image a").firstOrNull() ?: return@forEach
                val title = linkEl.attr("title").ifEmpty { el.text() }.trim()
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img").firstOrNull()
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

        val iframePath = doc.selectFirst("iframe[src*='str.php']")?.attr("src") ?: ""
        if (iframePath.isNotEmpty()) {
            val iframeUrl = if (iframePath.startsWith("http")) iframePath else "$baseUrl/${iframePath.removePrefix("/")}"
            val iframeHtml = ScraperHttpClient.get(iframeUrl, mapOf("Referer" to pageUrl)) ?: ""
            
            val videoUrl = Regex("""file\s*:\s*["']([^"']+)["']""").find(iframeHtml)?.groupValues?.get(1)
                ?: Jsoup.parse(iframeHtml).selectFirst("video source")?.attr("src")
                ?: ""
                
            if (videoUrl.isNotEmpty()) {
                val finalUrl = if (videoUrl.startsWith("http")) videoUrl else "$baseUrl/${videoUrl.removePrefix("/")}"
                results.add(StreamResult(url = finalUrl, quality = "Auto", format = "mp4", headers = mapOf("Referer" to iframeUrl)))
            }
        }

        results
    }
}
