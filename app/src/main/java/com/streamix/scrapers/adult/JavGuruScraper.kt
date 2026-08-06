package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class JavGuruScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "javguru",
        displayName = "Jav Guru",
        baseUrl = "https://jav.guru",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page == 1) baseUrl else "$baseUrl/page/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/?s=$query" // pagination not immediately obvious in simple search
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("article, .inside-article").forEach { el ->
                val linkEl = el.select(".post-thumbnail a, .entry-header a").firstOrNull() ?: return@forEach
                val href = linkEl.attr("href")
                if (href.isEmpty() || href == "#") return@forEach

                val img = linkEl.select("img").firstOrNull()
                val title = img?.attr("alt")?.ifEmpty { el.select(".entry-title").text() } ?: el.select(".entry-title").text()
                val thumb = img?.attr("src") ?: ""

                results.add(ScrapeResult(
                    id = href, title = title.trim(),
                    posterUrl = thumb.takeIf { it.startsWith("http") },
                    pageUrl = href, sourceId = info.id
                ))
            }
            
            if (results.isEmpty()) {
                // Fallback for different grid layout
                doc.select("div.row div.imgg").forEach { el ->
                    val linkEl = el.select("a").firstOrNull() ?: return@forEach
                    val href = linkEl.attr("href")
                    val img = linkEl.select("img").firstOrNull()
                    val title = img?.attr("alt") ?: ""
                    val thumb = img?.attr("src") ?: ""
                    results.add(ScrapeResult(
                        id = href, title = title.trim(),
                        posterUrl = thumb.takeIf { it.startsWith("http") },
                        pageUrl = href, sourceId = info.id
                    ))
                }
            }

            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        val results = mutableListOf<StreamResult>()

        // JavGuru usually embeds videos from other providers (doodstream, mixdrop, etc.)
        // Extracting these requires specific extractors. For now, look for iframe src.
        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.contains("dood") || src.contains("mixdrop") || src.contains("streamtape")) {
                results.add(StreamResult(url = src, quality = "External", format = "mp4"))
            }
        }
        
        // Check for direct links or m3u8 in scripts
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        results
    }
}
