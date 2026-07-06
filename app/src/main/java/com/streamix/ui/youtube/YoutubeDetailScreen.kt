package com.streamix.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.streamix.ui.player.PlayerManager
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@Composable
fun YoutubeDetailScreen(
    navController: NavController,
    videoId: String,
    viewModel: YoutubeDetailViewModel = hiltViewModel()
) {
    val title by viewModel.videoTitle.collectAsState()
    val links by viewModel.videoLinks.collectAsState()
    val relatedVideos by viewModel.relatedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val context = LocalContext.current
    
    LaunchedEffect(videoId) {
        viewModel.load(videoId)
    }

    LaunchedEffect(links, title) {
        if (links.isNotEmpty() && title.isNotBlank()) {
            PlayerManager.play(
                video = com.streamix.core.model.SearchResult(
                    id = videoId,
                    title = title,
                    posterPath = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                    mediaType = "youtube"
                ),
                links = links,
                related = relatedVideos
            )
            // Navigate back immediately as the player is now global
            navController.popBackStack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Red)
        }
    }
}
