package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class CollectionOfBestPornProvider : MainAPI() {
    override var mainUrl = "https://collectionofbestporn.com"
    override var name = "CollectionOfBestPorn"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "${mainUrl}/most-recent" to "All Videos",
        "${mainUrl}/most-viewed/month" to "Most Viewed",
        "${mainUrl}/category/big-ass" to "Big Ass",
        "${mainUrl}/category/big-tits" to "Big Tits",
        "${mainUrl}/category/latin" to "Latin",
        "${mainUrl}/category/family" to "Family",
        "${mainUrl}/category/lingerie" to "Lingerie",
        "${mainUrl}/category/milf" to "Milf",
        "${mainUrl}/category/asian" to "Asian"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("div.video-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("div.video-thumb a") ?: return null
        val url = fixUrl(anchor.attr("href"))

        val img = anchor.selectFirst("img")
        val posterUrl = fixUrl(img?.attr("src") ?: "")

        val title = this.selectFirst("div.video-desc div.title span")?.text()
            ?: img?.attr("alt")
            ?: return null

        return newMovieSearchResponse(title, "$url|$posterUrl", TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search/${query}/page/1").document
        return document.select("div.video-item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(data: String): LoadResponse? {
        val parts = data.split("|")
        val url = parts[0]
        val incomingPoster = parts.getOrNull(1)

        val document = app.get(url).document
        val title = document.selectFirst("h1.video-title")?.text()?.trim() ?: return null

        val plot = document.selectFirst("h1.video-title")?.text()?.trim()
        val tags = document.select("div.tags ul.item-list li").map { it.text() }
        val recommendations = document.select("div.video-item").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = incomingPoster
            this.plot = plot
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val sources = document.select("video source")
        
        sources.forEach { source ->
            val videoUrl = source.attr("src")
            val qualityStr = source.attr("res") 
            val label = source.attr("label")
            
            if (videoUrl.isNotEmpty()) {
                val quality = when (qualityStr) {
                    "360" -> 360
                    "480" -> 480
                    "720" -> 720
                    "1080" -> 1080
                    else -> label.filter { it.isDigit() }.toIntOrNull() ?: 0
                }
                
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = videoUrl,
                        referer = mainUrl,
                        quality = quality,
                        type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    )
                )
            }
        }

        return sources.isNotEmpty()
    }
}
