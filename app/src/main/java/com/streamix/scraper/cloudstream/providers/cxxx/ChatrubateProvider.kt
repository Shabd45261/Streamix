package com.streamix.scraper.cloudstream.providers.cxxx

import com.fasterxml.jackson.annotation.JsonProperty
import com.streamix.scraper.cloudstream.*
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson

class ChatrubateProvider : MainAPI() {
    override var mainUrl = "https://chaturbate.com"
    override var name = "Chatrubate"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = listOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=m&limit=90" to "Male",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
        "/api/ts/roomlist/room-list/?genders=t&limit=90" to "Trans"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page == 1) 0 else 90 * (page - 1)
        val response = app.get("$mainUrl${request.data}&offset=$offset").text
        val parsed = parseJson(response, Response::class)
        
        val rooms = parsed.rooms.map { room ->
            newLiveSearchResponse(room.username, "$mainUrl/${room.username}", TvType.Live) {
                this.posterUrl = room.img
            }
        }
        return newHomePageResponse(HomePageList(request.name, rooms, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/api/ts/roomlist/room-list/?hashtags=$query&limit=90&offset=0").text
        val parsed = parseJson(response, Response::class)
        
        return parsed.rooms.map { room ->
            newLiveSearchResponse(room.username, "$mainUrl/${room.username}", TvType.Live) {
                this.posterUrl = room.img
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: "Chatrubate Room"
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
        val doc = app.get(data).document
        val script = doc.select("script").find { it.html().contains("window.initialRoomDossier") }
        val jsonStr = script?.html()?.substringAfter("window.initialRoomDossier = \"")?.substringBefore("\";")?.unescapeUnicode() ?: return false
        val m3u8Url = "\"hls_source\": \"(.*).m3u8\"".toRegex().find(jsonStr)?.groups?.get(1)?.value
        
        if (m3u8Url != null) {
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = "$m3u8Url.m3u8",
                    referer = data,
                    quality = 0,
                    type = ExtractorLinkType.M3U8
                )
            )
            return true
        }
        return false
    }

    data class Room(
        @JsonProperty("img") val img: String = "",
        @JsonProperty("username") val username: String = ""
    )

    data class Response(
        @JsonProperty("rooms") val rooms: List<Room> = emptyList()
    )

    private fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
        String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
    }
}
