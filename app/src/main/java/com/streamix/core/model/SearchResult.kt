package com.streamix.core.model

data class SearchResult(
    val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val year: String = "",
    val duration: String = "",
    val views: String = "",
    val rating: String = "",
    val tags: List<String> = emptyList(),
    val performers: List<String> = emptyList(),
    val studio: String = "",
    val description: String = "",
    val progress: Long = 0L,
    val totalDuration: Long = 0L,
    val isShort: Boolean = false,
    val fallbackUrl: String? = null,
    val fallbackStudio: String? = null
)
