package com.streamix.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.streamix.core.model.Profile
import com.streamix.ui.components.StreamixBottomDock
import com.streamix.ui.home.HomeScreen
import com.streamix.ui.search.SearchScreen
import com.streamix.ui.detail.DetailScreen
import com.streamix.ui.adult.AdultDetailScreen
import com.streamix.ui.library.LibraryScreen
import com.streamix.ui.youtube.YoutubeDetailScreen
import com.streamix.ui.youtube.YoutubeChannelScreen
import com.streamix.ui.youtube.YouTubeLoginScreen
import com.streamix.ui.movies.MoviesHomeScreen
import com.streamix.ui.movies.MoviesDetailScreen
import com.streamix.ui.settings.SettingsScreen
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.core.storage.PreferencesManager
import androidx.compose.ui.platform.LocalContext
import com.streamix.ui.player.PlayerManager
import com.streamix.ui.player.EmbeddedPlayer
import com.streamix.core.utils.UrlUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

import com.streamix.ui.components.UpdateDialog
import com.streamix.ui.components.StackedDock
import androidx.compose.material3.*
import com.streamix.ui.components.DownloadProgressDialog
import com.streamix.ui.navigation.UpdateViewModel
import com.streamix.ui.navigation.UpdateUIState
import com.streamix.MainActivity
import android.app.Activity
import android.content.Intent
import com.streamix.ui.components.DockFront
import com.streamix.ui.player.MinimizedPlayerBar
import com.streamix.ui.player.HistoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.streamix.core.analytics.AnalyticsManager
import com.streamix.core.analytics.AnalyticsEvent

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay

val LocalBottomDockVisible = compositionLocalOf { mutableStateOf(true) }
val LocalMainPagerIndex = compositionLocalOf { mutableStateOf(-1) }
val LocalProfile = compositionLocalOf { mutableStateOf(Profile.YOUTUBE) }

@Composable
fun StreamixNavGraph(analyticsManager: AnalyticsManager) {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.YOUTUBE) }
    val colors = LocalCustomColors.current
    val bottomDockVisible = rememberSaveable { mutableStateOf(true) }
    val mainPagerIndex = remember { mutableStateOf(-1) }
    
    val profileStack = remember { mutableStateListOf<Profile>() }
    var backPressedOnce by remember { mutableStateOf(false) }

    val historyViewModel: HistoryViewModel = hiltViewModel()
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val playerViewModel: com.streamix.ui.player.PlayerViewModel = hiltViewModel()
    
    val updateState by updateViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(isAuto = true)
    }
    
    val context = LocalContext.current
    val activity = context as? Activity
    val intent = (context as? MainActivity)?.intent

    LaunchedEffect(intent) {
        val data = intent?.data ?: return@LaunchedEffect
        
        // Handle streamix://watch scheme OR https gateway URL
        val isStreamixScheme = data.scheme == "streamix" && data.host == "watch"
        val isGatewayUrl = data.host == "shabd45261.github.io" && data.path?.contains("Streamix.github.io") == true
        
        if (isStreamixScheme || isGatewayUrl) {
            val profile = data.getQueryParameter("profile")
            val id = data.getQueryParameter("id")
            val t = data.getQueryParameter("t")?.toLongOrNull() ?: 0L
            val season = data.getQueryParameter("season")
            val episode = data.getQueryParameter("episode")
            
            if (id != null) {
                // Update Profile
                when (profile) {
                    "youtube" -> profileState.value = Profile.YOUTUBE
                    "movies" -> profileState.value = Profile.MOVIES
                    "adult" -> profileState.value = Profile.ADULT
                }

                PlayerManager.initialPlaybackPosition.value = t * 1000

                val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
                val route = when (profile) {
                    "youtube" -> "youtube_detail?videoId=$encodedId"
                    "movies" -> "movies_detail?movieId=$encodedId&season=$season&episode=$episode"
                    "adult" -> "adult_detail/$encodedId"
                    else -> null
                }

                route?.let {
                    navController.navigate(it) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
        // Legacy support
        else if (data.host == "v.inmoviebox.com") {
            val movieId = data.pathSegments.firstOrNull() ?: return@LaunchedEffect
            val timestamp = data.getQueryParameter("t")?.toLongOrNull() ?: 0L
            
            PlayerManager.initialPlaybackPosition.value = timestamp * 1000
            navController.navigate("movies_detail?movieId=$movieId") {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        
        // Clear intent so it doesn't trigger again on recomposition
        (context as? MainActivity)?.setIntent(null)
    }

    val prefs = remember { PreferencesManager(context) }
    val floatingDockEnabled by prefs.floatingDockEnabled.collectAsState(initial = false)
    var frontCard by rememberSaveable { mutableStateOf(DockFront.NAV) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        currentRoute?.let { 
            analyticsManager.logScreen(it)
        }
    }
    
    val playerVisible by PlayerManager.isVisible
    val playerMinimized by PlayerManager.isMinimized
    val currentVideo by PlayerManager.currentVideo
    val videoLinks by PlayerManager.videoLinks
    val relatedVideos by PlayerManager.relatedVideos
    val playerEpisodes by PlayerManager.episodes
    
    var showLibraryDialogByPlayer by remember { mutableStateOf(false) }
    val isHidden by PlayerManager.isHiddenTemporarily

    // Intercept profile changes to update stack
    val switchProfile: (Profile) -> Unit = remember {
        { newProfile ->
            if (profileState.value != newProfile) {
                profileStack.add(profileState.value)
                profileState.value = newProfile
            }
        }
    }

    CompositionLocalProvider(
        LocalBottomDockVisible provides bottomDockVisible,
        LocalMainPagerIndex provides mainPagerIndex,
        LocalProfile provides profileState
    ) {
        BackHandler {
            if (playerVisible && !playerMinimized && !isHidden) {
                PlayerManager.isMinimized.value = true
            } else if (navController.previousBackStackEntry != null) {
                // Only pop if we are NOT on a main screen route, or if we want to allow popping main screens
                // But the user wants double click to exit from home.
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                val isMainScreen = currentRoute == Screen.Home.route || 
                                   currentRoute == Screen.Search.route || 
                                   currentRoute == Screen.Library.route || 
                                   currentRoute == Screen.Settings.route ||
                                   currentRoute == Screen.Shorts.route

                if (isMainScreen) {
                    // Let the MainScreen's BackHandler or the exit logic handle it
                    if (profileStack.isNotEmpty()) {
                        val lastProfile = profileStack.removeAt(profileStack.size - 1)
                        profileState.value = lastProfile
                    } else {
                        if (backPressedOnce) {
                            (context as? ComponentActivity)?.finish()
                        } else {
                            backPressedOnce = true
                            Toast.makeText(context, "Double tap back to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    navController.popBackStack()
                }
            } else if (profileStack.isNotEmpty()) {
                val lastProfile = profileStack.removeAt(profileStack.size - 1)
                profileState.value = lastProfile
            } else {
                if (backPressedOnce) {
                    (context as? ComponentActivity)?.finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "Double tap back to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }

        LaunchedEffect(backPressedOnce) {
            if (backPressedOnce) {
                delay(2000)
                backPressedOnce = false
            }
        }

        Scaffold(
            containerColor = colors.primary,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { 
                val isPlayerFullScreen = playerVisible && !playerMinimized && !isHidden
                val showTraditionalDock = bottomDockVisible.value && !isPlayerFullScreen &&
                                          (!floatingDockEnabled || !playerVisible || !playerMinimized)
                
                // Ensure MinimizedPlayerBar shows up even if dock is hidden (e.g. on Shorts)
                val showMinimizedBarOnly = !bottomDockVisible.value && playerVisible && playerMinimized && !isHidden && !floatingDockEnabled

                if (showTraditionalDock || showMinimizedBarOnly) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (playerVisible && playerMinimized && !isHidden) {
                            val thumbUrl = remember(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id) {
                                UrlUtils.resolveImageUrl(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id ?: "")
                            }
                            Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) {
                                MinimizedPlayerBar(
                                    title = currentVideo?.title ?: "",
                                    subtitle = currentVideo?.studio ?: "Streamix",
                                    thumbUrl = thumbUrl,
                                    isPlaying = PlayerManager.isPlayingState.value,
                                    progress = PlayerManager.playbackProgress.value,
                                    onTogglePlay = {
                                        if (PlayerManager.isPlayingState.value) PlayerManager.pause()
                                        else PlayerManager.resume()
                                    },
                                    onClick = { PlayerManager.isMinimized.value = false },
                                    onClose = { PlayerManager.close() },
                                    height = 64.dp
                                )
                            }
                        }
                        if (showTraditionalDock) {
                            StreamixBottomDock(navController, profileOverride = profileState.value, includePadding = false)
                        }
                    }
                }
            }
        )
{ padding ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Home.route,
                    modifier         = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) { MainScreen(navController, profileState, switchProfile) }
                    composable(Screen.Shorts.route) { MainScreen(navController, profileState, switchProfile) }
                    composable(
                        route = Screen.Search.route,
                        arguments = listOf(
                            navArgument("query") { 
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { MainScreen(navController, profileState, switchProfile) }
                    composable(Screen.Library.route) { MainScreen(navController, profileState, switchProfile) }
                    composable(Screen.Settings.route) { MainScreen(navController, profileState, switchProfile) }
                    
                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(
                            navArgument("mediaId") { type = NavType.StringType },
                            navArgument("mediaType") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        DetailScreen(navController, backStackEntry.arguments)
                    }
                    composable(
                        route = Screen.AdultDetail.route,
                        arguments = listOf(
                            navArgument("pageUrl") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val pageUrl = backStackEntry.arguments?.getString("pageUrl") ?: ""
                        AdultDetailScreen(pageUrl, navController)
                    }
                    composable(Screen.YoutubeLogin.route) {
                        val context = LocalContext.current
                        val preferencesManager = remember { PreferencesManager(context) }
                        YouTubeLoginScreen(navController, preferencesManager, analyticsManager)
                    }
                    composable(
                        route = Screen.YoutubeDetail.route,
                        arguments = listOf(navArgument("videoId") { 
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                        YoutubeDetailScreen(navController, videoId)
                    }
                    composable(
                        route = Screen.YoutubeChannel.route,
                        arguments = listOf(navArgument("channelUrl") { 
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val channelUrl = backStackEntry.arguments?.getString("channelUrl") ?: ""
                        YoutubeChannelScreen(navController, channelUrl)
                    }
                    composable(
                        route = Screen.MoviesDetail.route,
                        arguments = listOf(
                            navArgument("movieId") { 
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("apiName") {
                                type = NavType.StringType
                                defaultValue = "MovieBoxPh"
                            },
                            navArgument("season") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("episode") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("fallbackUrl") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                        val apiName = backStackEntry.arguments?.getString("apiName") ?: "MovieBoxPh"
                        val season = backStackEntry.arguments?.getString("season")?.toIntOrNull()
                        val episode = backStackEntry.arguments?.getString("episode")?.toIntOrNull()
                        val fallbackUrl = backStackEntry.arguments?.getString("fallbackUrl")
                        MoviesDetailScreen(navController, movieId, apiName, season, episode, fallbackUrl)
                    }
                }

                if (playerVisible && currentVideo != null && !isHidden) {
                    EmbeddedPlayer(
                        id = currentVideo!!.id,
                        title = currentVideo!!.title,
                        subtitle = currentVideo!!.studio.ifBlank { "Streamix" },
                        mediaType = currentVideo!!.mediaType,
                        links = videoLinks,
                        relatedVideos = relatedVideos,
                        episodes = playerEpisodes,
                        posterUrl = currentVideo!!.posterPath,
                        isMinimized = playerMinimized,
                        initialPosition = PlayerManager.initialPlaybackPosition.value,
                        onMinimizedChange = { PlayerManager.isMinimized.value = it },
                        onClose = { PlayerManager.close() },
                        isPlayingInitially = true,
                        onProgressUpdate = { pos, total ->
                            historyViewModel.updateProgress(currentVideo!!, pos, total, PlayerManager.currentEpisode.value)
                        },
                        onVideoSelect = { video ->
                            PlayerManager.isMinimized.value = false
                            val encoded = java.net.URLEncoder.encode(video.id, "UTF-8")
                            val apiEncoded = java.net.URLEncoder.encode(video.studio, "UTF-8")
                            if (video.mediaType == "youtube") {
                                navController.navigate("youtube_detail?videoId=$encoded")
                            } else if (video.mediaType == "movie" || video.mediaType == "tv") {
                                navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                            } else if (video.mediaType == "adult") {
                                navController.navigate("adult_detail/$encoded")
                            }
                        },
                        onEpisodeSelect = { ep ->
                            currentVideo?.let { video ->
                                playerViewModel.playNextEpisode(ep, video)
                            }
                        },
                        onChannelClick = { url ->
                            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                            navController.navigate("youtube_channel?channelUrl=$encoded")
                            PlayerManager.isMinimized.value = true
                        },
                        modifier = Modifier.fillMaxSize(),
                        isStacked = floatingDockEnabled && playerMinimized,
                        playerViewModel = playerViewModel
                    )
                }

                if (floatingDockEnabled && playerVisible && playerMinimized && !isHidden) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        if (bottomDockVisible.value) {
                            StackedDock(
                                navBar = { StreamixBottomDock(navController, profileOverride = profileState.value) },
                                playerBar = {
                                    val thumbUrl = remember(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id) {
                                        UrlUtils.resolveImageUrl(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id ?: "")
                                    }
                                    val isLiked by (currentVideo?.let { historyViewModel.isLiked(it.id) } ?: kotlinx.coroutines.flow.flowOf(false)).collectAsState(initial = false)
                                    
                                    MinimizedPlayerBar(
                                        title = currentVideo?.title ?: "",
                                        subtitle = currentVideo?.studio ?: "Streamix",
                                        thumbUrl = thumbUrl,
                                        isPlaying = PlayerManager.isPlayingState.value,
                                        progress = PlayerManager.playbackProgress.value,
                                        onTogglePlay = {
                                            if (PlayerManager.isPlayingState.value) PlayerManager.pause()
                                            else PlayerManager.resume()
                                        },
                                        onClick = { PlayerManager.isMinimized.value = false },
                                        onClose = { PlayerManager.close() },
                                        onNext = {
                                            if (playerEpisodes.isNotEmpty() && PlayerManager.currentEpisode.value != null) {
                                                val idx = playerEpisodes.indexOfFirst { it.data == PlayerManager.currentEpisode.value?.data }
                                                if (idx != -1 && idx < playerEpisodes.size - 1) playerViewModel.playNextEpisode(playerEpisodes[idx + 1], currentVideo!!)
                                            } else if (relatedVideos.isNotEmpty()) {
                                                val firstRelated = relatedVideos.first()
                                                // Trigger onVideoSelect which handles navigation/playback
                                                val encoded = java.net.URLEncoder.encode(firstRelated.id, "UTF-8")
                                                val apiEncoded = java.net.URLEncoder.encode(firstRelated.studio, "UTF-8")
                                                if (firstRelated.mediaType == "youtube") {
                                                    navController.navigate("youtube_detail?videoId=$encoded")
                                                } else if (firstRelated.mediaType == "movie" || firstRelated.mediaType == "tv") {
                                                    navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                                } else if (firstRelated.mediaType == "adult") {
                                                    navController.navigate("adult_detail/$encoded")
                                                }
                                            }
                                        },
                                        onPrevious = {
                                            if (playerEpisodes.isNotEmpty() && PlayerManager.currentEpisode.value != null) {
                                                val idx = playerEpisodes.indexOfFirst { it.data == PlayerManager.currentEpisode.value?.data }
                                                if (idx > 0) playerViewModel.playNextEpisode(playerEpisodes[idx - 1], currentVideo!!)
                                            }
                                        },
                                        onAddToLibrary = { showLibraryDialogByPlayer = true },
                                        onLike = { currentVideo?.let { historyViewModel.addToWatchlist(it, "Liked") } },
                                        isLiked = isLiked,
                                        height = 70.dp
                                    )
                                },
                                frontCard = frontCard,
                                onFrontCardChange = { frontCard = it }
                            )
                        } else {
                            // Show only minimized bar when dock is hidden (Shorts)
                            val thumbUrl = remember(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id) {
                                UrlUtils.resolveImageUrl(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id ?: "")
                            }
                            Box(Modifier.padding(bottom = 16.dp, start = 12.dp, end = 12.dp)) {
                                val isLiked by (currentVideo?.let { historyViewModel.isLiked(it.id) } ?: kotlinx.coroutines.flow.flowOf(false)).collectAsState(initial = false)

                                MinimizedPlayerBar(
                                    title = currentVideo?.title ?: "",
                                    subtitle = currentVideo?.studio ?: "Streamix",
                                    thumbUrl = thumbUrl,
                                    isPlaying = PlayerManager.isPlayingState.value,
                                    progress = PlayerManager.playbackProgress.value,
                                    onTogglePlay = {
                                        if (PlayerManager.isPlayingState.value) PlayerManager.pause()
                                        else PlayerManager.resume()
                                    },
                                    onClick = { PlayerManager.isMinimized.value = false },
                                    onClose = { PlayerManager.close() },
                                    onNext = {
                                        if (playerEpisodes.isNotEmpty() && PlayerManager.currentEpisode.value != null) {
                                            val idx = playerEpisodes.indexOfFirst { it.data == PlayerManager.currentEpisode.value?.data }
                                            if (idx != -1 && idx < playerEpisodes.size - 1) playerViewModel.playNextEpisode(playerEpisodes[idx + 1], currentVideo!!)
                                        } else if (relatedVideos.isNotEmpty()) {
                                            val firstRelated = relatedVideos.first()
                                            val encoded = java.net.URLEncoder.encode(firstRelated.id, "UTF-8")
                                            val apiEncoded = java.net.URLEncoder.encode(firstRelated.studio, "UTF-8")
                                            if (firstRelated.mediaType == "youtube") {
                                                navController.navigate("youtube_detail?videoId=$encoded")
                                            } else if (firstRelated.mediaType == "movie" || firstRelated.mediaType == "tv") {
                                                navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                            } else if (firstRelated.mediaType == "adult") {
                                                navController.navigate("adult_detail/$encoded")
                                            }
                                        }
                                    },
                                    onPrevious = {
                                        if (playerEpisodes.isNotEmpty() && PlayerManager.currentEpisode.value != null) {
                                            val idx = playerEpisodes.indexOfFirst { it.data == PlayerManager.currentEpisode.value?.data }
                                            if (idx > 0) playerViewModel.playNextEpisode(playerEpisodes[idx - 1], currentVideo!!)
                                        }
                                    },
                                    onAddToLibrary = { showLibraryDialogByPlayer = true },
                                    onLike = { currentVideo?.let { historyViewModel.addToWatchlist(it, "Liked") } },
                                    isLiked = isLiked,
                                    height = 70.dp
                                )
                            }
                        }
                    }
                }

                if (showLibraryDialogByPlayer && currentVideo != null) {
                    AlertDialog(
                        onDismissRequest = { showLibraryDialogByPlayer = false },
                        title = { Text("Add to Library", color = Color.White) },
                        containerColor = Color(0xFF1A1A1A),
                        text = {
                            Column {
                                listOf("Watching", "Completed", "On-Hold", "Dropped", "Plan to Watch", "Favorites").forEach { status ->
                                    TextButton(
                                        onClick = { 
                                            historyViewModel.addToWatchlist(currentVideo!!, status)
                                            showLibraryDialogByPlayer = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(status, color = Color.White)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLibraryDialogByPlayer = false }) {
                                Text("CANCEL", color = Color.White.copy(0.6f))
                            }
                        }
                    )
                }

                // Update UI
                when (val state = updateState) {
                    is UpdateUIState.UpdateAvailable -> {
                        UpdateDialog(
                            info = state.info,
                            onDownload = { updateViewModel.downloadUpdate(state.info) },
                            onIgnore = { updateViewModel.ignoreUpdate(state.info.latestVersion) },
                            onExit = { (context as? Activity)?.finish() }
                        )
                    }
                    is UpdateUIState.Downloading -> {
                        DownloadProgressDialog(
                            progress = state.progress,
                            onCancel = { updateViewModel.resetState() }
                        )
                    }
                    is UpdateUIState.DownloadCompleted -> {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Download Complete", color = colors.secondary) },
                            text = { Text("The update has been downloaded. Tap to install.", color = colors.secondary.copy(0.7f)) },
                            confirmButton = {
                                Button(onClick = { updateViewModel.installUpdate(state.file) }) {
                                    Text("Install")
                                }
                            },
                            containerColor = colors.primary
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
