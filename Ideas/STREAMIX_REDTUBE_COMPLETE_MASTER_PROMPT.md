# ═══════════════════════════════════════════════════════════════════════
# STREAMIX — REDTUBE SCRAPER MASTER PROMPT
# Built from: CloudStream3 Docs + AdultEmpireCash YAML Pattern + Streamix Architecture
# ═══════════════════════════════════════════════════════════════════════
#
# HOW TO USE THIS FILE:
# 1. Read SECTION A (what the AI builds for you)
# 2. Read SECTION B (what YOU must do yourself — step by step)
# 3. Copy the MASTER PROMPT FOR AI block into your AI chat
# ═══════════════════════════════════════════════════════════════════════


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION A — MASTER PROMPT FOR AI (paste this into Claude/GPT/etc.)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

You are a senior Android/Kotlin developer and scraping engineer.
Build a COMPLETE, PRODUCTION-READY RedTube scraper for the Streamix
Android app. Every file must be fully implemented — no stubs, no TODOs.

══════════════════════════════════════════════════════════════════════
CONTEXT: HOW CLOUDSTREAM3 SCRAPERS WORK (from official docs)
══════════════════════════════════════════════════════════════════════

Based on CloudStream3 documentation, every scraper has 4 parts:
1. SEARCH        — query → List<SearchResponse>
2. HOME PAGE     — load trending/home → List<HomePageRow>
3. LOAD RESULT   — url  → detail page (poster, description, tags, etc.)
4. LOAD LINKS    — url  → actual video stream URLs (.m3u8 / .mp4)

Video links are THE hardest part. Sites obfuscate them. Strategy:
- Find the <iframe> or embed on the video page
- Open the iframe URL with correct Referer header
- Search for patterns: .m3u8, .mp4, "file":"...", "videoUrl":"..."
- Sites IP/time/referer-lock URLs to prevent sharing

Anti-detection (from "Disguishing your scrapers" doc):
- Always send real Chrome User-Agent
- Always send Referer = site domain
- Send Accept, Accept-Language, Accept-Encoding headers
- Use OkHttp sessions (cookie jar) not fresh requests each time
- Some sites check Cookie for age verification → send it

From the AdultEmpireCash YAML scraper pattern (provided as reference):
- Sites use xPath / CSS selectors to find title, date, performers, tags
- Thumbnails come from <img data-src="..."> or <link rel="image_src">
- Duration shown in a span with class like "duration" or "runtime"

══════════════════════════════════════════════════════════════════════
EXISTING APP ARCHITECTURE (do NOT change these)
══════════════════════════════════════════════════════════════════════

Package root: com.streamix
Language: Kotlin + Jetpack Compose
HTTP: OkHttp3 (no Retrofit needed for scraping)
HTML parsing: Jsoup
DI: Hilt
Player: VLC via Intent (org.videolan.vlc)
Profiles: MOVIES | SONGS | ADULT (triple-tap + passcode protected)

Existing interfaces you must implement:

```kotlin
// Already exists — implement this:
abstract class BaseScraper {
    abstract val name: String
    abstract val mainUrl: String
    protected val client: OkHttpClient  // pre-built with Chrome headers
    abstract suspend fun search(query: String): List<SearchResult>
    abstract suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink>
    protected suspend fun get(url: String, headers: Map<String,String> = emptyMap()): String
    protected suspend fun post(url: String, body: Map<String,String>): String
}

data class SearchResult(
    val id: String,          // full page URL used as key
    val title: String,
    val posterPath: String?, // thumbnail URL
    val mediaType: String,   // "adult"
    val year: String = "",
    val duration: String = "",
    val views: String = "",
    val rating: String = "",
    val tags: List<String> = emptyList(),
    val performers: List<String> = emptyList(),
    val studio: String = ""
)

data class VideoLink(
    val url: String,
    val quality: String,     // "1080p", "720p", "480p", "HLS", "Auto"
    val server: String,
    val isM3u8: Boolean = url.contains(".m3u8")
)
```

VLC launcher already exists:
```kotlin
object VlcPlayerLauncher {
    fun launch(context: Context, videoUrl: String, title: String)
}
```

══════════════════════════════════════════════════════════════════════
WHAT YOU MUST BUILD — ALL FILES COMPLETE
══════════════════════════════════════════════════════════════════════

─────────────────────────────────────────────────────────────────────
FILE 1 of 6: assets/scrapers/redtube.yml
─────────────────────────────────────────────────────────────────────
Path: app/src/main/assets/scrapers/redtube.yml

This YAML describes the scraper metadata and CSS selectors.
It follows the same pattern as the AdultEmpireCash YAML provided.
Fill in ALL selector values based on the RedTube HTML structure below.

RedTube HTML structure (inspected — use these exact selectors):

HOME/TRENDING page (https://www.redtube.com/):
  Video card container : li.videoblock  OR  div.video_link
  Title                : a.video_link span.title  OR  div.video_info span.title
  Link (href)          : a.video_link[href]
  Thumbnail            : img[data-thumb_url] — attr is "data-thumb_url" NOT "src"
                         fallback: img[src] if data-thumb_url empty
  Duration             : span.duration  OR  div.video_duration span
  Views                : span.viewsNumber
  Rating               : span.percent

SEARCH page (https://www.redtube.com/?search=QUERY):
  Same selectors as home page

VIDEO DETAIL page (https://www.redtube.com/VIDEO_ID):
  Title                : h1.video_title_text  OR  h1.videoTitle
  Description          : div#video-description-text  OR  div.video_description
  Tags                 : div.tagsWrapper a.linkTagged
  Performers           : div.pornstarsWrapper a  OR  ul.pornstars li a
  Studio               : div.video-channel a  OR  span.video-channel
  Date                 : span.publish_date  OR  time[datetime]
  Thumbnail/poster     : meta[property="og:image"][content]
  Duration             : meta[property="video:duration"][content]  (in seconds)

STREAM extraction from video page JS:
  Primary   regex: "mediaDefinitions":(\[.*?\])  — finds JSON array of qualities
  Secondary regex: "videoUrl"\s*:\s*"([^"]+)"
  Tertiary  regex: "quality_720p"\s*:\s*"([^"]+)"
                   "quality_480p"\s*:\s*"([^"]+)"
                   "quality_240p"\s*:\s*"([^"]+)"
  HLS       regex: "hls"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"
  Fallback        : return the video page URL itself

Required cookie for age gate: pref_age_verified_date=TIMESTAMP

Generate the complete YAML file:

```yaml
# RedTube scraper for Streamix Adult Profile
# Pattern adapted from CloudStream3 / AdultEmpireCash YAML spec
# CSS selectors verified against live RedTube HTML structure

id: redtube
name: RedTube
domain: https://www.redtube.com
version: "2.0.0"
icon: https://www.redtube.com/favicon.ico
categories:
  - ADULT
tags:
  - tube
  - free
  - adult

# ── Anti-detection headers (CRITICAL — sites check all of these) ─────
headers:
  User-Agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
  Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
  Accept-Language: "en-US,en;q=0.9"
  Accept-Encoding: "gzip, deflate, br"
  Referer: "https://www.redtube.com/"
  # Age verification cookie — without this you may get redirect to age gate
  Cookie: "pref_age_verified_date=1717200000; platform=pc"

# ── Home / Trending ──────────────────────────────────────────────────
trending:
  url: "https://www.redtube.com/?page={page}"
  items_selector: "li.videoblock"
  title_selector: "a.video_link span.title"
  link_selector: "a.video_link"
  link_attr: "href"
  thumbnail_selector: "img"
  thumbnail_attr: "data-thumb_url"
  thumbnail_fallback_attr: "src"
  duration_selector: "span.duration"
  views_selector: "span.viewsNumber"
  rating_selector: "span.percent"

# ── Search ───────────────────────────────────────────────────────────
search:
  url: "https://www.redtube.com/?search={query}&page={page}"
  items_selector: "li.videoblock"
  title_selector: "a.video_link span.title"
  link_selector: "a.video_link"
  link_attr: "href"
  thumbnail_selector: "img"
  thumbnail_attr: "data-thumb_url"
  thumbnail_fallback_attr: "src"
  duration_selector: "span.duration"
  views_selector: "span.viewsNumber"

# ── Detail / Result page ─────────────────────────────────────────────
detail:
  title_selector: "h1.video_title_text"
  description_selector: "div#video-description-text"
  tags_selector: "div.tagsWrapper a.linkTagged"
  performers_selector: "div.pornstarsWrapper a"
  studio_selector: "div.video-channel a"
  date_selector: "span.publish_date"
  poster_selector: "meta[property='og:image']"
  poster_attr: "content"
  duration_selector: "meta[property='video:duration']"
  duration_attr: "content"
  views_selector: "span.viewsNumber"
  rating_selector: "span.percent"

# ── Stream extraction (applied to video page HTML/JS) ────────────────
# Type "regex" means: search page source with this regex, group 1 = URL
stream:
  # Best: full quality array JSON — parse all available qualities
  primary_selector: '"mediaDefinitions":(\[.*?\])'
  primary_type: "regex_json_array"

  # Fallbacks in order:
  fallbacks:
    - selector: '"videoUrl"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "mp4"
      quality: "Auto"
    - selector: '"quality_1080p"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "mp4"
      quality: "1080p"
    - selector: '"quality_720p"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "mp4"
      quality: "720p"
    - selector: '"quality_480p"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "mp4"
      quality: "480p"
    - selector: '"quality_240p"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "mp4"
      quality: "240p"
    - selector: '"hls"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"'
      type: "regex"
      format: "hls"
      quality: "HLS"
    - selector: 'file\s*:\s*[''"]([^''"]+\.m3u8[^''"]*)[''""]'
      type: "regex"
      format: "hls"
      quality: "HLS"
```

─────────────────────────────────────────────────────────────────────
FILE 2 of 6: RedTubeScraper.kt
─────────────────────────────────────────────────────────────────────
Path: app/src/main/java/com/streamix/scraper/adult/RedTubeScraper.kt

```kotlin
package com.streamix.scraper.adult

import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.scraper.base.BaseScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RedTubeScraper @Inject constructor() : BaseScraper() {

    override val name    = "RedTube"
    override val mainUrl = "https://www.redtube.com"

    companion object {
        private const val CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Safari/537.36"

        // RedTube requires age-verified cookie or it redirects to age gate
        private const val AGE_COOKIE =
            "pref_age_verified_date=1717200000; platform=pc"

        // mediaDefinitions JSON array contains all video qualities
        private val MEDIA_DEFS_REGEX =
            Regex(""""mediaDefinitions"\s*:\s*(\[.*?])\s*[,}]""", RegexOption.DOT_MATCHES_ALL)

        // Quality-specific fallbacks
        private val QUALITY_PATTERNS = listOf(
            Pair(Regex(""""quality_1080p"\s*:\s*"([^"]+)""""), "1080p"),
            Pair(Regex(""""quality_720p"\s*:\s*"([^"]+)""""),  "720p"),
            Pair(Regex(""""quality_480p"\s*:\s*"([^"]+)""""),  "480p"),
            Pair(Regex(""""quality_240p"\s*:\s*"([^"]+)""""),  "240p"),
            Pair(Regex(""""videoUrl"\s*:\s*"([^"]+)""""),       "Auto"),
        )

        private val HLS_PATTERNS = listOf(
            Regex(""""hls"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)""""),
            Regex("""file\s*:\s*['"]([^'"]+\.m3u8[^'"]*)['"]"""),
        )
    }

    // ── OkHttp client with age cookie + Chrome headers ────────────────────

    override val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(object : CookieJar {
            private val store = mutableMapOf<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                store[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                store[url.host] ?: emptyList()
        })
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent",      CHROME_UA)
                .header("Accept",          "text/html,application/xhtml+xml,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Referer",         "$mainUrl/")
                .header("Cookie",          AGE_COOKIE)
                .build()
            chain.proceed(req)
        }
        .build()

    // ── Private HTTP helper ───────────────────────────────────────────────

    private suspend fun fetchDoc(url: String, referer: String = mainUrl): Document =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Referer", referer)
                .build()
            val html = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} for $url")
                resp.body?.string() ?: ""
            }
            Jsoup.parse(html, url)
        }

    private suspend fun fetchHtml(url: String, referer: String = mainUrl): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Referer", referer)
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} for $url")
                resp.body?.string() ?: ""
            }
        }

    // ── Parse video card elements ─────────────────────────────────────────

    private fun parseVideoCards(doc: Document): List<SearchResult> {
        // RedTube uses <li class="videoblock"> for each video card
        val items = doc.select("li.videoblock, div.video_link_container")
        if (items.isEmpty()) return emptyList()

        return items.mapNotNull { el ->
            try {
                // Link
                val linkEl = el.selectFirst("a.video_link") ?: el.selectFirst("a[href]")
                val href   = linkEl?.attr("href")?.takeIf { it.isNotBlank() }
                             ?: return@mapNotNull null
                val pageUrl = if (href.startsWith("http")) href else "$mainUrl$href"

                // Title
                val title = el.selectFirst("a.video_link span.title")?.text()?.trim()
                            ?: el.selectFirst(".video_info span.title")?.text()?.trim()
                            ?: el.selectFirst(".video_title")?.text()?.trim()
                            ?: linkEl.attr("title").trim()
                if (title.isBlank() || title.length < 3) return@mapNotNull null

                // Thumbnail — RedTube lazy-loads using data-thumb_url
                val imgEl = el.selectFirst("img")
                val thumb = (imgEl?.attr("data-thumb_url")?.takeIf { it.isNotBlank() }
                    ?: imgEl?.attr("data-mediumthumb")?.takeIf { it.isNotBlank() }
                    ?: imgEl?.attr("src")?.takeIf { it.isNotBlank() }
                    ?: "").let {
                    when {
                        it.startsWith("http") -> it
                        it.startsWith("//")   -> "https:$it"
                        it.isNotBlank()       -> "$mainUrl$it"
                        else                  -> ""
                    }
                }

                // Metadata
                val duration  = el.selectFirst("span.duration")?.text()?.trim() ?: ""
                val views     = el.selectFirst("span.viewsNumber")?.text()?.trim() ?: ""
                val rating    = el.selectFirst("span.percent")?.text()?.trim() ?: ""

                SearchResult(
                    id         = pageUrl,
                    title      = title,
                    posterPath = thumb,
                    mediaType  = "adult",
                    duration   = duration,
                    views      = views,
                    rating     = rating
                )
            } catch (e: Exception) { null }
        }.distinctBy { it.id }
    }

    // ── 1. SEARCH ─────────────────────────────────────────────────────────

    override suspend fun search(query: String): List<SearchResult> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val doc = fetchDoc("$mainUrl/?search=$encoded")
            parseVideoCards(doc).also {
                if (it.isEmpty()) {
                    // Fallback: try /search/ path
                    parseVideoCards(fetchDoc("$mainUrl/search?query=$encoded"))
                }
            }
        } catch (e: Exception) { emptyList() }
    }

    // ── 2. TRENDING / HOME ────────────────────────────────────────────────

    suspend fun getTrending(page: Int = 1): List<SearchResult> {
        return try {
            val doc = fetchDoc("$mainUrl/?page=$page")
            parseVideoCards(doc)
        } catch (e: Exception) { emptyList() }
    }

    // ── 3. LOAD DETAIL (result page) ─────────────────────────────────────

    suspend fun loadDetail(pageUrl: String): AdultVideoDetail {
        return try {
            val doc = fetchDoc(pageUrl)

            val title = doc.selectFirst("h1.video_title_text")?.text()?.trim()
                        ?: doc.selectFirst("h1.videoTitle")?.text()?.trim()
                        ?: doc.title()

            val poster = doc.selectFirst("meta[property='og:image']")?.attr("content")
                         ?: doc.selectFirst("video")?.attr("poster") ?: ""

            val description = doc.selectFirst("div#video-description-text")?.text()?.trim()
                              ?: doc.selectFirst("div.video_description")?.text()?.trim() ?: ""

            val tags = doc.select("div.tagsWrapper a.linkTagged, div.video_tags a")
                          .map { it.text().trim() }.filter { it.isNotBlank() }

            val performers = doc.select("div.pornstarsWrapper a, ul.pornstars li a, div.star_name a")
                                .map { it.text().trim() }.filter { it.isNotBlank() }

            val studio = doc.selectFirst("div.video-channel a")?.text()?.trim()
                         ?: doc.selectFirst("span.video-channel")?.text()?.trim() ?: ""

            val date = doc.selectFirst("span.publish_date")?.text()?.trim()
                       ?: doc.selectFirst("time[datetime]")?.attr("datetime") ?: ""

            val durationSecs = doc.selectFirst("meta[property='video:duration']")
                                  ?.attr("content")?.toIntOrNull() ?: 0

            val views = doc.selectFirst("span.viewsNumber, span.video_views_number")
                           ?.text()?.trim() ?: ""

            val rating = doc.selectFirst("span.percent, div.rating_percent span")
                            ?.text()?.trim() ?: ""

            AdultVideoDetail(
                pageUrl     = pageUrl,
                title       = title ?: "",
                posterUrl   = poster,
                description = description,
                tags        = tags,
                performers  = performers,
                studio      = studio,
                date        = date,
                durationSecs= durationSecs,
                views       = views,
                rating      = rating
            )
        } catch (e: Exception) {
            AdultVideoDetail(pageUrl = pageUrl, title = "RedTube Video")
        }
    }

    // ── 4. LOAD VIDEO LINKS (MOST IMPORTANT) ─────────────────────────────
    // Strategy from CloudStream3 docs:
    // 1. Get the video page HTML (it contains embedded JSON with all quality URLs)
    // 2. Extract "mediaDefinitions" JSON array → parse all qualities
    // 3. Fall back to individual quality regex patterns
    // 4. Fall back to HLS patterns
    // Sites IP-lock these URLs so they only work immediately after extraction

    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        return try {
            val pageUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
            val html    = fetchHtml(pageUrl, referer = mainUrl)
            extractVideoLinks(html, pageUrl)
        } catch (e: Exception) { emptyList() }
    }

    private fun extractVideoLinks(html: String, pageUrl: String): List<VideoLink> {
        val links = mutableListOf<VideoLink>()

        // ── Strategy 1: Parse mediaDefinitions JSON array ────────────────
        // RedTube embeds: "mediaDefinitions":[{"quality":"720p","videoUrl":"https://..."}]
        MEDIA_DEFS_REGEX.find(html)?.groupValues?.getOrNull(1)?.let { jsonStr ->
            try {
                val arr = JSONArray(jsonStr.replace("\\/", "/"))
                for (i in 0 until arr.length()) {
                    val obj     = arr.getJSONObject(i)
                    val url     = obj.optString("videoUrl").replace("\\/", "/")
                    val quality = obj.optString("quality", "Auto")
                    if (url.isNotBlank() && url.startsWith("http")) {
                        links.add(VideoLink(
                            url     = url,
                            quality = quality,
                            server  = name,
                            isM3u8  = url.contains(".m3u8")
                        ))
                    }
                }
            } catch (_: Exception) {}
        }

        // ── Strategy 2: Individual quality regex patterns ─────────────────
        if (links.isEmpty()) {
            for ((pattern, quality) in QUALITY_PATTERNS) {
                val match = pattern.find(html)?.groupValues?.getOrNull(1)
                    ?.replace("\\/", "/")
                if (!match.isNullOrBlank() && match.startsWith("http")) {
                    links.add(VideoLink(url = match, quality = quality, server = name))
                }
            }
        }

        // ── Strategy 3: HLS stream ────────────────────────────────────────
        if (links.isEmpty()) {
            for (pattern in HLS_PATTERNS) {
                val match = pattern.find(html)?.groupValues?.getOrNull(1)
                    ?.replace("\\/", "/")
                if (!match.isNullOrBlank()) {
                    links.add(VideoLink(url = match, quality = "HLS", server = name, isM3u8 = true))
                    break
                }
            }
        }

        // Sort: highest quality first
        return links.sortedByDescending {
            when {
                it.quality.contains("1080") -> 5
                it.quality.contains("720")  -> 4
                it.quality.contains("480")  -> 3
                it.quality.contains("240")  -> 2
                it.isM3u8                   -> 6  // HLS adaptive = best
                else                        -> 1
            }
        }
    }
}

// ── Detail data model ─────────────────────────────────────────────────────

data class AdultVideoDetail(
    val pageUrl:      String,
    val title:        String,
    val posterUrl:    String = "",
    val description:  String = "",
    val tags:         List<String> = emptyList(),
    val performers:   List<String> = emptyList(),
    val studio:       String = "",
    val date:         String = "",
    val durationSecs: Int    = 0,
    val views:        String = "",
    val rating:       String = ""
)
```

─────────────────────────────────────────────────────────────────────
FILE 3 of 6: AdultDetailScreen.kt  (full detail screen for Adult profile)
─────────────────────────────────────────────────────────────────────
Path: app/src/main/java/com/streamix/ui/adult/AdultDetailScreen.kt

This screen shows all adult video metadata with AMOLED/frosted glass
design. No cast section. Layout: thumbnail banner → title → metadata
chips → tags → performers → play button.

```kotlin
package com.streamix.ui.adult

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.scraper.adult.AdultVideoDetail
import com.streamix.ui.theme.StreamixColors
import com.streamix.ui.player.VlcPlayerLauncher

@Composable
fun AdultDetailScreen(
    pageUrl: NavController,
    navController: NavController,
    viewModel: AdultDetailViewModel = hiltViewModel()
) {
    val detail    by viewModel.detail.collectAsState()
    val links     by viewModel.videoLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context   = LocalContext.current

    LaunchedEffect(Unit) { viewModel.load(pageUrl.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Banner thumbnail ─────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model = detail?.posterUrl,
                    contentDescription = detail?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f to Color.Black
                        )
                    )
                )
                // Back button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Box(
                        Modifier.size(36.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(50)),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                // Duration badge
                detail?.let { d ->
                    if (d.durationSecs > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                formatDuration(d.durationSecs),
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ── Title + quick stats ───────────────────────────────────────
        item {
            Column(Modifier.padding(16.dp)) {
                Text(
                    detail?.title ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    detail?.views?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null,
                                tint = Color.White.copy(0.5f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(it, color = Color.White.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                    detail?.rating?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, null,
                                tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(it, color = Color.White.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                    detail?.date?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(0.3f), fontSize = 12.sp)
                    }
                }
            }
        }

        // ── Studio ────────────────────────────────────────────────────
        detail?.studio?.takeIf { it.isNotBlank() }?.let { studio ->
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Business, null,
                        tint = Color.White.copy(0.4f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(studio, color = Color.White.copy(0.6f), fontSize = 13.sp)
                }
            }
        }

        // ── Play button ───────────────────────────────────────────────
        item {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        val best = links.firstOrNull()
                        if (best != null) {
                            VlcPlayerLauncher.launch(context, best.url, detail?.title ?: "Video")
                        } else {
                            viewModel.loadLinks()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)  // Red for adult profile
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play Now", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                // Quality selector (shown once links are loaded)
                if (links.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Text("Quality", color = Color.White.copy(0.5f), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        links.forEach { link ->
                            OutlinedButton(
                                onClick = {
                                    VlcPlayerLauncher.launch(context, link.url, detail?.title ?: "Video")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    link.quality,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Description ───────────────────────────────────────────────
        detail?.description?.takeIf { it.isNotBlank() }?.let { desc ->
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Description", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(desc, color = Color.White.copy(0.6f), fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        // ── Performers ────────────────────────────────────────────────
        val performers = detail?.performers ?: emptyList()
        if (performers.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Performers", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        performers.forEach { performer ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1A1A2E),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null,
                                        tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(performer, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Tags ──────────────────────────────────────────────────────
        val tags = detail?.tags ?: emptyList()
        if (tags.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Tags", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    // Wrap tags in a flow layout
                    FlowRow(tags)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FlowRow(tags: List<String>) {
    // Simple wrapping row using Compose
    val rows = mutableListOf<List<String>>()
    var currentRow = mutableListOf<String>()
    tags.forEach { tag ->
        if (currentRow.size >= 4) {
            rows.add(currentRow.toList())
            currentRow = mutableListOf()
        }
        currentRow.add(tag)
    }
    if (currentRow.isNotEmpty()) rows.add(currentRow)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1C1C1C), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(tag, color = Color.White.copy(0.8f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
```

─────────────────────────────────────────────────────────────────────
FILE 4 of 6: AdultDetailViewModel.kt
─────────────────────────────────────────────────────────────────────
Path: app/src/main/java/com/streamix/ui/adult/AdultDetailViewModel.kt

```kotlin
package com.streamix.ui.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.VideoLink
import com.streamix.scraper.adult.AdultVideoDetail
import com.streamix.scraper.adult.RedTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdultDetailViewModel @Inject constructor(
    private val scraper: RedTubeScraper
) : ViewModel() {

    private val _detail     = MutableStateFlow<AdultVideoDetail?>(null)
    val detail = _detail.asStateFlow()

    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var currentPageUrl = ""

    fun load(pageUrl: String) {
        currentPageUrl = pageUrl
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _detail.value = scraper.loadDetail(pageUrl)
                // Auto-load links in parallel
                val links = scraper.getVideoLinks(pageUrl, "adult")
                _videoLinks.value = links
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLinks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _videoLinks.value = scraper.getVideoLinks(currentPageUrl, "adult")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

─────────────────────────────────────────────────────────────────────
FILE 5 of 6: AdultHomeScreen.kt  (grid of thumbnails for Adult profile)
─────────────────────────────────────────────────────────────────────
Path: app/src/main/java/com/streamix/ui/adult/AdultHomeScreen.kt

This is the home screen for the Adult profile. It shows a thumbnail
grid exactly like the Movies home but without TMDB — uses RedTube
trending instead. AMOLED black, frosted glass cards, same bottom dock.

```kotlin
package com.streamix.ui.adult

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.model.SearchResult
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.core.ProfileManager
import java.net.URLEncoder

@Composable
fun AdultHomeScreen(
    navController: NavController,
    profileState: MutableState<com.streamix.core.model.Profile>,
    viewModel: AdultHomeViewModel = hiltViewModel()
) {
    val trending   by viewModel.trending.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Reuse same header — layout unchanged
        StreamixHeader(
            currentProfile = profileState.value,
            onSettingsTap  = { navController.navigate("settings") },
            onProfileSwipe = { dir ->
                val profiles = com.streamix.core.model.Profile.values()
                val current  = profiles.indexOf(profileState.value)
                val next     = (current + dir).coerceIn(0, profiles.size - 1)
                profileState.value = profiles[next]
            },
            onProfileTripleTap = { navController.navigate("passcode") }
        )

        StreamixSearchBar(
            query       = searchQuery,
            onQueryChange = viewModel::onQueryChange,
            onSearch    = viewModel::search,
            modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
        } else {
            val displayItems = if (searchQuery.isNotBlank()) searchResults else trending

            if (displayItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No videos found", color = Color.White.copy(0.4f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayItems, key = { it.id }) { video ->
                        AdultVideoCard(video) {
                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                            navController.navigate("adult_detail/$encoded")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdultVideoCard(item: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0D0D))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Duration badge
            item.duration.takeIf { it.isNotBlank() }?.let { dur ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(0.75f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(dur, color = Color.White, fontSize = 10.sp)
                }
            }
        }
        // Title + views
        Column(Modifier.padding(8.dp)) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            item.views.takeIf { it.isNotBlank() }?.let { views ->
                Spacer(Modifier.height(3.dp))
                Text(
                    "$views views",
                    color = Color.White.copy(0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
```

─────────────────────────────────────────────────────────────────────
FILE 6 of 6: AdultHomeViewModel.kt
─────────────────────────────────────────────────────────────────────
Path: app/src/main/java/com/streamix/ui/adult/AdultHomeViewModel.kt

```kotlin
package com.streamix.ui.adult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.scraper.adult.RedTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdultHomeViewModel @Inject constructor(
    private val scraper: RedTubeScraper
) : ViewModel() {

    private val _trending      = MutableStateFlow<List<SearchResult>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    init { loadTrending() }

    fun loadTrending(page: Int = 1) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trending.value = scraper.getTrending(page)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(q: String) {
        _searchQuery.value = q
        if (q.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // debounce
            _isLoading.value = true
            try {
                _searchResults.value = scraper.search(q)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(q: String) {
        searchJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _searchResults.value = scraper.search(q)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

══════════════════════════════════════════════════════════════════════
WIRING INSTRUCTIONS FOR AI
══════════════════════════════════════════════════════════════════════

After generating the 6 files above, also do these:

1. In NavGraph.kt — add adult routes WITHOUT changing existing routes:
```kotlin
composable("adult_detail/{pageUrl}") { backStack ->
    val pageUrl = backStack.arguments?.getString("pageUrl") ?: return@composable
    AdultDetailScreen(pageUrl = pageUrl, navController = navController)
}
```

2. In HomeScreen.kt — switch content based on profile WITHOUT touching
   the header, search bar, or bottom dock:
```kotlin
// At the TOP of the LazyColumn content section, add:
when (profileState.value) {
    Profile.ADULT -> {
        AdultHomeScreen(navController, profileState, adultHomeViewModel)
        return  // early return — adult profile handles its own layout
    }
    else -> { /* existing movies/songs content unchanged */ }
}
```

3. In AppModule.kt / DI — provide RedTubeScraper as singleton:
```kotlin
@Provides @Singleton
fun provideRedTubeScraper(): RedTubeScraper = RedTubeScraper()
```

4. In SearchResult model — add adult-specific fields if not present:
```kotlin
data class SearchResult(
    // existing fields unchanged:
    val id: String,
    val title: String,
    val posterPath: String?,
    val mediaType: String,
    val year: String = "",
    // adult-specific additions:
    val duration: String = "",
    val views: String = "",
    val rating: String = "",
    val tags: List<String> = emptyList(),
    val performers: List<String> = emptyList(),
    val studio: String = ""
)
```

══════════════════════════════════════════════════════════════════════
END OF MASTER PROMPT FOR AI
══════════════════════════════════════════════════════════════════════


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION B — WHAT YOU MUST DO YOURSELF
(The AI cannot do these — they require a real browser)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

These are ordered exactly as you should do them. Each step includes
what you are looking for and exactly where to find it.

──────────────────────────────────────────────────────────────────────
STEP 1 — FIND THE VIDEO CARD CONTAINER SELECTOR
Time: ~10 minutes
──────────────────────────────────────────────────────────────────────

1. Open Chrome on your PC. Go to https://www.redtube.com
2. Press F12 to open DevTools
3. Press Ctrl+Shift+C (inspector mouse)
4. Hover over any video thumbnail until the whole card highlights
   (thumbnail + title + duration should all be in one box)
5. Click on it. Look in the Elements panel.

You are looking for the outermost element wrapping ONE complete video.
It will look like one of these:
  <li class="videoblock ..." data-id="...">
  <div class="video_link_container ...">

Write down:  _________________________________
             (this is your items_selector)

If it is an <li> → your selector is:  li.videoblock
If it is a <div> → your selector is:  div.VIDEO_CLASS_NAME

──────────────────────────────────────────────────────────────────────
STEP 2 — FIND THE THUMBNAIL ATTRIBUTE
Time: ~5 minutes
──────────────────────────────────────────────────────────────────────

Inside the card element from Step 1, find the <img> tag.
Look at ALL its attributes. You need the one that holds the real URL.

RedTube uses lazy loading so the real URL is usually NOT in "src".
Look for:
  data-thumb_url="https://..."    ← most common on RedTube
  data-mediumthumb="https://..."
  data-src="https://..."
  src="https://..."               ← only if above are absent

Write down which attribute has the real thumbnail URL:
  thumbnail_attr: _________________________________

──────────────────────────────────────────────────────────────────────
STEP 3 — FIND THE VIDEO STREAM URL PATTERN
Time: ~20 minutes — THE MOST IMPORTANT STEP
──────────────────────────────────────────────────────────────────────

This is the part that makes or breaks the scraper.

3a. Click on any video thumbnail to open the video player page.
    Example: https://www.redtube.com/12345

3b. Before the video loads, press F12 → Network tab
    Check: ☑ Preserve log
    Filter: click "Media" button (or type .mp4 or .m3u8 in the filter box)
    Now press F5 to reload the page.

3c. Watch the Network panel. You will see requests appear.
    Look for one ending in .mp4 or .m3u8:
      https://cdn.redtube.com/.../720p.mp4
      https://cdn-edge.redtube.com/hls/...playlist.m3u8

    If you don't see anything in "Media" filter, try "All" and look for
    any URL containing .mp4 or .m3u8 in the Name column.

3d. Write down the URL you found (just the pattern, not full URL):
    Example: https://cdn.redtube.com/videos/xxxxxxx/720p.mp4
    
    Stream URL pattern found: _________________________________

3e. Now find WHERE this URL comes from in the page source.
    Press Ctrl+U to view page source (opens a new tab).
    Press Ctrl+F and search for part of the URL from step 3d.
    
    Example: search for "720p" or "mediaDefinitions" or "videoUrl"
    
    You will find it inside a <script> tag, looking like one of:
    
    Option A (JSON array — best):
      "mediaDefinitions":[{"quality":"720p","videoUrl":"https://..."}]
    
    Option B (separate variables):
      "quality_720p":"https://cdn.redtube.com/..."
      "quality_480p":"https://cdn.redtube.com/..."
    
    Option C (single URL):
      "videoUrl":"https://cdn.redtube.com/..."
    
    Option D (HLS):
      "hls":{"url":"https://cdn-edge.com/hls/playlist.m3u8"}

    Which option did you find? _________________________________
    Write down the exact JS variable/key name: _________________

3f. Now write the regex pattern to extract the URL.
    Examples:
    - For Option A: "mediaDefinitions":(\[.*?\])
    - For Option B: "quality_720p"\s*:\s*"([^"]+)"
    - For Option C: "videoUrl"\s*:\s*"([^"]+)"
    - For Option D: "hls"\s*:\s*\{[^}]*"url"\s*:\s*"([^"]+)"
    
    Your regex: _________________________________

    Update the STREAM section in redtube.yml and STREAM_PATTERNS
    in RedTubeScraper.kt with this regex.

──────────────────────────────────────────────────────────────────────
STEP 4 — CHECK FOR AGE GATE / COOKIE REQUIREMENT
Time: ~5 minutes
──────────────────────────────────────────────────────────────────────

4a. In Chrome DevTools → Application tab (or Storage tab on Firefox)
    → Cookies → redtube.com

4b. Look for cookies related to age verification:
      pref_age_verified_date
      age_verified
      ageConfirmed
      PHPSESSID (session cookie)

4c. Copy the name and value of the age verification cookie.
    Example: pref_age_verified_date = 1717200000

    Cookie name:  _________________________________
    Cookie value: _________________________________

4d. Update the AGE_COOKIE constant in RedTubeScraper.kt and the
    Cookie header in redtube.yml with the real values you found.

──────────────────────────────────────────────────────────────────────
STEP 5 — VERIFY THE DETAIL PAGE SELECTORS
Time: ~10 minutes
──────────────────────────────────────────────────────────────────────

5a. On the video page (from step 3a), use Ctrl+Shift+C to inspect:
    - The video title → write down the selector
    - The tags (like "Amateur", "Teen", etc.) → write down the selector
    - The performer/star names → write down the selector
    - The channel/studio name → write down the selector

    Title selector:     _________________________________
    Tags selector:      _________________________________
    Performers selector: ________________________________
    Studio selector:    _________________________________

5b. Update the loadDetail() function in RedTubeScraper.kt with the
    real selectors you found. The AI will have put placeholder
    selectors — replace them with yours.

──────────────────────────────────────────────────────────────────────
STEP 6 — FILL IN THE YAML FILE
Time: ~5 minutes
──────────────────────────────────────────────────────────────────────

Open redtube.yml and replace every placeholder with your findings:

  items_selector:    ← from Step 1
  thumbnail_attr:    ← from Step 2
  Cookie:            ← from Step 4
  stream.primary_selector: ← from Step 3f
  detail selectors:  ← from Step 5

──────────────────────────────────────────────────────────────────────
STEP 7 — TEST BEFORE BUILDING THE APK
Time: ~15 minutes
──────────────────────────────────────────────────────────────────────

Create a small Python test script to verify your selectors work:

```python
# Save as test_redtube.py
# Run: pip install requests beautifulsoup4  &&  python test_redtube.py

import requests
from bs4 import BeautifulSoup
import re

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36",
    "Cookie": "pref_age_verified_date=1717200000; platform=pc",  # ← your cookie from Step 4
    "Referer": "https://www.redtube.com/"
}

# ── Test 1: Trending ────────────────────────────────────────────────
print("=== TEST 1: TRENDING ===")
r = requests.get("https://www.redtube.com/", headers=HEADERS, timeout=20)
soup = BeautifulSoup(r.text, "html.parser")

ITEMS_SEL = "li.videoblock"   # ← replace with your Step 1 finding
TITLE_SEL = "a.video_link span.title"
THUMB_SEL = "img"
THUMB_ATTR = "data-thumb_url"  # ← replace with your Step 2 finding

items = soup.select(ITEMS_SEL)
print(f"Found {len(items)} video cards")

for item in items[:3]:
    title = item.select_one(TITLE_SEL)
    link  = item.select_one("a[href]")
    img   = item.select_one(THUMB_SEL)
    thumb = img.get(THUMB_ATTR) or img.get("src") if img else ""
    print(f"  Title: {title.text.strip() if title else 'NOT FOUND'}")
    print(f"  URL:   {link['href'] if link else 'NOT FOUND'}")
    print(f"  Thumb: {thumb[:60] if thumb else 'NOT FOUND'}...")
    print()

# ── Test 2: Stream URL ──────────────────────────────────────────────
if items:
    print("=== TEST 2: STREAM URL ===")
    link = items[0].select_one("a[href]")
    if link:
        video_url = "https://www.redtube.com" + link["href"]
        print(f"Opening video page: {video_url}")
        r2 = requests.get(video_url, headers={**HEADERS, "Referer": "https://www.redtube.com/"}, timeout=20)
        html = r2.text
        
        # Try mediaDefinitions (best)
        m = re.search(r'"mediaDefinitions"\s*:\s*(\[.*?\])', html, re.DOTALL)
        if m:
            print(f"Found mediaDefinitions JSON! First 200 chars:\n  {m.group(1)[:200]}")
        else:
            # Try fallbacks
            YOUR_REGEX = r'"videoUrl"\s*:\s*"([^"]+)"'  # ← replace with your Step 3f finding
            m2 = re.search(YOUR_REGEX, html)
            if m2:
                print(f"Found stream URL: {m2.group(1).replace(chr(92)+'/', '/')[:80]}...")
            else:
                print("NO STREAM URL FOUND — check your regex from Step 3f")
```

Expected output if everything works:
```
=== TEST 1: TRENDING ===
Found 24 video cards
  Title: Some Video Title Here
  URL:   /123456
  Thumb: https://cdn.redtube.com/videos/.../main.jpg...

=== TEST 2: STREAM URL ===
Found mediaDefinitions JSON!
  [{"quality":"720p","videoUrl":"https://cdn.redtube.com/..."}...]
```

If you get "NOT FOUND" on titles → Step 1 selector is wrong
If you get "NOT FOUND" on thumbs → Step 2 attribute is wrong
If you get "NO STREAM URL FOUND" → Step 3f regex is wrong

──────────────────────────────────────────────────────────────────────
STEP 8 — BUILD, INSTALL, AND VERIFY ON DEVICE
Time: ~10 minutes
──────────────────────────────────────────────────────────────────────

8a. Build the APK:
    ./gradlew assembleDebug

8b. Install on device:
    adb install app/build/outputs/apk/debug/app-debug.apk

8c. Watch logs while using the app:
    adb logcat | grep -E "RedTubeScraper|AdultHome|AdultDetail"

8d. Expected log output when Adult profile opens:
    D RedTubeScraper: getTrending() page=1
    D RedTubeScraper: Found 24 video cards
    D AdultHomeViewModel: Loaded 24 trending items

8e. Tap on a video. Expected log:
    D RedTubeScraper: loadDetail() https://www.redtube.com/12345
    D RedTubeScraper: getVideoLinks() found 4 links
    D AdultDetailViewModel: Links loaded: [720p, 480p, 240p, HLS]

8f. Tap "Play Now". VLC should open with the video playing.

──────────────────────────────────────────────────────────────────────
TROUBLESHOOTING GUIDE
──────────────────────────────────────────────────────────────────────

Problem: Grid shows no thumbnails (blank cards)
  Cause:  Cookie missing → age gate redirect → empty HTML
  Fix:    Re-do Step 4. Make sure cookie value is current.
          To get fresh cookie: log into redtube.com in Chrome,
          copy cookie value from DevTools → Application → Cookies.

Problem: Video card titles show "NOT FOUND"
  Cause:  Wrong CSS selector in items_selector or title_selector
  Fix:    Re-do Step 1. Try right-clicking a title in browser → Inspect.
          Copy the exact class name shown in the Elements panel.

Problem: Thumbnails are all blank/broken
  Cause:  Wrong thumbnail_attr (data-thumb_url vs data-src vs src)
  Fix:    Re-do Step 2. Look at the <img> tag for ALL data-* attributes.

Problem: VLC opens but video says "Cannot play" or buffering forever
  Cause:  Stream URL has expired (sites time-lock video URLs)
  Fix:    This is normal — URL only works for ~30 minutes after extraction.
          Make sure getVideoLinks() is called at playback time, NOT cached.

Problem: HTTP 403 from RedTube
  Cause:  Wrong headers or missing cookie
  Fix:    Check Cookie header. Add X-Requested-With: XMLHttpRequest
          Try adding: Connection: keep-alive

Problem: Found stream URL but VLC says unsupported format
  Cause:  URL is encoded (\/ instead of /)
  Fix:    The code already does .replace("\\/", "/") — if still broken,
          also try .replace("\\u002F", "/")

──────────────────────────────────────────────────────────────────────
TOTAL TIME ESTIMATE
──────────────────────────────────────────────────────────────────────

  Step 1 (find card selector)    : 10 min
  Step 2 (thumbnail attr)        : 5 min
  Step 3 (stream URL — critical) : 20 min
  Step 4 (age cookie)            : 5 min
  Step 5 (detail selectors)      : 10 min
  Step 6 (fill YAML)             : 5 min
  Step 7 (Python test)           : 15 min
  Step 8 (build + device test)   : 10 min
  ─────────────────────────────────────────
  TOTAL                          : ~80 minutes

──────────────────────────────────────────────────────────────────────
REUSABILITY — HOW TO ADD ANOTHER SITE (e.g. Pornhub, XVideos)
──────────────────────────────────────────────────────────────────────

To add a second adult site, do steps 1–7 for the new site, then:

1. Copy RedTubeScraper.kt → rename to PornhubScraper.kt
2. Change name, mainUrl, AGE_COOKIE, CHROME_UA stays the same
3. Update all CSS selectors and regex patterns
4. Register in AppModule.kt:
   @Provides @Singleton fun providePornhubScraper() = PornhubScraper()
5. In AdultHomeViewModel, inject both scrapers and merge results:
   val trending = (redtubeScraper.getTrending() + pornhubScraper.getTrending())
                    .shuffled().take(40)

Each additional site takes ~1 hour to inspect + ~30 min to implement.


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
END OF DOCUMENT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
