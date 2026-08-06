package com.streamix.scrapers

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

object ScraperHttpClient {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val host = originalRequest.url.host
            
            val requestBuilder = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "max-age=0")
                .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Bit\";v=\"24\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                
            // Handle site-specific headers
            if (host.contains("pornhub.com")) {
                requestBuilder.header("Cookie", "age_verified=1")
            } else if (host.contains("xvideos.com")) {
                requestBuilder.header("Cookie", "hentai=1")
            } else if (host.contains("xhamster.com")) {
                requestBuilder.header("Cookie", "age_verified=1")
            } else if (host.contains("redgifs.com")) {
                requestBuilder.header("Origin", "https://www.redgifs.com")
            }
                
            chain.proceed(requestBuilder.build())
        }
        .build()

    fun get(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val request = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val requestBody = body.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).apply {
                headers.forEach { (k, v) -> header(k, v) }
            }.build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getWithResponse(url: String, headers: Map<String, String> = emptyMap()): Response? {
        return try {
            val request = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            client.newCall(request).execute()
        } catch (e: Exception) {
            null
        }
    }
}
