package com.streamix.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.model.VideoLink
import com.streamix.core.utils.ShareUtils
import com.streamix.ui.navigation.LocalBottomDockVisible
import com.streamix.ui.player.EmbeddedPlayer

@Composable
fun YoutubeDetailScreen(
    navController: NavController,
    videoId: String,
    viewModel: YoutubeDetailViewModel = hiltViewModel()
) {
    val title by viewModel.videoTitle.collectAsState()
    val description by viewModel.description.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val bottomDockVisible = LocalBottomDockVisible.current

    val activity = context as? android.app.Activity
    val isLandscape = activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    
    val videoUrl = "https://www.youtube.com/watch?v=$videoId"
    val links = listOf(VideoLink(url = videoUrl, quality = "Auto", server = "YouTube"))

    LaunchedEffect(videoId) {
        viewModel.load(videoId)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isLandscape) {
            EmbeddedPlayer(
                id = videoId,
                links = links,
                posterUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                modifier = Modifier.fillMaxSize(),
                isPlayingInitially = true
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        EmbeddedPlayer(
                            id = videoId,
                            links = links,
                            posterUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                            modifier = Modifier.fillMaxSize(),
                            isPlayingInitially = true
                        )
                    }
                }

                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            lineHeight = 26.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                var isLiked by remember { mutableStateOf(false) }
                                IconButton(onClick = { isLiked = !isLiked }) { 
                                    Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.White) 
                                }
                                Text("Like", color = Color.White, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { 
                                    ShareUtils.shareLink(context, title, videoUrl)
                                }) { Icon(Icons.Default.Share, null, tint = Color.White) }
                                Text("Share", color = Color.White, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { 
                                    android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.Download, null, tint = Color.White) }
                                Text("Download", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("Description", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (description.isNotBlank()) description else if (isLoading) "Loading description..." else "No description available",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }

            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .statusBarsPadding()
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
