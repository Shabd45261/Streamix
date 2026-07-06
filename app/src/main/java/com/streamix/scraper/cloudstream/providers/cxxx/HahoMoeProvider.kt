package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class HahoMoeProvider : MainAPI() {
    override var mainUrl = "https://hahomoe.com"
    override var name = "HahoMoe"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "" to "Latest Updates",
        "popular" to "Popular"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.isEmpty()) "$mainUrl/page/$page" else "$mainUrl/${request.data}/page/$page"
        val document = app.get(url).document
        val home = document.select("div.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true))
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = aTag.attr("title").trim()
        val href = fixUrl(aTag.attr("href"))
        val img = aTag.selectFirst("img")
        val posterUrl = fixUrl(img?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("div.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster = fixUrl(document.selectFirst("meta[property='og:image']")?.attr("content") ?: "")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = document.select("div.entry-content p").text().trim()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false
        
        callback.invoke(
            newExtractorLink(name, name, iframe, data, 0)
        )
        return true
    }
}
