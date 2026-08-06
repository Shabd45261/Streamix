package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import android.util.Base64

open class UniversalAdultScraper(
    id: String,
    displayName: String,
    baseUrl: String,
    private val listSelector: String = "div.item, article, li.pcVideoListItem, .thumb-block, .video-item, .box",
    private val titleSelector: String = "a.title, .name, h2, h3, a[title], .video-title",
    private val thumbSelector: String = "img",
    private val linkSelector: String = "a"
) : BaseScraper {

    override val info = ScraperInfo(
        id = id,
        displayName = displayName,
        baseUrl = baseUrl,
        categories = listOf(ContentCategory.ADULT)
    )

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page == 1) info.baseUrl else "${info.baseUrl}/page/$page/"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "${info.baseUrl}/?s=$query&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, info.baseUrl)
            val results = mutableListOf<ScrapeResult>()

            // Cloudflare/JS challenge detection
            if (html.contains("cf-browser-verification") || html.contains("Checking your browser before accessing")) {
                return emptyList()
            }

            doc.select(listSelector).forEach { el ->
                val linkEl = el.select(linkSelector).firstOrNull() ?: return@forEach
                val titleEl = el.select(titleSelector).firstOrNull() ?: linkEl
                val title = titleEl.attr("title").ifEmpty { titleEl.text() }.trim()
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else {
                    if (href.startsWith("/")) "${info.baseUrl}$href" else "${info.baseUrl}/$href"
                }
                
                val img = el.select(thumbSelector).firstOrNull()
                val thumb = img?.attr("data-src") ?: 
                            img?.attr("data-original") ?: 
                            img?.attr("data-lazy-src") ?: 
                            img?.attr("data-srcset")?.split(" ")?.firstOrNull() ?:
                            img?.attr("srcset")?.split(" ")?.firstOrNull() ?:
                            img?.attr("src") ?: ""

                val absoluteThumb = if (thumb.startsWith("//")) {
                    "https:$thumb"
                } else if (thumb.startsWith("/")) {
                    "${info.baseUrl}$thumb"
                } else if (thumb.isNotEmpty() && !thumb.startsWith("http")) {
                    "${info.baseUrl}/$thumb"
                } else {
                    thumb
                }

                results.add(ScrapeResult(
                    id = link, title = title,
                    posterUrl = absoluteThumb.takeIf { it.isNotEmpty() },
                    pageUrl = link, sourceId = info.id
                ))
            }
            results.distinctBy { it.pageUrl }.take(30)
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val results = mutableListOf<StreamResult>()

        // Enhanced regex for video links
        val m3u8Regex = Regex("""(https?://[^\s"']+\.m3u8[^\s"']*)""")
        m3u8Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "hls"))
        }

        val mp4Regex = Regex("""(https?://[^\s"']+\.mp4[^\s"']*)""")
        mp4Regex.findAll(html).forEach { m ->
            results.add(StreamResult(url = m.value, quality = "Auto", format = "mp4"))
        }

        // Check for common obfuscated links (simple base64 check)
        val base64VideoRegex = Regex("""[a-zA-Z0-9+/=]{100,}""")
        base64VideoRegex.findAll(html).forEach { m ->
            try {
                val decoded = Base64.decode(m.value, Base64.DEFAULT).decodeToString()
                if (decoded.contains(".m3u8") || decoded.contains(".mp4")) {
                    val url = if (decoded.startsWith("http")) decoded else "${info.baseUrl}$decoded"
                    results.add(StreamResult(url = url, quality = "Decoded", format = if (decoded.contains(".m3u8")) "hls" else "mp4"))
                }
            } catch (_: Exception) {}
        }

        // Check iframes and source tags
        val doc = Jsoup.parse(html)
        doc.select("iframe[src], source[src], video[src], a[href*=.m3u8], a[href*=.mp4]").forEach { tag ->
            val src = tag.attr("src").ifEmpty { tag.attr("href") }
            if (src.contains(".m3u8") || src.contains(".mp4")) {
                val url = if (src.startsWith("http")) src else {
                    if (src.startsWith("//")) "https:$src" else "${info.baseUrl}$src"
                }
                results.add(StreamResult(url = url, quality = "Source", format = if (url.contains(".m3u8")) "hls" else "mp4"))
            }
        }

        results.distinctBy { it.url }
    }
}
