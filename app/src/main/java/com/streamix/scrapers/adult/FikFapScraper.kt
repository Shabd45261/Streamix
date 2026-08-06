package com.streamix.scrapers.adult

import android.util.Log
import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class FikFapScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "fikfap",
        displayName = "FikFap",
        baseUrl = "https://fikfap.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page > 1) "$baseUrl/trending/page/$page/" else "$baseUrl/trending/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        val url = if (page > 1) "$baseUrl/search/$encoded/page/$page/" else "$baseUrl/search/$encoded/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.item, article.post, div.post-item").forEach { el ->
                val linkEl = el.select("a").firstOrNull() ?: return@forEach
                val href = linkEl.attr("href")
                if (href.isEmpty() || !href.contains("/v/")) return@forEach

                val title = el.select("div.title, .post-title, h2").text().ifEmpty { linkEl.attr("title") }.ifEmpty { "FikFap Video" }.trim()
                val link = if (href.startsWith("http")) href else "$baseUrl${if (href.startsWith("/")) "" else "/"}$href"
                
                // Enhanced thumbnail extraction
                val img = el.select("img").firstOrNull()
                val thumb = img?.attr("data-src") ?: img?.attr("src") ?: img?.attr("data-original") ?: ""
                
                // Duration and other metadata
                val duration = el.select("span.duration, .time").text()

                results.add(ScrapeResult(
                    id = link, title = title,
                    posterUrl = thumb.takeIf { it.isNotEmpty() }?.let { 
                        if (it.startsWith("//")) "https:$it" 
                        else if (it.startsWith("/")) "$baseUrl$it"
                        else if (!it.startsWith("http")) "$baseUrl/$it"
                        else it 
                    },
                    pageUrl = link, sourceId = info.id,
                    duration = duration
                ))
            }
            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { 
            Log.e("FikFapScraper", "Parse list failed", e)
            emptyList() 
        }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val doc = Jsoup.parse(html)
        val results = mutableListOf<StreamResult>()

        try {
            // Check for direct video tags
            doc.select("video source, video").forEach { video ->
                val src = video.attr("src").ifEmpty { video.attr("data-src") }
                if (src.isNotEmpty()) {
                    val finalUrl = if (src.startsWith("http")) src else {
                        if (src.startsWith("//")) "https:$src" else "$baseUrl${if (src.startsWith("/")) "" else "/"}$src"
                    }
                    val format = if (finalUrl.contains(".m3u8")) "hls" else "mp4"
                    results.add(StreamResult(url = finalUrl, quality = "Auto", format = format))
                }
            }

            // Check for iframe players
            if (results.isEmpty()) {
                doc.select("iframe[src*='player'], iframe[src*='video']").forEach { iframe ->
                    val src = iframe.attr("src")
                    if (src.isNotEmpty()) {
                        val finalUrl = if (src.startsWith("http")) src else {
                            if (src.startsWith("//")) "https:$src" else "$baseUrl${if (src.startsWith("/")) "" else "/"}$src"
                        }
                        results.add(StreamResult(url = finalUrl, quality = "External", format = "mp4"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FikFapScraper", "Get streams failed", e)
        }

        results
    }
}