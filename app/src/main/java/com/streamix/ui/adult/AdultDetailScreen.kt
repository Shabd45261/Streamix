package com.streamix.ui.adult

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.streamix.ui.player.EmbeddedPlayer
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AdultDetailScreen(
    pageUrl: String,
    navController: NavController,
    viewModel: AdultDetailViewModel = hiltViewModel()
) {
    val detail    by viewModel.detail.collectAsState()
    val links     by viewModel.videoLinks.collectAsState()
    val trending  by viewModel.trending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val initialPosition by viewModel.initialPosition.collectAsState()
    val context = LocalContext.current
    
    val activity = context as? Activity
    
    LaunchedEffect(pageUrl) {
        val decoded = try { URLDecoder.decode(pageUrl, "UTF-8") } catch (_: Exception) { pageUrl }
        viewModel.load(decoded)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (links.isNotEmpty() || isLoading) {
            EmbeddedPlayer(
                id = pageUrl,
                title = detail?.title ?: if (isLoading) "Loading..." else "Adult Video",
                subtitle = detail?.studio ?: "Adult",
                mediaType = "adult",
                links = links,
                relatedVideos = trending,
                posterUrl = detail?.posterUrl,
                modifier = Modifier.fillMaxSize(),
                isPlayingInitially = true,
                initialPosition = initialPosition,
                onFullScreenToggle = { /* handled inside */ },
                onProgressUpdate = { current, total ->
                    viewModel.updateProgress(current, total)
                },
                onVideoSelect = { video ->
                    val encoded = URLEncoder.encode(video.id, "UTF-8")
                    navController.navigate("adult_detail/$encoded") {
                        popUpTo("adult_detail") { inclusive = false }
                    }
                }
            )
        }
    }
}
