package com.streamix.scraper.moviebox

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import org.jsoup.nodes.Document
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VegamoviesScraper @Inject constructor() : BaseScraper() {

    override val name = "Vegamovies"
    override val mainUrl = "https://vegamovies.im" // Updated common domain

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val doc = fetchDoc("$mainUrl/?s=$encoded", mainUrl)
            parseMovieCards(doc)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMovieCards(doc: Document): List<SearchResult> {
        return doc.select("article, div.movie-card, .blog-post").mapNotNull { el ->
            val linkEl = el.selectFirst("a")
            val link = linkEl?.attr("href") ?: return@mapNotNull null
            val title = el.selectFirst("h2, .entry-title, .title")?.text()?.trim() ?: linkEl.attr("title") ?: ""
            val imgEl = el.selectFirst("img")
            val thumb = imgEl?.attr("data-src") ?: imgEl?.attr("src") ?: ""

            SearchResult(
                id = link,
                title = title,
                posterPath = thumb,
                mediaType = "movie"
            )
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return try {
            val html = fetchHtml(mediaId, mainUrl)
            val links = mutableListOf<VideoLink>()
            
            // Vegamovies often has multiple "Download" buttons that lead to intermediate pages.
            // For now, we search for common patterns in the detail page.
            Regex("""https?://[^\s"']+\.(m3u8|mp4|mkv)""").findAll(html).forEach {
                links.add(VideoLink(it.value, "Auto", name))
            }
            
            links.ifEmpty { listOf(VideoLink(mediaId, "Source", name)) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
