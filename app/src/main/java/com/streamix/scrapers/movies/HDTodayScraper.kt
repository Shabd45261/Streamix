package com.streamix.scrapers.movies

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class HDTodayScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "hdtoday",
        displayName = "HDToday",
        baseUrl = "https://hdtoday.tv",
        categories = listOf(ContentCategory.MOVIES, ContentCategory.SHOWS)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get("$baseUrl/home", mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val html = ScraperHttpClient.get("$baseUrl/search/$encoded", mapOf("Referer" to baseUrl))
            ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select(".flw-item").forEach { el ->
                val titleEl = el.select(".film-name a").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img")
                var thumb = img.attr("data-src").ifEmpty { img.attr("src") }
                if (thumb.startsWith("//")) thumb = "https:$thumb"

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

            // Get content ID
            val contentId = doc.select("[data-id]").attr("data-id")
                .ifEmpty { pageUrl.split("-").last() }

            val isEpisode = pageUrl.contains("/tv/") || pageUrl.contains("/episode/")

            // Fetch servers
            val serversUrl = if (isEpisode)
                "$baseUrl/ajax/v2/episode/servers/$contentId"
            else
                "$baseUrl/ajax/v2/movie/servers/$contentId"

            val serversHtml = ScraperHttpClient.get(serversUrl, mapOf(
                "Referer" to pageUrl,
                "X-Requested-With" to "XMLHttpRequest"
            )) ?: return@withContext emptyList()

            val serversDoc = Jsoup.parse(serversHtml)
            val serverId = serversDoc.select(".nav-item a, [data-id]").firstOrNull()?.attr("data-id")
                ?: return@withContext emptyList()

            // Fetch source URL
            val sourcesUrl = if (isEpisode)
                "$baseUrl/ajax/v2/episode/sources/$serverId"
            else
                "$baseUrl/ajax/v2/movie/sources/$serverId"

            val sourcesResponse = ScraperHttpClient.get(sourcesUrl, mapOf(
                "Referer" to pageUrl,
                "X-Requested-With" to "XMLHttpRequest"
            )) ?: return@withContext emptyList()

            val json = org.json.JSONObject(sourcesResponse)
            val embedUrl = json.optString("link")

            if (embedUrl.isEmpty()) return@withContext emptyList()

            // Try to extract m3u8 from embed
            extractFromEmbed(embedUrl, pageUrl)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun extractFromEmbed(embedUrl: String, referer: String): List<StreamResult> {
        val results = mutableListOf<StreamResult>()
        try {
            val html = ScraperHttpClient.get(embedUrl, mapOf("Referer" to referer)) ?: return results

            // Try to find m3u8 directly
            Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""").find(html)?.groupValues?.get(1)?.let { url ->
                results.add(StreamResult(url = url, quality = "Auto", format = "hls"))
            }

            // Try sources JSON
            val sourcesRegex = Regex("""sources\s*:\s*\[([^\]]+)\]""")
            sourcesRegex.find(html)?.groupValues?.get(1)?.let { sourcesStr ->
                Regex(""""file"\s*:\s*"([^"]+)"""").findAll(sourcesStr).forEach { m ->
                    val url = m.groupValues[1].replace("\\/", "/")
                    val format = if (url.contains(".m3u8")) "hls" else "mp4"
                    results.add(StreamResult(url = url, quality = "Auto", format = format))
                }
            }
        } catch (_: Exception) {}
        return results
    }
}
