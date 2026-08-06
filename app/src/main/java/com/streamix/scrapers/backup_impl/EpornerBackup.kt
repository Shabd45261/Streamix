package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject
import java.math.BigInteger

class EpornerBackupScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "eporner_backup",
        displayName = "Eporner (Backup)",
        baseUrl = "https://www.eporner.com",
        categories = listOf(ContentCategory.ADULT)
    )

    private val baseUrl get() = info.baseUrl

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val html = ScraperHttpClient.get("$baseUrl/most-viewed/$page") ?: return@withContext emptyList()
        parseList(html)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val subquery = query.replace(" ", "-")
        val html = ScraperHttpClient.get("$baseUrl/search/$subquery/$page") ?: return@withContext emptyList()
        parseList(html)
    }

    private fun parseList(html: String): List<ScrapeResult> {
        return try {
            val doc = Jsoup.parse(html, baseUrl)
            val results = mutableListOf<ScrapeResult>()

            doc.select("#div-search-results div.mb").forEach { el ->
                val titleEl = el.select("div.mbunder p.mbtit a").firstOrNull() ?: return@forEach
                val title = titleEl.text().trim()
                val href = titleEl.attr("href")
                if (title.isEmpty() || href.isEmpty()) return@forEach

                val link = if (href.startsWith("http")) href else "$baseUrl$href"
                val img = el.select("img").firstOrNull()
                val thumb = img?.attr("data-src")?.ifEmpty { img.attr("src") } ?: img?.attr("src") ?: ""

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
        val doc = ScraperHttpClient.get(pageUrl) ?: return@withContext emptyList()
        val vid = Regex("EP\\.video\\.player\\.vid = '([^']+)'").find(doc)?.groupValues?.get(1) ?: return@withContext emptyList()
        val hash = Regex("EP\\.video\\.player\\.hash = '([^']+)'").find(doc)?.groupValues?.get(1) ?: return@withContext emptyList()
        
        val apiUrl = "https://www.eporner.com/xhr/video/$vid?hash=${base36(hash)}"
        val json = ScraperHttpClient.get(apiUrl) ?: return@withContext emptyList()
        
        val results = mutableListOf<StreamResult>()
        try {
            val jsonObject = JSONObject(json)
            val sources = jsonObject.getJSONObject("sources")
            val mp4Sources = sources.getJSONObject("mp4")
            
            mp4Sources.keys().forEach { quality ->
                val sourceObject = mp4Sources.getJSONObject(quality)
                val src = sourceObject.getString("src")
                val label = sourceObject.optString("labelShort", quality)
                results.add(StreamResult(url = src, quality = label, format = "mp4"))
            }
        } catch (_: Exception) {}
        
        results
    }

    private fun base36(hash: String): String {
        return try {
            if (hash.length >= 32) {
                val part1 = BigInteger(hash.substring(0, 8), 16).toString(36)
                val part2 = BigInteger(hash.substring(8, 16), 16).toString(36)
                val part3 = BigInteger(hash.substring(16, 24), 16).toString(36)
                val part4 = BigInteger(hash.substring(24, 32), 16).toString(36)
                part1 + part2 + part3 + part4
            } else ""
        } catch (_: Exception) { "" }
    }
}
