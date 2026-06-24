package com.streamix.core.model

data class VideoLink(
    val url: String,
    val quality: String,       // "1080p", "720p", "HLS", "MP4", "Auto"
    val server: String,        // "Moviebox", "Moviebox IN", "RedTube"
    val isM3u8: Boolean = url.contains(".m3u8"),
    val subtitleUrl: String? = null
)
