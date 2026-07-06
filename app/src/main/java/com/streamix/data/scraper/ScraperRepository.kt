package com.streamix.data.scraper

import com.streamix.core.model.VideoLink
import com.streamix.scraper.cloudstream.ProviderRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperRepository @Inject constructor() {
    suspend fun getVideoLinks(mediaId: String, mediaType: String, title: String? = null): Result<List<VideoLink>> {
        // Redirect to MoviesScraperRepository logic or similar if needed
        // For now, this is a placeholder to prevent breakage
        return Result.failure(Exception("Use MoviesScraperRepository for movies"))
    }

    suspend fun getVideoLinksFromServer(
        mediaId: String, mediaType: String, serverIndex: Int
    ): Result<List<VideoLink>> {
        return Result.failure(Exception("Use MoviesScraperRepository for movies"))
    }
}
