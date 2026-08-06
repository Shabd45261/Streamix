package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MotherlessBackupScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "motherless_backup",
        displayName = "Motherless (Backup)",
        baseUrl = "https://motherless.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = if (page == 1) "$baseUrl/porn/public/videos" else "$baseUrl/porn/public/videos?page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/term/videos/$query?range=0&size=0&sort=relevance&page=$page"
        val html = ScraperHttpClient.get(url) ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("div.thumb-container.video").forEach { el ->
                val titleEl = el.select("a.caption, div.captions a").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim()
                val href = el.select("a").firstOrNull()?.attr("href") ?: ""
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img.static").firstOrNull() ?: el.select("img").firstOrNull()
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
        val results = mutableListOf<StreamResult>()

        val regex = Regex("__fileurl = '([^']*)';", RegexOption.IGNORE_CASE)
        regex.find(html)?.groupValues?.get(1)?.let { url ->
            results.add(StreamResult(url = url, quality = "Auto", format = "mp4"))
        }

        results
    }
}
