package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson
import com.fasterxml.jackson.annotation.JsonProperty

class ChaturbateProvider : MainAPI() {
    override var mainUrl = "https://chaturbate.com"
    override var name = "Chaturbate"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW, TvType.Live)

    override val mainPage = listOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
        "/api/ts/roomlist/room-list/?regions=AS&limit=90" to "Asia"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page == 1) 0 else 90 * (page - 1)
        val response = app.get("$mainUrl${request.data}&offset=$offset").text
        val parsed = parseJson(response, Response::class)
        
        val rooms = parsed.rooms
            .filter { it.gender != "s" && it.gender != "m" }
            .map { room ->
                newLiveSearchResponse(room.username, "$mainUrl/${room.username}", TvType.Live) {
                    this.posterUrl = room.img
                }
            }
        return newHomePageResponse(HomePageList(request.name, rooms, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/api/ts/roomlist/room-list/?keywords=$query&limit=90&offset=0", headers = mapOf("X-Requested-With" to "XMLHttpRequest")).text
        val parsed = parseJson(response, Response::class)
        
        return parsed.rooms
            .filter { it.gender != "s" && it.gender != "m" }
            .map { room ->
                newLiveSearchResponse(room.username, "$mainUrl/${room.username}", TvType.Live) {
                    this.posterUrl = room.img
                }
            }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: "Chaturbate Room"
        val poster = document.selectFirst("[property='og:image']")?.attr("content")
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newMovieLoadResponse(title, url, TvType.Live, url) {
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
        val username = data.split("/").last { it.isNotEmpty() }
        val apiUrl = "https://chaturbate.com/api/chatvideocontext/$username/"

        val response = app.get(
            apiUrl,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to data,
                "Accept" to "application/json"
            )
        ).text

        val parsedResponse = parseJson(response, ChatResponse::class)
        val m3u8Url = parsedResponse.hlsSource ?: return false

        callback.invoke(
            newExtractorLink(
                source = name,
                name = name,
                url = m3u8Url,
                referer = data,
                quality = 0,
                type = ExtractorLinkType.M3U8
            )
        )
        return true
    }

    data class ChatResponse(
        @JsonProperty("hls_source") val hlsSource: String? = null
    )

    data class Room(
        @JsonProperty("img") val img: String = "",
        @JsonProperty("username") val username: String = "",
        @JsonProperty("gender") val gender: String = ""
    )

    data class Response(
        @JsonProperty("rooms") val rooms: List<Room> = emptyList()
    )
}
