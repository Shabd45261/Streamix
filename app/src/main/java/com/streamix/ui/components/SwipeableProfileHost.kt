package com.streamix.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableProfileHost(
    mainContent:   @Composable () -> Unit,
    shortsContent: @Composable (isActive: Boolean) -> Unit
) {
    // Page 0 = Home, Page 1 = Shorts
    val pagerState = rememberPagerState(
        initialPage  = 0,
        pageCount    = { 2 }
    )
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (com.streamix.ui.shorts.ShortsPlaylistManager.playlistRequest.value != null) {
            pagerState.scrollToPage(1)
        }
    }

    if (pagerState.currentPage == 1) {
        BackHandler {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            com.streamix.ui.player.PlayerManager.pause()
            com.streamix.ui.player.PlayerManager.isHiddenTemporarily.value = true
        } else {
            com.streamix.ui.player.PlayerManager.isHiddenTemporarily.value = false
            // Keep it paused if coming back from shorts as requested?
            // "keep paused but minimized when i navigate back from the shorts to the main homescreen"
            // So DON'T call resume() automatically?
            // Actually the request says: "video should pause when i navigate to shorts and keep paused ... when i navigate back"
            // So I should only set isHiddenTemporarily = false, but not resume.
        }
    }

    HorizontalPager(
        state    = pagerState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = false,
        beyondBoundsPageCount = 0
    ) { page ->
        Box(Modifier.fillMaxSize()) {
            when (page) {
                0 -> mainContent()
                1 -> shortsContent(pagerState.currentPage == 1)
            }
        }
    }
}
