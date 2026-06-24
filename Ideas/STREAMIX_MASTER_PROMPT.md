# STREAMIX — COMPLETE MASTER BUILD PROMPT
## Android Streaming App | Full Implementation Guide

---

## TABLE OF CONTENTS

1. [Project Overview & Vision](#1-project-overview--vision)
2. [Design System](#2-design-system)
3. [Folder Structure](#3-folder-structure)
4. [Tech Stack & Dependencies](#4-tech-stack--dependencies)
5. [Core App Architecture](#5-core-app-architecture)
6. [UI Screens — Implementation](#6-ui-screens--implementation)
7. [Profile System](#7-profile-system)
8. [Video Streaming — Scraper Integration](#8-video-streaming--scraper-integration)
9. [TMDB Integration](#9-tmdb-integration)
10. [VLC Player Integration](#10-vlc-player-integration)
11. [CloudStream Extension System](#11-cloudstream-extension-system)
12. [Adult Profile & Passcode System](#12-adult-profile--passcode-system)
13. [Navigation & Bottom Dock](#13-navigation--bottom-dock)
14. [Settings Screen](#14-settings-screen)
15. [Library Screen](#15-library-screen)
16. [Shorts Screen](#16-shorts-screen)
17. [Build & Release Config](#17-build--release-config)

---

## 1. PROJECT OVERVIEW & VISION

**App Name:** Streamix  
**Platform:** Android (minSdk 21, targetSdk 34)  
**Language:** Kotlin  
**Architecture:** MVVM + Clean Architecture  

### User Journey (Allen's Story)
Allen finishes a long work day and opens Streamix on his phone. He is instantly greeted by a **black AMOLED, translucent frosted-glass UI** that feels premium. He searches for a movie, sees clean results from TMDB, clicks a movie, sees its details page (with cast, overview, genre tags), then plays it via VLC using video links scraped from Moviebox/Moviebox IN.

### Key Differentiators
- Black AMOLED + translucent frosted glass = premium feel
- 3-profile system: Movies, Songs (placeholder), Adult (triple-tap locked)
- Swipe-up/down profile switching on the header icon
- TMDB for all metadata, posters, thumbnails
- Moviebox + Moviebox IN as Server 1 & 2 (auto-failover)
- VLC as external video player
- Bottom dock styled after OrionStore reference image

---

## 2. DESIGN SYSTEM

### Color Palette
```kotlin
object StreamixColors {
    val Background     = Color(0xFF000000)  // True AMOLED black
    val Surface        = Color(0xFF0A0A0A)  // Elevated surface
    val GlassOverlay   = Color(0x1AFFFFFF)  // 10% white for glass effect
    val GlassBorder    = Color(0x33FFFFFF)  // 20% white border
    val TextPrimary    = Color(0xFFFFFFFF)  // Pure white
    val TextSecondary  = Color(0xFFAAAAAA)  // Muted text
    val Accent         = Color(0xFFFFFFFF)  // White accent (AMOLED-safe)
    val AccentBlue     = Color(0xFF4D8FFF)  // For interactive elements
    val TagBackground  = Color(0xFF1C1C1C)  // Genre tag bg
    val BottomDock     = Color(0xFF111111)  // Dock background
    val RippleWhite    = Color(0x22FFFFFF)  // Touch ripple
}
```

### Typography
```kotlin
val StreamixTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge     = TextStyle(fontSize = 16.sp, color = Color.White),
    bodyMedium    = TextStyle(fontSize = 14.sp, color = Color(0xFFAAAAAA)),
    labelSmall    = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp)
)
```

### Frosted Glass Card Modifier
```kotlin
fun Modifier.frostedGlass(
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.12f
): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.White.copy(alpha = alpha / 2)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )
    .border(
        width = 0.5.dp,
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    )
    // Use RenderEffect blur on API 31+ or software blur fallback below
```

### Glass Effect Implementation (API 31+ and fallback)
```kotlin
@Composable
fun FrostedGlassBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+ hardware blur
        Box(
            modifier = modifier
                .graphicsLayer {
                    renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
                }
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            content = content
        )
    } else {
        // Fallback: semi-transparent with border
        Box(
            modifier = modifier
                .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            content = content
        )
    }
}
```

---

## 3. FOLDER STRUCTURE

```
streamix/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/streamix/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── StreamixApp.kt
│   │   │   │   │
│   │   │   │   ├── core/
│   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── NetworkModule.kt       (Retrofit + OkHttp setup)
│   │   │   │   │   │   ├── TmdbApiService.kt
│   │   │   │   │   │   └── ScraperService.kt
│   │   │   │   │   ├── storage/
│   │   │   │   │   │   ├── PreferencesManager.kt  (DataStore)
│   │   │   │   │   │   └── WatchlistDao.kt        (Room)
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Movie.kt
│   │   │   │   │   │   ├── TvShow.kt
│   │   │   │   │   │   ├── SearchResult.kt
│   │   │   │   │   │   ├── VideoLink.kt
│   │   │   │   │   │   └── Profile.kt
│   │   │   │   │   └── utils/
│   │   │   │   │       ├── Extensions.kt
│   │   │   │   │       └── Constants.kt
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── tmdb/
│   │   │   │   │   │   ├── TmdbRepository.kt
│   │   │   │   │   │   └── TmdbMapper.kt
│   │   │   │   │   ├── scraper/
│   │   │   │   │   │   ├── MovieboxScraper.kt     (Server 1)
│   │   │   │   │   │   ├── MovieboxInScraper.kt   (Server 2)
│   │   │   │   │   │   └── ScraperRepository.kt   (failover logic)
│   │   │   │   │   └── watchlist/
│   │   │   │   │       └── WatchlistRepository.kt
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── SearchUseCase.kt
│   │   │   │   │   ├── GetMovieDetailUseCase.kt
│   │   │   │   │   ├── GetVideoLinksUseCase.kt
│   │   │   │   │   └── ProfileUseCase.kt
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── FrostedGlassBox.kt
│   │   │   │   │   │   ├── StreamixHeader.kt
│   │   │   │   │   │   ├── SearchBar.kt
│   │   │   │   │   │   ├── MovieCard.kt
│   │   │   │   │   │   ├── GenreTag.kt
│   │   │   │   │   │   ├── ProfileSwitcher.kt
│   │   │   │   │   │   └── BottomDock.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   │   ├── search/
│   │   │   │   │   │   ├── SearchScreen.kt
│   │   │   │   │   │   └── SearchViewModel.kt
│   │   │   │   │   ├── detail/
│   │   │   │   │   │   ├── DetailScreen.kt
│   │   │   │   │   │   └── DetailViewModel.kt
│   │   │   │   │   ├── library/
│   │   │   │   │   │   ├── LibraryScreen.kt
│   │   │   │   │   │   └── LibraryViewModel.kt
│   │   │   │   │   ├── shorts/
│   │   │   │   │   │   └── ShortsScreen.kt
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   │   ├── profile/
│   │   │   │   │   │   ├── ProfileSwitcherOverlay.kt
│   │   │   │   │   │   └── PasscodeScreen.kt
│   │   │   │   │   └── navigation/
│   │   │   │   │       └── NavGraph.kt
│   │   │   │   │
│   │   │   │   └── scraper/
│   │   │   │       ├── base/
│   │   │   │       │   ├── BaseScraper.kt
│   │   │   │       │   └── VideoExtractor.kt
│   │   │   │       ├── moviebox/
│   │   │   │       │   ├── MovieboxProvider.kt
│   │   │   │       │   └── MovieboxInProvider.kt
│   │   │   │       └── adult/
│   │   │   │           └── AdultScraper.kt
│   │   │   │
│   │   │   └── res/
│   │   │       ├── drawable/
│   │   │       │   └── ic_streamix_logo.xml
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 4. TECH STACK & DEPENDENCIES

### `libs.versions.toml`
```toml
[versions]
kotlin = "1.9.22"
compose-bom = "2024.02.00"
hilt = "2.50"
retrofit = "2.9.0"
okhttp = "4.12.0"
room = "2.6.1"
datastore = "1.0.0"
coil = "2.5.0"
vlc-android = "3.5.1"
jsoup = "1.17.2"
coroutines = "1.7.3"
lifecycle = "2.7.0"
navigation = "2.7.6"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

# Hilt DI
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.1.0" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore
datastore-prefs = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Image Loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# VLC
vlc-android = { group = "org.videolan.android", name = "libvlc-all", version.ref = "vlc-android" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Lifecycle
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

[plugins]
android-application = { id = "com.android.application", version = "8.2.2" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.22-1.0.17" }
```

### `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.streamix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.streamix"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "TMDB_API_KEY", "\"YOUR_TMDB_API_KEY_HERE\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.jsoup)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.prefs)
    implementation(libs.coil.compose)
    implementation(libs.vlc.android)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
}
```

---

## 5. CORE APP ARCHITECTURE

### `StreamixApp.kt`
```kotlin
@HiltAndroidApp
class StreamixApp : Application()
```

### `MainActivity.kt`
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        
        setContent {
            StreamixTheme {
                StreamixNavGraph()
            }
        }
    }
}
```

### `NavGraph.kt`
```kotlin
sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Search  : Screen("search")
    object Detail  : Screen("detail/{mediaId}/{mediaType}")
    object Library : Screen("library")
    object Shorts  : Screen("shorts")
    object Settings: Screen("settings")
    object Passcode: Screen("passcode")
}

@Composable
fun StreamixNavGraph() {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.MOVIES) }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = { StreamixBottomDock(navController) }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route)    { HomeScreen(navController, profileState) }
            composable(Screen.Search.route)  { SearchScreen(navController) }
            composable(Screen.Detail.route)  { DetailScreen(navController, it.arguments) }
            composable(Screen.Library.route) { LibraryScreen(navController) }
            composable(Screen.Shorts.route)  { ShortsScreen() }
            composable(Screen.Settings.route){ SettingsScreen() }
            composable(Screen.Passcode.route){ PasscodeScreen(navController, profileState) }
        }
    }
}
```

### `Profile.kt`
```kotlin
enum class Profile(val label: String, val icon: ImageVector) {
    MOVIES("Movies", Icons.Default.Movie),
    SONGS("Songs",   Icons.Default.MusicNote),
    ADULT("Adult",   Icons.Default.EighteenUpRating)
}
```

### `PreferencesManager.kt`
```kotlin
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.createDataStoreWithDefaults()
    
    companion object {
        val ADULT_PASSCODE_KEY = stringPreferencesKey("adult_passcode")
        val ADULT_PASSCODE_SET = booleanPreferencesKey("adult_passcode_set")
        val CURRENT_PROFILE    = stringPreferencesKey("current_profile")
    }
    
    val adultPasscode: Flow<String?> = dataStore.data
        .map { it[ADULT_PASSCODE_KEY] }
    
    val isPasscodeSet: Flow<Boolean> = dataStore.data
        .map { it[ADULT_PASSCODE_SET] ?: false }
    
    suspend fun setAdultPasscode(passcode: String) {
        dataStore.edit {
            it[ADULT_PASSCODE_KEY] = passcode
            it[ADULT_PASSCODE_SET] = true
        }
    }
}
```

---

## 6. UI SCREENS — IMPLEMENTATION

### StreamixHeader Component
```kotlin
// Design: Streamix logo (rounded-rect icon) + "Streamix" text on left
// Right side: Settings icon + Profile Switcher icon
// Search bar below header (frosted glass style, extended full-width)

@Composable
fun StreamixHeader(
    currentProfile: Profile,
    onSettingsTap: () -> Unit,
    onProfileSwipe: (direction: Int) -> Unit,  // -1 = up (prev), +1 = down (next)
    onProfileTripleTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App icon + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_streamix_logo),
                        contentDescription = "Streamix",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Streamix",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Right: Settings + Profile Switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSettingsTap) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF222222), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                ProfileSwitcherButton(
                    currentProfile = currentProfile,
                    onSwipe = onProfileSwipe,
                    onTripleTap = onProfileTripleTap
                )
            }
        }
    }
}
```

### Search Bar Component (Frosted Glass)
```kotlin
@Composable
fun StreamixSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            decorationBox = { inner ->
                Row(
                    Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null,
                        tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box { inner() }
                    if (query.isEmpty()) {
                        Text("Search movies, shows...",
                            color = Color.White.copy(0.4f), fontSize = 15.sp)
                    }
                }
            }
        )
    }
}
```

### Home Screen
```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val trendingMovies by viewModel.trending.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        item {
            StreamixHeader(
                currentProfile = profileState.value,
                onSettingsTap = { navController.navigate(Screen.Settings.route) },
                onProfileSwipe = { dir ->
                    val profiles = Profile.values()
                    val current = profiles.indexOf(profileState.value)
                    val next = (current + dir).coerceIn(0, profiles.size - 1)
                    profileState.value = profiles[next]
                },
                onProfileTripleTap = { navController.navigate(Screen.Passcode.route) }
            )
        }
        item {
            StreamixSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            // Featured banner (top trending item)
            trendingMovies.firstOrNull()?.let { featured ->
                FeaturedBanner(featured, navController)
            }
        }
        item {
            HorizontalMovieRow(title = "Trending Now", movies = trendingMovies, navController)
        }
        item {
            HorizontalMovieRow(title = "Top Rated", movies = viewModel.topRated.collectAsState().value, navController)
        }
    }
}
```

### Search Screen
```kotlin
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query   by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabs = listOf("Content", "Collections", "Cast & Crew")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        StreamixHeader(/* ... */)
        
        // Search bar (active/focused)
        StreamixSearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::search,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { i, tab ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { viewModel.selectTab(i) },
                    text = { Text(tab) }
                )
            }
        }
        
        // Results list
        LazyColumn {
            item {
                Text("SEARCH RESULTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.5f),
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            }
            items(results) { item ->
                SearchResultItem(item) {
                    navController.navigate("detail/${item.id}/${item.mediaType}")
                }
                Divider(color = Color.White.copy(0.07f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster thumbnail
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w92${result.posterPath}",
            contentDescription = null,
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(result.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                "${result.year} • ${result.mediaType.replaceFirstChar { it.uppercase() }}",
                color = Color.White.copy(0.5f),
                fontSize = 13.sp
            )
        }
    }
}
```

### Detail Screen
```kotlin
@Composable
fun DetailScreen(
    navController: NavController,
    arguments: Bundle?,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val mediaId   = arguments?.getString("mediaId") ?: return
    val mediaType = arguments?.getString("mediaType") ?: "movie"
    val detail    by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(mediaId) { viewModel.load(mediaId, mediaType) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        item {
            // Backdrop + gradient overlay
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w780${detail?.backdropPath}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Play button overlay
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    IconButton(
                        onClick = { viewModel.playVideo(mediaId, mediaType) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(0.25f), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, null,
                            tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                // Gradient bottom fade
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black),
                                startY = 150f
                            )
                        )
                )
            }
        }
        
        item {
            // Poster + title row
            Row(Modifier.padding(16.dp)) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${detail?.posterPath}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 90.dp, height = 130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "${if(mediaType=="tv") "Show" else "Movie"} • ${detail?.year} • ${detail?.runtime}m",
                        color = Color.White.copy(0.5f), fontSize = 12.sp
                    )
                    Text(detail?.title ?: "", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    // Metadata grid
                    MetadataRow("Director", detail?.director ?: "")
                    MetadataRow("Country", detail?.country ?: "")
                    MetadataRow("Language", detail?.language ?: "")
                    MetadataRow("Age Rating", detail?.ageRating ?: "")
                }
            }
        }
        
        item {
            // Action buttons
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Watch Now (primary)
                Button(
                    onClick = { viewModel.playVideo(mediaId, mediaType) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954) // green for watch
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Now")
                }
                Spacer(Modifier.height(8.dp))
                // Add to Library
                OutlinedButton(
                    onClick = { viewModel.toggleLibrary() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, null,
                        tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Collection", color = Color.White)
                }
            }
        }
        
        item {
            // Overview
            Column(Modifier.padding(16.dp)) {
                Text("Overview", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(detail?.overview ?: "", color = Color.White.copy(0.7f), fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
        
        item {
            // Genre tags
            FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                detail?.genres?.forEach { genre ->
                    GenreTag(genre)
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        
        // Server selection (shown when loading video links)
        item {
            ServerSelector(viewModel)
        }
    }
}

@Composable
fun GenreTag(label: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF1C1C1C), RoundedCornerShape(20.dp))
            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
```

---

## 7. PROFILE SYSTEM

### ProfileSwitcherButton
```kotlin
// The profile switcher button sits in the header (where moon icon was in reference)
// Swipe up/down = switch between Movies/Songs
// Triple tap = unlock Adult profile (requires passcode)

@Composable
fun ProfileSwitcherButton(
    currentProfile: Profile,
    onSwipe: (Int) -> Unit,
    onTripleTap: () -> Unit
) {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    val tripleTapWindow = 600L // ms

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF222222), CircleShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30) onSwipe(-1)  // swipe up = prev
                    else if (dragAmount > 30) onSwipe(1) // swipe down = next
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < tripleTapWindow) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = now
                    if (tapCount >= 3) {
                        tapCount = 0
                        onTripleTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = currentProfile.icon,
            contentDescription = currentProfile.label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
```

### Profile Switcher Overlay (full sheet)
```kotlin
// When the user taps the profile switcher, show a frosted glass bottom sheet
// Showing: Movies (film icon) | Songs (music note) | Adult (18+ - triple tap only)

@Composable
fun ProfileSwitcherOverlay(
    currentProfile: Profile,
    onSelect: (Profile) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.6f))
            .clickable(onClick = onDismiss),
        Alignment.BottomCenter
    ) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 48.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Switch Profile", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                
                listOf(Profile.MOVIES, Profile.SONGS).forEach { profile ->
                    ProfileOption(profile, currentProfile == profile) {
                        onSelect(profile)
                        onDismiss()
                    }
                }
                
                // Adult: shown as locked
                ProfileOption(Profile.ADULT, currentProfile == Profile.ADULT, locked = true) {
                    // Only reachable via triple tap
                }
            }
        }
    }
}

@Composable
fun ProfileOption(
    profile: Profile,
    selected: Boolean,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White.copy(0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(profile.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(profile.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (locked) {
            Icon(Icons.Default.Lock, null,
                tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
        } else if (selected) {
            Icon(Icons.Default.Check, null,
                tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
```

---

## 8. VIDEO STREAMING — SCRAPER INTEGRATION

### BaseScraper Architecture
```kotlin
// Based on CloudStream 3 provider pattern
// Reference docs: Creating your own Providers + Finding video links docs

abstract class BaseScraper {
    abstract val name: String
    abstract val mainUrl: String
    
    protected val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .header("Referer", mainUrl)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
            )
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
    abstract suspend fun search(query: String): List<SearchResult>
    abstract suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink>
    
    protected suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val req = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().body?.string() ?: ""
        }
    }
    
    protected suspend fun post(url: String, body: Map<String, String>): String {
        val formBody = FormBody.Builder().apply {
            body.forEach { (k, v) -> add(k, v) }
        }.build()
        val req = Request.Builder().url(url).post(formBody).build()
        return withContext(Dispatchers.IO) {
            client.newCall(req).execute().body?.string() ?: ""
        }
    }
}
```

### MovieboxProvider (Server 1)
```kotlin
// Pattern from phisher98/cloudstream-extensions-phisher
// Uses Jsoup for HTML parsing, OkHttp for requests

class MovieboxProvider : BaseScraper() {
    override val name = "Moviebox"
    override val mainUrl = "https://moviebox.ng"  // Update if domain changes
    
    override suspend fun search(query: String): List<SearchResult> {
        val doc = Jsoup.parse(get("$mainUrl/search?q=${query.replace(" ", "+")}"))
        return doc.select(".movie-item, .film-card").mapNotNull { el ->
            val title = el.select(".title, h3, .movie-title").text().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val href  = el.select("a").attr("href").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val img   = el.select("img").attr("src").orEmpty()
            SearchResult(
                id = href,
                title = title,
                posterPath = img,
                mediaType = if (href.contains("series") || href.contains("tv")) "tv" else "movie",
                year = el.select(".year").text()
            )
        }
    }
    
    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        // Step 1: Load detail page
        val baseUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
        val doc = Jsoup.parse(get(baseUrl))
        
        // Step 2: Find iFrame/embed sources
        val iframes = doc.select("iframe[src], .player iframe, #player iframe")
        val sources = mutableListOf<VideoLink>()
        
        iframes.forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty()) {
                // Step 3: Extract video link from embed
                val embedSources = extractFromEmbed(src)
                sources.addAll(embedSources)
            }
        }
        
        // Also check for direct m3u8/mp4 in page scripts
        val pageHtml = doc.html()
        extractM3u8FromPage(pageHtml)?.let {
            sources.add(VideoLink(url = it, quality = "Auto", server = name))
        }
        
        return sources
    }
    
    private suspend fun extractFromEmbed(embedUrl: String): List<VideoLink> {
        return try {
            val embedHtml = get(embedUrl, mapOf("Referer" to mainUrl))
            val links = mutableListOf<VideoLink>()
            
            // Look for m3u8 pattern
            val m3u8Regex = Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"")
            m3u8Regex.findAll(embedHtml).forEach { match ->
                links.add(VideoLink(url = match.groupValues[1], quality = "HLS", server = name))
            }
            
            // Look for mp4
            val mp4Regex = Regex("\"(https?://[^\"]+\\.mp4[^\"]*)\"")
            mp4Regex.findAll(embedHtml).forEach { match ->
                links.add(VideoLink(url = match.groupValues[1], quality = "MP4", server = name))
            }
            
            links
        } catch (e: Exception) { emptyList() }
    }
    
    private fun extractM3u8FromPage(html: String): String? {
        return Regex("file:\\s*['\"]([^'\"]+\\.m3u8[^'\"]*)['\"]")
            .find(html)?.groupValues?.get(1)
    }
}
```

### MovieboxInProvider (Server 2)
```kotlin
class MovieboxInProvider : BaseScraper() {
    override val name = "Moviebox IN"
    override val mainUrl = "https://moviebox.in"  // Update if domain changes
    
    override suspend fun search(query: String): List<SearchResult> {
        // Similar implementation to MovieboxProvider with site-specific selectors
        val doc = Jsoup.parse(get("$mainUrl/search?query=${query.replace(" ", "+")}"))
        return doc.select(".search-result, .movie-card").mapNotNull { el ->
            val title = el.select("h2, .title, .name").text().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val href  = el.select("a").first()?.attr("href") ?: return@mapNotNull null
            val img   = el.select("img").attr("src").orEmpty()
            SearchResult(
                id = href, title = title, posterPath = img,
                mediaType = if (href.contains("series")) "tv" else "movie",
                year = el.select(".year, .release").text()
            )
        }
    }
    
    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        val baseUrl = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
        val doc = Jsoup.parse(get(baseUrl))
        val sources = mutableListOf<VideoLink>()
        
        // Try API endpoint common to .in variant
        val apiResponse = try {
            get("$mainUrl/api/sources/${mediaId.substringAfterLast("/")}")
        } catch (e: Exception) { "" }
        
        if (apiResponse.isNotEmpty()) {
            val json = Gson().fromJson(apiResponse, JsonObject::class.java)
            json.getAsJsonArray("sources")?.forEach { src ->
                val obj = src.asJsonObject
                val url = obj.get("file")?.asString ?: return@forEach
                sources.add(VideoLink(url = url, quality = obj.get("label")?.asString ?: "Auto", server = name))
            }
        }
        
        return sources
    }
}
```

### ScraperRepository (Failover Logic)
```kotlin
@Singleton
class ScraperRepository @Inject constructor(
    private val server1: MovieboxProvider,
    private val server2: MovieboxInProvider
) {
    // Try Server 1 first, fallback to Server 2 on failure/empty result
    suspend fun getVideoLinks(mediaId: String, mediaType: String): Result<List<VideoLink>> {
        return try {
            val links = server1.getVideoLinks(mediaId, mediaType)
            if (links.isNotEmpty()) Result.success(links)
            else {
                // Failover to Server 2
                val fallback = server2.getVideoLinks(mediaId, mediaType)
                if (fallback.isNotEmpty()) Result.success(fallback)
                else Result.failure(Exception("No video links found on any server"))
            }
        } catch (e: Exception) {
            // Server 1 failed, try Server 2
            try {
                Result.success(server2.getVideoLinks(mediaId, mediaType))
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }
    
    // For manual server selection from UI
    suspend fun getVideoLinksFromServer(
        mediaId: String, mediaType: String, serverIndex: Int
    ): Result<List<VideoLink>> {
        val scraper = if (serverIndex == 0) server1 else server2
        return try {
            Result.success(scraper.getVideoLinks(mediaId, mediaType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### VideoLink Model
```kotlin
data class VideoLink(
    val url: String,
    val quality: String,       // "1080p", "720p", "HLS", "MP4"
    val server: String,        // "Moviebox", "Moviebox IN"
    val isM3u8: Boolean = url.contains(".m3u8"),
    val subtitleUrl: String? = null
)
```

---

## 9. TMDB INTEGRATION

### TMDB API Service
```kotlin
// Base URL: https://api.themoviedb.org/3/
// API Key: added in BuildConfig
// Image Base: https://image.tmdb.org/t/p/w{size}{path}
//   Sizes: w92, w154, w185, w342, w500, w780, original

interface TmdbApiService {
    
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key")     apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("query")       query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page")        page: Int = 1
    ): TmdbSearchResponse
    
    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("append_to_response") append: String = "credits,videos"
    ): TmdbMovieDetail
    
    @GET("tv/{tv_id}")
    suspend fun getTvDetail(
        @Path("tv_id")    tvId: String,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY,
        @Query("append_to_response") append: String = "credits,videos,seasons"
    ): TmdbTvDetail
    
    @GET("trending/{media_type}/week")
    suspend fun getTrending(
        @Path("media_type") type: String = "all",
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbPagedResponse
    
    @GET("movie/top_rated")
    suspend fun getTopRated(
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbPagedResponse
    
    @GET("collection/{collection_id}")
    suspend fun getCollection(
        @Path("collection_id") id: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbCollection
    
    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovies(
        @Path("person_id") id: Int,
        @Query("api_key") apiKey: String = BuildConfig.TMDB_API_KEY
    ): TmdbPersonCredits
}
```

### TMDB Response Models
```kotlin
data class TmdbSearchResponse(
    @SerializedName("results") val results: List<TmdbSearchResult>,
    @SerializedName("total_pages") val totalPages: Int
)

data class TmdbSearchResult(
    @SerializedName("id")            val id: Int,
    @SerializedName("title")         val title: String?,
    @SerializedName("name")          val name: String?,
    @SerializedName("media_type")    val mediaType: String,
    @SerializedName("poster_path")   val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date")  val releaseDate: String?,
    @SerializedName("first_air_date")val firstAirDate: String?,
    @SerializedName("overview")      val overview: String?
) {
    val displayTitle get() = title ?: name ?: "Unknown"
    val year get() = (releaseDate ?: firstAirDate)?.take(4) ?: ""
}

data class TmdbMovieDetail(
    @SerializedName("id")            val id: Int,
    @SerializedName("title")         val title: String,
    @SerializedName("overview")      val overview: String,
    @SerializedName("poster_path")   val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date")  val releaseDate: String,
    @SerializedName("runtime")       val runtime: Int,
    @SerializedName("vote_average")  val voteAverage: Double,
    @SerializedName("genres")        val genres: List<TmdbGenre>,
    @SerializedName("credits")       val credits: TmdbCredits?,
    @SerializedName("origin_country")val countries: List<String>?,
    @SerializedName("spoken_languages") val languages: List<TmdbLanguage>?,
    @SerializedName("belongs_to_collection") val collection: TmdbCollectionRef?,
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatings?
)
```

### TmdbRepository
```kotlin
@Singleton
class TmdbRepository @Inject constructor(
    private val api: TmdbApiService
) {
    suspend fun search(query: String): List<SearchResult> {
        return api.searchMulti(query = query).results.map { it.toSearchResult() }
    }
    
    suspend fun getDetail(id: String, type: String): MediaDetail {
        return if (type == "movie") {
            api.getMovieDetail(id).toMediaDetail()
        } else {
            api.getTvDetail(id).toMediaDetail()
        }
    }
    
    suspend fun getTrending(): List<SearchResult> {
        return api.getTrending().results.map { it.toSearchResult() }
    }
    
    private fun TmdbSearchResult.toSearchResult() = SearchResult(
        id = id.toString(),
        title = displayTitle,
        posterPath = posterPath,
        mediaType = mediaType,
        year = year
    )
}
```

---

## 10. VLC PLAYER INTEGRATION

### Open VLC for playback
```kotlin
// VLC is launched as an external intent — simplest and most reliable approach
// Add VLC dependency or use intent-based launch

object VlcPlayerLauncher {

    fun launch(context: Context, videoUrl: String, title: String) {
        try {
            // Method 1: LibVLC intent (most compatible)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), getMimeType(videoUrl))
                setPackage("org.videolan.vlc")
                putExtra("title", title)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Method 2: Generic video intent if VLC not installed
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), getMimeType(videoUrl))
            }
            context.startActivity(Intent.createChooser(fallback, "Open with"))
        }
    }
    
    private fun getMimeType(url: String) = when {
        url.contains(".m3u8") -> "application/x-mpegURL"
        url.contains(".mp4")  -> "video/mp4"
        url.contains(".mkv")  -> "video/x-matroska"
        else -> "video/*"
    }
}
```

### Add to AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Query VLC package -->
<queries>
    <package android:name="org.videolan.vlc" />
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:mimeType="video/*" />
    </intent>
</queries>
```

### DetailViewModel — play video
```kotlin
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val tmdbRepo: TmdbRepository,
    private val scraperRepo: ScraperRepository
) : ViewModel() {
    
    private val _videoLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val videoLinks = _videoLinks.asStateFlow()
    
    private val _selectedServer = MutableStateFlow(0)
    val selectedServer = _selectedServer.asStateFlow()
    
    fun playVideo(context: Context, mediaId: String, mediaType: String, title: String) {
        viewModelScope.launch {
            val result = scraperRepo.getVideoLinks(mediaId, mediaType)
            result.onSuccess { links ->
                _videoLinks.value = links
                links.firstOrNull()?.let { link ->
                    VlcPlayerLauncher.launch(context, link.url, title)
                }
            }.onFailure {
                // Show error state
            }
        }
    }
    
    fun switchServer(context: Context, mediaId: String, mediaType: String, title: String, serverIndex: Int) {
        viewModelScope.launch {
            _selectedServer.value = serverIndex
            val result = scraperRepo.getVideoLinksFromServer(mediaId, mediaType, serverIndex)
            result.onSuccess { links ->
                links.firstOrNull()?.let { VlcPlayerLauncher.launch(context, it.url, title) }
            }
        }
    }
}
```

### ServerSelector UI
```kotlin
@Composable
fun ServerSelector(viewModel: DetailViewModel, context: Context) {
    val selectedServer by viewModel.selectedServer.collectAsState()
    val servers = listOf("Server 1 — Moviebox", "Server 2 — Moviebox IN")
    
    Column(Modifier.padding(16.dp)) {
        Text("Servers", color = Color.White.copy(0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row {
            servers.forEachIndexed { i, label ->
                FilterChip(
                    selected = selectedServer == i,
                    onClick = { viewModel.switchServer(context, ..., i) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White.copy(0.15f),
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent,
                        labelColor = Color.White.copy(0.5f)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}
```

---

## 11. CLOUDSTREAM EXTENSION SYSTEM

### Key Concepts from Documentation
The app integrates with the CloudStream scraper pattern:

1. **Provider structure** = Search + Home + Load Result + Load Video Links
2. **Video links are the hardest part** — always implement and test these first
3. **Disguise scrapers** using proper headers (User-Agent, Referer, X-Requested-With)
4. **Find video links** by: locating iframe → opening iframe → finding m3u8/mp4 in network tab
5. **HTML parsing** uses Jsoup (CSS selectors like `doc.select("div.card-body")`)

### Anti-Detection Headers (from Disguishing docs)
```kotlin
val SCRAPER_HEADERS = mapOf(
    "User-Agent"      to "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
    "Accept"          to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language" to "en-US,en;q=0.9",
    "Accept-Encoding" to "gzip, deflate, br",
    "Connection"      to "keep-alive",
    "Upgrade-Insecure-Requests" to "1",
    "Sec-Fetch-Dest"  to "document",
    "Sec-Fetch-Mode"  to "navigate",
    "Sec-Fetch-Site"  to "none",
    "Cache-Control"   to "max-age=0"
)
```

### Video Link Extraction Strategy (from Finding video links docs)
```kotlin
// Strategy implemented in scrapers:
// 1. Load detail page → find <iframe src="..."> elements
// 2. Load each iframe URL (with Referer = main site URL)
// 3. In iframe HTML, search for:
//    - .m3u8 URLs (HLS streams)
//    - .mp4 URLs (direct video)
//    - JSON objects with "file", "src", "source" keys
//    - Encrypted/obfuscated JS (may need JS engine like Duktape)
// 4. Apply IP/time/referer headers to final video URL

suspend fun extractVideoFromIframe(iframeUrl: String, referer: String): List<VideoLink> {
    val html = get(iframeUrl, mapOf(
        "Referer" to referer,
        "User-Agent" to SCRAPER_HEADERS["User-Agent"]!!
    ))
    
    return buildList {
        // Direct HLS
        Regex("""["'](https?://[^"']*\.m3u8[^"']*)["']""")
            .findAll(html).forEach { add(VideoLink(it.groupValues[1], "HLS", "")) }
        
        // Direct MP4
        Regex("""["'](https?://[^"']*\.mp4[^"']*)["']""")
            .findAll(html).forEach { add(VideoLink(it.groupValues[1], "MP4", "")) }
        
        // JSON sources array
        Regex(""""file"\s*:\s*"([^"]+)"""")
            .findAll(html).forEach { add(VideoLink(it.groupValues[1], "Auto", "")) }
        
        // JWPlayer / Video.js sources
        Regex(""""src"\s*:\s*"(https?://[^"]+)"""")
            .findAll(html).forEach { add(VideoLink(it.groupValues[1], "Auto", "")) }
    }
}
```

---

## 12. ADULT PROFILE & PASSCODE SYSTEM

### Flow
1. User triple-taps the profile switcher icon in header
2. If passcode NOT yet set → show SetPasscodeScreen (first time)
3. If passcode IS set → show EnterPasscodeScreen
4. On success → switch to Adult profile
5. Adult profile shows content from AdultScraper (Redtube/similar)

### PasscodeScreen.kt
```kotlin
@Composable
fun PasscodeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    viewModel: PasscodeViewModel = hiltViewModel()
) {
    val isPasscodeSet by viewModel.isPasscodeSet.collectAsState(false)
    var entered by remember { mutableStateOf("") }
    var isError  by remember { mutableStateOf(false) }
    var isSettingNew by remember { mutableStateOf(false) }
    var confirmPass by remember { mutableStateOf("") }
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null,
                tint = Color.White.copy(0.7f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            
            Text(
                if (!isPasscodeSet) "Set Adult Passcode" else "Enter Passcode",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (!isPasscodeSet) "Choose a 4-digit PIN to protect adult content"
                else "Enter your 4-digit PIN",
                color = Color.White.copy(0.5f), fontSize = 14.sp, textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (i < entered.length) Color.White else Color.White.copy(0.2f),
                                CircleShape
                            )
                    )
                }
            }
            
            if (isError) {
                Spacer(Modifier.height(12.dp))
                Text("Incorrect PIN", color = Color.Red, fontSize = 13.sp)
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Number pad
            val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
            LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                items(digits) { digit ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .clickable(enabled = digit.isNotEmpty()) {
                                when (digit) {
                                    "⌫" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                    else -> {
                                        if (entered.length < 4) entered += digit
                                        if (entered.length == 4) {
                                            if (!isPasscodeSet) {
                                                viewModel.savePasscode(entered)
                                                profileState.value = Profile.ADULT
                                                navController.popBackStack()
                                            } else {
                                                viewModel.verifyPasscode(entered) { success ->
                                                    if (success) {
                                                        profileState.value = Profile.ADULT
                                                        navController.popBackStack()
                                                    } else {
                                                        isError = true
                                                        entered = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit.isNotEmpty()) {
                            Text(digit, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                        }
                    }
                }
            }
        }
    }
}
```

### Adult Scraper Note
```kotlin
// Adult scraper should follow same BaseScraper pattern
// Target: public adult video sites
// Layout mirrors Movies profile but WITHOUT: descriptions, actors, cast sections
// Same VLC player integration
// Use FrostedGlass + AMOLED black theme same as rest of app

class AdultScraper : BaseScraper() {
    override val name = "Adult"
    override val mainUrl = "https://www.redtube.com"
    
    override suspend fun search(query: String): List<SearchResult> {
        val doc = Jsoup.parse(get(
            "$mainUrl/?search=${query.replace(" ", "+")}",
            mapOf("Referer" to mainUrl)
        ))
        return doc.select(".video_link_container, .videoblock").mapNotNull { el ->
            val title = el.select(".title, .video_title a").text().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val href  = el.select("a").first()?.attr("href") ?: return@mapNotNull null
            val img   = el.select("img").attr("data-thumb_url").ifEmpty { el.select("img").attr("src") }
            SearchResult(id = href, title = title, posterPath = img, mediaType = "adult", year = "")
        }
    }
    
    override suspend fun getVideoLinks(mediaId: String, mediaType: String): List<VideoLink> {
        val url = if (mediaId.startsWith("http")) mediaId else "$mainUrl$mediaId"
        val html = get(url, mapOf("Referer" to mainUrl))
        
        return buildList {
            // Look for mediaDefinitions JSON object
            Regex(""""videoUrl"\s*:\s*"([^"]+)"""").findAll(html).forEach {
                add(VideoLink(it.groupValues[1].replace("\\/", "/"), "Auto", name))
            }
            Regex(""""quality_720p"\s*:\s*"([^"]+)"""").findAll(html).forEach {
                add(VideoLink(it.groupValues[1].replace("\\/", "/"), "720p", name))
            }
        }
    }
}
```

---

## 13. NAVIGATION & BOTTOM DOCK

### Bottom Dock (OrionStore-inspired)
```kotlin
// Reference: OrionStore bottom dock with icon + label layout
// Items: Home | Shorts | Library | Settings
// Frosted glass background, rounded top corners

data class DockItem(val route: String, val icon: ImageVector, val label: String)

val DOCK_ITEMS = listOf(
    DockItem(Screen.Home.route,     Icons.Default.Home,       "Home"),
    DockItem(Screen.Shorts.route,   Icons.Default.PlayCircle, "Shorts"),
    DockItem(Screen.Library.route,  Icons.Default.VideoLibrary,"Library"),
    DockItem(Screen.Settings.route, Icons.Default.Settings,   "Settings")
)

@Composable
fun StreamixBottomDock(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DOCK_ITEMS.forEach { item ->
                    val selected = currentRoute == item.route
                    DockButton(
                        item = item,
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DockButton(item: DockItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) Color.White else Color.White.copy(0.4f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            color = if (selected) Color.White else Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
```

---

## 14. SETTINGS SCREEN

```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        item { SettingsHeader() }
        
        item { SettingsSectionTitle("Playback") }
        item { SettingsToggle("Auto-play next episode", viewModel.autoPlay) }
        item { SettingsToggle("Remember server preference", viewModel.rememberServer) }
        
        item { SettingsSectionTitle("Content") }
        item { SettingsAction("Change Adult Passcode") { viewModel.changePasscode() } }
        item { SettingsToggle("Include adult in search", viewModel.adultInSearch) }
        
        item { SettingsSectionTitle("About") }
        item { SettingsInfo("Version", "1.0.0") }
        item { SettingsInfo("TMDB", "Powered by TMDB") }
    }
}
```

---

## 15. LIBRARY SCREEN

```kotlin
// Shows: Watchlist / Watched items saved by user
// Stored in Room database

@Composable
fun LibraryScreen(navController: NavController, viewModel: LibraryViewModel = hiltViewModel()) {
    val watchlist by viewModel.watchlist.collectAsState()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(Color.Black).statusBarsPadding(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            Text("My Library", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.padding(4.dp, 8.dp))
        }
        items(watchlist) { item ->
            MovieGridCard(item) {
                navController.navigate("detail/${item.id}/${item.mediaType}")
            }
        }
    }
}
```

---

## 16. SHORTS SCREEN

```kotlin
// Placeholder screen — vertical scrolling short clips feed
// Can be populated later with YouTube Shorts API or similar

@Composable
fun ShortsScreen() {
    Box(
        Modifier.fillMaxSize().background(Color.Black),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PlayCircleOutline, null,
                tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("Shorts coming soon", color = Color.White.copy(0.4f), fontSize = 16.sp)
        }
    }
}
```

---

## 17. BUILD & RELEASE CONFIG

### AndroidManifest.xml (key parts)
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <queries>
        <package android:name="org.videolan.vlc" />
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <data android:mimeType="video/*" />
        </intent>
    </queries>

    <application
        android:name=".StreamixApp"
        android:allowBackup="false"
        android:icon="@drawable/ic_streamix_logo"
        android:label="Streamix"
        android:theme="@style/Theme.Streamix"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### `res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### ProGuard Rules
```proguard
# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# TMDB models
-keep class com.streamix.core.model.** { *; }
-keep class com.streamix.data.tmdb.** { *; }

# VLC
-keep class org.videolan.** { *; }
```

---

## IMPLEMENTATION CHECKLIST

### Phase 1 — Core UI
- [ ] Set up project with all dependencies
- [ ] Implement Theme (colors, typography, glass effects)
- [ ] Build BottomDock component
- [ ] Build StreamixHeader component
- [ ] Build SearchBar component (frosted glass)
- [ ] Build HomeScreen skeleton
- [ ] Build SearchScreen
- [ ] Build DetailScreen (static UI first)

### Phase 2 — TMDB Integration
- [ ] Set up Retrofit + TmdbApiService
- [ ] Implement TmdbRepository
- [ ] Wire HomeViewModel (trending, top rated)
- [ ] Wire SearchViewModel (search multi)
- [ ] Wire DetailViewModel (movie/tv details)
- [ ] Test all TMDB API calls

### Phase 3 — Scraper Integration
- [ ] Implement BaseScraper
- [ ] Implement MovieboxProvider (Server 1)
- [ ] Implement MovieboxInProvider (Server 2)
- [ ] Implement ScraperRepository (failover)
- [ ] Wire video link extraction to DetailScreen
- [ ] Implement VLC launcher
- [ ] Test full play flow

### Phase 4 — Profile System
- [ ] Implement Profile enum + PreferencesManager
- [ ] Implement ProfileSwitcherButton (swipe + triple-tap)
- [ ] Implement PasscodeScreen
- [ ] Implement AdultScraper (placeholder)
- [ ] Wire profile state across app

### Phase 5 — Library + Persistence
- [ ] Set up Room database
- [ ] Implement WatchlistDao + WatchlistRepository
- [ ] Wire LibraryScreen
- [ ] Add add/remove from library on DetailScreen

### Phase 6 — Polish
- [ ] Implement SettingsScreen
- [ ] Add ShortsScreen placeholder
- [ ] Test all navigation flows
- [ ] Performance audit (image caching, lazy loading)
- [ ] ProGuard + release build

---

## IMPORTANT NOTES FOR IMPLEMENTATION

1. **TMDB API Key**: Get from https://www.themoviedb.org/settings/api and place in `BuildConfig`

2. **Scraper domains may change**: Moviebox and Moviebox IN URLs can change. Implement a remote config mechanism (simple JSON endpoint) to update base URLs without app update.

3. **Cloudstream reference repos**:
   - https://github.com/phisher98/cloudstream-extensions-phisher
   - https://github.com/NivinCNC/CNCVerse-Cloud-Stream-Extension
   - https://github.com/Asm0d3usX/CloudX-V2
   Study Kotlin scraper patterns there for video extraction techniques.

4. **Adult content**: The adult scraper must only be accessible via triple-tap + passcode. Never show adult content thumbnails/titles in Movies or Shorts profiles.

5. **VLC dependency**: Use `org.videolan.android:libvlc-all` for in-app playback, OR use intent-based VLC launch. Intent approach is simpler and recommended to start.

6. **Glass effect**: `BlurEffect` requires API 31+. Always provide a solid semi-transparent fallback for API 21-30.

7. **Scraper legal note**: Only use publicly accessible scrapers. The referenced GitHub repos contain open-source scrapers for sites that are publicly accessible.
```
