package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

class HentaiCityProvider : MainAPI() {
    override var name = "HentaiCity"
    override var mainUrl = "https://www.hentaicity.com"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "" to "New Hentai Releases"
    ).map { MainPageRequest(it.second, it.first, true) }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a.thumb-img")
        val titleElement = this.selectFirst("p > a.video-title")

        val href = fixUrl(linkElement?.attr("href") ?: return null)
        val title = titleElement?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: titleElement?.attr("title")?.trim()?.takeIf { it.isNotBlank() }
            ?: this.selectFirst("a.video-title")?.attr("title")?.trim()
            ?: "Video"

        val posterElement = linkElement.selectFirst("img.thumbtrailer__image") ?: this.selectFirst("img[src]")
        val posterUrl = fixUrl(posterElement?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("div.new-releases > div.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList("New Releases", items, isHorizontalImages = true))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/video/$query"
        val document = app.get(searchUrl).document
        return document.select("section.content > div.thumb-list div.outer-item > div.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = fixUrl(document.selectFirst("div#playerz video")?.attr("poster")
            ?: document.selectFirst("meta[property=og:image]")?.attr("content") ?: "")
        val synopsis = document.selectFirst("div.detail-box > div.ubox-text")?.html()?.replace("<br>", "\n")?.let { Jsoup.parse(it).text() }
                       ?: document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
        val tags = document.select("div#taglink a:not(:has(svg))").map { it.text().trim() }
        
        val recommendations = document.select("div#related_videos div.outer-item > div.item").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = synopsis
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
        var found = false
        
        document.selectFirst("div#playerz video source[type='application/x-mpegURL']")?.attr("src")?.let { hlsUrl ->
            callback.invoke(newExtractorLink(name, name, fixUrl(hlsUrl), mainUrl, 0, ExtractorLinkType.M3U8))
            found = true
        }
        
        document.selectFirst("meta[property='og:video:url']")?.attr("content")?.let { mp4Url ->
            callback.invoke(newExtractorLink(name, name, fixUrl(mp4Url), mainUrl, 0, ExtractorLinkType.VIDEO))
            found = true
        }

        if (!found) {
            document.select("script").forEach { script ->
                if (script.data().contains("fluidPlayer(")) {
                    val hlsRegex = Regex("""source\s*:\s*["']([^"']+\.m3u8[^"']*)["']""")
                    hlsRegex.find(script.data())?.groupValues?.get(1)?.let { extractedHlsUrl ->
                        callback.invoke(newExtractorLink(name, name, fixUrl(extractedHlsUrl), mainUrl, 0, ExtractorLinkType.M3U8))
                        found = true
                    }
                }
            }
        }
        return found
    }
}
