package com.streamix.core.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object DownloadUtils {
    fun downloadVideo(context: Context, url: String, title: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading video from Streamix")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$title.mp4")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
