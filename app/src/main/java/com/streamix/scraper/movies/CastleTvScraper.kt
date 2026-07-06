package com.streamix.scraper.movies

import android.util.Log
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import com.streamix.scraper.cloudstream.app
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson
import com.streamix.scraper.cloudstream.utils.mapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastleTvScraper @Inject constructor() : BaseScraper() {

    override val name = "Castle TV"
    override val mainUrl = "https://api.hlowb.com"
    
    private val CASTLE_SUFFIX = "castle_india_A" 

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext getTrending()
            
            val securityKey = getSecurityKey() ?: return@withContext emptyList()
            val searchUrl = "$mainUrl/film-api/v1.1.0/movie/searchByKeyword?channel=IndiaA&clientType=1&keyword=${java.net.URLEncoder.encode(query, "UTF-8")}&lang=en-US&mode=1&packageName=com.external.castle&page=1&size=30"
            
            val response = app.get(searchUrl)
            val encryptedData = response.text
            
            if (encryptedData.isNullOrBlank()) return@withContext emptyList()
            
            val decryptedJson = decryptData(encryptedData, securityKey) ?: return@withContext emptyList()
            val searchResponse = mapper.readValue<SearchApiResponse>(decryptedJson)
            
            searchResponse.data.rows?.mapNotNull { item ->
                item.toSearchResult()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("CastleTvScraper", "Search failed", e)
            emptyList()
        }
    }

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> = withContext(Dispatchers.IO) {
        try {
            val parts = mediaId.split("_")
            val movieId = parts[0]
            val episodeId = if (parts.size > 1) parts[1] else {
                getFirstEpisodeId(movieId) ?: return@withContext emptyList()
            }
            
            val securityKey = getSecurityKey() ?: return@withContext emptyList()
            val links = mutableListOf<VideoLink>()
            
            val resolutions = listOf(3, 2, 1) // 1080p, 720p, 480p
            
            for (resolution in resolutions) {
                val videoUrl = "$mainUrl/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US"
                val postBody = """
                    {
                      "mode": "1",
                      "appMarket": "GuanWang",
                      "clientType": "1",
                      "woolUser": "false",
                      "apkSignKey": "ED0955EB04E67A1D9F3305B95454FED485261475",
                      "androidVersion": "13",
                      "movieId": "$movieId",
                      "episodeId": "$episodeId",
                      "isNewUser": "true",
                      "resolution": "$resolution",
                      "packageName": "com.external.castle"
                    }
                """.trimIndent()

                val videoResponse = app.post(
                    url = videoUrl,
                    requestBody = postBody.toRequestBody("application/json".toMediaType()),
                )

                val encryptedData = videoResponse.text
                if (encryptedData.isNullOrBlank()) continue

                val decryptedJson = decryptData(encryptedData, securityKey) ?: continue
                val videoData = mapper.readValue<VideoResponse>(decryptedJson).data

                if (videoData.videoUrl != null && videoData.permissionDenied != true) {
                    val qualityLabel = when (resolution) {
                        3 -> "1080p"
                        2 -> "720p"
                        1 -> "480p"
                        else -> "Auto"
                    }
                    links.add(VideoLink(videoData.videoUrl, qualityLabel, name, videoData.videoUrl.contains(".m3u8")))
                }
            }
            
            links
        } catch (e: Exception) {
            Log.e("CastleTvScraper", "Failed to get links", e)
            emptyList()
        }
    }

    private suspend fun getFirstEpisodeId(movieId: String): String? {
        val details = loadDetail(movieId)
        return details?.episodes?.firstOrNull()?.id
    }

    suspend fun loadDetail(movieId: String): CastleMovieDetail? = withContext(Dispatchers.IO) {
        try {
            val securityKey = getSecurityKey() ?: return@withContext null
            val detailsUrl = "$mainUrl/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&lang=en-US&movieId=$movieId&packageName=com.external.castle"
            
            val response = app.get(detailsUrl)
            val decryptedJson = decryptData(response.text, securityKey) ?: return@withContext null
            val detailsResponse = mapper.readValue<MovieDetailsResponse>(decryptedJson)
            val data = detailsResponse.data
            
            CastleMovieDetail(
                id = data.id?.toString() ?: movieId,
                title = data.title ?: "Unknown Title",
                description = data.briefIntroduction ?: "",
                posterUrl = data.coverVerticalImage ?: data.coverHorizontalImage,
                backgroundPosterUrl = data.coverHorizontalImage ?: data.coverVerticalImage,
                year = data.publishTime?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).year.toString()
                } ?: "",
                episodes = data.episodes?.mapIndexed { index, ep ->
                    CastleEpisode(
                        id = ep.id?.toString() ?: "",
                        title = ep.title ?: "Episode ${index + 1}",
                        episodeNumber = index + 1,
                        posterUrl = ep.coverImage
                    )
                } ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e("CastleTvScraper", "Failed to load detail", e)
            null
        }
    }

    suspend fun getTrending(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val securityKey = getSecurityKey() ?: return@withContext emptyList()
            val url = "$mainUrl/film-api/v0.1/category/home?channel=IndiaA&clientType=1&lang=en-US&locationId=1001&mode=1&packageName=com.external.castle&page=1&size=17"
            val response = app.get(url)
            val encryptedData = try {
                mapper.readValue<CastleApiResponse>(response.text).data
            } catch (e: Exception) {
                response.text
            }
            
            if (encryptedData.isNullOrBlank()) return@withContext emptyList()
            
            val decryptedJson = decryptData(encryptedData, securityKey) ?: return@withContext emptyList()
            val decryptedResponse = mapper.readValue<DecryptedResponse>(decryptedJson)
            
            decryptedResponse.data.rows?.flatMap { row ->
                row.contents?.mapNotNull { it.toSearchResult() } ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("CastleTvScraper", "Failed to get trending", e)
            emptyList()
        }
    }

    suspend fun getRecommended(): List<SearchResult> = withContext(Dispatchers.IO) {
        // Shuffled trending as recommended
        getTrending().shuffled()
    }

    private suspend fun getSecurityKey(): String? {
        return try {
            val url = "$mainUrl/v0.1/system/getSecurityKey/1?channel=IndiaA&clientType=1&lang=en-US"
            val response = app.get(url)
            val securityResponse = mapper.readValue<SecurityKeyResponse>(response.text)
            if (securityResponse.code == 200) securityResponse.data else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptData(encryptedB64: String, apiKeyB64: String): String? {
        return try {
            val aesKey = deriveKey(apiKeyB64)
            val encryptedData = Base64.getDecoder().decode(encryptedB64)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(aesKey, "AES")
            val ivSpec = IvParameterSpec(aesKey)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decrypted = cipher.doFinal(encryptedData)
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun deriveKey(apiKeyB64: String): ByteArray {
        val apiKeyBytes = Base64.getDecoder().decode(apiKeyB64)
        val keyMaterial = apiKeyBytes + CASTLE_SUFFIX.toByteArray(StandardCharsets.US_ASCII)
        return when {
            keyMaterial.size < 16 -> keyMaterial + ByteArray(16 - keyMaterial.size)
            keyMaterial.size > 16 -> keyMaterial.copyOfRange(0, 16)
            else -> keyMaterial
        }
    }

    private fun ContentItem.toSearchResult(): SearchResult? {
        val id = redirectId?.toString() ?: return null
        return SearchResult(
            id = id,
            title = title ?: "Unknown",
            posterPath = coverImage,
            mediaType = "castle_movie",
            year = publishTime?.let { timestamp ->
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).year.toString()
            } ?: ""
        )
    }

    private fun SearchResultItem.toSearchResult(): SearchResult? {
        val id = id?.toString() ?: return null
        return SearchResult(
            id = id,
            title = title ?: "Unknown",
            posterPath = coverVerticalImage ?: coverHorizontalImage,
            mediaType = "castle_movie",
            year = publishTime?.let { timestamp ->
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).year.toString()
            } ?: ""
        )
    }

    // Data classes
    data class CastleMovieDetail(
        val id: String,
        val title: String,
        val description: String,
        val posterUrl: String?,
        val backgroundPosterUrl: String?,
        val year: String,
        val episodes: List<CastleEpisode>
    )

    data class CastleEpisode(
        val id: String,
        val title: String,
        val episodeNumber: Int,
        val posterUrl: String?
    )

    private data class CastleApiResponse(val code: Int, val msg: String, val data: String? = null)
    private data class SecurityKeyResponse(val code: Int, val msg: String, val data: String)
    private data class DecryptedResponse(val code: Int, val msg: String, val data: HomePageData)
    private data class HomePageData(val rows: List<HomePageRow>? = null)
    private data class HomePageRow(val name: String? = null, val contents: List<ContentItem>? = null)
    private data class ContentItem(
        val title: String? = null,
        val coverImage: String? = null,
        val redirectId: Long? = null,
        val movieType: Int? = null,
        val publishTime: Long? = null
    )
    private data class SearchApiResponse(val data: SearchData)
    private data class SearchData(val rows: List<SearchResultItem>? = null)
    private data class SearchResultItem(
        val id: Long? = null,
        val title: String? = null,
        val coverHorizontalImage: String? = null,
        val coverVerticalImage: String? = null,
        val publishTime: Long? = null
    )
    private data class MovieDetailsResponse(val data: MovieDetails)
    private data class MovieDetails(
        val id: Long? = null,
        val title: String? = null,
        val briefIntroduction: String? = null,
        val coverVerticalImage: String? = null,
        val coverHorizontalImage: String? = null,
        val publishTime: Long? = null,
        val episodes: List<ApiEpisode>? = null
    )
    private data class ApiEpisode(
        val id: Long? = null,
        val title: String? = null,
        val coverImage: String? = null
    )
    private data class VideoResponse(val data: VideoData)
    private data class VideoData(val videoUrl: String? = null, val permissionDenied: Boolean? = null)
}
