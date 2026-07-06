package com.streamix.scraper.adult

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkxxxScraper @Inject constructor() : BaseScraper() {

    override val name = "OkXXX"
    override val mainUrl = "https://ok.xxx"

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val path = if (query.isEmpty()) "/trending/1/" else "/search/${URLEncoder.encode(query, "UTF-8")}/1/"
            val doc = fetchDoc("$mainUrl$path")
            
            doc.select("div.item, div.video-item").mapNotNull { el ->
                val aTag = el.selectFirst("a") ?: return@mapNotNull null
                val title = (aTag.attr("title").ifEmpty { el.selectFirst(".title, .name")?.text() })?.trim() ?: ""
                if (title.isEmpty()) return@mapNotNull null
                
                val href = aTag.attr("href")
                val fullUrl = if (href.startsWith("http")) href else "$mainUrl$href"
                
                val img = el.selectFirst("img")
                val posterUrl = img?.attr("data-src") ?: img?.attr("data-original") ?: img?.attr("src") ?: ""

                SearchResult(
                    id = fullUrl,
                    title = title,
                    posterPath = posterUrl,
                    mediaType = "adult",
                    duration = el.selectFirst(".duration, .time")?.text()?.trim() ?: "",
                    views = el.selectFirst(".views, .metadata")?.text()?.trim() ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return try {
            val pageUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
            val doc = fetchDoc(pageUrl, mainUrl)
            
            val links = mutableListOf<VideoLink>()
            
            // Try to find script containing video data
            val html = doc.html()
            
            // Look for common patterns in adult site scripts (flashvars, video_url, etc)
            Regex("""(?:video_url|file|url|src)\s*[:=]\s*["'](https?[:\\/][^"']+)["']""", RegexOption.IGNORE_CASE)
                .findAll(html).forEach { match ->
                    val url = match.groupValues[1].replace("\\/", "/")
                    if (url.contains(".m3u8") || url.contains(".mp4")) {
                        links.add(VideoLink(url, if (url.contains(".m3u8")) "HLS" else "MP4", name, url.contains(".m3u8")))
                    }
                }

            val source = doc.selectFirst("video source, video#my-video source")
            val initialUrl = source?.attr("src") ?: ""

            if (initialUrl.isNotEmpty()) {
                val redirectedM3u = fetchHtml(initialUrl, pageUrl)
                
                val playlist = redirectedM3u
                if (playlist.contains("#EXTM3U")) {
                    val regex = Regex("""#EXT-X-STREAM-INF:.*RESOLUTION=\d+x(\d+).*?\n(https[^\n]+)""")
                    regex.findAll(playlist).forEach {
                        val quality = it.groupValues[1] + "p"
                        val streamUrl = it.groupValues[2]
                        links.add(VideoLink(streamUrl, quality, name, true))
                    }
                }
                
                if (links.isEmpty()) {
                    links.add(VideoLink(initialUrl, "Auto", name, initialUrl.contains(".m3u8")))
                }
            }
            
            if (links.isEmpty()) {
                val html = doc.html()
                // Improved regex for various stream formats
                Regex("""(https?:\\?/\\?/[^"'\s<>]+?\.(?:m3u8|mp4|webm|mkv)[^"'\s<>]*)""", RegexOption.IGNORE_CASE)
                    .findAll(html).forEach { 
                        val url = it.groupValues[1].replace("\\/", "/")
                        if (!url.contains("google.com") && !url.contains("facebook.com")) {
                            links.add(VideoLink(url, if (url.contains(".m3u8")) "HLS" else "Direct", name, url.contains(".m3u8")))
                        }
                    }
            }

            links.distinctBy { it.url }.sortedByDescending { it.isM3u8 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun loadDetail(url: String): AdultVideoDetail {
        return try {
            val doc = fetchDoc(url, mainUrl)
            val title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: "OkXXX Video"
            val poster = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""
            
            val description = doc.select("div.desc")
                .map { it.text().trim() }
                .firstOrNull { it.startsWith("Description:", ignoreCase = true) }
                ?.removePrefix("Description:")?.trim() ?: ""

            AdultVideoDetail(
                pageUrl = url,
                title = title,
                posterUrl = poster,
                description = description,
                views = doc.selectFirst(".views")?.text()?.trim() ?: "",
                rating = doc.selectFirst(".percent")?.text()?.trim() ?: ""
            )
        } catch (e: Exception) {
            AdultVideoDetail(url, "OkXXX Video")
        }
    }
}
