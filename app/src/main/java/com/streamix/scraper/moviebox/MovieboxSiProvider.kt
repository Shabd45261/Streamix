package com.streamix.scraper.moviebox

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import org.jsoup.nodes.Document
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieboxSiProvider @Inject constructor() : BaseScraper() {

    override val name = "Moviebox"
    override val mainUrl = "https://moviebox.si"

    override val client = super.client.newBuilder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", mainUrl)
                .build()
            chain.proceed(req)
        }
        .build()

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val doc = fetchDoc("$mainUrl/search/?q=$encoded")
            parseMovieCards(doc)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMovieCards(doc: Document): List<SearchResult> {
        return doc.select("div.movie-card, article, .post-item, .film-card").mapNotNull { el ->
            val linkEl = el.selectFirst("a")
            val link = linkEl?.attr("href") ?: return@mapNotNull null
            val fullUrl = if (link.startsWith("http")) link else if (link.startsWith("/")) "$mainUrl$link" else "$mainUrl/$link"
            
            val title = el.selectFirst("h3, .title, .entry-title")?.text()?.trim() ?: linkEl.attr("title") ?: ""
            if (title.isEmpty()) return@mapNotNull null
            
            val imgEl = el.selectFirst("img")
            val thumb = imgEl?.attr("data-src") ?: imgEl?.attr("src") ?: ""

            SearchResult(
                id = fullUrl,
                title = title,
                posterPath = if (thumb.startsWith("//")) "https:$thumb" else if (thumb.startsWith("http")) thumb else if (thumb.startsWith("/")) "$mainUrl$thumb" else thumb,
                mediaType = if (fullUrl.contains("series") || fullUrl.contains("tv")) "tv" else "movie",
                year = el.selectFirst(".year, .entry-date")?.text() ?: ""
            )
        }.distinctBy { it.id }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return try {
            val pageUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
            val html = fetchHtml(pageUrl, mainUrl)

            val links = mutableListOf<VideoLink>()

            // Extract HLS
            Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").findAll(html).forEach {
                links.add(VideoLink(it.groupValues[1], "HLS", name, true))
            }

            // Extract MP4
            Regex("""["'](https?://[^"']+\.mp4[^"']*)["']""").findAll(html).forEach {
                links.add(VideoLink(it.groupValues[1], "MP4", name, false))
            }
            
            links.ifEmpty { listOf(VideoLink(pageUrl, "Auto", name)) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
