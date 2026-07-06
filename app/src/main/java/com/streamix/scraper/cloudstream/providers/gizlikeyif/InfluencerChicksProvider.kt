package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class InfluencerChicksProvider : MainAPI() {
    override var mainUrl = "https://influencerchicks.com"
    override var name = "InfluencerChicks"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "${mainUrl}/category/youtube-3" to "Youtuber",
        "${mainUrl}/category/celebrity-2" to "Celebrity",
        "${mainUrl}/category/twitch-1" to "Twitch",
        "${mainUrl}/category/instagram-4" to "Instagram",
        "${mainUrl}/category/patreon-2" to "Patreon"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/page/$page").document
        val home = document.select("ul.g1-collection-items li.g1-collection-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrl(this.selectFirst("article .entry-featured-media a")?.attr("href") ?: return null)
        val title = this.selectFirst("article .entry-title a")?.text()?.trim() ?: return null

        val posterImg = this.selectFirst("article .entry-featured-media img")
        val posterUrl = fixUrl(posterImg?.attr("data-src") ?: posterImg?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document
        return document.select("ul.g1-collection-items li.g1-collection-item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null

        val poster = fixUrl(document.selectFirst("video")?.attr("poster")
            ?: document.selectFirst("div.g1-content-narrow img")?.let { img ->
                img.attr("data-src").takeIf { it.isNotBlank() } ?: img.attr("src")
            } ?: "")

        val plot = document.selectFirst("div.g1-content-narrow p")?.text()?.trim()
        val tags = document.select("p.entry-tags a").map { it.text().trim() }.take(5)
        val recommendations = document.select("a.g1-frame").mapNotNull { 
            val href = fixUrl(it.attr("href"))
            val rTitle = it.attr("title").takeIf { it.isNotBlank() } ?: it.selectFirst("h3.entry-title a")?.text()?.trim() ?: ""
            val img = it.selectFirst("img")
            val rPoster = fixUrl(img?.attr("data-src") ?: img?.attr("src") ?: "")
            newMovieSearchResponse(rTitle, href, TvType.NSFW) { this.posterUrl = rPoster }
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
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
        val video = document.selectFirst("video")
        val videoUrl = video?.attr("src")?.takeIf { it.isNotBlank() }
            ?: video?.selectFirst("source")?.attr("src")?.takeIf { it.isNotBlank() }
            ?: return false

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = videoUrl,
                referer = mainUrl,
                quality = 0,
                type = ExtractorLinkType.VIDEO
            )
        )
        return true
    }
}
