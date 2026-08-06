package com.streamix.core.plugin

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class ContentCategory { MOVIES, SHOWS, ANIME, MUSIC, ADULT, PODCASTS }
enum class ContentType     { MOVIE, SERIES, EPISODE, MUSIC, PLAYLIST }
enum class StreamFormat    { HLS, DASH, MP4, WEBM, MKV, AUTO }

// ─── Extension Manifest ──────────────────────────────────────────────────────

data class ExtensionManifest(
    val id           : String,
    val name         : String,
    val version      : String,
    val mainClass    : String,
    val categories   : List<ContentCategory>,
    val author       : String,
    val iconUrl      : String = "",
    val requiresLogin: Boolean = false,
    val changelog    : String = ""
)

// ─── Search ──────────────────────────────────────────────────────────────────

data class SearchResult(
    val id          : String,
    val title       : String,
    val posterUrl   : String?,
    val year        : Int?,
    val type        : ContentType,
    val sourceId    : String = "",     // filled in by PluginManager after fetch
    val description : String? = null
)

data class SearchPage(
    val results : List<SearchResult>,
    val hasMore : Boolean,
    val page    : Int = 1
)

// ─── Detail ──────────────────────────────────────────────────────────────────

data class Episode(
    val id          : String,
    val title       : String,
    val episodeNum  : Int,
    val seasonNum   : Int,
    val description : String? = null,
    val thumbUrl    : String? = null,
    val durationMs  : Long = 0L
)

data class Season(
    val seasonNum : Int,
    val title     : String = "Season $seasonNum",
    val episodes  : List<Episode>
)

data class ContentDetail(
    val id          : String,
    val title       : String,
    val description : String?,
    val posterUrl   : String?,
    val backdropUrl : String?,
    val type        : ContentType,
    val year        : Int? = null,
    val rating      : Float? = null,
    val genres      : List<String> = emptyList(),
    val cast        : List<String> = emptyList(),
    val seasons     : List<Season> = emptyList(),   // for SERIES
    val streamLinks : List<StreamLink> = emptyList() // for MOVIE/MUSIC
)

// ─── Streams ─────────────────────────────────────────────────────────────────

data class StreamLink(
    val url     : String,
    val quality : String,               // e.g. "1080p", "720p", "Auto"
    val format  : StreamFormat,
    val headers : Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class SubtitleTrack(
    val url      : String,
    val language : String,  // ISO 639-1, e.g. "en", "hi"
    val label    : String   // display name, e.g. "English", "हिन्दी"
)

// ─── Remote Extension (Store catalog entry) ──────────────────────────────────

data class RemoteExtension(
    val id          : String,
    val name        : String,
    val version     : String,
    val categories  : List<ContentCategory>,
    val apkUrl      : String,
    val manifestUrl : String,
    val iconUrl     : String,
    val author      : String,
    val changelog   : String
)

// ─── Install State ───────────────────────────────────────────────────────────

sealed class InstallState {
    data class Progress(val percent: Int) : InstallState()
    object Success : InstallState()
    data class Error(val message: String) : InstallState()
}

// ─── StreamSource Interface ───────────────────────────────────────────────────

interface StreamSource {
    val manifest: ExtensionManifest

    /**
     * Search for content. page is 1-indexed.
     */
    suspend fun search(query: String, category: ContentCategory? = null, page: Int = 1): Result<SearchPage>

    /**
     * Load full detail for a content item identified by [id].
     * For series, detail.seasons will be populated.
     */
    suspend fun loadDetail(id: String): Result<ContentDetail>

    /**
     * Get stream links for a given contentId.
     * For episodes, id is the episode id. For movies, it's the movie id.
     */
    suspend fun getStreams(contentId: String): Result<List<StreamLink>>

    /**
     * Optional: return trending/featured items for the home screen.
     * Default returns empty list.
     */
    suspend fun getFeatured(): Result<List<SearchResult>> = Result.success(emptyList())

    /**
     * Optional: authenticate the user (only if manifest.requiresLogin == true).
     */
    suspend fun login(username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Login not supported"))
}
