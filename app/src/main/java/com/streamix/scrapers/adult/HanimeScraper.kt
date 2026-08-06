package com.streamix.scrapers.adult

import com.streamix.core.plugin.ContentCategory
import com.streamix.scrapers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class HanimeScraper : BaseScraper {

    override val info = ScraperInfo(
        id = "hanime",
        displayName = "Hanime",
        baseUrl = "https://hanime.tv",
        categories = listOf(ContentCategory.ADULT)
    )

    override suspend fun getTrending(page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "https://search.htv-services.com/"
        val data = JSONObject().apply {
            put("search_text", "")
            put("tags", JSONArray())
            put("tags_mode", "AND")
            put("brands", JSONArray())
            put("blacklist", JSONArray())
            put("order_by", "trending") // Sort by trending
            put("ordering", "desc")
            put("page", page - 1)
        }
        
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(data.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: return@withContext emptyList()
        
        try {
            val root = JSONObject(json)
            val hits = root.getJSONArray("hits")
            val results = mutableListOf<ScrapeResult>()
            for (i in 0 until hits.length()) {
                val v = hits.getJSONObject(i)
                results.add(ScrapeResult(
                    id = v.getString("id"),
                    title = v.getString("name"),
                    posterUrl = v.optString("cover_url"),
                    pageUrl = "${info.baseUrl}/videos/hentai/${v.getString("slug")}",
                    sourceId = info.id
                ))
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun search(query: String, page: Int): List<ScrapeResult> = withContext(Dispatchers.IO) {
        val url = "https://search.htv-services.com/"
        val data = JSONObject().apply {
            put("search_text", query)
            put("tags", JSONArray())
            put("tags_mode", "AND")
            put("brands", JSONArray())
            put("blacklist", JSONArray())
            put("order_by", "created_at_unix")
            put("ordering", "desc")
            put("page", page - 1)
        }
        
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(data.toString().toRequestBody("application/json".toMediaType()))
            .build()
            
        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: return@withContext emptyList()
        
        try {
            val arr = JSONArray(json)
            val results = mutableListOf<ScrapeResult>()
            for (i in 0 until arr.length()) {
                val v = arr.getJSONObject(i)
                results.add(ScrapeResult(
                    id = v.getInt("id").toString(),
                    title = v.getString("name"),
                    posterUrl = v.optString("cover_url"),
                    pageUrl = "${info.baseUrl}/videos/hentai/${v.getString("slug")}",
                    sourceId = info.id
                ))
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getStreamLinks(pageUrl: String): List<StreamResult> = withContext(Dispatchers.IO) {
        val id = if (pageUrl.contains("?id=")) pageUrl.substringAfter("?id=").substringBefore("&") 
                 else if (pageUrl.startsWith("http")) "" // Need to fetch page to get ID or extract from slug
                 else pageUrl

        val apiUrl = "https://hanime.tv/api/v8/video?id=$id"
        val json = ScraperHttpClient.get(apiUrl) ?: return@withContext emptyList()
        
        val results = mutableListOf<StreamResult>()
        try {
            val obj = JSONObject(json)
            val servers = obj.getJSONObject("videos_manifest").getJSONArray("servers")
            for (i in 0 until servers.length()) {
                val server = servers.getJSONObject(i)
                val streams = server.getJSONArray("streams")
                for (j in 0 until streams.length()) {
                    val stream = streams.getJSONObject(j)
                    val streamUrl = stream.getString("url")
                    if (streamUrl.isNotEmpty()) {
                        results.add(StreamResult(
                            url = streamUrl,
                            quality = stream.getString("height") + "p",
                            format = "hls"
                        ))
                    }
                }
            }
        } catch (_: Exception) {}
        
        results
    }
}
