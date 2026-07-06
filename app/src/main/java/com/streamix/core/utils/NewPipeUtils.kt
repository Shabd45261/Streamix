package com.streamix.core.utils

import android.content.Context
import com.streamix.core.storage.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.Localization
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object NewPipeUtils {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val prefs = PreferencesManager(context)
        NewPipe.init(StreamixDownloader(client, prefs), Localization.DEFAULT)
        isInitialized = true
    }

    private class StreamixDownloader(
        private val client: OkHttpClient,
        private val prefs: PreferencesManager
    ) : Downloader() {
        companion object {
            const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }

        override fun execute(request: Request): Response {
            val httpMethod = request.httpMethod()
            val url = request.url()
            val headers = request.headers()
            val dataToSend = request.dataToSend()

            val requestBuilder = okhttp3.Request.Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")

            // Add YouTube cookies for ALL youtube-related domains
            if (url.contains("youtube.com") || url.contains("googlevideo.com") || url.contains("youtube-nocookie.com") || url.contains("youtu.be")) {
                val cookies = runBlocking { prefs.youtubeCookies.first() }
                if (!cookies.isNullOrBlank()) {
                    requestBuilder.addHeader("Cookie", cookies)
                    // Some feeds require these headers to match the cookies
                    requestBuilder.addHeader("x-youtube-client-name", "1")
                    requestBuilder.addHeader("x-youtube-client-version", "2.20240522.01.00")
                }
            }

            headers.forEach { (name, values) ->
                requestBuilder.removeHeader(name)
                values.forEach { value ->
                    requestBuilder.addHeader(name, value)
                }
            }

            val response = client.newCall(requestBuilder.build()).execute()
            
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBody = response.body?.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }
}
