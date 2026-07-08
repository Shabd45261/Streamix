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
import android.util.Base64

class MovieBoxProvider : MainAPI() {

    companion object {
        val HOST_POOL = listOf(
            "https://api6.aoneroom.com",
            "https://api5.aoneroom.com",
            "https://api4.aoneroom.com",
            "https://api4sg.aoneroom.com",
            "https://api3.aoneroom.com",
        )
        var bearerToken: String? = null

        fun decodeJwtExpiry(token: String): Long {
            return try {
                val payload = token.split(".").getOrNull(1) ?: return 0L
                val padded = payload.replace("-", "+").replace("_", "/")
                    .let { it + "=".repeat((4 - it.length % 4) % 4) }
                val json = Base64.decode(padded, Base64.DEFAULT).toString(Charsets.UTF_8)
                JSONObject(json).getLong("exp")
            } catch (_: Exception) { 0L }
        }

        fun isTokenValid(token: String?): Boolean {
            if (token.isNullOrBlank() || token == "null") return false
            val exp = decodeJwtExpiry(token)
            return exp > System.currentTimeMillis() / 1000 + 600
        }
    }

    override var mainUrl = HOST_POOL[4]
    override var name = "MovieBoxPh"
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

    private suspend fun getCachedToken(): String {
        if (isTokenValid(bearerToken)) return bearerToken!!
        
        for (host in HOST_POOL) {
            val url = "$host/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"
            val (brand, model) = randomBrandModel()
            val xClientToken = generateXClientToken()
            val xTrSignature = generateXTrSignature("GET", "application/json", "application/json", url)
            
            val headers = mapOf(
                "user-agent" to "com.community.oneroom/50020088 (Linux; U; Android 13; en_US; $brand; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
                "accept" to "application/json",
                "content-type" to "application/json",
                "x-client-token" to xClientToken,
                "x-tr-signature" to xTrSignature,
                "x-client-info" to """{"package_name":"com.community.oneroom","version_name":"3.0.13.0325.03","version_code":50020088,"os":"android","os_version":"13","device_id":"$deviceId","install_store":"ps","system_language":"en","net":"NETWORK_WIFI","region":"US","timezone":"Asia/Calcutta","sp_code":""}""",
                "x-client-status" to "0"
            )
            
            try {
                val response = app.get(url, headers = headers, timeout = 10)
                val xUser = response.headers["x-user"]
                if (!xUser.isNullOrBlank()) {
                    val token = jacksonObjectMapper().readTree(xUser)["token"]?.asText()
                    if (token != null && token != "null") {
                        bearerToken = token
                        mainUrl = host
                        return token
                    }
                }
            } catch (_: Exception) { }
        }
        return ""
    }

    override val mainPage = mainPageOf(
        "4516404531735022304" to "Trending",
        "5692654647815587592" to "Trending in Cinema",
        "414907768299210008"  to "Bollywood",
        "3859721901924910512" to "South Indian",
        "8019599703232971616" to "Hollywood",
        "4741626294545400336" to "Top Series This Week",
        "8434602210994128512" to "Anime",
        "1255898847918934600" to "Reality TV",
        "4903182713986896328" to "Indian Drama",
        "7878715743607948784" to "Korean Drama",
        "8788126208987989488" to "Chinese Drama",
        "3910636007619709856" to "Western TV",
        "5177200225164885656" to "Turkish Drama",
        "1|1" to "Movies",
        "1|2" to "Series",
        "1|1006" to "Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val perPage = 15
        val token = getCachedToken()
        val url = if (request.data.contains("|")) "$mainUrl/wefeed-mobile-bff/subject-api/list" else "$mainUrl/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=${request.data}&page=$page&perPage=$perPage"

        val data1 = request.data
        val mainParts = data1.substringBefore(";").split("|")
        val pg = mainParts.getOrNull(0)?.toIntOrNull() ?: 1
        val channelId = mainParts.getOrNull(1)

        val options = mutableMapOf<String, String>()
        data1.substringAfter(";", "")
            .split(";")
            .forEach {
                val parts = it.split("=")
                val k = parts.getOrNull(0)
                val v = parts.getOrNull(1)
                if (!k.isNullOrBlank() && !v.isNullOrBlank()) {
                    options[k] = v
                }
            }

        val classify = options["classify"] ?: "All"
        val country  = options["country"] ?: "All"
        val year     = options["year"] ?: "All"
        val genre    = options["genre"] ?: "All"
        val sort     = options["sort"] ?: "ForYou"

        val jsonBody = """{"page":$pg,"perPage":$perPage,"channelId":"$channelId","classify":"$classify","country":"$country","year":"$year","genre":"$genre","sort":"$sort"}"""
        val ts = System.currentTimeMillis()
        val xClientToken = generateXClientToken(ts)
        val xTrSignature = if (request.data.contains("|")) 
            generateXTrSignature("POST", "application/json", "application/json; charset=utf-8", url , jsonBody, hardcodedTimestamp = ts)
        else 
            generateXTrSignature("GET", "application/json", "application/json", url, hardcodedTimestamp = ts)

        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"sdk_gphone64_x86_64","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "Authorization" to "Bearer $token"
        )

        val response = if (request.data.contains("|")) 
            app.post(url, headers = headers, requestBody = jsonBody.toRequestBody("application/json".toMediaType())) 
        else 
            app.get(url, headers = headers)

        val root = try { mapper.readTree(response.text) } catch (_: Exception) { null } ?: return newHomePageResponse(emptyList())
        val items = root["data"]?.get("items") ?: root["data"]?.get("subjects") ?: return newHomePageResponse(emptyList())
        val data = items.mapNotNull { item ->
            val title = item["title"]?.asText()?.substringBefore("[") ?: return@mapNotNull null
            val id = item["subjectId"]?.asText() ?: return@mapNotNull null
            val type = if (item["subjectType"]?.asInt() == 2) TvType.TvSeries else TvType.Movie
            newMovieSearchResponse(title, id, type) {
                this.posterUrl = item["cover"]?.get("url")?.asText()
                this.score = Score.from10(item["imdbRatingValue"]?.asText())
            }
        }
        return newHomePageResponse(request.name, data)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val token = getCachedToken()
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = """{"page": $page, "perPage": 20, "keyword": "$query"}"""
        val ts = System.currentTimeMillis()
        val xClientToken = generateXClientToken(ts)
        val xTrSignature = generateXTrSignature("POST", "application/json", "application/json; charset=utf-8", url, jsonBody, hardcodedTimestamp = ts)
        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 16; en_IN; sdk_gphone64_x86_64; Build/BP22.250325.006; Cronet/133.0.6876.3)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"sdk_gphone64_x86_64","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "Authorization" to "Bearer $token"
        )
        val response = app.post(url, headers = headers, requestBody = jsonBody.toRequestBody("application/json".toMediaType()))
        val root = try { mapper.readTree(response.text) } catch (_: Exception) { null } ?: return newSearchResponseList(emptyList())
        val results = root.get("data")?.get("results") ?: return newSearchResponseList(emptyList())
        val searchList = mutableListOf<SearchResponse>()
        for (result in results) {
            val subjects = result["subjects"] ?: continue
            for (subject in subjects) {
                val title = subject["title"]?.asText() ?: continue
                val id = subject["subjectId"]?.asText() ?: continue
                val type = if (subject["subjectType"]?.asInt() == 2) TvType.TvSeries else TvType.Movie
                searchList.add(newMovieSearchResponse(title, id, type) {
                    this.posterUrl = subject["cover"]?.get("url")?.asText()
                    this.score = Score.from10(subject["imdbRatingValue"]?.asText())
                })
            }
        }
        return newSearchResponseList(searchList)
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = if (url.contains("subjectId=")) url.substringAfter("subjectId=").substringBefore("&") else url.substringAfterLast("/")
        val token = getCachedToken()
        val finalUrl = "$mainUrl/wefeed-mobile-bff/subject-api/get?subjectId=$id"
        val headers = getHeaders(finalUrl, "GET", token = token)

        val response = try { app.get(finalUrl, headers = headers) } catch (_: Exception) { return null }
        val detailData = try { mapper.readTree(response.text)["data"] } catch (_: Exception) { null } ?: return null

        val title = detailData["title"]?.asText()?.substringBefore("[") ?: ""
        val type = if (detailData["subjectType"]?.asInt() == 2) TvType.TvSeries else TvType.Movie
        val poster = detailData["cover"]?.get("url")?.asText()
        val plot = detailData["description"]?.asText()
        val year = detailData["releaseDate"]?.asText()?.take(4)?.toIntOrNull()
        val rating = Score.from10(detailData["imdbRatingValue"]?.asText())

        val res = if (type == TvType.TvSeries) {
            val episodes = mutableListOf<Episode>()
            try {
                val seasonUrl = "$mainUrl/wefeed-mobile-bff/subject-api/season-info?subjectId=$id"
                val sResponse = app.get(seasonUrl, headers = getHeaders(seasonUrl, "GET", token = token))
                mapper.readTree(sResponse.text)["data"]?.get("seasons")?.forEach { season ->
                    val se = season["se"]?.asInt() ?: 1
                    val maxEp = season["maxEp"]?.asInt() ?: 1
                    for (ep in 1..maxEp) {
                        episodes.add(newEpisode("$id|$se|$ep") { 
                            this.season = se
                            this.episode = ep
                            this.name = "Episode $ep"
                            this.posterUrl = poster 
                        })
                    }
                }
            } catch (_: Exception) { }
            newTvSeriesLoadResponse(title, finalUrl, type, episodes.sortedWith(compareBy({ it.season }, { it.episode })))
        } else {
            newMovieLoadResponse(title, finalUrl, type, id)
        }

        res.apply {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.score = rating
            this.tags = detailData["genre"]?.asText()?.split(",")?.map { it.trim() } ?: emptyList()
            
            // Extract trailers if available
            detailData["trailer"]?.get("videoUrl")?.asText()?.let { trailerUrl ->
                if (trailerUrl.isNotEmpty()) {
                    this.trailers.add(TrailerData(trailerUrl))
                }
            }
        }
        return res
    }

    private suspend fun getHeaders(url: String, method: String, body: String? = null, token: String? = null): Map<String, String> {
        val ts = System.currentTimeMillis()
        val sig = generateXTrSignature(method, "application/json", "application/json", url, body, hardcodedTimestamp = ts)
        val finalToken = token ?: getCachedToken()
        return mapOf(
            "user-agent" to "com.community.mbox.in/50020088 (Linux; U; Android 13; en_IN; Pixel 7; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-client-token" to generateXClientToken(ts),
            "x-tr-signature" to sig,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.13.0325.03","version_code":50020088,"os":"android","os_version":"13","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"Pixel 7","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "Authorization" to "Bearer $finalToken"
        )
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val parts = data.split("|")
        val sid = parts[0]
        val se = parts.getOrNull(1) ?: "0"
        val ep = parts.getOrNull(2) ?: "0"
        
        val token = getCachedToken()
        val url = "$mainUrl/wefeed-mobile-bff/subject-api/play-info?subjectId=$sid&se=$se&ep=$ep"
        val ts = System.currentTimeMillis()
        val xClientToken = generateXClientToken(ts)
        val xTrSignature = generateXTrSignature("GET", "application/json", "application/json", url, hardcodedTimestamp = ts)
        val headers = mapOf(
            "user-agent" to "com.community.mbox.in/50020042 (Linux; U; Android 13; en_IN; Pixel 7; Build/TQ3A.230901.001; Cronet/145.0.7582.0)",
            "accept" to "application/json",
            "content-type" to "application/json",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"13","device_id":"$deviceId","install_store":"ps","gaid":"d7578036d13336cc","brand":"google","model":"Pixel 7","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}""",
            "x-client-status" to "0",
            "Authorization" to "Bearer $token"
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
