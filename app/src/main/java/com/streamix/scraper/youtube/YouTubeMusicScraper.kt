package com.streamix.scraper.youtube

import com.streamix.core.model.MusicTrack
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicScraper @Inject constructor() : BaseScraper() {

    override val name = "YouTube Music"
    override val mainUrl = "https://music.youtube.com"

    override suspend fun search(query: String): List<SearchResult> {
        return emptyList()
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return emptyList()
    }

    suspend fun getTrendingSongs(): List<MusicTrack> {
        return try {
            val doc = fetchDoc("$mainUrl/explore")
            parseMusicCards(doc)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMusicCards(doc: Document): List<MusicTrack> {
        return doc.select("ytmusic-card-renderer").mapNotNull { el ->
            MusicTrack(
                id = el.attr("data-video-id") ?: "",
                title = el.selectFirst(".title")?.text() ?: "",
                artist = el.selectFirst(".subtitle")?.text() ?: "",
                thumbnailUrl = el.selectFirst("img")?.attr("src") ?: "",
                audioUrl = "" // Use YouTube video URL for playback
            )
        }
    }
}
