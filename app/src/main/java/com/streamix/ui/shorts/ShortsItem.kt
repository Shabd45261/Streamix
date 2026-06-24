package com.streamix.ui.shorts

data class ShortsItem(
    val id:           String,
    val title:        String,
    val thumbnailUrl: String,
    val streamUrl:    String,   // direct MP4 or HLS URL
    val channelName:  String = "",
    val description:  String? = null,
    val duration:     Int    = 0,   // seconds
    val views:        String = "",
    val likes:        String = "",
    val isLiked:      Boolean = false,
    val isDisliked:   Boolean = false,
    val source:       ShortsContext = ShortsContext.YOUTUBE
)

enum class ShortsContext { YOUTUBE, ADULT }
