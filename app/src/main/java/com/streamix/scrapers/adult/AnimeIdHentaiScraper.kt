package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class AnimeIdHentaiScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "animeidhentai",
        displayName = "AnimeIdHentai",
        baseUrl = "https://animeidhentai.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page == 1) "$baseUrl/trending" else "$baseUrl/trending/page/$page"
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

            doc.select("article.anime.poster.por").forEach { el ->
                val titleEl = el.selectFirst("header > div.ttl") ?: return@forEach
                val title = titleEl.text().trim()
                val linkEl = el.selectFirst("a.lnk-blk") ?: return@forEach
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img")
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

        val iframe = doc.selectFirst("div.embed > iframe")?.attr("src") ?: ""
        if (iframe.isNotEmpty()) {
            val iframeHtml = ScraperHttpClient.get(iframe, mapOf("Referer" to pageUrl)) ?: ""
            val serverId = Jsoup.parse(iframeHtml).selectFirst("div.servers li")?.attr("data-id") ?: ""
            
            if (serverId.isNotEmpty()) {
                val playerUrl = "https://nhplayer.com/$serverId"
                val playerHtml = ScraperHttpClient.get(playerUrl, mapOf("Referer" to iframe)) ?: ""
                val videoUrl = Regex("""file:\s*"([^"]+)"""").find(playerHtml)?.groupValues?.get(1) ?: ""
                
                if (videoUrl.isNotEmpty()) {
                    results.add(StreamResult(url = videoUrl, quality = "Auto", format = "mp4", headers = mapOf("Referer" to playerUrl)))
                }
            }
        }
        
        results
    }
}
