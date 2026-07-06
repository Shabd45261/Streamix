package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element
import okhttp3.FormBody

class PornhoarderProvider : MainAPI() {
    override var mainUrl = "https://ww3.pornhoarder.org"
    override var name = "Pornhoarder"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    private val ajaxUrl = "$mainUrl/ajax_search.php"

    override val mainPage = mainPageOf(
        "Latest" to "Latest Videos",
        "Popular" to "Popular Videos",
        "/trending-videos/" to "Trending Videos",
        "/random-videos/" to "Random Videos"
    )

    private fun getRequestBody(query: String, isLatest: Boolean, page: Int): FormBody {
        return FormBody.Builder()
            .addEncoded("search", query)
            .addEncoded("sort", if (isLatest) "0" else "2")
            .addEncoded("date", "0")
            .addEncoded("servers[]", "40")
            .addEncoded("servers[]", "45")
            .addEncoded("servers[]", "12")
            .addEncoded("author", "0")
            .addEncoded("page", page.toString())
            .build()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return if (request.data == "Latest" || request.data == "Popular") {
            val body = getRequestBody("", request.data == "Latest", page)
            val document = app.post(ajaxUrl, requestBody = body).document
            val responseList = document.select(".video article").mapNotNull { it.toSearchResult() }
            newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)
        } else {
            val document = app.get("$mainUrl${request.data}?page=$page").document
            val responseList = document.select(".video article").mapNotNull { it.toSearchResult() }
            newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select(".video-content h1").text().replace("| PornHoarder.tv", "").trim()
        val a = this.selectFirst(".video-link") ?: return null
        val href = fixUrl(a.attr("href"))
        val posterUrl = this.selectFirst(".video-image.primary.b-lazy")?.attr("data-src")
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..2) {
            val requestBody = getRequestBody(query, true, i)
            val document = app.post(ajaxUrl, requestBody = requestBody).document
            val results = document.select(".video article").mapNotNull { it.toSearchResult() }
            searchResponse.addAll(results)
            if (results.isEmpty()) break
        }
        return searchResponse.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.replace("| PornHoarder.tv", "")?.trim() ?: "Video"
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val iframe = doc.select(".video-player iframe").attr("src")
        if (iframe.isNotEmpty()) {
            callback(newExtractorLink(name, name, fixUrl(iframe), data, 0))
            return true
        }
        return false
    }
}
