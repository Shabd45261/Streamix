package com.streamix.scraper.adult

import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import com.streamix.core.model.SearchResult
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdultMetadataScraper @Inject constructor() : BaseScraper() {
    override val name = "MetadataProvider"
    override val mainUrl = ""

    override suspend fun search(query: String): List<SearchResult> = emptyList()
    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> = emptyList()

    suspend fun getMetadata(url: String): AdultVideoDetail {
        return try {
            val domain = extractDomain(url)
            val doc = fetchDoc(url, domain)
            
            val title = doc.selectFirst("meta[property='og:title']")?.attr("content")
                        ?: doc.selectFirst("h1, .video-title, .title")?.text()?.trim()
                        ?: "Video"
            
            val description = doc.selectFirst("meta[property='og:description']")?.attr("content")
                              ?: doc.selectFirst(".description, #video-description-text, .video-description, .post-content")?.text()?.trim()
                              ?: ""
            
            var poster = doc.selectFirst("meta[property='og:image']")?.attr("content")
                         ?: doc.selectFirst("video")?.attr("poster")
                         ?: doc.selectFirst("meta[name='twitter:image']")?.attr("content") ?: ""
            
            if (poster.startsWith("//")) poster = "https:$poster"
            else if (poster.startsWith("/") && domain.isNotEmpty()) poster = "$domain$poster"

            AdultVideoDetail(
                pageUrl = url,
                title = title,
                posterUrl = poster,
                description = cleanDescription(description),
                views = doc.selectFirst(".views, .video-views, .views-count")?.text()?.trim() ?: "",
                rating = doc.selectFirst(".percent, .rating, .video-rating")?.text()?.trim() ?: ""
            )
        } catch (e: Exception) {
            AdultVideoDetail(url, "Video Detail")
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) { "" }
    }

    private fun cleanDescription(desc: String): String {
        val lowercase = desc.lowercase()
        if (lowercase.contains("cookie") || lowercase.contains("optimize site") || lowercase.contains("privacy policy")) {
            return ""
        }
        return desc
    }
}
