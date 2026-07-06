package com.streamix.core.utils

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareLink(context: Context, title: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n\nWatch here: $url\n\nShared via Streamix")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
