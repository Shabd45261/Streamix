package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Rule34VideoScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "rule34video",
        displayName = "Rule34Video",
        baseUrl = "https://rule34video.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) {
            "$baseUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=post_date"
        } else {
            "$baseUrl/?mode=async&function=get_block&block_id=custom_list_videos_most_recent_videos&tag_ids&sort_by=post_date&from=${page.toString().padStart(2, '0')}"
        }
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page <= 1) {
            "$baseUrl/search/$query/"
        } else {
            "$baseUrl/search/$query/?mode=async&function=get_block&block_id=custom_list_videos_videos_list_search&q=$query&from_videos=${page.toString().padStart(2, '0')}"
        }
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.item.thumb").forEach { el ->
                val titleEl = el.selectFirst("div.thumb_title") ?: return@forEach
                val title = titleEl.text().trim()
                val linkEl = el.selectFirst("a.th") ?: return@forEach
                val href = linkEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.selectFirst("img")
                val thumb = img?.attr("data-original")?.ifEmpty { img.attr("src") } ?: img?.attr("src") ?: ""

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
        val results = mutableListOf<StreamResult>()

        val videoAltRegex = Regex("""video_alt_url\d*:\s*'([^']+)'""")
        val videoPrimaryRegex = Regex("""video_url:\s*'([^']+)'""")

        val links = videoAltRegex.findAll(html).map { it.groupValues[1] }.toList() +
                    videoPrimaryRegex.findAll(html).map { it.groupValues[1] }.toList()

        links.distinct().forEach { link ->
            val quality = when {
                link.contains("2160p") || link.contains("4k") -> "4K"
                link.contains("1080p") -> "1080p"
                link.contains("720p") -> "720p"
                link.contains("480p") -> "480p"
                link.contains("360") -> "360p"
                else -> "Auto"
            }
            results.add(StreamResult(url = link, quality = quality, format = "mp4", headers = mapOf("Referer" to "$baseUrl/")))
        }

        results
    }
}
