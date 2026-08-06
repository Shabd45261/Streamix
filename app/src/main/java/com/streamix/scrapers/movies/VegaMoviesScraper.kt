package com.streamix.scrapers.movies

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class VegaMoviesScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "vegamovies",
        displayName = "VegaMovies",
        baseUrl = "https://vegamovies.skin",
        categories = listOf(ContentCategory.MOVIES, ContentCategory.SHOWS)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get("$baseUrl/page/$page/", mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val html = ScraperHttpClient.get("$baseUrl/?s=$encoded&page=$page", mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("article.post, .article-card").forEach { el ->
                val titleEl = el.select("h2 a, h3 a, .entry-title a").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img").firstOrNull()
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
        try {
            val html = ScraperHttpClient.get(pageUrl, mapOf("Referer" to baseUrl))
                ?: return@withContext emptyList()
            val doc = Jsoup.parse(html, baseUrl)

            val results = mutableListOf<StreamResult>()

            // Look for download/stream buttons
            doc.select("a[href*='gdtot'], a[href*='gdflix'], a[href*='v-cloud'], a[href*='driveleech']").forEach { a ->
                val url = a.attr("href")
                if (url.startsWith("http")) {
                    results.add(StreamResult(url = url, quality = a.text().ifEmpty { "Stream" }, format = "mp4"))
                }
            }

            // Look for direct video URLs
            doc.select("source[src]").forEach { src ->
                val url = src.attr("src")
                if (url.startsWith("http")) {
                    val format = if (url.contains(".m3u8")) "hls" else "mp4"
                    results.add(StreamResult(url = url, quality = src.attr("label").ifEmpty { "Auto" }, format = format))
                }
            }

            results
        } catch (e: Exception) { emptyList() }
    }
}
