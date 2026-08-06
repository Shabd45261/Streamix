package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class EromeScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "erome",
        displayName = "Erome",
        baseUrl = "https://www.erome.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) "$baseUrl/explore" else "$baseUrl/explore?page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?q=$query&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.album").forEach { el ->
                val titleEl = el.selectFirst("a.album-title") ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img.album-thumbnail.active") ?: el.selectFirst("img")
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

        doc.select("div.video video").forEach { videoTag ->
            val source = videoTag.selectFirst("source") ?: return@forEach
            val url = source.attr("src")
            if (url.isNotEmpty() && url.startsWith("http")) {
                val label = source.attr("label").ifEmpty { "HD" }
                val format = if (url.contains(".m3u8")) "hls" else "mp4"
                results.add(StreamResult(
                    url = url, quality = label, format = format,
                    headers = mapOf("Referer" to baseUrl)
                ))
            }
        }
        results
    }
}
