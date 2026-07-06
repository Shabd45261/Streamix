package com.streamix.scraper.cloudstream.providers.movies

import android.net.Uri
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.streamix.scraper.cloudstream.*
import com.streamix.scraper.cloudstream.utils.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max
import java.security.SecureRandom

class MovieBoxINProvider : MainAPI() {

    override var mainUrl = "https://api3.aoneroom.com"
    override var name = "MovieBoxIN"
    override val hasMainPage = true
    override var lang = "hi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val secretKeyDefault = base64Decode("NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw==")
    private val secretKeyAlt = base64Decode("WHFuMm5uTzQxL0w5Mm8xaXVYaFNMSFRiWHZZNFo1Wlo2Mm04bVNMQQ==")

    private fun md5(input: ByteArray): String {
        return MessageDigest.getInstance("MD5").digest(input)
            .joinToString("") { "%02x".format(it) }
    }

    private fun reverseString(input: String): String = input.reversed()

    private fun generateXClientToken(hardcodedTimestamp: Long? = null): String {
        val timestamp = (hardcodedTimestamp ?: System.currentTimeMillis()).toString()
        val reversed = reverseString(timestamp)
        val hash = md5(reversed.toByteArray())
        return "$timestamp,$hash"
    }

    private val random = SecureRandom()

    fun generateDeviceId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    val deviceId = generateDeviceId()

    data class BrandModel(val brand: String, val model: String)

    private val brandModels = mapOf(
        "Samsung" to listOf("SM-S918B", "SM-A528B", "SM-M336B"),
        "Xiaomi" to listOf("2201117TI", "M2012K11AI", "Redmi Note 11"),
        "OnePlus" to listOf("LE2111", "CPH2449", "IN2023"),
        "Google" to listOf("Pixel 6", "Pixel 7", "Pixel 8"),
        "Realme" to listOf("RMX3085", "RMX3360", "RMX3551")
    )

    fun randomBrandModel(): BrandModel {
        val brand = brandModels.keys.random()
        val model = brandModels[brand]!!.random()
        return BrandModel(brand, model)
    }

    private fun buildCanonicalString(
        method: String,
        accept: String?,
        contentType: String?,
        url: String,
        body: String?,
        timestamp: Long
    ): String {
        val parsed = Uri.parse(url)
        val path = parsed.path ?: ""
        val query = if (parsed.queryParameterNames.isNotEmpty()) {
            parsed.queryParameterNames.sorted().joinToString("&") { key ->
                parsed.getQueryParameters(key).joinToString("&") { value -> "$key=$value" }
            }
        } else ""
        val canonicalUrl = if (query.isNotEmpty()) "$path?$query" else path
        val bodyBytes = body?.toByteArray(Charsets.UTF_8)
        val bodyHash = if (bodyBytes != null) {
            val trimmed = if (bodyBytes.size > 102400) bodyBytes.copyOfRange(0, 102400) else bodyBytes
            md5(trimmed)
        } else ""
        val bodyLength = bodyBytes?.size?.toString() ?: ""
        return "${method.uppercase()}\n${accept ?: ""}\n${contentType ?: ""}\n$bodyLength\n$timestamp\n$bodyHash\n$canonicalUrl"
    }

    private fun generateXTrSignature(
        method: String,
        accept: String?,
        contentType: String?,
        url: String,
        body: String? = null,
        useAltKey: Boolean = false,
        hardcodedTimestamp: Long? = null
    ): String {
        val timestamp = hardcodedTimestamp ?: System.currentTimeMillis()
        val canonical = buildCanonicalString(method, accept, contentType, url, body, timestamp)
        val secret = if (useAltKey) secretKeyAlt else secretKeyDefault
        val secretBytes = base64DecodeArray(secret)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))
        val signature = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        val signatureB64 = base64Encode(signature)
        return "$timestamp|2|$signatureB64"
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/wefeed-mobile-bff/tab-operating?page=1&tabId=0&version="
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature("GET", "application/json", "application/json", url)

        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "connection" to "keep-alive",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"${randomBrandModel().model}","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "x-play-mode" to "2"
        )

        val response = app.get(url, headers = headers)
        val responseBody = response.text

        fun parseSubject(subjectJson: JsonNode?): SearchResponse? {
            subjectJson ?: return null
            val subjectId = subjectJson["subjectId"]?.asText() ?: return null
            val title = subjectJson["title"]?.asText() ?: return null
            val coverUrl = subjectJson["cover"]?.get("url")?.asText()
            val subjectType = when (subjectJson["subjectType"]?.asInt()) {
                1 -> TvType.Movie
                2 -> TvType.TvSeries
                else -> TvType.Movie
            }
            return newMovieSearchResponse(title, subjectId, subjectType) {
                this.posterUrl = coverUrl
                this.score = Score.from10(subjectJson["imdbRatingValue"]?.asText())
            }
        }

        val homePageLists = try {
            val root = mapper.readTree(responseBody)
            val sections = root["data"]?.get("items") ?: return newHomePageResponse(emptyList())

            sections.mapNotNull { section ->
                val title = section["title"]?.asText()?.let {
                    if (it.equals("banner", ignoreCase = true)) "🔥Top Picks" else it
                } ?: return@mapNotNull null
                val type = section["type"]?.asText()

                val mediaList = when (type) {
                    "BANNER" -> section["banner"]?.get("banners")
                        ?.mapNotNull { bannerItem -> parseSubject(bannerItem["subject"]) }
                    "SUBJECTS_MOVIE" -> section["subjects"]
                        ?.mapNotNull { subjectItem -> parseSubject(subjectItem) }
                    "CUSTOM" -> section["customData"]?.get("items")
                        ?.mapNotNull { customItem -> parseSubject(customItem["subject"]) }
                    else -> null
                }

                if (mediaList.isNullOrEmpty()) {
                    null
                } else {
                    HomePageList(title, mediaList)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

        return newHomePageResponse(homePageLists)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = """{"page": $page, "perPage": 20, "keyword": "$query"}"""
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature("POST", "application/json", "application/json; charset=utf-8", url, jsonBody)
        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "connection" to "keep-alive",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"${randomBrandModel().model}","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0"
        )
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val response = app.post(url, headers = headers, requestBody = requestBody)

        val responseBody = response.text
        val root = mapper.readTree(responseBody)
        val results = root.get("data")?.get("results") ?: return newSearchResponseList(emptyList())
        val searchList = mutableListOf<SearchResponse>()
        for (result in results) {
            val subjects = result["subjects"] ?: continue
            for (subject in subjects) {
                val title = subject["title"]?.asText() ?: continue
                val id = subject["subjectId"]?.asText() ?: continue
                val coverImg = subject["cover"]?.get("url")?.asText()
                val subjectType = subject["subjectType"]?.asInt() ?: 1
                val type = when (subjectType) {
                    1 -> TvType.Movie
                    2 -> TvType.TvSeries
                    else -> TvType.Movie
                }
                searchList.add(
                    newMovieSearchResponse(title, id, type) {
                        this.posterUrl = coverImg
                        this.score = Score.from10(subject["imdbRatingValue"]?.asText())
                    }
                )
            }
        }
        return newSearchResponseList(searchList)
    }

    override suspend fun load(url: String): LoadResponse {
        val id = if (url.contains("subjectId=")) url.substringAfter("subjectId=").substringBefore("&") else url.substringAfterLast("/")
        val finalUrl = "$mainUrl/wefeed-mobile-bff/subject-api/get?subjectId=$id"
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature("GET", "application/json", "application/json", finalUrl)

        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; ${randomBrandModel().model}; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "connection" to "keep-alive",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"${randomBrandModel().model}","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "x-play-mode" to "2"
        )

        val response = app.get(finalUrl, headers = headers)
        if (response.code != 200) {
            throw ErrorLoadingException("Failed to load data")
        }

        val root = mapper.readTree(response.text)
        val data = root["data"] ?: throw ErrorLoadingException("No data")

        val title = data["title"]?.asText()?.substringBefore("[") ?: ""
        val description = data["description"]?.asText()
        val releaseDate = data["releaseDate"]?.asText()
        val duration = data["duration"]?.asText()
        val genre = data["genre"]?.asText()
        val imdbRating = data["imdbRatingValue"]?.asText()?.toDoubleOrNull()?.times(10)?.toInt()
        val year = releaseDate?.take(4)?.toIntOrNull()
        val coverUrl = data["cover"]?.get("url")?.asText()

        val subjectType = data["subjectType"]?.asInt() ?: 1
        val type = when (subjectType) {
            1 -> TvType.Movie
            2 -> TvType.TvSeries
            7 -> TvType.TvSeries
            else -> TvType.Movie
        }

        val res = if (type == TvType.TvSeries) {
            val episodes = mutableListOf<Episode>()
            try {
                val seasonUrl = "$mainUrl/wefeed-mobile-bff/subject-api/season-info?subjectId=$id"
                val seasonSig = generateXTrSignature("GET", "application/json", "application/json", seasonUrl)
                val seasonHeaders = headers.toMutableMap().apply { put("x-tr-signature", seasonSig) }
                val seasonResponse = app.get(seasonUrl, headers = seasonHeaders)
                if (seasonResponse.code == 200) {
                    val seasonRoot = mapper.readTree(seasonResponse.text)
                    seasonRoot["data"]?.get("seasons")?.forEach { season ->
                        val seasonNumber = season["se"]?.asInt() ?: 1
                        val maxEp = season["maxEp"]?.asInt() ?: 1
                        for (ep in 1..maxEp) {
                            episodes.add(newEpisode("$id|$seasonNumber|$ep") {
                                this.season = seasonNumber
                                this.episode = ep
                                this.name = "Episode $ep"
                                this.posterUrl = coverUrl
                            })
                        }
                    }
                }
            } catch (_: Exception) {}
            newTvSeriesLoadResponse(title, finalUrl, type, episodes)
        } else {
            newMovieLoadResponse(title, finalUrl, type, id)
        }

        return res.apply {
            this.posterUrl = coverUrl
            this.plot = description
            this.year = year
            this.tags = genre?.split(",")?.map { it.trim() }
            this.score = imdbRating
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val parts = data.split("|")
        val sid = parts[0]
        val se = parts.getOrNull(1) ?: "0"
        val ep = parts.getOrNull(2) ?: "0"
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$sid&se=$se&ep=$ep"
        val xClientToken = generateXClientToken()
        val xTrSignature = generateXTrSignature("GET", "application/json", "application/json", url)
        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 13; en_IN; Pixel 7; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"13","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"Pixel 7","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0"
        )

        val response = try { app.get(url, headers = headers) } catch (_: Exception) { return false }
        val root = try { mapper.readTree(response.text) } catch (_: Exception) { null } ?: return false
        val streams = root["data"]?.get("streams") ?: return false
        streams.forEach { stream ->
            val streamUrl = stream["url"]?.asText() ?: return@forEach
            val res = stream["resolutions"]?.asText() ?: ""
            val quality = getHighestQuality(res) ?: 0
            callback.invoke(newExtractorLink(name, name, streamUrl, mainUrl, quality, ExtractorLinkType.M3U8) {
                val cookie = stream["signCookie"]?.asText()
                if (!cookie.isNullOrEmpty()) this.headers = mapOf("Cookie" to cookie)
            })
        }
        return true
    }
}
