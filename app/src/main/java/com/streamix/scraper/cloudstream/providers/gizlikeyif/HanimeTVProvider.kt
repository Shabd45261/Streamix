package com.streamix.scraper.cloudstream.providers.gizlikeyif

import com.streamix.scraper.cloudstream.*
import com.streamix.scraper.cloudstream.utils.AppUtils.parseJson
import com.fasterxml.jackson.annotation.JsonProperty
import java.security.MessageDigest

class HanimeTVProvider : MainAPI() {
    override var mainUrl = "https://hanime.tv"
    override var name = "HanimeTV"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    companion object {
        private const val FAHApi = "https://cached.freeanimehentai.net"
    }

    override val mainPage = listOf(
        "trending" to "Trending",
        "new-release" to "New Releases"
    ).map { MainPageRequest(it.second, it.first, true) }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/browse/tags/${request.data}?page=$page"
        val html = app.get(url).text
        val videoRegex = """\{id:\d+,name:"([^"]*)",slug:"([^"]*)",.*?poster_url:"([^"]*?)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val matches = videoRegex.findAll(html).toList()

        val results = matches.mapNotNull { match ->
            val videoName = match.groupValues[1]
            val slug = match.groupValues[2]
            val posterUrl = match.groupValues[3].replace("\\u002F", "/")
            newMovieSearchResponse(videoName, "$mainUrl/videos/hentai/$slug", TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
        return newHomePageResponse(HomePageList(request.name, results, isHorizontalImages = true))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("https://cached.freeanimehentai.net/api/v10/search_hvs", headers = mapOf("Origin" to mainUrl)).text
        val videos = parseJson(response, Array<HvsVideo>::class).toList()
        
        return videos.filter { it.name.contains(query, ignoreCase = true) }.map { v ->
            newMovieSearchResponse(v.name, "$mainUrl/videos/hentai/${v.slug}", TvType.NSFW) {
                this.posterUrl = v.posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val slug = url.substringAfterLast("/")
        val time = (System.currentTimeMillis() / 1000).toString()
        val signature = signatureCek(time)
        val headers = mapOf(
            "X-Time" to time,
            "X-Signature" to signature,
            "Origin" to mainUrl
        )

        val response = app.get("$FAHApi/api/v8/video?id=$slug&", headers = headers).text
        val apiResponse = parseJson(response, VideoApiResponse::class)
        val video = apiResponse.hanimeVideo

        return newMovieLoadResponse(video.name, url, TvType.NSFW, video.id.toString()) {
            this.posterUrl = video.posterUrl
            this.plot = video.description
            this.tags = video.hanimeTags?.map { it.text }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val time = (System.currentTimeMillis() / 1000).toString()
        val signature = signatureCek(time)
        val headers = mapOf("X-Time" to time, "X-Signature" to signature, "Origin" to mainUrl)

        val response = app.get("$FAHApi/api/v8/guest/videos/$data/manifest", headers = headers).text
        val manifestResponse = parseJson(response, VideoManifestResponse::class)

        manifestResponse.videosManifest.servers.forEach { server ->
            server.streams.forEach { stream ->
                callback.invoke(
                    newExtractorLink(
                        source = "HanimeTV - ${server.name}",
                        name = "Hanime",
                        url = stream.url,
                        referer = "$mainUrl/",
                        quality = stream.height.toIntOrNull() ?: 0,
                        type = ExtractorLinkType.M3U8
                    )
                )
            }
        }
        return true
    }

    private fun signatureCek(time: String): String {
        val message = "$time,Xkdi29,$mainUrl,mn2,$time"
        val bytes = MessageDigest.getInstance("SHA-256").digest(message.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    data class HvsVideo(
        @JsonProperty("name") val name: String,
        @JsonProperty("slug") val slug: String,
        @JsonProperty("poster_url") val posterUrl: String
    )

    data class VideoApiResponse(
        @JsonProperty("hentai_video") val hanimeVideo: HanimeVideo
    )

    data class HanimeVideo(
        @JsonProperty("id") val id: Int,
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("poster_url") val posterUrl: String? = null,
        @JsonProperty("hentai_tags") val hanimeTags: List<HanimeTag>? = null
    )

    data class HanimeTag(@JsonProperty("text") val text: String)

    data class VideoManifestResponse(
        @JsonProperty("videos_manifest") val videosManifest: VideosManifest
    )

    data class VideosManifest(
        @JsonProperty("servers") val servers: List<VideoServer>
    )

    data class VideoServer(
        @JsonProperty("name") val name: String,
        @JsonProperty("streams") val streams: List<VideoStream>
    )

    data class VideoStream(
        @JsonProperty("url") val url: String,
        @JsonProperty("height") val height: String
    )
}
