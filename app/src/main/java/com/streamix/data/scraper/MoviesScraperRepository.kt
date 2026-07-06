package com.streamix.data.scraper

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.*
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

data class MovieHomeRow(
    val name: String,
    val items: List<SearchResult>
)

@Singleton
class MoviesScraperRepository @Inject constructor() {

    private val providerPriority = listOf("MovieBoxPh", "MovieBoxIN")

    suspend fun getHomeRowsForProvider(name: String): List<MovieHomeRow> = coroutineScope {
        val provider = ProviderRegistry.getProviderByName(name) ?: return@coroutineScope emptyList()
        try {
            Log.d("MoviesRepo", "Fetching home data rows for $name...")
            val resp = provider.getMainPage(1, MainPageRequest("Home", "1"))
            return@coroutineScope resp?.items?.map { homeList ->
                MovieHomeRow(
                    name = homeList.name,
                    items = homeList.list.map { it.toSearchResult(provider.name) }
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("MoviesRepo", "$name top level failed", e)
            emptyList()
        }
    }

    suspend fun getHomeRows(): List<MovieHomeRow> = getHomeRowsForProvider("MovieBoxPh")

    suspend fun search(query: String): List<SearchResult> = coroutineScope {
        Log.d("MoviesRepo", "Searching for '$query' with fallback logic")
        
        for (name in providerPriority) {
            val provider = ProviderRegistry.getProviderByName(name) ?: continue
            try {
                val results = provider.search(query, 1)
                val items = results?.items?.map { it.toSearchResult(provider.name) } ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d("MoviesRepo", "Found ${items.size} results from $name")
                    return@coroutineScope items
                }
            } catch (e: Exception) {
                Log.e("MoviesRepo", "$name search error", e)
            }
        }
        emptyList()
    }

    suspend fun getDetail(url: String, apiName: String): LoadResponse? {
        val provider = ProviderRegistry.getProviderByName(apiName) 
            ?: ProviderRegistry.getProviderByName("MovieBoxPh") 
            ?: return null
            
        return try {
            provider.load(url)
        } catch (e: Exception) {
            Log.e("MoviesRepo", "Error loading detail for $url from $apiName", e)
            null
        }
    }

    suspend fun getVideoLinks(data: String, apiName: String, title: String? = null): List<VideoLink> = coroutineScope {
        val links = mutableListOf<VideoLink>()
        
        val primaryProvider = ProviderRegistry.getProviderByName(apiName)
        if (primaryProvider != null) {
            try {
                primaryProvider.loadLinks(data, false, {}) { link ->
                    links.add(VideoLink(
                        url = link.url,
                        quality = if (link.quality > 0) "${link.quality}p" else "Auto",
                        server = link.source,
                        isM3u8 = link.isM3u8
                    ))
                }
            } catch (e: Exception) {
                Log.e("MoviesRepo", "Error loading primary links from $apiName", e)
            }
        }

        if (links.isEmpty() && title != null) {
            val normTitle = normalizeTitle(title)
            for (name in providerPriority) {
                if (name == apiName) continue
                val provider = ProviderRegistry.getProviderByName(name) ?: continue
                try {
                    val results = provider.search(title, 1)
                    val match = results?.items?.find { normalizeTitle(it.name) == normTitle }
                    if (match != null) {
                        val detail = provider.load(match.url)
                        if (detail != null) {
                            provider.loadLinks(detail.dataUrl, false, {}) { link ->
                                links.add(VideoLink(
                                    url = link.url,
                                    quality = if (link.quality > 0) "${link.quality}p" else "Auto",
                                    server = link.source,
                                    isM3u8 = link.isM3u8
                                ))
                            }
                            if (links.isNotEmpty()) break
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        links.sortedByDescending { 
            val q = it.quality.substringBefore("p").toIntOrNull() ?: 0
            q
        }
    }

    private fun SearchResponse.toSearchResult(providerName: String): SearchResult {
        val q = this.quality ?: ""
        val isYear = q.matches(Regex("\\d{4}"))
        return SearchResult(
            id = this.url,
            title = this.name,
            posterPath = this.posterUrl,
            mediaType = if (this.type == TvType.TvSeries) "tv" else "movie",
            rating = if (isYear) "" else q,
            studio = providerName,
            year = if (isYear) q else "",
            duration = if (q.contains(":")) q else ""
        )
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase().replace(Regex("[^a-z0-9]"), "")
            .replace("watchonline", "")
            .replace("fullmovie", "")
            .replace("download", "")
            .replace("hindi", "")
            .replace("english", "")
            .replace("dubbed", "")
            .replace("series", "")
    }

    suspend fun getHomeData(): List<SearchResult> {
        return getHomeRows().flatMap { it.items }.distinctBy { it.id }
    }
}
