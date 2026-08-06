package com.streamix.core.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionStoreRepository @Inject constructor(
    private val repoManager: RepoManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches all extensions from all configured repos.
     * Returns a merged list plus the most-recent update date string.
     */
    suspend fun fetchExtensions(): Result<Pair<List<RemoteExtension>, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val allExtensions = mutableListOf<RemoteExtension>()
            var latestDate = "Unknown"
            repoManager.getRepos().forEach { repo ->
                val response = try {
                    client.newCall(Request.Builder().url(repo.url).build()).execute()
                } catch (e: Exception) {
                    null
                }
                
                if (response?.isSuccessful == true) {
                    val body = response.body?.string() ?: return@forEach
                    val (exts, date) = parseIndexJson(body)
                    allExtensions += exts
                    latestDate = date
                }
            }
            
            // Add Adult extensions from Stash Community Scrapers (Fetched from GitHub)
            try {
                val stashRequest = Request.Builder()
                    .url("https://api.github.com/repos/stashapp/CommunityScrapers/contents/scrapers")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val stashResponse = client.newCall(stashRequest).execute()
                if (stashResponse.isSuccessful) {
                    val stashBody = stashResponse.body?.string() ?: ""
                    val stashArray = org.json.JSONArray(stashBody)
                    for (i in 0 until stashArray.length()) {
                        val item = stashArray.getJSONObject(i)
                        if (item.getString("type") == "dir") {
                            val name = item.getString("name")
                            allExtensions += RemoteExtension(
                                id = "stash-adult-$name",
                                name = "$name (Stash)",
                                version = "1.0.0",
                                categories = listOf(ContentCategory.ADULT),
                                apkUrl = "mock://stash/$name",
                                manifestUrl = "mock://stash/$name/manifest",
                                iconUrl = "", // Could try to find a generic icon or site favicon
                                author = "Stash Community",
                                changelog = "Imported from Stash Community Scrapers"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to some hardcoded ones if GitHub fails
                allExtensions += listOf(
                    RemoteExtension(
                        id = "stash-adult-xvideos",
                        name = "XVideos (Stash)",
                        version = "1.0.0",
                        categories = listOf(ContentCategory.ADULT),
                        apkUrl = "mock://stash/xvideos",
                        manifestUrl = "mock://stash/xvideos/manifest",
                        iconUrl = "https://www.xvideos.com/img/xvideos.com.png",
                        author = "Stash Community",
                        changelog = "Initial release from Stash scrapers"
                    )
                )
            }

            Pair(allExtensions.distinctBy { it.id }, latestDate)
        }
    }

    private fun parseIndexJson(json: String): Pair<List<RemoteExtension>, String> {
        val root = org.json.JSONObject(json)
        val date = root.optString("lastUpdated", "Unknown")
        val arr  = root.getJSONArray("extensions")
        val list = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            RemoteExtension(
                id          = obj.getString("id"),
                name        = obj.getString("name"),
                version     = obj.getString("version"),
                categories  = obj.getJSONArray("categories").let { a ->
                    (0 until a.length()).map { i -> ContentCategory.valueOf(a.getString(i)) }
                },
                apkUrl      = obj.getString("apkUrl"),
                manifestUrl = obj.getString("manifestUrl"),
                iconUrl     = obj.optString("iconUrl", ""),
                author      = obj.optString("author", "community"),
                changelog   = obj.optString("changelog", "")
            )
        }
        return Pair(list, date)
    }
}
