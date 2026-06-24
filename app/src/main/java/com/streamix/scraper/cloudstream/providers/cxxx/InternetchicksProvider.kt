package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class InternetchicksProvider : MainAPI() {
    override var mainUrl = "https://internetchicks.com"
    override var name = "InternetChicks"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "$mainUrl" to "Recent",
        "$mainUrl/most-viewed/" to "Most Viewed",
        "$mainUrl/top-rated/" to "Top Rated"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val items = document.select("article.post-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, items), true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.post-title a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("h3.post-title a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article.post-item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.post-title")?.text() ?: "Video"
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = document.selectFirst("div.post-content")?.text()
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false
        val videoUrl = app.get(iframe).document.selectFirst("video source")?.attr("src") ?: return false
        callback(newExtractorLink(name, name, fixUrl(videoUrl), data, 0))
        return true
    }
}
