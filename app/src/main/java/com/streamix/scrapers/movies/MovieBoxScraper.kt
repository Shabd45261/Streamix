package com.streamix.scrapers.movies

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.SecureRandom

class MovieBoxScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "moviebox",
        displayName = "MovieBox",
        baseUrl = "https://api3.aoneroom.com",
        categories = listOf(ContentCategory.MOVIES, ContentCategory.SHOWS)
    )

    private val secretKeyDefault = "NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw=="
    private val secretKeyAlt = "WHFuMm5uTzQxL0w5Mm8xaXVYaFNMSFRiWHZZNFo1Wlo2Mm04bVNMQQ=="
    private val deviceId = generateDeviceId()

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "${info.baseUrl}/wefeed-mobile-bff/subject-api/list"
        val jsonBody = """{"page":$page,"perPage":20,"channelId":"1","classify":"All","country":"All","year":"All","genre":"All","sort":"ForYou"}"""
        
        val response = post(url, jsonBody) ?: return@withContext emptyList()
        parseList(response)
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "${info.baseUrl}/wefeed-mobile-bff/subject-api/search/v2"
        val jsonBody = """{"page": $page, "perPage": 20, "keyword": "$query"}"""
        
        val response = post(url, jsonBody) ?: return@withContext emptyList()
        parseList(response)
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        // pageUrl is expected to be the subjectId for MovieBox
        val id = pageUrl.substringAfterLast("subjectId=", pageUrl)
        val url = "${info.baseUrl}/wefeed-mobile-bff/subject-api/play-info?subjectId=$id&se=0&ep=0"
        
        val response = get(url) ?: return@withContext emptyList()
        val results = mutableListOf<StreamResult>()
        
        try {
            val data = JSONObject(response).optJSONObject("data")
            val streams = data?.optJSONArray("streams")
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val stream = streams.getJSONObject(i)
                    val streamUrl = stream.optString("url")
                    val format = stream.optString("format", "mp4").lowercase()
                    val quality = stream.optString("resolutions", "Auto")
                    
                    results.add(StreamResult(
                        url = streamUrl,
                        quality = quality,
                        format = if (format == "hls") "hls" else "mp4",
                        headers = mapOf("Referer" to info.baseUrl)
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        results
    }

    private fun parseList(jsonStr: String): List<ScrapeResult> {
        val results = mutableListOf<ScrapeResult>()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data")
            val items = data?.optJSONArray("items") ?: data?.optJSONArray("results") ?: data?.optJSONArray("subjects")
            
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.get(i)
                    val obj = item as? JSONObject ?: continue
                    
                    val title = obj.optString("title").substringBefore("[")
                    val id = obj.optString("subjectId")
                    val cover = obj.optJSONObject("cover")?.optString("url")
                    
                    results.add(ScrapeResult(
                        id = id,
                        title = title,
                        posterUrl = cover,
                        pageUrl = id, // We use ID as pageUrl for play-info
                        sourceId = info.id
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return results
    }

    private suspend fun post(url: String, body: String): String? {
        val timestamp = System.currentTimeMillis()
        val headers = getHeaders("POST", url, body, timestamp)
        return ScraperHttpClient.post(url, body, headers)
    }

    private suspend fun get(url: String): String? {
        val timestamp = System.currentTimeMillis()
        val headers = getHeaders("GET", url, null, timestamp)
        return ScraperHttpClient.get(url, headers)
    }

    private fun getHeaders(method: String, url: String, body: String?, timestamp: Long): Map<String, String> {
        val xClientToken = generateXClientToken(timestamp)
        val xTrSignature = generateXTrSignature(method, url, body, timestamp)
        
        return mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "x-client-token" to xClientToken,
            "x-tr-signature" to xTrSignature,
            "x-client-info" to """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"14","device_id":"$deviceId","brand":"Google","model":"Pixel 8","system_language":"en","net":"NETWORK_WIFI","region":"US"}""",
            "User-Agent" to "com.community.mbox.in/50020042 (Linux; U; Android 14; en_US; Pixel 8; Build/BP22.250325.006; Cronet/133.0.6876.3)"
        )
    }

    private fun generateXClientToken(timestamp: Long): String {
        val reversed = timestamp.toString().reversed()
        val hash = md5(reversed.toByteArray())
        return "$timestamp,$hash"
    }

    private fun generateXTrSignature(method: String, url: String, body: String?, timestamp: Long): String {
        val path = java.net.URI(url).path
        val bodyHash = if (body != null) md5(body.toByteArray()) else ""
        val bodyLength = body?.length?.toString() ?: ""
        
        // Canonical string: METHOD\nACCEPT\nCONTENT_TYPE\nBODY_LENGTH\nTIMESTAMP\nBODY_HASH\nPATH
        val canonical = "$method\napplication/json\napplication/json\n$bodyLength\n$timestamp\n$bodyHash\n$path"
        
        val secretBytes = Base64.decode(secretKeyDefault, Base64.DEFAULT)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))
        val signature = Base64.encodeToString(mac.doFinal(canonical.toByteArray()), Base64.NO_WRAP)
        
        return "$timestamp|2|$signature"
    }

    private fun md5(input: ByteArray): String {
        return MessageDigest.getInstance("MD5").digest(input)
            .joinToString("") { "%02x".format(it) }
    }

    private fun generateDeviceId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
