package com.streamix.data.scraper

import com.streamix.core.model.VideoLink
import com.streamix.scraper.moviebox.MovieboxInProvider
import com.streamix.scraper.moviebox.MovieboxProvider
import com.streamix.scraper.moviebox.MovieboxSiProvider
import com.streamix.scraper.moviebox.VegamoviesScraper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperRepository @Inject constructor(
    private val server1: MovieboxProvider,
    private val server2: MovieboxInProvider,
    private val server3: MovieboxSiProvider,
    private val server4: VegamoviesScraper
) {
    suspend fun getVideoLinks(mediaId: String, mediaType: String, title: String? = null): Result<List<VideoLink>> {
        val servers = listOf(server1, server3, server2, server4)
        
        // If it's a numeric ID (likely TMDB), we need to search by title on the provider site
        val isTmdbId = mediaId.all { it.isDigit() }
        
        for (server in servers) {
            try {
                val targetId = if (isTmdbId && !title.isNullOrEmpty()) {
                    val searchResults = server.search(title)
                    // Try exact match then partial match
                    val match = searchResults.firstOrNull { it.title.equals(title, ignoreCase = true) }
                        ?: searchResults.firstOrNull { it.title.contains(title, ignoreCase = true) }
                        ?: searchResults.firstOrNull()
                    
                    match?.id ?: continue
                } else mediaId

                val links = server.getVideoLinks(targetId, mediaType)
                if (links.isNotEmpty() && links.any { it.url.startsWith("http") }) {
                    return Result.success(links)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return Result.failure(Exception("No video links found on any server"))
    }

    suspend fun getVideoLinksFromServer(
        mediaId: String, mediaType: String, serverIndex: Int
    ): Result<List<VideoLink>> {
        val scraper = when (serverIndex) {
            0 -> server1
            1 -> server2
            2 -> server3
            3 -> server4
            else -> server1
        }
        return try {
            Result.success(scraper.getVideoLinks(mediaId, mediaType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
