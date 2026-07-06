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
import androidx.compose.ui.Alignment

val LocalBottomDockVisible = compositionLocalOf { mutableStateOf(true) }

@Composable
fun StreamixNavGraph() {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.YOUTUBE) }
    val colors = LocalCustomColors.current
    val bottomDockVisible = rememberSaveable { mutableStateOf(true) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHome = currentRoute == Screen.Home.route
    val isChannel = currentRoute?.startsWith("youtube_channel") == true
    val isDetail = currentRoute?.contains("youtube_detail") == true
    val isMoviesDetail = currentRoute?.contains("movies_detail") == true
    val isHidden = PlayerManager.isHiddenTemporarily.value

    CompositionLocalProvider(LocalBottomDockVisible provides bottomDockVisible) {
        val playerVisible = PlayerManager.isVisible.value
        val playerMinimized = PlayerManager.isMinimized.value
        val currentVideo = PlayerManager.currentVideo.value
        val videoLinks = PlayerManager.videoLinks.value
        val relatedVideos = PlayerManager.relatedVideos.value
        val playerEpisodes = PlayerManager.episodes.value

        Scaffold(
            containerColor = colors.primary,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { 
                if (bottomDockVisible.value && !(!playerMinimized && playerVisible && !isHidden)) {
                    StreamixBottomDock(navController) 
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Home.route,
                    modifier         = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(navController, profileState)
                    }
                    composable(
                        route = Screen.Search.route + "/{query}",
                        arguments = listOf(navArgument("query") { 
                            type = NavType.StringType
                            nullable = true
                        })
                    ) { backStackEntry ->
                        val query = backStackEntry.arguments?.getString("query")
                        SearchScreen(navController, query)
                    }
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
                    composable(Screen.Library.route) {
                        LibraryScreen(navController, profileState)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(navController)
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
                                defaultValue = "MovieBox"
                            },
                            navArgument("fallbackUrl") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
                        val apiName = backStackEntry.arguments?.getString("apiName") ?: "MovieBox"
                        val fallbackUrl = backStackEntry.arguments?.getString("fallbackUrl")
                        MoviesDetailScreen(navController, movieId, apiName, fallbackUrl)
                    }
                }

                if (playerVisible && currentVideo != null && (isHome || isChannel || isMoviesDetail) && !isHidden) {
                    EmbeddedPlayer(
                        id = currentVideo.id,
                        title = currentVideo.title,
                        subtitle = currentVideo.studio.ifBlank { "MovieBox" },
                        mediaType = currentVideo.mediaType,
                        links = videoLinks,
                        relatedVideos = relatedVideos,
                        episodes = playerEpisodes,
                        posterUrl = currentVideo.posterPath,
                        isMinimized = playerMinimized,
                        onMinimizedChange = { PlayerManager.isMinimized.value = it },
                        onClose = { PlayerManager.close() },
                        isPlayingInitially = true,
                        onVideoSelect = { video ->
                            val encoded = java.net.URLEncoder.encode(video.id, "UTF-8")
                            val apiEncoded = java.net.URLEncoder.encode(video.studio, "UTF-8")
                            if (video.mediaType == "youtube") {
                                navController.navigate("youtube_detail?videoId=$encoded")
                            } else if (video.mediaType == "movie") {
                                navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                            }
                        },
                        onEpisodeSelect = { ep ->
                            // When an episode is selected from the player's now playing list
                            // We need to fetch links for it. This logic should ideally be in a central place.
                            // For now, we can trigger a re-load in the detail screen if it's open, 
                            // or just call the repository directly.
                            // However, the easiest way is to let the player components handle link fetching if we had a dedicated VM for it.
                            // For now, let's keep it simple: if it's a series, the detail screen is likely the one that launched it.
                        },
                        onChannelClick = { url ->
                            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                            navController.navigate("youtube_channel?channelUrl=$encoded")
                            PlayerManager.isMinimized.value = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
