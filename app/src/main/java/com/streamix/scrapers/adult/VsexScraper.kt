package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class VsexScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "vsex",
        displayName = "VSex",
        baseUrl = "https://vsex.in",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) baseUrl else "$baseUrl/page/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val start = (page - 1) * 28
        val url = "$baseUrl/index.php?do=search&subaction=search&search_start=$start&full_search=0&story=$query"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.thumb").forEach { el ->
                val titleEl = el.selectFirst("h2.th-title") ?: return@forEach
                val title = titleEl.text().trim()
                val linkEl = el.selectFirst("a") ?: return@forEach
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img")
                val thumb = img?.attr("data-src")?.ifEmpty { img.attr("src") } ?: img?.attr("src") ?: ""

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

        val iframe = doc.selectFirst("iframe")?.attr("src") ?: ""
        if (iframe.isNotEmpty()) {
            // Check for direct video links or common extractors
            if (iframe.contains("filemoon") || iframe.contains("vtube") || iframe.contains("streamwish")) {
                results.add(StreamResult(url = iframe, quality = "External", format = "mp4"))
            }
        }
        
        // Grep for m3u8 as fallback
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        results
    }
}
