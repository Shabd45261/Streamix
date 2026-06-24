package com.streamix.core.network

import com.streamix.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/all/week")
    suspend fun getTrending(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbPagedResponse

    @GET("movie/top_rated")
    suspend fun getTopRated(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbPagedResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("append_to_response") append: String = "credits,videos"
    ): TmdbMovieDetail

    @GET("tv/{tv_id}")
    suspend fun getTvDetail(
        @Path("tv_id") tvId: String,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("append_to_response") append: String = "credits,videos,seasons"
    ): TmdbTvDetail
}

data class TmdbSearchResponse(val results: List<TmdbSearchResult>)
data class TmdbPagedResponse(val results: List<TmdbSearchResult>)
data class TmdbSearchResult(
    val id: Int,
    val title: String?,
    val name: String?,
    val media_type: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String?,
    val first_air_date: String?
)

data class TmdbMovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val release_date: String,
    val runtime: Int?,
    val genres: List<TmdbGenre>,
    val credits: TmdbCredits?,
    val vote_average: Double = 0.0
)

data class TmdbTvDetail(
    val id: Int,
    val name: String,
    val overview: String,
    val poster_path: String?,
    val backdrop_path: String?,
    val first_air_date: String,
    val episode_run_time: List<Int>?,
    val genres: List<TmdbGenre>,
    val credits: TmdbCredits?,
    val seasons: List<TmdbSeason>?,
    val vote_average: Double = 0.0,
    val status: String = ""
)

data class TmdbGenre(val name: String)

data class TmdbCredits(val cast: List<TmdbCastMember>)
data class TmdbCastMember(
    val name: String,
    val character: String,
    val profile_path: String?
)

data class TmdbSeason(
    val id: Int,
    val season_number: Int,
    val name: String,
    val episode_count: Int,
    val poster_path: String?,
    val air_date: String?
)
