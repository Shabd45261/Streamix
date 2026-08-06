package com.streamix.core.utils

import android.content.Context
import android.content.Intent
import java.net.URLEncoder

object ShareUtils {
    private const val GATEWAY_URL = "https://shabd45261.github.io/Streamix.github.io/"

    fun shareLink(context: Context, title: String, url: String, timestamp: Long = 0L) {
        // Legacy support or fallback
        val finalUrl = if (timestamp > 0) {
            if (url.contains("?")) "$url&t=${timestamp/1000}" else "$url?t=${timestamp/1000}"
        } else {
            url
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\nWatch here: $finalUrl\n\nShared via Streamix")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    fun shareContent(
        context: Context,
        title: String,
        profile: String,
        contentType: String,
        id: String,
        timestamp: Long = 0L,
        season: Int? = null,
        episode: Int? = null
    ) {
        val t = timestamp / 1000
        val encodedId = URLEncoder.encode(id, "UTF-8")
        
        val urlBuilder = StringBuilder(GATEWAY_URL)
            .append("?profile=").append(profile)
            .append("&type=").append(contentType)
            .append("&id=").append(encodedId)
        
        if (t > 0) urlBuilder.append("&t=").append(t)
        if (season != null) urlBuilder.append("&season=").append(season)
        if (episode != null) urlBuilder.append("&episode=").append(episode)

        val finalUrl = urlBuilder.toString()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\nWatch here: $finalUrl\n\nShared via Streamix")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
