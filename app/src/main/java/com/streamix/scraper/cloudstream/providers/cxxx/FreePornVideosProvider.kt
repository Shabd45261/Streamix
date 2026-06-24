package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class FreePornVideosProvider : MainAPI() {
    override var mainUrl = "https://www.freepornvideos.xxx"
    override var name = "Free Porn Videos"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "most-popular/week" to "Most Popular",
        "networks/brazzers-com" to "Brazzers",
        "networks/bangbros" to "BangBros"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/$page/").document
        val home = document.select("#list_videos_common_videos_list_items > div.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true))
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("strong.title").text()
        val aTag = this.selectFirst("a") ?: return null
        val href = fixUrl(aTag.attr("href"))
        val img = aTag.selectFirst("img")
        val posterUrl = fixUrl(img?.attr("data-src") ?: img?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val slug = query.filter { it.isWhitespace() || it.isLetterOrDigit() }.trim().replace("\\s+".toRegex(), "-").lowercase()
        val document = app.get("$mainUrl/search/$slug/1/").document
        return document.select("#custom_list_videos_videos_list_search_result_items > div.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.headline > h1")?.text()?.trim() ?: return null
        val poster = fixUrl(document.selectFirst("meta[property='og:image']")?.attr("content") ?: "")

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = document.select("div.description em").text().trim()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("video source").forEach { res ->
            val srcUrl = fixUrl(res.attr("src"))
            callback.invoke(
                newExtractorLink(name, name, srcUrl, data, 0)
            )
        }
        return true
    }
}
