package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject

class JavtifulScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "javtiful",
        displayName = "Javtiful",
        baseUrl = "https://javtiful.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/trending/?page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search/videos?search_query=$query&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.card.border-0").forEach { el ->
                val linkEl = el.selectFirst("a") ?: return@forEach
                val href = linkEl.attr("href")
                val title = el.select("div.d-flex.align-items-center a").attr("title").trim()
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = linkEl.selectFirst("img")
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

        val token = doc.selectFirst("#token_full")?.attr("data-csrf-token") ?: ""
        val script = doc.selectFirst("script:containsData(vcpov)")?.data() ?: ""
        val postId = Regex("vcpov\\s+=\\s+`(.*?)`").find(script)?.groupValues?.get(1) ?: ""
        
        if (postId.isNotEmpty()) {
            // This usually requires a POST to /ajax/get_cdn with video_id, pid_c, and token
            // For now, look for direct m3u8 as fallback
        }
        
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        results
    }
}
