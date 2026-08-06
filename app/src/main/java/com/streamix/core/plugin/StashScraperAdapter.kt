package com.streamix.core.plugin

import com.streamix.core.MainAPI
import com.streamix.core.TvType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class StashScraperAdapter(
    override val manifest: ExtensionManifest,
    private val api: MainAPI
) : StreamSource {

    override suspend fun search(query: String, category: ContentCategory?, page: Int): Result<SearchPage> = withContext(Dispatchers.IO) {
        runCatching {
            val results = withTimeoutOrNull(15000) {
                var res = if (query.isBlank()) api.getLatest() else api.search(query)
                
                // Fallback if latest is empty
                if (res.isEmpty() && query.isBlank()) {
                    res = api.search("trending")
                }
                if (res.isEmpty() && query.isBlank()) {
                    res = api.search("hot")
                }
                res
            } ?: emptyList()

            SearchPage(
                results = results.map { 
                    SearchResult(
                        id = it.url,
                        title = it.name,
                        posterUrl = it.posterUrl,
                        year = null,
                        type = when(it.type) {
                            TvType.Movie -> ContentType.MOVIE
                            TvType.TvSeries -> ContentType.SERIES
                            else -> ContentType.MOVIE
                        },
                        sourceId = manifest.id
                    )
                },
                hasMore = false
            )
        }
    }

    override suspend fun loadDetail(id: String): Result<ContentDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val response = withTimeoutOrNull(15000) { api.load(id) } 
                ?: throw Exception("Timeout loading details")
                
            ContentDetail(
                id = response.url,
                title = response.name,
                description = response.description,
                posterUrl = response.posterUrl,
                backdropUrl = response.posterUrl,
                type = if (response.episodes?.size ?: 0 > 1) ContentType.SERIES else ContentType.MOVIE,
                year = response.year,
                rating = response.rating?.toFloatOrNull(),
                seasons = if (response.episodes?.size ?: 0 > 1) {
                    listOf(Season(1, "Episodes", response.episodes!!.mapIndexed { index, ep ->
                        Episode(ep.url, ep.name ?: "Episode ${index + 1}", index + 1, 1, ep.description, ep.posterUrl)
                    }))
                } else emptyList()
            )
        }
    }

    override suspend fun getStreams(contentId: String): Result<List<StreamLink>> = withContext(Dispatchers.IO) {
        runCatching {
            val links = mutableListOf<StreamLink>()
            withTimeoutOrNull(20000) {
                api.loadLinks(contentId, false) { videoLink ->
                    links.add(StreamLink(
                        url = videoLink.url,
                        quality = videoLink.quality ?: "Auto",
                        format = if (videoLink.isHls) StreamFormat.HLS else StreamFormat.MP4
                    ))
                }
            }
            links
        }
    }

    override suspend fun getFeatured(): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val latest = withTimeoutOrNull(15000) {
                val res = api.getLatest()
                if (res.isEmpty()) api.search("featured") else res
            } ?: emptyList()
            
            latest.map {
                SearchResult(
                    id = it.url,
                    title = it.name,
                    posterUrl = it.posterUrl,
                    year = null,
                    type = ContentType.MOVIE,
                    sourceId = manifest.id
                )
            }
        }
    }
}
