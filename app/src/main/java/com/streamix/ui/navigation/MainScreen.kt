package com.streamix.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.streamix.core.model.Profile
import com.streamix.ui.home.HomeScreen
import com.streamix.ui.search.SearchScreen
import com.streamix.ui.library.LibraryScreen
import com.streamix.ui.settings.SettingsScreen
import com.streamix.ui.shorts.ShortsScreen
import com.streamix.ui.shorts.ShortsContext
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.compose.currentBackStackEntryAsState
import com.streamix.ui.player.PlayerManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    profileState: MutableState<Profile>
) {
    val profile = profileState.value
    val scope = rememberCoroutineScope()

    val screens = remember(profile) {
        when (profile) {
            Profile.YOUTUBE, Profile.ADULT -> listOf(
                Screen.Home,
                Screen.Shorts,
                Screen.Search,
                Screen.Library,
                Screen.Settings
            )
            else -> listOf(
                Screen.Home,
                Screen.Search,
                Screen.Library,
                Screen.Settings
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { screens.size })
    
    // History for back button
    val pageHistory = rememberSaveable(
        saver = listSaver<SnapshotStateList<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { 
        mutableStateListOf(0) 
    }

    // Update history when page changes
    LaunchedEffect(pagerState.currentPage) {
        if (pageHistory.isEmpty() || pageHistory.last() != pagerState.currentPage) {
            pageHistory.add(pagerState.currentPage)
            if (pageHistory.size > 20) pageHistory.removeAt(0)
        }
    }

    // Back handler: only enabled if we have history AND player is not in full-screen mode
    val playerMinimized = PlayerManager.isMinimized.value
    val playerVisible = PlayerManager.isVisible.value
    val isPlayerFullScreen = playerVisible && !playerMinimized

    BackHandler(enabled = pageHistory.size > 1 && !isPlayerFullScreen) {
        pageHistory.removeAt(pageHistory.size - 1)
        val prevPage = pageHistory.last()
        scope.launch {
            pagerState.animateScrollToPage(prevPage)
        }
    }

    // Handle external navigation by updating pager
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        val route = navBackStackEntry?.destination?.route ?: return@LaunchedEffect
        val baseRoute = route.split("?")[0].split("/")[0]
        val index = screens.indexOfFirst { it.route.startsWith(baseRoute) }
        if (index != -1 && index != pagerState.currentPage) {
            pagerState.scrollToPage(index)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        beyondBoundsPageCount = 1,
        pageSpacing = 16.dp,
        flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = 0.3f
        )
    ) { page ->
        val screen = screens.getOrNull(page) ?: return@HorizontalPager
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                is Screen.Home -> HomeScreen(navController, profileState)
                is Screen.Shorts -> {
                    val context = if (profile == Profile.ADULT) ShortsContext.ADULT else ShortsContext.YOUTUBE
                    ShortsScreen(
                        context = context,
                        onClose = { 
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        isScreenActive = pagerState.currentPage == page
                    )
                }
                is Screen.Search -> {
                    val query = navBackStackEntry?.arguments?.getString("query")
                    SearchScreen(navController, query)
                }
                is Screen.Library -> LibraryScreen(navController, profileState)
                is Screen.Settings -> SettingsScreen(navController)
                else -> {}
            }
        }
    }
}
