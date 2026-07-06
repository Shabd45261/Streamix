package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class LiveCamRipsProvider : MainAPI() {
    override var mainUrl = "https://livecamrips.to"
    override var name = "LiveCamRips"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "${mainUrl}/tag/18" to "18",
        "${mainUrl}/tag/petite" to "Petite",
        "${mainUrl}/tag/cute" to "Cute",
        "${mainUrl}/tag/couple" to "Couple",
        "${mainUrl}/tag/goth" to "Goth",
        "${mainUrl}/tag/elegant" to "Elegant",
        "${mainUrl}/tag/milf" to "Milf",
        "${mainUrl}/tag/shy" to "Shy",
        "${mainUrl}/tag/latina" to "Latina"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}/$page"
        val response = app.get(url, headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "${mainUrl}/"
        ))

        val document = response.document
        val items = document.select("div.col-xl-3").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("span.tm-text-gray-light")?.text() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst("img.img-fluid")?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "${mainUrl}/search/$query"
        val document = app.get(url, headers = mapOf("Referer" to "${mainUrl}/")).document
        return document.select("div.tm-gallery div.col-12").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = mapOf(
            "Referer" to url,
            "User-Agent" to USER_AGENT
        )).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val plot = document.selectFirst("div.video-caption p")?.text()
        val actors = document.select("span.valor a").map { ActorData(Actor(it.text())) }
        val tags = document.select("div.video-caption a").map { it.text() }
        
        val isModelPage = url.contains("/model/")
        
        // If it's a model page, it has multiple videos. For now, let's treat it as a load response with recommendations.
        val recommendations = document.select("div.col-xl-3, div.col-12").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.plot = plot
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
        val document = app.get(data, headers = mapOf(
            "Referer" to data,
            "User-Agent" to USER_AGENT
        )).document

        val iframe = document.selectFirst("iframe.embed-responsive-item")?.attr("src") ?: return false
        
        // Handle iframe if it's a direct video or common host
        if (iframe.contains("mixdrop")) {
             // For now just try to load as direct link if possible, or use the iframe
             callback.invoke(
                newExtractorLink(
                    source = name,
                    name = "MixDrop",
                    url = iframe,
                    referer = data,
                    quality = 0,
                    type = ExtractorLinkType.VIDEO
                )
             )
        } else if (iframe.contains("abstream.to")) {
            val html = app.get(iframe, referer = data).text
            val vidstackRegex = Regex("""file:"([^"]*)"""")
            val videoUrl = vidstackRegex.find(html)?.groupValues?.get(1)
            if (videoUrl != null) {
                callback.invoke(
                    newExtractorLink(
                        source = "Abstream",
                        name = "Abstream",
                        url = videoUrl,
                        referer = iframe,
                        quality = 0,
                        type = ExtractorLinkType.M3U8
                    )
                )
            }
        } else {
             callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = iframe,
                    referer = data,
                    quality = 0,
                    type = if (iframe.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                )
             )
        }

        return true
    }
}
