package com.streamix.scraper.cloudstream.providers.cxxx

import com.streamix.scraper.cloudstream.*
import org.jsoup.nodes.Element
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder

class HStreamProvider : MainAPI() {
    override var mainUrl = "https://hstream.moe"
    override var name = "HStream"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = listOf(
        "${mainUrl}/search?order=recently-uploaded&page=" to "Latest",
        "${mainUrl}/search?order=view-count&page=" to "Popular"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}$page").document
        val home = document.select("div.items-center div.w-full > a").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, home, isHorizontalImages = true), hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img")?.attr("alt") ?: return null
        val href = fixUrl(this.attr("href"))
        val posterUrl = fixUrl(this.select("img").attr("src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/search?search=$query&page=1").document
        return document.select("div.items-center div.w-full > a").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.relative h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")
        val genres = document.select("ul.list-none.text-center li a").map { it.text() }
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = genres
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data)
        val cookies = response.headers.values("Set-Cookie")
        val cookieHeader = cookies.joinToString("; ") { it.substringBefore(";") }
        val token = cookies.flatMap { it.split(";") }
            .find { it.trim().startsWith("XSRF-TOKEN=") }
            ?.substringAfter("XSRF-TOKEN=")
            ?.let { URLDecoder.decode(it, "utf-8") }
            ?: ""
        
        val document = response.document
        val episodeId = document.selectFirst("input#e_id")?.attr("value") ?: return false
        val body = """{"episode_id": "$episodeId"}""".toRequestBody("application/json".toMediaType())

        val headers = mapOf(
            "Referer" to data,
            "Origin" to mainUrl,
            "X-Requested-With" to "XMLHttpRequest",
            "X-XSRF-TOKEN" to token,
            "Cookie" to cookieHeader
        )

        val req = app.post("$mainUrl/player/api", headers = headers, requestBody = body).text
        val parsed = parseJson(req, PlayerApiResponse::class)
        
        val urlBase = (parsed.stream_domains.randomOrNull() ?: "") + "/" + parsed.stream_url
        val resolutions = listOfNotNull("720", "1080", if (parsed.resolution == "4k") "2160" else null)
        
        resolutions.forEach { resolution ->
            val videoUrl = urlBase + getVideoUrlPath(parsed.legacy != 0, resolution)
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = videoUrl,
                    referer = data,
                    quality = resolution.toIntOrNull() ?: 0,
                    type = if (videoUrl.endsWith(".mpd")) ExtractorLinkType.DASH else ExtractorLinkType.VIDEO
                )
            )
        }
        return true
    }

    private fun getVideoUrlPath(isLegacy: Boolean, resolution: String): String {
        return if (isLegacy) {
            if (resolution == "720") "/x264.720p.mp4" else "/av1.$resolution.webm"
        } else {
            "/$resolution/manifest.mpd"
        }
    }

    data class PlayerApiResponse(
        val legacy: Int = 0,
        val resolution: String = "4k",
        val stream_url: String,
        val stream_domains: List<String>,
    )
}
