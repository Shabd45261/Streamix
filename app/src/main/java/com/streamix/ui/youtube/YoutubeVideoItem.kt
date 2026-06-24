package com.streamix.ui.youtube

data class YoutubeVideoItem(
    val id:           String,   // YouTube video ID
    val title:        String,
    val thumbnailUrl: String,
    val channelName:  String = "",
    val viewCount:    String = "",
    val duration:     String = "",
    val publishedAt:  String = ""
)
