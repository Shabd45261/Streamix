package com.streamix.scraper.youtube

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import android.util.Log

@Singleton
class YouTubeScraper @Inject constructor() : BaseScraper() {

    override val name = "YouTube"
    override val mainUrl = "https://www.youtube.com"
    private val service = ServiceList.YouTube
    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val searchInfo = SearchInfo.getInfo(service, service.searchQHFactory.fromQuery(query))
            searchInfo.relatedItems.mapNotNull { item ->
                when (item) {
                    is StreamInfoItem -> item.toSearchResult()
                    is ChannelInfoItem -> item.toSearchResult()
                    is PlaylistInfoItem -> item.toSearchResult()
                    else -> null
                }
            }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Search failed for query: $query", e)
            emptyList()
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> = withContext(Dispatchers.IO) {
        try {
            val cleanId = when {
                mediaId.contains("v=") -> mediaId.substringAfter("v=").substringBefore("&")
                mediaId.contains("/shorts/") -> mediaId.substringAfter("/shorts/").substringBefore("?")
                mediaId.startsWith("http") -> mediaId.substringAfterLast("/")
                else -> mediaId
            }

            // Prioritize /shorts/ URL for speed if it's likely a short
            val urlsToTry = listOf(
                "https://www.youtube.com/shorts/$cleanId",
                "https://www.youtube.com/watch?v=$cleanId"
            )

            var streamInfo: StreamInfo? = null
            var lastError: Exception? = null

            for (url in urlsToTry) {
                try {
                    streamInfo = StreamInfo.getInfo(service, url)
                    if (streamInfo != null) break
                } catch (e: Exception) {
                    lastError = e
                    Log.w("YouTubeScraper", "Failed to get info for $url: ${e.message}")
                }
            }

            if (streamInfo == null) throw lastError ?: Exception("Could not get stream info")

            getVideoLinksFromInfo(streamInfo)
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get video links for: $mediaId", e)
            emptyList()
        }
    }

    fun getVideoLinksFromInfo(streamInfo: StreamInfo): List<VideoLink> {
        val links = mutableListOf<VideoLink>()
        
        // 1. DASH manifest - Adaptive (Auto) - HIGH PRIORITY for full resolution control (1080p, 4K etc)
        val dashMpdUrl = streamInfo.dashMpdUrl
        if (!dashMpdUrl.isNullOrBlank()) {
            links.add(VideoLink(dashMpdUrl, "Auto (DASH)", name, false))
        }

        // 2. HLS stream - Live / Fallback
        val hlsUrl = streamInfo.hlsUrl
        if (!hlsUrl.isNullOrBlank()) {
            links.add(VideoLink(hlsUrl, "Auto (HLS)", name, true))
        }

        // 3. Combined video + audio streams (Specific resolutions) - HIGHEST PRIORITY for Shorts or offline
        streamInfo.videoStreams?.forEach { stream ->
            val streamUrl = stream.url
            if (!streamUrl.isNullOrBlank()) {
                val label = stream.resolution ?: "Auto"
                links.add(VideoLink(streamUrl, label, name, streamUrl.contains(".m3u8")))
            }
        }
        
        // 4. Fallback to video-only if needed
        if (links.isEmpty()) {
            streamInfo.videoOnlyStreams?.forEach { stream ->
                val streamUrl = stream.url
                if (!streamUrl.isNullOrBlank()) {
                    links.add(VideoLink(streamUrl, (stream.resolution ?: "Video") + " (No Audio)", name, false))
                }
            }
        }

        return links.distinctBy { it.quality }
    }

    suspend fun getTrending(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/feed/trending"
            val kioskInfo = KioskInfo.getInfo(service, url)
            kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get trending", e)
            emptyList()
        }
    }

    suspend fun getHomeFeed(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // "Home" is the main YouTube URL, personalized with cookies
            // Try desktop first, then mobile if it fails or returns less items
            val desktopUrl = "https://www.youtube.com/?app=desktop"
            val kioskInfo = KioskInfo.getInfo(service, desktopUrl)
            val desktopItems = kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() }
            
            if (desktopItems.size > 10) return@withContext desktopItems

            val mobileUrl = "https://m.youtube.com/"
            val mobileKiosk = KioskInfo.getInfo(service, mobileUrl)
            val mobileItems = mobileKiosk.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() }
            
            (desktopItems + mobileItems).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get home feed", e)
            getTrending()
        }
    }

    suspend fun getJustAdded(): List<SearchResult> = withContext(Dispatchers.IO) {
        search("latest videos")
    }

    suspend fun getRecommended(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // First try home feed for recommendations
            val home = getHomeFeed()
            if (home.isNotEmpty()) return@withContext home
            
            // Fallback to subscription feed
            val subs = getSubscriptionFeed()
            if (subs.isNotEmpty()) return@withContext subs
            
            search("recommended videos")
        } catch (e: Exception) {
            search("recommended videos")
        }
    }

    suspend fun getSubscriptionFeed(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/feed/subscriptions"
            val kioskInfo = KioskInfo.getInfo(service, url)
            kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get subscription feed", e)
            emptyList()
        }
    }

    suspend fun getShortsFeed(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/shorts"
            val kioskInfo = KioskInfo.getInfo(service, url)
            val allShorts = mutableListOf<SearchResult>()
            
            allShorts.addAll(kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() })
            
            var nextPage = kioskInfo.nextPage
            var pagesFetched = 1
            // Fetch fewer pages initially for speed, user can load more via infinite scroll
            while (pagesFetched < 3 && nextPage != null) {
                try {
                    val moreItems = KioskInfo.getMoreItems(service, url, nextPage)
                    allShorts.addAll(moreItems.items.filterIsInstance<StreamInfoItem>().map { it.toSearchResult() })
                    nextPage = moreItems.nextPage
                    pagesFetched++
                } catch (e: Exception) { break }
                if (allShorts.distinctBy { it.id }.size >= 60) break
            }

            // Mix in some home items that look like shorts (personalized)
            val homeShorts = getHomeFeed().filter { 
                val parts = it.duration.split(":")
                it.duration.isEmpty() || parts.size < 2 || (parts.size == 2 && (parts[0].toIntOrNull() ?: 0) < 2)
            }
            
            (allShorts + homeShorts).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get shorts feed", e)
            searchShorts("")
        }
    }

    suspend fun getChannelVideos(channelUrl: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val channelInfo = ChannelInfo.getInfo(service, channelUrl)
            
            // YouTube channel videos are now in tabs (Videos, Shorts, etc.)
            // We try to find the "Videos" tab first.
            val videosTab = channelInfo.tabs.firstOrNull { 
                it.contentFilters?.any { filter -> filter.equals("videos", ignoreCase = true) } == true
            } ?: channelInfo.tabs.firstOrNull()

            if (videosTab != null) {
                val tabInfo = org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo.getInfo(service, videosTab)
                tabInfo.relatedItems.mapNotNull { 
                    when (it) {
                        is StreamInfoItem -> it.toSearchResult()
                        is PlaylistInfoItem -> it.toSearchResult()
                        else -> null
                    }
                }
            } else {
                // Fallback to search if no tabs found
                search(channelInfo.name)
            }
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get channel videos for: $channelUrl", e)
            // Final fallback: try to search the URL/ID directly
            val query = when {
                channelUrl.contains("/@") -> channelUrl.substringAfterLast("/@")
                channelUrl.contains("/channel/") -> channelUrl.substringAfterLast("/")
                else -> channelUrl.substringAfterLast("/")
            }
            search(query)
        }
    }

    suspend fun searchShortsIncremental(
        query: String, 
        maxPagesPerVariation: Int = 12,
        targetTotal: Int = 150,
        onResults: (List<SearchResult>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val variations = if (maxPagesPerVariation <= 2) listOf(query) else listOf(query, "$query shorts", "$query #shorts", "shorts $query", "#shorts $query", "youtube shorts $query")
        val collectedCount = java.util.concurrent.atomic.AtomicInteger(0)
        
        coroutineScope {
            variations.forEach { q ->
                launch {
                    try {
                        if (collectedCount.get() >= targetTotal) return@launch
                        
                        val qh = service.searchQHFactory.fromQuery(q)
                        val searchInfo = SearchInfo.getInfo(service, qh)
                        
                        val initialShorts = searchInfo.relatedItems.filterIsInstance<StreamInfoItem>()
                            .filter { item ->
                                item.duration <= 90 || item.url.contains("/shorts/", ignoreCase = true) || item.duration <= 0
                            }
                            .map { it.toSearchResult() }
                        
                        if (initialShorts.isNotEmpty()) {
                            collectedCount.addAndGet(initialShorts.size)
                            onResults(initialShorts)
                        }

                        var nextPage = searchInfo.nextPage
                        var pagesFetched = 1
                        while (pagesFetched < maxPagesPerVariation && nextPage != null && collectedCount.get() < targetTotal) { 
                            try {
                                val moreItems = SearchInfo.getMoreItems(service, qh, nextPage)
                                val nextShorts = moreItems.items.filterIsInstance<StreamInfoItem>()
                                    .filter { item ->
                                        item.duration <= 90 || item.url.contains("/shorts/", ignoreCase = true) || item.duration <= 0
                                    }
                                    .map { it.toSearchResult() }
                                
                                if (nextShorts.isNotEmpty()) {
                                    collectedCount.addAndGet(nextShorts.size)
                                    onResults(nextShorts)
                                }
                                nextPage = moreItems.nextPage
                                pagesFetched++
                            } catch (e: Exception) { break }
                        }
                    } catch (e: Exception) {
                        Log.w("YouTubeScraper", "Variation search failed for $q")
                    }
                }
            }
        }
    }

    suspend fun searchShorts(query: String, limit: Int = 150): List<SearchResult> = withContext(Dispatchers.IO) {
        val allShorts = mutableListOf<SearchResult>()
        val maxPages = if (limit <= 20) 2 else 12
        searchShortsIncremental(query, maxPagesPerVariation = maxPages, targetTotal = limit) { 
            synchronized(allShorts) { allShorts.addAll(it) } 
        }
        allShorts.distinctBy { it.id }
    }

    suspend fun getFullStreamInfo(mediaId: String): StreamInfo? = withContext(Dispatchers.IO) {
        try {
            val url = if (mediaId.startsWith("http")) mediaId else "https://www.youtube.com/watch?v=$mediaId"
            StreamInfo.getInfo(service, url)
        } catch (e: Exception) {
            Log.e("YouTubeScraper", "Failed to get full stream info for: $mediaId", e)
            null
        }
    }

    fun StreamInfoItem.toSearchResult(): SearchResult {
        val videoId = try {
            val handler = service.getStreamLHFactory().fromUrl(url)
            handler.id
        } catch (e: Exception) {
            when {
                url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?")
                url.contains("youtu.be/") -> url.substringAfterLast("/")
                url.startsWith("/") && url.contains("watch?v=") -> url.substringAfter("v=").substringBefore("&")
                else -> url.substringAfterLast("/")
            }
        }.trim().ifEmpty { 
            url.trim()
        }
        
        val formattedDuration = if (duration > 0) {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
            else "%d:%02d".format(minutes, seconds)
        } else ""

        return SearchResult(
            id = videoId,
            title = name,
            posterPath = thumbnails?.lastOrNull()?.getUrl() ?: thumbnails?.firstOrNull()?.getUrl(),
            mediaType = "youtube",
            duration = formattedDuration,
            views = viewCount.toString(),
            studio = uploaderName
        )
    }

    fun ChannelInfoItem.toSearchResult(): SearchResult {
        return SearchResult(
            id = url, // Use URL for channel navigation
            title = name,
            posterPath = thumbnails?.lastOrNull()?.getUrl() ?: thumbnails?.firstOrNull()?.getUrl(),
            mediaType = "youtube_channel",
            duration = "",
            views = "$subscriberCount subscribers • $streamCount videos",
            studio = ""
        )
    }

    fun PlaylistInfoItem.toSearchResult(): SearchResult {
        return SearchResult(
            id = url,
            title = name,
            posterPath = thumbnails?.lastOrNull()?.getUrl() ?: thumbnails?.firstOrNull()?.getUrl(),
            mediaType = "youtube_playlist",
            duration = streamCount.toString(), // Use duration field for video count
            views = uploaderName ?: "",
            studio = ""
        )
    }
}
