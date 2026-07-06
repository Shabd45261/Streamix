package com.streamix.data.tmdb

import com.streamix.core.model.SearchResult
import com.streamix.core.network.TmdbApiService
import com.streamix.core.network.TmdbSearchResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val api: TmdbApiService
) {
    suspend fun search(query: String): List<SearchResult> {
        return try {
            api.searchMulti(query = query).results.map { it.toSearchResult() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrending(): List<SearchResult> {
        return try {
            api.getTrending().results.map { it.toSearchResult() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopRated(): List<SearchResult> {
        return try {
            api.getTopRated().results.map { it.toSearchResult() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetail(id: String) = try { api.getMovieDetail(id) } catch (e: Exception) { null }
    suspend fun getTvDetail(id: String) = try { api.getTvDetail(id) } catch (e: Exception) { null }

    private fun TmdbSearchResult.toSearchResult() = SearchResult(
        id = id.toString(),
        title = title ?: name ?: "Unknown",
        posterPath = poster_path,
        mediaType = media_type ?: "movie",
        year = (release_date ?: first_air_date)?.take(4) ?: ""
    )
}
