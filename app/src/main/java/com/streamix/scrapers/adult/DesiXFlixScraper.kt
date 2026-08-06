package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class DesiXFlixScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "desixflix",
        displayName = "DesiXFlix",
        baseUrl = "https://desixflix.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/page/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/?s=$query"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("article > a").forEach { el ->
                val href = el.attr("href")
                val title = el.attr("title").trim()
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img").firstOrNull()
                val thumb = img?.attr("data-src")?.ifEmpty { img.attr("src") } ?: ""

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

        val embedUrl = doc.selectFirst("div.video-player > meta[itemprop=embedURL], meta[itemprop=contentURL]")?.attr("content") ?: ""
        if (embedUrl.isNotEmpty()) {
            results.add(StreamResult(url = embedUrl, quality = "Auto", format = "mp4"))
        }
        
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        results
    }
}
