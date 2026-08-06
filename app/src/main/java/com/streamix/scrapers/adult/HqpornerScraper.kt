package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class HqpornerScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "hqporner",
        displayName = "Hqporner",
        baseUrl = "https://hqporner.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) "$baseUrl/top/month" else "$baseUrl/top/month?p=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/?q=$query&p=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.box.page-content div.row section").forEach { el ->
                val titleEl = el.selectFirst("h3 a") ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img")
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
        val results = mutableListOf<StreamResult>()

        val rawUrl = Regex("""url: '/blocks/altplayer\.php\?i=//(.*?)',""").find(html)?.groupValues?.get(1) ?: ""
        if (rawUrl.isNotEmpty()) {
            val extractorUrl = "https://$rawUrl"
            // This is a redirector, usually to mp4 files.
            val extractorHtml = ScraperHttpClient.get(extractorUrl, mapOf("Referer" to pageUrl)) ?: ""
            
            // Grep for source files in extractor
            val sourceRegex = Regex("""file\s*:\s*["']([^"']+)["']""")
            sourceRegex.findAll(extractorHtml).forEach { m ->
                val url = m.groupValues[1]
                if (url.startsWith("http")) {
                    results.add(StreamResult(url = url, quality = "Auto", format = "mp4", headers = mapOf("Referer" to extractorUrl)))
                }
            }
        }
        
        // Fallback for m3u8
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls", headers = mapOf("Referer" to pageUrl)))
        }

        results
    }
}
