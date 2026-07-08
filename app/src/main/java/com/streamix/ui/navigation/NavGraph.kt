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
import com.streamix.ui.profile.PasscodeScreen
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

import com.streamix.ui.navigation.MainScreen
import com.streamix.ui.components.StackedDock
import com.streamix.ui.components.DockFront
import com.streamix.ui.player.MinimizedPlayerBar
import com.streamix.ui.player.HistoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay

val LocalBottomDockVisible = compositionLocalOf { mutableStateOf(true) }

@Composable
fun StreamixNavGraph() {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.YOUTUBE) }
    val colors = LocalCustomColors.current
    val bottomDockVisible = rememberSaveable { mutableStateOf(true) }
    
    val profileStack = remember { mutableStateListOf<Profile>() }
    var backPressedOnce by remember { mutableStateOf(false) }

    val historyViewModel: HistoryViewModel = hiltViewModel()
    
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val floatingDockEnabled by prefs.floatingDockEnabled.collectAsState(initial = false)
    var frontCard by rememberSaveable { mutableStateOf(DockFront.NAV) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Update bottom dock visibility based on current route
    LaunchedEffect(currentRoute) {
        bottomDockVisible.value = when (currentRoute) {
            Screen.Shorts.route, Screen.Passcode.route -> false
            else -> true
        }
    }
    
    val playerVisible by PlayerManager.isVisible
    val playerMinimized by PlayerManager.isMinimized
    val currentVideo by PlayerManager.currentVideo
    val videoLinks by PlayerManager.videoLinks
    val relatedVideos by PlayerManager.relatedVideos
    val playerEpisodes by PlayerManager.episodes
    val isHidden by PlayerManager.isHiddenTemporarily

    // Check if current route is one of the main ones
    val isMainScreen = currentRoute == null || 
                      currentRoute == Screen.Home.route || 
                      currentRoute == Screen.Shorts.route || 
                      currentRoute.startsWith("search") || 
                      currentRoute == Screen.Library.route || 
                      currentRoute == Screen.Settings.route

    // Intercept profile changes to update stack
    val switchProfile: (Profile) -> Unit = remember {
        { newProfile ->
            if (profileState.value != newProfile) {
                profileStack.add(profileState.value)
                profileState.value = newProfile
            }
        }
    }

    CompositionLocalProvider(LocalBottomDockVisible provides bottomDockVisible) {
        BackHandler {
            if (playerVisible && !playerMinimized && !isHidden) {
                PlayerManager.isMinimized.value = true
            } else if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
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
                val showTraditionalDock = bottomDockVisible.value && 
                                          !(!playerMinimized && playerVisible && !isHidden) &&
                                          (!floatingDockEnabled || !playerVisible || !playerMinimized)

                if (showTraditionalDock) {
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
                        StreamixBottomDock(navController, includePadding = false)
                    }
                }
            }
        ) { padding ->
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
                        YouTubeLoginScreen(navController, preferencesManager)
                    }
                    composable(Screen.Passcode.route) {
                        PasscodeScreen(navController, profileState)
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
                            navArgument("fallbackUrl") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                        val apiName = backStackEntry.arguments?.getString("apiName") ?: "MovieBoxPh"
                        val fallbackUrl = backStackEntry.arguments?.getString("fallbackUrl")
                        MoviesDetailScreen(navController, movieId, apiName, fallbackUrl)
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
                            // Logic for episode selection in player can be added here if needed
                        },
                        onChannelClick = { url ->
                            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                            navController.navigate("youtube_channel?channelUrl=$encoded")
                            PlayerManager.isMinimized.value = true
                        },
                        modifier = Modifier.fillMaxSize(),
                        isStacked = floatingDockEnabled && playerMinimized
                    )
                }

                if (floatingDockEnabled && playerVisible && playerMinimized && !isHidden && bottomDockVisible.value) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        StackedDock(
                            navBar = { StreamixBottomDock(navController) },
                            playerBar = {
                                val thumbUrl = remember(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id) {
                                    UrlUtils.resolveImageUrl(currentVideo?.posterPath, currentVideo?.mediaType, currentVideo?.id ?: "")
                                }
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
                                    height = 70.dp
                                )
                            },
                            frontCard = frontCard,
                            onFrontCardChange = { frontCard = it }
                        )
                    }
                }
            }
        }
    }
}
