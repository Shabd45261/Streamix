package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class XVideosScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "xvideos",
        displayName = "xVideos",
        baseUrl = "https://www.xvideos.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/new/$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/?k=$encoded&p=${page - 1}"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseVideoList(html)
    }

    private fun parseVideoList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select(".thumb-block, .mozaique .thumb, div[id^='video_']").forEach { el ->
                val linkEl = el.select("a").firstOrNull { it.attr("href").contains("/video") } 
                    ?: el.select("a").firstOrNull() ?: return@forEach
                val href = linkEl.attr("href")
                if (href.isEmpty() || href == "#") return@forEach

                val title = el.select(".title, p.mozaique-title").text().trim()
                    .ifEmpty { linkEl.attr("title") }.ifEmpty { el.select("img").attr("title") }
                
                val link = if (href.startsWith("http")) href else {
                    if (href.startsWith("/")) "$baseUrl$href" else "$baseUrl/$href"
                }
                
                val img = el.select("img").firstOrNull()
                val thumbRaw = img?.attr("data-src") ?: img?.attr("src") ?: ""
                val thumb = if (thumbRaw.startsWith("//")) "https:$thumbRaw" 
                            else if (thumbRaw.startsWith("/")) "$baseUrl$thumbRaw"
                            else thumbRaw
                
                val duration = el.select(".duration").text()

                if (title.isNotEmpty()) {
                    results.add(ScrapeResult(
                        id = link,
                        title = title,
                        posterUrl = thumb.takeIf { it.isNotEmpty() },
                        pageUrl = link,
                        duration = duration,
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

        // xVideos HLS
        val hlsRegex = Regex("""html5player\.setVideoHLS\('([^']+)'\)""")
        hlsRegex.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
        }

        // xVideos MP4 high quality
        val mp4High = Regex("""html5player\.setVideoUrlHigh\('([^']+)'\)""")
        mp4High.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "720p", format = "mp4"))
        }

        // xVideos MP4 low quality
        val mp4Low = Regex("""html5player\.setVideoUrlLow\('([^']+)'\)""")
        mp4Low.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "480p", format = "mp4"))
        }

        results
    }
}
