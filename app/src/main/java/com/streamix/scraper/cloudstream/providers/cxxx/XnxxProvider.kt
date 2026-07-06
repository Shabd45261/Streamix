package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class XnxxProvider : MainAPI() {
    override var mainUrl = "https://www.xnxx.com"
    override var name = "XNXX"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val homePageListsResult = mutableListOf<HomePageList>()

        if (page == 1) { 
            val document = app.get(mainUrl).document
            val scriptElements = document.select("script:containsData(xv.cats.write_thumb_block_list)")

            if (scriptElements.isNotEmpty()) {
                val scriptContent = scriptElements.html()
                val regex = Regex("""xv\.cats\.write_thumb_block_list\s*\(\s*(\[(?:.|\n)*?\])\s*,\s*['"]home-cat-list['"]""")
                val matchResult = regex.find(scriptContent)
                
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    try {
                        val sections = listOf(
                            "Today's Selection" to "$mainUrl/todays-selection",
                            "Trending" to "$mainUrl/hits",
                            "Best" to "$mainUrl/best",
                            "Fresh" to "$mainUrl/fresh"
                        )
                        
                        sections.forEach { (title, url) ->
                            val videos = fetchSectionVideos(url)
                            if (videos.isNotEmpty()) homePageListsResult.add(HomePageList(title, videos))
                        }
                    } catch (_: Exception) { }
                }
            }
        }

        if (homePageListsResult.isEmpty()) {
            val videos = fetchSectionVideos("$mainUrl/todays-selection")
            if (videos.isNotEmpty()) homePageListsResult.add(HomePageList("Today's Selection", videos))
        }
        
        return HomePageResponse(homePageListsResult, false)
    }

    private suspend fun fetchSectionVideos(sectionUrl: String): List<SearchResponse> {
        return try {
            val document = app.get(sectionUrl).document 
            document.select("div.mozaique div.thumb-block").mapNotNull { it.toSearchResponse() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val titleElement = this.selectFirst(".thumb-under p a") ?: return null
        val rawTitle = titleElement.attr("title") 
        val title = Parser.unescapeEntities(rawTitle, false)

        val rawHref = titleElement.attr("href")
        val finalHref = fixUrl(rawHref)
        val posterUrl = fixUrlNull(this.selectFirst(".thumb img")?.attr("data-src")?.let { if (it.startsWith("//")) "https:$it" else it })
        
        return newMovieSearchResponse(title, finalHref, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? { 
        val searchUrl = "$mainUrl/search/$query"
        val videos = fetchSectionVideos(searchUrl) 
        return if (videos.isEmpty()) null else videos
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        val rawOgTitle = document.selectFirst("meta[property=og:title]")?.attr("content")
        val rawPageTitle = document.selectFirst(".video-title strong")?.text()
        val title = (rawOgTitle?.let { Parser.unescapeEntities(it, false) } ?: rawPageTitle?.let { Parser.unescapeEntities(it, false) }) ?: "Unknown Title"
        
        val poster = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val description = document.selectFirst("p.video-description")?.text()?.trim()?.let { Parser.unescapeEntities(it, false) }

        val tags = document.select(".metadata-row.video-tags a:not(#suggestion)")
            .map { Parser.unescapeEntities(it.text(), false).trim() } 
            .filter { it.isNotEmpty() }

        val scriptElements = document.select("script:containsData(html5player.setVideoHLS)")
        var hlsLink: String? = null

        if (scriptElements.isNotEmpty()) {
            val scriptContent = scriptElements.html()
            hlsLink = Regex("""html5player\.setVideoHLS\s*\(\s*['"](.*?)['"]\s*\)""").find(scriptContent)?.groupValues?.get(1)
        }
        
        val videoDataString = if (hlsLink != null) "hls:$hlsLink" else ""

        val relatedVideos = document.select("div.mozaique div.thumb-block").mapNotNull { it.toSearchResponse() }

        return newMovieLoadResponse(title, url, TvType.NSFW, videoDataString) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = relatedVideos
        }
    }

    override suspend fun loadLinks(
        data: String, 
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.startsWith("hls:")) {
            val videoStreamUrl = data.substringAfter("hls:")
            if (videoStreamUrl.isNotBlank()) {
                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = videoStreamUrl,
                        referer = mainUrl,
                        quality = 0,
                        type = ExtractorLinkType.M3U8,
                    )
                )
                return true
            }
        }
        return false
    }
}
