package com.streamix.scraper.cloudstream

import com.streamix.scraper.cloudstream.mvvm.logError
import com.streamix.scraper.cloudstream.utils.AppUtils.toJson
import kotlinx.serialization.Serializable
import okhttp3.Interceptor

@Serializable
enum class TvType {
    Movie,
    TvSeries,
    Anime,
    OVA,
    Live,
    NSFW,
    Others
}

enum class DubStatus {
    None,
    Dubbed,
    Subbed
}

enum class ExtractorLinkType {
    VIDEO,
    M3U8,
    DASH,
    TORRENT,
    MAGNET
}

enum class SearchQuality {
    Cam, CamRip, HdCam, Telesync, WorkPrint, Telecine, HQ, HD, HDR, BlueRay, DVD, SD, FourK, UHD, SDR, WebRip
}

enum class ProviderType {
    MetaProvider,
    DirectProvider,
}

enum class VPNStatus {
    None,
    MightBeNeeded,
    Torrent,
}

interface SearchResponse {
    val name: String
    val url: String
    val apiName: String
    var type: TvType?
    var posterUrl: String?
    var quality: String?
}

@Serializable
data class MovieSearchResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override var type: TvType? = null,
    override var posterUrl: String? = null,
    override var quality: String? = null
) : SearchResponse

@Serializable
data class TorrentSearchResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override var type: TvType? = null,
    override var posterUrl: String? = null,
    override var quality: String? = null
) : SearchResponse

@Serializable
data class LiveSearchResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override var type: TvType? = null,
    override var posterUrl: String? = null,
    override var quality: String? = null
) : SearchResponse

@Serializable
data class TvSeriesSearchResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override var type: TvType? = null,
    override var posterUrl: String? = null,
    override var quality: String? = null
) : SearchResponse

@Serializable
data class AnimeSearchResponse(
    override val name: String,
    override val url: String,
    override val apiName: String,
    override var type: TvType? = null,
    override var posterUrl: String? = null,
    override var quality: String? = null
) : SearchResponse

@Serializable
data class Episode(
    val data: String,
    val name: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val posterUrl: String? = null,
    val description: String? = null
)

@Serializable
data class Actor(val name: String, val image: String? = null)

@Serializable
data class ActorData(val actor: Actor, val roleString: String? = null)

@Serializable
data class TrailerData(
    val extractorUrl: String,
    val referer: String? = null,
    val raw: Boolean = false
)

@Serializable
data class LoadResponse(
    val name: String,
    val url: String,
    val apiName: String,
    val type: TvType,
    val dataUrl: String,
    var posterUrl: String? = null,
    var plot: String? = null,
    var tags: List<String>? = null,
    var duration: Int? = null,
    var recommendations: List<SearchResponse>? = null,
    var actors: List<ActorData>? = null,
    var trailers: MutableList<TrailerData> = mutableListOf()
)

data class HomePageList(
    val name: String,
    val list: List<SearchResponse>,
    val isHorizontalImages: Boolean = false
)

data class HomePageResponse(
    val items: List<HomePageList>,
    val hasNext: Boolean = false
)

data class SearchResponseList(
    val items: List<SearchResponse>,
    val hasNext: Boolean = false
)

data class MainPageRequest(
    val name: String,
    val data: String,
    val horizontalImages: Boolean = false
)

data class SubtitleFile(
    val lang: String,
    val url: String
)

data class ExtractorLink(
    val source: String,
    val name: String,
    val url: String,
    val referer: String,
    val quality: Int,
    val type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    val isM3u8: Boolean = type == ExtractorLinkType.M3U8
)

abstract class MainAPI {
    open var name: String = "NONE"
    open var mainUrl: String = "NONE"
    open var lang: String = "en"
    open val hasMainPage: Boolean = false
    open val hasQuickSearch: Boolean = false
    open val supportedTypes: Set<TvType> = setOf(TvType.Movie)
    open val vpnStatus: VPNStatus = VPNStatus.None
    open val providerType: ProviderType = ProviderType.DirectProvider

    open val mainPage: List<MainPageRequest> = emptyList()

    open suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        throw NotImplementedError()
    }

    open suspend fun search(query: String): List<SearchResponse>? {
        throw NotImplementedError()
    }

    open suspend fun search(query: String, page: Int): SearchResponseList? {
        val results = search(query) ?: return null
        return newSearchResponseList(results, false)
    }

    open suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    open suspend fun load(url: String): LoadResponse? {
        throw NotImplementedError()
    }

    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        throw NotImplementedError()
    }

    open fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor? = null

    fun fixUrl(url: String): String {
        if (url.startsWith("http") || url.startsWith("{") || url.startsWith("[")) return url
        if (url.isEmpty()) return ""
        if (url.startsWith("//")) return "https:$url"
        return if (url.startsWith("/")) "$mainUrl$url" else "$mainUrl/$url"
    }

    fun fixUrlNull(url: String?): String? = url?.let { fixUrl(it) }
}

// Helper builders to match Cloudstream's style
fun MainAPI.newMovieSearchResponse(name: String, url: String, type: TvType, fix: Boolean = true, initializer: MovieSearchResponse.() -> Unit = {}): MovieSearchResponse {
    return MovieSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type).apply(initializer)
}

fun MainAPI.newTvSeriesSearchResponse(name: String, url: String, type: TvType, fix: Boolean = true, initializer: TvSeriesSearchResponse.() -> Unit = {}): TvSeriesSearchResponse {
    return TvSeriesSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type).apply(initializer)
}

fun MainAPI.newAnimeSearchResponse(name: String, url: String, type: TvType, fix: Boolean = true, initializer: AnimeSearchResponse.() -> Unit = {}): AnimeSearchResponse {
    return AnimeSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type).apply(initializer)
}

fun MainAPI.newLiveSearchResponse(name: String, url: String, type: TvType, fix: Boolean = true, initializer: LiveSearchResponse.() -> Unit = {}): LiveSearchResponse {
    return LiveSearchResponse(name, if (fix) fixUrl(url) else url, this.name, type).apply(initializer)
}

fun MainAPI.newHomePageResponse(list: HomePageList, hasNext: Boolean? = null): HomePageResponse {
    return HomePageResponse(listOf(list), hasNext ?: list.list.isNotEmpty())
}

fun MainAPI.newSearchResponseList(list: List<SearchResponse>, hasNext: Boolean? = null): SearchResponseList {
    return SearchResponseList(list, hasNext ?: list.isNotEmpty())
}

fun MainAPI.newMovieLoadResponse(name: String, url: String, type: TvType, dataUrl: String, initializer: LoadResponse.() -> Unit = {}): LoadResponse {
    return LoadResponse(name, url, this.name, type, dataUrl).apply(initializer)
}

fun LoadResponse.addTrailer(url: String?, referer: String? = null, raw: Boolean = false) {
    if (url.isNullOrBlank()) return
    this.trailers.add(TrailerData(url, referer, raw))
}

fun MainAPI.newExtractorLink(
    source: String,
    name: String,
    url: String,
    referer: String,
    quality: Int,
    type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    initializer: ExtractorLink.() -> Unit = {}
): ExtractorLink {
    return ExtractorLink(source, name, url, referer, quality, type).apply(initializer)
}

fun MainAPI.mainPageOf(vararg pairs: Pair<String, String>): List<MainPageRequest> {
    return pairs.map { MainPageRequest(it.second, it.first, true) }
}
