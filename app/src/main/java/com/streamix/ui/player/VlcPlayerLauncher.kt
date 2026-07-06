package com.streamix.ui.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object VlcPlayerLauncher {

    fun launch(context: Context, videoUrl: String, title: String) {
        if (videoUrl.isEmpty()) {
            Toast.makeText(context, "Invalid video source", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(videoUrl)
        val mime = getMimeType(videoUrl)
        
        try {
            // First attempt: Strict VLC launch
            val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                setPackage("org.videolan.vlc")
                putExtra("title", title)
                // VLC specific extras
                putExtra("from_start", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(vlcIntent)
        } catch (e: ActivityNotFoundException) {
            // Second attempt: General chooser (maybe they have MPV, MX Player, etc.)
            try {
                val chooserIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Play with...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                
                // Also show a hint that VLC is recommended
                Toast.makeText(context, "VLC is recommended for best experience", Toast.LENGTH_SHORT).show()
            } catch (e2: Exception) {
                // Last resort: Go to Play Store for VLC
                Toast.makeText(context, "VLC Media Player required for playback", Toast.LENGTH_LONG).show()
                try {
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.videolan.vlc")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(marketIntent)
                } catch (anfe: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.videolan.vlc")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                }
            }
        }
    }

    private fun getMimeType(url: String) = when {
        url.contains(".m3u8") -> "application/x-mpegURL"
        url.contains(".mp4")  -> "video/mp4"
        url.contains(".mkv")  -> "video/x-matroska"
        url.contains("youtube.com") -> "video/*"
        else -> "video/*"
    }
}
