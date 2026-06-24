package com.streamix.scraper.moviebox

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieboxInProvider @Inject constructor() : BaseScraper() {
    override val name = "Moviebox IN"
    override val mainUrl = "https://moviebox.in"

    companion object {
        private const val CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    override val client = super.client.newBuilder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", CHROME_UA)
                .header("Referer", mainUrl)
                .build()
            chain.proceed(req)
        }
        .build()

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val html = get("$mainUrl/search?query=${query.replace(" ", "+")}")
            val doc = Jsoup.parse(html)
            doc.select(".search-result, .movie-card").mapNotNull { el ->
                val title = el.select("h2, .title, .name").text().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val href  = el.select("a").first()?.attr("href") ?: return@mapNotNull null
                val img   = el.select("img").attr("src").orEmpty()
                SearchResult(
                    id = href,
                    title = title,
                    posterPath = img,
                    mediaType = if (href.contains("series")) "tv" else "movie",
                    year = el.select(".year, .release").text()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return try {
            val baseUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
            val sources = mutableListOf<VideoLink>()

            val videoId = mediaId.substringAfterLast("/")
            val apiResponse = get("$mainUrl/api/sources/$videoId")

            if (apiResponse.isNotEmpty()) {
                val json = Gson().fromJson(apiResponse, JsonObject::class.java)
                json.getAsJsonArray("sources")?.forEach { src ->
                    val obj = src.asJsonObject
                    val url = obj.get("file")?.asString ?: return@forEach
                    sources.add(VideoLink(url = url, quality = obj.get("label")?.asString ?: "Auto", server = name))
                }
            }

            if (sources.isEmpty()) {
                val html = get(baseUrl)
                val doc = Jsoup.parse(html)
                doc.select("iframe[src]").forEach { iframe ->
                    val src = iframe.attr("src")
                    if (src.contains(".m3u8") || src.contains(".mp4")) {
                        sources.add(VideoLink(url = src, quality = "Auto", server = name))
                    }
                }
            }

            sources
        } catch (e: Exception) {
            emptyList()
        }
    }
}
