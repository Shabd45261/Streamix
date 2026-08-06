package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class PornHatScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "pornhat",
        displayName = "PornHat",
        baseUrl = "https://www.pornhat.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/trending/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search/$query/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.item").forEach { el ->
                val linkEl = el.selectFirst("a") ?: return@forEach
                val href = linkEl.attr("href")
                val title = linkEl.attr("title").trim()
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img")
                val thumb = img?.attr("data-original")?.ifEmpty { img.attr("src") } ?: img?.attr("src") ?: ""

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

        val source = doc.selectFirst("video#my-video source")
        val initialUrl = source?.attr("src") ?: ""

        if (initialUrl.isNotEmpty()) {
            // Usually this redirects to the actual m3u8
            val response = ScraperHttpClient.getWithResponse(initialUrl)
            val finalUrl = response?.request?.url?.toString() ?: initialUrl
            
            if (finalUrl.contains(".m3u8")) {
                results.add(StreamResult(url = finalUrl, quality = "Auto", format = "hls", headers = mapOf("Referer" to pageUrl)))
            } else {
                results.add(StreamResult(url = finalUrl, quality = "Auto", format = "mp4", headers = mapOf("Referer" to pageUrl)))
            }
        }
        
        // Check for direct m3u8 in page as fallback
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls", headers = mapOf("Referer" to pageUrl)))
        }

        results
    }
}
