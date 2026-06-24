# STREAMIX — SHORTS SWIPE + PROFILE REDESIGN
## Feature Change Master Prompt | Isolated Implementation Guide

---

## WHAT THIS CHANGES (Summary)

1. Remove Shorts from bottom dock → 3-button dock (Home | Library | Settings)
2. Add YouTube profile → 4 profiles: Movies | Songs | YouTube | Adult (hidden)
3. Shorts only on YouTube + Adult profiles
4. Swipe left on YouTube/Adult homescreen → Shorts pane (Instagram DM style)
5. Movies + Songs homescreens have no swipe/shorts

---

## FILES TO CHANGE vs FILES TO CREATE

### CHANGE (modify existing):
```
NavGraph.kt                          ← remove Shorts route from dock, add YouTube profile route
BottomDock.kt                        ← 3 items instead of 4, re-center
Profile.kt                           ← add YOUTUBE enum value
HomeScreen.kt                        ← wrap in HorizontalPager for YouTube/Adult only
AdultHomeScreen.kt                   ← wrap in HorizontalPager
```

### CREATE (new files):
```
ui/youtube/
  YoutubeHomeScreen.kt               ← YouTube profile home (search + grid)
  YoutubeHomeViewModel.kt            ← trending/search via YouTube Data API or scraper
  YoutubeDetailScreen.kt             ← video detail / player
  YoutubeDetailViewModel.kt

ui/shorts/
  ShortsScreen.kt                    ← full-screen vertical pager (reusable)
  ShortsViewModel.kt                 ← feeds shorts depending on profile context
  ShortsItem.kt                      ← single short data model

ui/components/
  SwipeableProfileHost.kt            ← HorizontalPager wrapper that enables swipe-to-shorts
```

---

## MASTER PROMPT FOR AI

Paste everything below this line into your AI chat.

══════════════════════════════════════════════════════════════════════
You are a senior Android/Kotlin developer working on the Streamix app.
The app uses Jetpack Compose, Hilt DI, MVVM, OkHttp3, and Navigation
Compose. DO NOT touch any files not listed below. DO NOT change the
design system (colors, typography, glass effects). DO NOT touch the
Movies or Songs profile screens.

Below is the complete list of changes needed. Implement ALL of them.
══════════════════════════════════════════════════════════════════════

---

## CHANGE 1 — Profile.kt  (add YOUTUBE)

**File:** `app/src/main/java/com/streamix/core/model/Profile.kt`

```kotlin
enum class Profile(val label: String, val icon: ImageVector) {
    MOVIES("Movies",   Icons.Default.Movie),
    SONGS("Songs",     Icons.Default.MusicNote),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),   // ← ADD THIS
    ADULT("Adult",     Icons.Default.EighteenUpRating)
}
```

Profile switch order (swipe on header icon):
- Swipe down → next  (Movies → Songs → YouTube)
- Swipe up   → prev  (YouTube → Songs → Movies)
- Adult is NEVER reachable via swipe — only triple-tap + passcode

---

## CHANGE 2 — BottomDock.kt  (3 items, no Shorts)

**File:** `app/src/main/java/com/streamix/ui/components/BottomDock.kt`

Remove Shorts from DOCK_ITEMS. Keep Home, Library, Settings.
Space them evenly across the full width.

```kotlin
val DOCK_ITEMS = listOf(
    DockItem(Screen.Home.route,     Icons.Default.Home,          "Home"),
    DockItem(Screen.Library.route,  Icons.Default.VideoLibrary,  "Library"),
    DockItem(Screen.Settings.route, Icons.Default.Settings,      "Settings")
)
```

No other changes to BottomDock.kt. Same glass, same height, same style.
Just 3 buttons now, equally spaced via `Arrangement.SpaceAround`.

---

## CHANGE 3 — NavGraph.kt  (add YouTube routes, remove Shorts from dock)

**File:** `app/src/main/java/com/streamix/ui/navigation/NavGraph.kt`

```kotlin
sealed class Screen(val route: String) {
    object Home           : Screen("home")
    object Search         : Screen("search")
    object Detail         : Screen("detail/{mediaId}/{mediaType}")
    object Library        : Screen("library")
    object Settings       : Screen("settings")
    object Passcode       : Screen("passcode")
    object YoutubeDetail  : Screen("youtube_detail/{videoId}")
    object AdultDetail    : Screen("adult_detail/{pageUrl}")
    // Shorts is NO LONGER a standalone nav route
    // It is a swipe pane inside YouTube and Adult homescreens
}

@Composable
fun StreamixNavGraph() {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.MOVIES) }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = { StreamixBottomDock(navController) }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController, profileState)
            }
            composable(Screen.Search.route) {
                SearchScreen(navController)
            }
            composable(
                route     = Screen.Detail.route,
                arguments = listOf(
                    navArgument("mediaId")   { type = NavType.StringType },
                    navArgument("mediaType") { type = NavType.StringType }
                )
            ) {
                DetailScreen(navController, it.arguments)
            }
            composable(Screen.Library.route)  { LibraryScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Passcode.route) { PasscodeScreen(navController, profileState) }
            composable(
                route     = Screen.YoutubeDetail.route,
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) {
                YoutubeDetailScreen(navController, it.arguments?.getString("videoId") ?: "")
            }
            composable(
                route     = Screen.AdultDetail.route,
                arguments = listOf(navArgument("pageUrl") { type = NavType.StringType })
            ) {
                AdultDetailScreen(navController, it.arguments?.getString("pageUrl") ?: "")
            }
        }
    }
}
```

---

## CHANGE 4 — HomeScreen.kt  (profile router)

**File:** `app/src/main/java/com/streamix/ui/home/HomeScreen.kt`

HomeScreen is now a pure router. It reads the current profile and
renders the correct screen. DO NOT modify the Movies or Songs UI at all.

```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    when (profileState.value) {

        Profile.MOVIES -> {
            // ── UNCHANGED: existing Movies home screen content ─────────────
            MoviesHomeContent(navController, profileState, homeViewModel)
        }

        Profile.SONGS -> {
            // ── UNCHANGED: existing Songs placeholder or screen ────────────
            SongsHomeContent(navController, profileState)
        }

        Profile.YOUTUBE -> {
            // ── NEW: YouTube profile with swipe-to-shorts ─────────────────
            SwipeableProfileHost(
                mainContent = {
                    YoutubeHomeScreen(navController, profileState)
                },
                shortsContent = {
                    ShortsScreen(
                        context = ShortsContext.YOUTUBE,
                        onClose = { /* swipe back handled by pager */ }
                    )
                }
            )
        }

        Profile.ADULT -> {
            // ── EXISTING: Adult home with swipe-to-shorts added ───────────
            SwipeableProfileHost(
                mainContent = {
                    AdultHomeScreen(navController, profileState)
                },
                shortsContent = {
                    ShortsScreen(
                        context = ShortsContext.ADULT,
                        onClose = { /* swipe back handled by pager */ }
                    )
                }
            )
        }
    }
}
```

---

## CREATE 5 — SwipeableProfileHost.kt  (the Instagram-style pager)

**File:** `app/src/main/java/com/streamix/ui/components/SwipeableProfileHost.kt`

This is the core of the feature. A HorizontalPager with 2 pages:
- Page 0 = Main home content (YouTube or Adult grid)
- Page 1 = Shorts vertical feed

The user swipes LEFT from page 0 to reach page 1 (shorts).
Swiping RIGHT from page 1 goes back to page 0 (home).

This matches Instagram's behaviour where swiping left opens DMs.

```kotlin
package com.streamix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableProfileHost(
    mainContent:   @Composable () -> Unit,
    shortsContent: @Composable () -> Unit
) {
    // Page 0 = Home, Page 1 = Shorts
    val pagerState = rememberPagerState(
        initialPage  = 0,
        pageCount    = { 2 }
    )

    HorizontalPager(
        state    = pagerState,
        modifier = Modifier.fillMaxSize(),
        // Swipe direction: left to go from home(0) → shorts(1)
        // This is the same direction as Instagram → DMs
        reverseLayout = false
    ) { page ->
        Box(Modifier.fillMaxSize()) {
            when (page) {
                0 -> mainContent()
                1 -> shortsContent()
            }
        }
    }
}
```

Optionally add a thin swipe indicator dot (like Instagram's camera dot):

```kotlin
// Add this INSIDE HomeScreen after SwipeableProfileHost
// Shows a small ">>" hint on YouTube/Adult home to indicate swipeable shorts

@Composable
fun ShortsSwipeHint(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(),
        exit    = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 8.dp, top = 80.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Swipe for Shorts",
                    tint  = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Shorts",
                    color    = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp
                )
            }
        }
    }
}
```

---

## CREATE 6 — ShortsScreen.kt  (full-screen vertical video feed)

**File:** `app/src/main/java/com/streamix/ui/shorts/ShortsScreen.kt`

Full-screen vertical pager. Each page is one short video playing in VLC
or inline. TikTok / Reels style layout.

Context enum tells the ViewModel which source to use:
- YOUTUBE → YouTube Shorts via YouTube Data API (shorts playlist)
- ADULT   → short clips from RedTube (duration < 10 min filter)

```kotlin
package com.streamix.ui.shorts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.streamix.ui.player.VlcPlayerLauncher

enum class ShortsContext { YOUTUBE, ADULT }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    context:   ShortsContext,
    onClose:   () -> Unit = {},
    viewModel: ShortsViewModel = hiltViewModel()
) {
    val items     by viewModel.shorts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val ctx       = LocalContext.current

    LaunchedEffect(context) { viewModel.load(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color    = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (items.isEmpty()) {
            Column(
                modifier              = Modifier.align(Alignment.Center),
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.PlayCircleOutline, null,
                    tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("No shorts available", color = Color.White.copy(0.4f), fontSize = 16.sp)
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { items.size })

            VerticalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = items[page]
                ShortVideoItem(
                    item    = item,
                    onPlay  = { VlcPlayerLauncher.launch(ctx, item.streamUrl, item.title) },
                    onLike  = { viewModel.toggleLike(item.id) }
                )
            }

            // Swipe back hint (top-left arrow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                IconButton(onClick = onClose) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(0.5f),
                                androidx.compose.foundation.shape.CircleShape),
                        Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "Back to Home",
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Text(
                    "Shorts",
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    modifier   = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun ShortVideoItem(
    item:   ShortsItem,
    onPlay: () -> Unit,
    onLike: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Thumbnail background
        AsyncImage(
            model            = item.thumbnailUrl,
            contentDescription = null,
            contentScale     = ContentScale.Crop,
            modifier         = Modifier.fillMaxSize()
        )

        // Dark scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Centre play button
        IconButton(
            onClick  = onPlay,
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .background(Color.White.copy(0.2f),
                    androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.Default.PlayArrow, "Play",
                tint = Color.White, modifier = Modifier.size(40.dp))
        }

        // Right-side action column (TikTok-style)
        Column(
            modifier            = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (item.isLiked) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        null,
                        tint     = if (item.isLiked) Color(0xFFE53935) else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(item.likes, color = Color.White, fontSize = 11.sp)
            }
            // Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { /* share */ }) {
                    Icon(Icons.Default.Share, null,
                        tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Text("Share", color = Color.White, fontSize = 11.sp)
            }
        }

        // Bottom info overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 80.dp, bottom = 32.dp)
        ) {
            Text(
                item.channelName,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.title,
                color    = Color.White.copy(0.85f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(4.dp))
                Text(
                    desc,
                    color    = Color.White.copy(0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

---

## CREATE 7 — ShortsItem.kt  (data model)

**File:** `app/src/main/java/com/streamix/ui/shorts/ShortsItem.kt`

```kotlin
package com.streamix.ui.shorts

data class ShortsItem(
    val id:           String,
    val title:        String,
    val thumbnailUrl: String,
    val streamUrl:    String,   // direct MP4 or HLS URL
    val channelName:  String = "",
    val description:  String? = null,
    val duration:     Int    = 0,   // seconds
    val views:        String = "",
    val likes:        String = "",
    val isLiked:      Boolean = false,
    val source:       ShortsContext = ShortsContext.YOUTUBE
)
```

---

## CREATE 8 — ShortsViewModel.kt

**File:** `app/src/main/java/com/streamix/ui/shorts/ShortsViewModel.kt`

```kotlin
package com.streamix.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.scraper.adult.RedTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val adultScraper: RedTubeScraper
    // inject YoutubeRepository when implemented
) : ViewModel() {

    private val _shorts    = MutableStateFlow<List<ShortsItem>>(emptyList())
    val shorts = _shorts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val likedIds   = mutableSetOf<String>()

    fun load(context: ShortsContext) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _shorts.value = when (context) {
                    ShortsContext.YOUTUBE -> loadYoutubeShorts()
                    ShortsContext.ADULT   -> loadAdultShorts()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadYoutubeShorts(): List<ShortsItem> {
        // Phase 1: return empty (YouTube Shorts API integration — next sprint)
        // Phase 2: call YoutubeRepository.getShorts()
        return emptyList()
    }

    private suspend fun loadAdultShorts(): List<ShortsItem> {
        // Get trending adult videos, filter to those under 10 minutes
        return try {
            adultScraper.getTrending(page = 1)
                .filter { result ->
                    // Parse duration string "MM:SS" or "H:MM:SS"
                    val secs = parseDuration(result.duration)
                    secs in 30..600  // 30 seconds to 10 minutes = "shorts"
                }
                .map { result ->
                    // Resolve actual stream URL for each short
                    val links  = adultScraper.getVideoLinks(result.id, "adult")
                    val stream = links.firstOrNull()?.url ?: result.id
                    ShortsItem(
                        id           = result.id,
                        title        = result.title,
                        thumbnailUrl = result.posterPath ?: "",
                        streamUrl    = stream,
                        channelName  = result.studio.takeIf { it.isNotBlank() } ?: "RedTube",
                        views        = result.views,
                        likes        = result.rating,
                        source       = ShortsContext.ADULT
                    )
                }
        } catch (e: Exception) { emptyList() }
    }

    fun toggleLike(id: String) {
        if (likedIds.contains(id)) likedIds.remove(id) else likedIds.add(id)
        _shorts.value = _shorts.value.map { item ->
            if (item.id == id) item.copy(isLiked = likedIds.contains(id)) else item
        }
    }

    private fun parseDuration(text: String): Int {
        val parts = text.split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2    -> parts[0] * 60 + parts[1]
            3    -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }
}
```

---

## CREATE 9 — YoutubeHomeScreen.kt

**File:** `app/src/main/java/com/streamix/ui/youtube/YoutubeHomeScreen.kt`

YouTube profile home. Same header/search bar as Movies. Grid of YouTube
video cards. Swipe left → Shorts (handled by SwipeableProfileHost wrapper).

```kotlin
package com.streamix.ui.youtube

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
import com.streamix.core.model.Profile
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import java.net.URLEncoder

@Composable
fun YoutubeHomeScreen(
    navController: NavController,
    profileState:  MutableState<Profile>,
    viewModel:     YoutubeHomeViewModel = hiltViewModel()
) {
    val trending     by viewModel.trending.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Reuse existing StreamixHeader — layout unchanged
        StreamixHeader(
            currentProfile     = profileState.value,
            onSettingsTap      = { navController.navigate("settings") },
            onProfileSwipe     = { dir ->
                val profiles = Profile.values().filter { it != Profile.ADULT }
                val current  = profiles.indexOf(profileState.value)
                val next     = (current + dir).coerceIn(0, profiles.size - 1)
                profileState.value = profiles[next]
            },
            onProfileTripleTap = { navController.navigate("passcode") }
        )

        StreamixSearchBar(
            query         = searchQuery,
            onQueryChange = viewModel::onQueryChange,
            onSearch      = viewModel::search,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Swipe hint indicator (first launch only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "← Shorts",
                color    = Color.White.copy(0.3f),
                fontSize = 11.sp
            )
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF0000))
            }
        } else {
            val displayItems = if (searchQuery.isNotBlank()) searchResults else trending

            LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                contentPadding      = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(displayItems, key = { it.id }) { video ->
                    YoutubeVideoCard(video) {
                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                        navController.navigate("youtube_detail/$encoded")
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubeVideoCard(item: YoutubeVideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0D0D))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model              = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
            // Duration badge
            item.duration.takeIf { it.isNotBlank() }?.let { dur ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(0.8f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(dur, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(
                item.title,
                color    = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                item.channelName,
                color    = Color.White.copy(0.5f),
                fontSize = 10.sp
            )
            Text(
                item.viewCount,
                color    = Color.White.copy(0.35f),
                fontSize = 10.sp
            )
        }
    }
}
```

---

## CREATE 10 — YoutubeVideoItem.kt  (data model)

**File:** `app/src/main/java/com/streamix/ui/youtube/YoutubeVideoItem.kt`

```kotlin
package com.streamix.ui.youtube

data class YoutubeVideoItem(
    val id:           String,   // YouTube video ID
    val title:        String,
    val thumbnailUrl: String,
    val channelName:  String = "",
    val viewCount:    String = "",
    val duration:     String = "",
    val publishedAt:  String = ""
)
```

---

## CREATE 11 — YoutubeHomeViewModel.kt

**File:** `app/src/main/java/com/streamix/ui/youtube/YoutubeHomeViewModel.kt`

```kotlin
package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeHomeViewModel @Inject constructor(
    // Phase 1: inject YoutubeRepository when implemented
    // private val youtubeRepo: YoutubeRepository
) : ViewModel() {

    private val _trending      = MutableStateFlow<List<YoutubeVideoItem>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YoutubeVideoItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    init { loadTrending() }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: replace with youtubeRepo.getTrending() once implemented
                // For now, static placeholder until YouTube Data API v3 is wired
                _trending.value = emptyList()
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
            delay(500)
            search(q)
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: youtubeRepo.search(q)
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

---

## FOLDER STRUCTURE DELTA (only new/changed files)

```
app/src/main/java/com/streamix/
│
├── core/model/
│   └── Profile.kt                    ← CHANGE: add YOUTUBE
│
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt               ← CHANGE: routes + remove Shorts dock
│   │
│   ├── components/
│   │   ├── BottomDock.kt             ← CHANGE: 3 items
│   │   └── SwipeableProfileHost.kt   ← NEW
│   │
│   ├── home/
│   │   └── HomeScreen.kt             ← CHANGE: profile router
│   │
│   ├── youtube/                      ← NEW FOLDER
│   │   ├── YoutubeVideoItem.kt
│   │   ├── YoutubeHomeScreen.kt
│   │   ├── YoutubeHomeViewModel.kt
│   │   ├── YoutubeDetailScreen.kt    ← (stub, wire later)
│   │   └── YoutubeDetailViewModel.kt ← (stub, wire later)
│   │
│   └── shorts/                       ← NEW FOLDER
│       ├── ShortsItem.kt
│       ├── ShortsScreen.kt
│       └── ShortsViewModel.kt
```

---

## DEPENDENCY ADDITION NEEDED

Add to `app/build.gradle.kts` — Foundation Pager is needed for both
the HorizontalPager (SwipeableProfileHost) and VerticalPager (Shorts):

```kotlin
// Already included via compose-bom — no extra line needed if using BOM 2024.02.00+
// But if Pager is missing, add explicitly:
implementation("androidx.compose.foundation:foundation:1.6.0")
```

HorizontalPager and VerticalPager are in `androidx.compose.foundation.pager`
since Compose Foundation 1.4.0. With the existing BOM this is already covered.

---

## BEHAVIOUR SPEC

```
PROFILE     | BOTTOM DOCK          | SWIPE LEFT         | SWIPE RIGHT
─────────────────────────────────────────────────────────────────────
Movies      | Home / Library / Sett | No action (blocked)| No action
Songs       | Home / Library / Sett | No action (blocked)| No action
YouTube     | Home / Library / Sett | Opens Shorts feed  | Back to Home
Adult       | Home / Library / Sett | Opens Shorts feed  | Back to Home
```

Profile switching:
```
HEADER ICON SWIPE DOWN  →  Movies → Songs → YouTube  (cycles)
HEADER ICON SWIPE UP    →  YouTube → Songs → Movies  (cycles)
HEADER ICON TRIPLE TAP  →  Always goes to Passcode → Adult
Adult never appears in swipe cycle
```

---

## WHAT YOU DO YOURSELF (not AI's job)

1. Wire YouTube Data API v3 key into `BuildConfig` when ready:
   `buildConfigField("String", "YOUTUBE_API_KEY", "\"YOUR_KEY\"")`

2. Implement `YoutubeRepository` using Retrofit against
   `https://www.googleapis.com/youtube/v3/` for trending + search

3. Implement `YoutubeDetailScreen` and `YoutubeDetailViewModel`
   to open videos in VLC or YouTube app via intent

4. For Adult Shorts: after completing RedTube selector inspection
   (Section B of the RedTube Master Prompt), the ShortsViewModel
   adult shorts will auto-work because it reuses RedTubeScraper

5. Test the swipe gesture on a physical device — emulators sometimes
   don't register horizontal swipes reliably

---

## THINGS NOT TO CHANGE

```
Movies homescreen content         ← DO NOT TOUCH
Songs homescreen content          ← DO NOT TOUCH
AdultHomeScreen grid layout       ← DO NOT TOUCH (only wrap in pager)
AdultDetailScreen                 ← DO NOT TOUCH
RedTubeScraper                    ← DO NOT TOUCH
VlcPlayerLauncher                 ← DO NOT TOUCH
PasscodeScreen                    ← DO NOT TOUCH
Design system (colors/glass/type) ← DO NOT TOUCH
ProGuard rules                    ← DO NOT TOUCH
AndroidManifest.xml               ← DO NOT TOUCH
```
