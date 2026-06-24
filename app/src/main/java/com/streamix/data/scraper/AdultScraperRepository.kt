package com.streamix.data.scraper

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.adult.AdultMetadataScraper
import com.streamix.scraper.adult.AdultVideoDetail
import com.streamix.scraper.cloudstream.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdultScraperRepository @Inject constructor(
    private val metadataScraper: AdultMetadataScraper
) {
    private val providers = ProviderRegistry.providers

    suspend fun getTrending(page: Int = 1): List<SearchResult> {
        val pornhat = ProviderRegistry.getProviderByName("PornHat") ?: return emptyList()
        return try {
            val response = pornhat.getMainPage(page, pornhat.mainPage.first())
            response?.items?.flatMap { it.list }?.map { it.toSearchResult(pornhat.name) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun search(query: String): List<SearchResult> = coroutineScope {
        providers.map { provider ->
            async {
                try {
                    provider.search(query)?.map { it.toSearchResult(provider.name) } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }.shuffled()
    }

    suspend fun getVideoLinks(url: String): List<VideoLink> {
        val provider = providers.find { url.startsWith(it.mainUrl) || url.contains(it.name, ignoreCase = true) }
            ?: return emptyList()

        val links = mutableListOf<VideoLink>()
        try {
            provider.loadLinks(url, false, {}) { link ->
                links.add(link.toVideoLink())
            }
        } catch (e: Exception) { }
        return links
    }

    suspend fun getDetail(url: String): AdultVideoDetail {
        val provider = providers.find { url.startsWith(it.mainUrl) || url.contains(it.name, ignoreCase = true) }

        return try {
            val loadResponse = provider?.load(url)
            if (loadResponse != null) {
                AdultVideoDetail(
                    pageUrl = url,
                    title = loadResponse.name,
                    posterUrl = loadResponse.posterUrl ?: "",
                    description = loadResponse.plot ?: "",
                    durationSecs = (loadResponse.duration ?: 0) * 60,
                    tags = loadResponse.tags ?: emptyList(),
                    studio = provider.name
                )
            } else {
                metadataScraper.getMetadata(url)
            }
        } catch (e: Exception) {
            metadataScraper.getMetadata(url)
        }
    }

    private fun SearchResponse.toSearchResult(providerName: String): SearchResult {
        return SearchResult(
            id = this.url,
            title = this.name,
            posterPath = this.posterUrl,
            mediaType = "adult",
            rating = this.quality ?: "",
            studio = providerName
        )
    }

    private fun ExtractorLink.toVideoLink(): VideoLink {
        return VideoLink(
            url = this.url,
            quality = if (this.quality > 0) "${this.quality}p" else if (this.isM3u8) "HLS" else "Auto",
            server = this.source,
            isM3u8 = this.isM3u8
        )
    }
}
