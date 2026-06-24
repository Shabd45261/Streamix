package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class FapixProvider : MainAPI() {
    override var mainUrl = "https://fapix.porn"
    override var name = "Fapix"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "${mainUrl}/videos/latest" to "Latest Videos",
        "${mainUrl}/bro-sis" to "Step Sister",
        "${mainUrl}/mother-and-son" to "StepMom",
        "${mainUrl}/rus-porn-2025" to "Russian Porn"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val home = document.select("div.video.trailer").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true))
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a") ?: return null
        val href = fixUrl(anchor.attr("href"))
        val title = anchor.selectFirst("div.title")?.attr("title")?.trim()
            ?: anchor.attr("alt")?.trim()
            ?: return null
        val posterUrl = fixUrl(anchor.selectFirst("img")?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search?q=${query}").document
        return document.select("div.video.trailer").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1[itemprop=name]")?.ownText()?.trim() ?: return null

        val poster = fixUrl(
            document.selectFirst("video#player")?.attr("poster")
                ?: document.selectFirst("video#player")?.attr("data-poster") ?: ""
        )

        val tags = document.select("span.info-row a.button").map { it.text().trim() }.take(5)

        val actors = document.select("a.tag-modifier[itemprop=actor]").mapNotNull {
            val actorName = it.selectFirst("span[itemprop=name]")?.text()?.trim() ?: return@mapNotNull null
            val image = fixUrl(it.selectFirst("img")?.attr("src") ?: "")
            ActorData(Actor(actorName, image))
        }

        val recommendations = document.select("div.video.trailer").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.tags = tags
            this.actors = actors
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
        val sourceUrl = document.selectFirst("video#player source")?.attr("src") ?: return false

        val finalUrl = app.get(
            sourceUrl,
            headers = mapOf("Referer" to mainUrl),
            allowRedirects = false
        ).headers["Location"] ?: sourceUrl 

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = finalUrl,
                referer = mainUrl,
                quality = 0,
                type = if (finalUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
            )
        )
        return true
    }
}
