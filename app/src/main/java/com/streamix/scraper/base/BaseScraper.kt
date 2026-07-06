package com.streamix.scraper.base

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.ui.shorts.ShortsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

abstract class BaseScraper {
    abstract val name: String
    abstract val mainUrl: String

    open val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Referer", mainUrl)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
            )
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    abstract suspend fun search(query: String): List<SearchResult>
    abstract suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink>
    open suspend fun getShorts(page: Int = 1): List<ShortsItem> = emptyList()

    protected suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val req = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().body?.string() ?: ""
        }
    }

    protected suspend fun post(url: String, body: Map<String, String>): String {
        val formBody = FormBody.Builder().apply {
            body.forEach { (k, v) -> add(k, v) }
        }.build()
        val req = Request.Builder().url(url).post(formBody).build()
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().body?.string() ?: ""
        }
    }

    protected suspend fun fetchDoc(url: String, referer: String? = null): Document {
        val headers = if (referer != null) mapOf("Referer" to referer) else emptyMap()
        return Jsoup.parse(get(url, headers), url)
    }

    protected suspend fun fetchHtml(url: String, referer: String? = null): String {
        val headers = if (referer != null) mapOf("Referer" to referer) else emptyMap()
        return get(url, headers)
    }
}
