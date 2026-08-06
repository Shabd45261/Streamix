package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class XNXXScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "xnxx",
        displayName = "XNXX",
        baseUrl = "https://www.xnxx.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page == 1) baseUrl else "$baseUrl/new/$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search/$encoded/${page - 1}"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    private fun parseVideoList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select(".thumb-block, .mozaique .thumb, div[id^='video_']").forEach { el ->
                val linkEl = el.select("a").firstOrNull { it.attr("href").contains("/video") }
                    ?: el.select(".thumb-under p a").firstOrNull() ?: el.select("a").firstOrNull() ?: return@forEach
                val href = linkEl.attr("href")
                if (href.isEmpty() || href == "#") return@forEach

                val title = linkEl.attr("title").ifEmpty { el.select(".thumb-under p a").text() }
                    .ifEmpty { el.select("img").attr("title") }.trim()
                
                val link = if (href.startsWith("http")) href else {
                    if (href.startsWith("/")) "$baseUrl$href" else "$baseUrl/$href"
                }
                
                val img = el.select(".thumb img").firstOrNull() ?: el.select("img").firstOrNull()
                val thumb = img?.attr("data-src") ?: img?.attr("src") ?: ""
                
                if (title.isNotEmpty()) {
                    results.add(ScrapeResult(
                        id = link,
                        title = title,
                        posterUrl = thumb.takeIf { it.startsWith("http") || it.startsWith("//") }?.let { if (it.startsWith("//")) "https:$it" else it },
                        pageUrl = link,
                        sourceId = info.id
                    ))
                }
            }
            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl, mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()

        val results = mutableListOf<StreamResult>()

        // XNXX HLS
        val hlsRegex = Regex("""html5player\.setVideoHLS\s*\(\s*['"](.*?)['"]\s*\)""")
        hlsRegex.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
        }

        // XNXX MP4 High
        val mp4High = Regex("""html5player\.setVideoUrlHigh\s*\(\s*['"](.*?)['"]\s*\)""")
        mp4High.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "720p", format = "mp4"))
        }

        // XNXX MP4 Low
        val mp4Low = Regex("""html5player\.setVideoUrlLow\s*\(\s*['"](.*?)['"]\s*\)""")
        mp4Low.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "480p", format = "mp4"))
        }

        results
    }
}
