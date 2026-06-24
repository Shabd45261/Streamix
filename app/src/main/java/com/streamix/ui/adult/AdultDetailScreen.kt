package com.streamix.ui.adult

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.streamix.core.utils.ShareUtils
import com.streamix.ui.detail.DetailActionButton
import com.streamix.ui.player.EmbeddedPlayer
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalLayoutApi::class)
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
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    
    val activity = context as? Activity
    val isLandscape = activity?.requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    LaunchedEffect(pageUrl) {
        val decoded = try { URLDecoder.decode(pageUrl, "UTF-8") } catch (_: Exception) { pageUrl }
        viewModel.load(decoded)
    }

    Box(Modifier.fillMaxSize().background(colors.primary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Player Box - Always present to prevent disposal on orientation change
            val playerHeight = if (isLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(260.dp)
            Box(modifier = playerHeight) {
                if (links.isNotEmpty() || (detail?.posterUrl != null)) {
                    EmbeddedPlayer(
                        id = pageUrl,
                        links = links,
                        posterUrl = detail?.posterUrl,
                        modifier = Modifier.fillMaxSize(),
                        isPlayingInitially = isLandscape, // Play if we just rotated to landscape
                        initialPosition = initialPosition,
                        onProgressUpdate = { current, total ->
                            viewModel.updateProgress(current, total)
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(colors.primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                        if (isLoading) CircularProgressIndicator(color = Color.Red)
                        else Text("No video streams found", color = colors.secondary.copy(0.4f))
                    }
                }
                
                // Back button only in portrait or as overlay
                if (!isLandscape) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (!isLandscape) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.padding(16.dp)) {
                            val title = detail?.title?.takeIf { it.isNotBlank() } ?: if (isLoading) "Loading..." else "Adult Video"
                            Text(
                                text = title, 
                                color = Color.White, 
                                fontWeight = FontWeight.Black, 
                                fontSize = 22.sp, 
                                lineHeight = 28.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            // Metadata Row
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Badge(text = detail?.studio ?: "Adult", containerColor = Color.White.copy(0.15f))
                                Badge(text = "TV-MA", containerColor = Color.White.copy(0.1f))
                                Text("•", color = Color.White.copy(0.4f))
                                Text(detail?.date ?: "2024", color = Color.White.copy(0.6f), fontSize = 13.sp)
                                Text("•", color = Color.White.copy(0.4f))
                                Text(detail?.rating ?: "8.5/10.0", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Description
                            val description = detail?.description?.takeIf { it.isNotBlank() } ?: "Metadata is not provided by site, video loading will fail if it does not exist on site."
                            Text(
                                text = description, 
                                color = Color.White.copy(alpha = 0.7f), 
                                fontSize = 14.sp, 
                                lineHeight = 20.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Cast / Performers
                    if (!detail?.performers.isNullOrEmpty()) {
                        item {
                            Column(Modifier.padding(vertical = 12.dp)) {
                                Text("Cast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(16.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    items(detail!!.performers) { performer ->
                                        CastItem(name = performer)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), 
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            var isLiked by remember { mutableStateOf(false) }
                            var isDisliked by remember { mutableStateOf(false) }
                            
                            DetailActionButton(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like") { 
                                isLiked = !isLiked
                                if (isLiked) isDisliked = false
                            }
                            DetailActionButton(if (isDisliked) Icons.Default.ThumbDownAlt else Icons.Default.ThumbDownOffAlt, "Dislike") { 
                                isDisliked = !isDisliked
                                if (isDisliked) isLiked = false
                            }
                            DetailActionButton(Icons.Default.Share, "Share") { 
                                ShareUtils.shareLink(context, detail?.title ?: "Adult Video", pageUrl)
                            }
                            DetailActionButton(Icons.Default.Download, "Download") { 
                                android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Categories / Tags
                    if (!detail?.tags.isNullOrEmpty()) {
                        item {
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail!!.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(tag, fontSize = 12.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color.White.copy(0.08f),
                                            labelColor = Color.White.copy(0.8f)
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    item {
                        Text("More Like This", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }

            items(trending) { video ->
                AdultVideoListCard(video) {
                    val encoded = URLEncoder.encode(video.id, "UTF-8")
                    navController.navigate("adult_detail/$encoded") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
                    
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, containerColor: Color) {
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CastItem(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
