package com.streamix.core.model

data class SponsorSegment(
    val start: Long, // in milliseconds
    val end: Long,   // in milliseconds
    val category: String
)
