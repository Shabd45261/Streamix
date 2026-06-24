package com.streamix.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.streamix.ui.settings.SettingsScreen
import com.streamix.ui.theme.LocalCustomColors

val LocalBottomDockVisible = compositionLocalOf { mutableStateOf(true) }

@Composable
fun StreamixNavGraph() {
    val navController = rememberNavController()
    val profileState  = rememberSaveable { mutableStateOf(Profile.YOUTUBE) }
    val colors = LocalCustomColors.current
    val bottomDockVisible = rememberSaveable { mutableStateOf(true) }

    CompositionLocalProvider(LocalBottomDockVisible provides bottomDockVisible) {
        Scaffold(
            containerColor = colors.primary,
            bottomBar = { 
                if (bottomDockVisible.value) {
                    StreamixBottomDock(navController) 
                }
            }
        ) { padding ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Home.route,
                modifier         = Modifier.padding(if (bottomDockVisible.value) padding else PaddingValues(0.dp))
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
                    LibraryScreen(navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController)
                }
                composable(Screen.Passcode.route) {
                    PasscodeScreen(navController, profileState)
                }
                composable(
                    route = Screen.YoutubeDetail.route,
                    arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                    YoutubeDetailScreen(navController, videoId)
                }
            }
        }
    }
}
