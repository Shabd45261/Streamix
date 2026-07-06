package com.streamix.scraper.cloudstream

import com.streamix.scraper.cloudstream.mvvm.logError
import com.streamix.scraper.cloudstream.utils.AppUtils.toJson
import kotlinx.serialization.Serializable
import okhttp3.Interceptor
import android.util.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.streamix.scraper.cloudstream.utils.mapper
import com.fasterxml.jackson.databind.JsonNode
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
enum class TvType {
    Movie,
    TvSeries,
    Anime,
    OVA,
    Live,
    NSFW,
    Others,
    AsianDrama,
    Cartoon,
    Torrent,
    Documentary,
    Music,
    AudioBook,
    CustomMedia,
    Audio,
    Podcast,
    Video
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

val INFER_TYPE = ExtractorLinkType.VIDEO

enum class SearchQuality {
    Cam, CamRip, HdCam, Telesync, WorkPrint, Telecine, HQ, HD, HDR, BlueRay, DVD, SD, FourK, UHD, SDR, WebRip
}

enum class Qualities(val value: Int) {
    P2160(2160),
    P1440(1440),
    P1080(1080),
    P720(720),
    P480(480),
    P360(360),
    P240(240),
    Unknown(0)
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

class ErrorLoadingException(message: String? = null) : Exception(message)

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
    override var quality: String? = null,
    var score: Int? = null
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
    var data: String,
    var name: String? = null,
    var season: Int? = null,
    var episode: Int? = null,
    var posterUrl: String? = null,
    var description: String? = null,
    var runTime: Int? = null,
    var date: String? = null
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
    var backgroundUrl: String? = null,
    var backgroundPosterUrl: String? = null,
    var plot: String? = null,
    var tags: List<String>? = null,
    var duration: Int? = null,
    var recommendations: List<SearchResponse>? = null,
    var actors: List<ActorData>? = null,
    var trailers: MutableList<TrailerData> = mutableListOf(),
    var year: Int? = null,
    var score: Int? = null,
    var logoUrl: String? = null
)

data class HomePageList(
    val name: String,
    var list: List<SearchResponse>,
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
    val url: String,
    val headers: Map<String, String>? = null
)

data class ExtractorLink(
    val source: String,
    val name: String,
    val url: String,
    val referer: String,
    var quality: Int,
    val type: ExtractorLinkType = ExtractorLinkType.VIDEO,
    val isM3u8: Boolean = type == ExtractorLinkType.M3U8,
    var headers: Map<String, String> = emptyMap()
)

object Score {
    fun from10(score: String?): Int? {
        return score?.toDoubleOrNull()?.times(10)?.toInt()
    }
}

abstract class MainAPI {
    open var name: String = "NONE"
    open var mainUrl: String = "NONE"
    open var lang: String = "en"
    open val hasMainPage: Boolean = false
    open val hasQuickSearch: Boolean = false
    open val supportedTypes: Set<TvType> = setOf(TvType.Movie)
    open val vpnStatus: VPNStatus = VPNStatus.None
    open val providerType: ProviderType = ProviderType.DirectProvider
    open val hasDownloadSupport: Boolean = false

    open val mainPage: List<MainPageRequest> = emptyList()

    open suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        return null
    }

    open suspend fun search(query: String): List<SearchResponse>? {
        return null
    }

    open suspend fun search(query: String, page: Int): SearchResponseList? {
        val results = search(query) ?: return null
        return newSearchResponseList(results, false)
    }

    open suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    open suspend fun load(url: String): LoadResponse? {
        return null
    }

    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
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

// Extension helpers
suspend fun <A, B> Iterable<A>.amap(f: suspend (A) -> B): List<B> = coroutineScope {
    this@amap.map { async { f(it) } }.awaitAll()
}

fun List<SearchResponse>.toNewSearchResponseList(hasNext: Boolean? = null): SearchResponseList = SearchResponseList(this, hasNext ?: false)

/** Cloudstream identical Base64 functions */
fun base64DecodeArray(string: String): ByteArray {
    return try {
        Base64.decode(string, Base64.DEFAULT)
    } catch (e: Exception) {
        string.toByteArray(Charsets.ISO_8859_1)
    }
}

fun base64Decode(string: String): String {
    val bytes = base64DecodeArray(string)
    return buildString(bytes.size) {
        for (b in bytes) {
            append((b.toInt() and 0xFF).toChar())
        }
    }
}

fun base64Encode(array: ByteArray): String {
    return Base64.encodeToString(array, Base64.NO_WRAP)
}

fun getHighestQuality(input: String): Int? {
    val qualities = listOf(
        "2160" to Qualities.P2160.value,
        "1440" to Qualities.P1440.value,
        "1080" to Qualities.P1080.value,
        "720"  to Qualities.P720.value,
        "480"  to Qualities.P480.value,
        "360"  to Qualities.P360.value,
        "240"  to Qualities.P240.value
    )

    for ((label, mappedValue) in qualities) {
        if (input.contains(label, ignoreCase = true)) {
            return mappedValue
        }
    }
    return null
}

fun <T> T.toJson(): String = mapper.writeValueAsString(this)

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

fun MainAPI.newHomePageResponse(items: List<HomePageList>, hasNext: Boolean = true): HomePageResponse {
    return HomePageResponse(items, hasNext)
}

fun MainAPI.newHomePageResponse(list: HomePageList, hasNext: Boolean = true): HomePageResponse {
    return HomePageResponse(listOf(list), hasNext)
}

fun MainAPI.newHomePageResponse(name: String, list: List<SearchResponse>, hasNext: Boolean = true): HomePageResponse {
    return HomePageResponse(listOf(HomePageList(name, list)), hasNext)
}

fun MainAPI.newSearchResponseList(list: List<SearchResponse>, hasNext: Boolean = true): SearchResponseList {
    return SearchResponseList(list, hasNext)
}

fun MainAPI.newMovieLoadResponse(name: String, url: String, type: TvType, dataUrl: String, initializer: LoadResponse.() -> Unit = {}): LoadResponse {
    val trimmed = dataUrl.trim()
    val realDataUrl = if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
         listOf(Episode(dataUrl, name, 1, 1)).toJson()
    } else dataUrl
    return LoadResponse(name, url, this.name, type, realDataUrl).apply(initializer)
}

fun MainAPI.newTvSeriesLoadResponse(name: String, url: String, type: TvType, episodes: List<Episode>, initializer: LoadResponse.() -> Unit = {}): LoadResponse {
    return LoadResponse(name, url, this.name, type, episodes.toJson()).apply(initializer)
}

fun MainAPI.newEpisode(data: Any, initializer: Episode.() -> Unit = {}): Episode {
    return Episode(if (data is String) data else data.toJson()).apply(initializer)
}

fun Episode.addDate(date: String?) {
    this.date = date
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

fun MainAPI.newSubtitleFile(url: String, lang: String): SubtitleFile = SubtitleFile(lang, url)

fun MainAPI.mainPageOf(vararg pairs: Pair<String, String>): List<MainPageRequest> {
    return pairs.map { MainPageRequest(it.second, it.first, true) }
}
