package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MissAVScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "missav",
        displayName = "MissAV",
        baseUrl = "https://missav.ai", // Updated from .ws
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/new?page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/search/$query?page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select(".thumbnail").forEach { el ->
                val titleEl = el.select(".text-secondary").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select(".w-full").firstOrNull() ?: el.select("img").firstOrNull()
                val thumb = img?.attr("data-src") ?: img?.attr("src") ?: ""

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

        // MissAV uses packed JS or direct source='...'
        val packedRegex = Regex("""source='([^']+)'""")
        packedRegex.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
        }

        // Fallback: search for any m3u8
        if (results.isEmpty()) {
            val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
            m3u8Regex.find(html)?.groupValues?.get(1)?.let { url ->
                results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
            }
        }

        results
    }
}
