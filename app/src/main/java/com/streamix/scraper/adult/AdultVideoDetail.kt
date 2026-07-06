package com.streamix.scraper.adult

data class AdultVideoDetail(
    val pageUrl:      String,
    val title:        String,
    val posterUrl:    String = "",
    val description:  String = "",
    val tags:         List<String> = emptyList(),
    val performers:   List<String> = emptyList(),
    val studio:       String = "",
    val date:         String = "",
    val durationSecs: Int    = 0,
    val views:        String = "",
    val rating:       String = ""
)
