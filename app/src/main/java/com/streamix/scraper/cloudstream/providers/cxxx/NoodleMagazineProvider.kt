package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import com.streamix.scraper.cloudstream.utils.mapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jsoup.nodes.Element

class NoodleMagazineProvider : MainAPI() {
    override var mainUrl = "https://noodlemagazine.com"
    override var name = "NoodleMagazine"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val pages = mutableListOf<HomePageList>()

        if (page <= 1) {
            try {
                val tagsDoc = app.get("$mainUrl/video/a").document
                tagsDoc.select("div.tags-scroll a").take(5).forEach { tag ->
                    val tagTitle = tag.text()
                    val tagHref = fixUrl(tag.attr("href"))
                    val items = app.get(tagHref).document.select("div.item").mapNotNull { it.toRes() }
                    if (items.isNotEmpty()) pages.add(HomePageList(tagTitle, items, isHorizontalImages = true))
                }
            } catch (e: Exception) { }
        }

        if (pages.isEmpty() || page > 1) {
            val latestUrl = if (page > 1) "$mainUrl/video/?p=${page - 1}" else "$mainUrl/video/"
            val latestItems = app.get(latestUrl).document.select("div.item").mapNotNull { it.toRes() }
            pages.add(HomePageList("Latest Videos", latestItems, isHorizontalImages = true))
        }

        return HomePageResponse(pages, true)
    }

    private fun Element.toRes(): SearchResponse? {
        val a = this.selectFirst("a") ?: return null
        val title = this.selectFirst("div.title, div.title a")?.text()
            ?: this.selectFirst("img")?.attr("alt")
            ?: return null

        val img = this.selectFirst("img")
        val poster = fixUrlNull(img?.attr("data-src")?.ifBlank { null } ?: img?.attr("src"))
        val href = fixUrl(a.attr("href"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val q = query.trim().replace(" ", "-")
        val url = "$mainUrl/video/$q"
        return app.get(url).document.select("div.item").mapNotNull { it.toRes() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: return null
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            posterUrl = fixUrlNull(doc.selectFirst("meta[property=og:image]")?.attr("content"))
            plot = doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            recommendations = doc.select("div.item").mapNotNull { it.toRes() }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val text = app.get(data).text

        val jsonString = Regex(
            """window\.playlist\s*=\s*(\{.*?\});""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).find(text)?.groupValues?.get(1) ?: return false

        val playlistData = try {
            mapper.readValue<PlaylistData>(jsonString)
        } catch (e: Exception) {
            return false
        }

        playlistData.sources.forEach { s ->
            val quality = s.label.filter { it.isDigit() }.toIntOrNull() ?: 0
            callback(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = s.file,
                    referer = mainUrl,
                    quality = quality
                )
            )
        }
        return true
    }

    data class PlaylistData(val sources: List<VideoSource> = emptyList())
    data class VideoSource(val file: String = "", val label: String = "")
}
