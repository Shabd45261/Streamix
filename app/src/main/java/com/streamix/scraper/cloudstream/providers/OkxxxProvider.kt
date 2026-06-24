package com.streamix.scraper.cloudstream.providers

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element

class OkxxxProvider : MainAPI() {
    override var mainUrl = "https://okxxx1.com"
    override var name = "Ok.xxx"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = listOf(
        "$mainUrl" to "Latest Videos",
        "$mainUrl/trending" to "Trending Videos",
        "$mainUrl/popular" to "Popular Videos"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/$page/").document
        val home = document.select("div.item, div.video-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            )
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val aTag = this.selectFirst("a") ?: return null
        val title = aTag.attr("title").trim().ifEmpty { 
            this.selectFirst(".title, .name")?.text()?.trim() ?: ""
        }.ifEmpty { null } ?: return null

        val href = fixUrl(aTag.attr("href"))
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("data-src")
            ?: this.selectFirst("img")?.attr("data-original")
            ?: this.selectFirst("img")?.attr("src") ?: "")

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/$query/1/"
        val document = app.get(url).document
        return document.select("div.item, div.video-item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: return null
        val poster = fixUrl(document.selectFirst("meta[property=og:image]")?.attr("content") ?: "")

        val plot = document.select("div.desc")
            .map { it.text().trim() }
            .firstOrNull { it.startsWith("Description:", ignoreCase = true) }
            ?.removePrefix("Description:")?.trim()

        val tags = document.select("ul.video-tags li a, .tags a").map { it.text().trim() }

        val recommendations = document.select("div.item, div.video-item").mapNotNull { it.toSearchResult() }

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
        val source = document.selectFirst("video source") ?: return false
        val initialUrl = fixUrl(source.attr("src"))

        if (initialUrl.isNotBlank()) {
            try {
                val redirectedM3u = app.get(initialUrl).url
                val playlist = app.get(redirectedM3u).text

                val regex = Regex("""#EXT-X-STREAM-INF:.*RESOLUTION=\d+x(\d+).*?\n(https[^\n]+)""")
                val matches = regex.findAll(playlist).toList()
                
                if (matches.isNotEmpty()) {
                    matches.forEach {
                        val quality = it.groupValues[1].toIntOrNull() ?: 0
                        val streamUrl = it.groupValues[2]

                        callback.invoke(
                            newExtractorLink(
                                source = name,
                                name = name,
                                url = streamUrl,
                                referer = data,
                                quality = quality,
                                type = ExtractorLinkType.M3U8
                            )
                        )
                    }
                } else if (playlist.contains("#EXTM3U")) {
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = name,
                            url = redirectedM3u,
                            referer = data,
                            quality = 0,
                            type = ExtractorLinkType.M3U8
                        )
                    )
                }
            } catch (e: Exception) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = initialUrl,
                        referer = data,
                        quality = 0,
                        type = if (initialUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    )
                )
            }
        }

        return true
    }
}
