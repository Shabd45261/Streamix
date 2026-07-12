package com.streamix.ui.detail

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.utils.ShareUtils
import com.streamix.ui.navigation.LocalBottomDockVisible
import com.streamix.ui.player.EmbeddedPlayer
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.core.model.VideoLink

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavController,
    arguments: Bundle?,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val mediaId   = arguments?.getString("mediaId") ?: return
    val mediaType = arguments?.getString("mediaType") ?: "movie"
    val movieDetail by viewModel.movieDetail.collectAsState()
    val tvDetail    by viewModel.tvDetail.collectAsState()
    val videoLinks  by viewModel.videoLinks.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val context     = LocalContext.current
    val colors = LocalCustomColors.current
    val bottomDockVisible = LocalBottomDockVisible.current

    val title = if (mediaType == "movie") movieDetail?.title else tvDetail?.name
    val overview = if (mediaType == "movie") movieDetail?.overview else tvDetail?.overview
    val posterPath = if (mediaType == "movie") movieDetail?.poster_path else tvDetail?.poster_path
    val backdropPath = if (mediaType == "movie") movieDetail?.backdrop_path else tvDetail?.backdrop_path
    val releaseDate = if (mediaType == "movie") movieDetail?.release_date else tvDetail?.first_air_date
    val rating = if (mediaType == "movie") movieDetail?.vote_average else tvDetail?.vote_average
    val genres = if (mediaType == "movie") movieDetail?.genres else tvDetail?.genres
    val cast = if (mediaType == "movie") movieDetail?.credits?.cast else tvDetail?.credits?.cast
    val seasons = if (mediaType == "movie") null else tvDetail?.seasons
    val status = if (mediaType == "movie") "Movie" else tvDetail?.status ?: "Series"

    var isPlayerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(mediaId) { viewModel.load(mediaId, mediaType) }
    
    // Hide dock when player is visible
    LaunchedEffect(isPlayerVisible) {
        bottomDockVisible.value = !isPlayerVisible
    }

    if (isPlayerVisible && videoLinks.isNotEmpty()) {
        EmbeddedPlayer(
            id = mediaId,
            title = title ?: "Video",
            subtitle = status,
            links = videoLinks,
            modifier = Modifier.fillMaxSize(),
            isPlayingInitially = true,
            onFullScreenToggle = { full ->
                if (!full) isPlayerVisible = false
            }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(440.dp)) {
                    AsyncImage(
                        model = if (!backdropPath.isNullOrEmpty()) "https://image.tmdb.org/t/p/w780$backdropPath" else "https://image.tmdb.org/t/p/w780$posterPath",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.4f), Color.Black),
                                startY = 500f
                            )
                        )
                    )
                    
                    // Back Button
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(16.dp).statusBarsPadding().size(40.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Play Button in center
                    IconButton(
                        onClick = { 
                            viewModel.playVideo(context, mediaId, mediaType, title ?: "")
                            isPlayerVisible = true
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        if (isLoading && videoLinks.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp).offset(y = (-30).dp)) {
                    Text(
                        text = title ?: "Loading...",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        lineHeight = 38.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Badge(text = "TV-MA", containerColor = Color.White.copy(0.1f))
                        Text("•", color = Color.White.copy(0.4f))
                        Text(status, color = Color.White.copy(0.6f), fontSize = 13.sp)
                        Text("•", color = Color.White.copy(0.4f))
                        Text(releaseDate?.take(4) ?: "2024", color = Color.White.copy(0.6f), fontSize = 13.sp)
                        Text("•", color = Color.White.copy(0.4f))
                        Text("${String.format("%.1f", rating ?: 0.0)}/10.0", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text(
                        overview ?: "No description available.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))
                }
            }

            // Cast
            if (!cast.isNullOrEmpty()) {
                item {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("Cast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(Modifier.height(16.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(cast.take(15)) { member ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
                                    AsyncImage(
                                        model = "https://image.tmdb.org/t/p/w185${member.profile_path}",
                                        contentDescription = member.name,
                                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(0.1f)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(member.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(member.character, color = Color.White.copy(0.5f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Action Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
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
                        ShareUtils.shareLink(context, title ?: "Video", "https://www.themoviedb.org/$mediaType/$mediaId")
                    }
                    DetailActionButton(Icons.Default.Download, "Download") { 
                        android.widget.Toast.makeText(context, "Download not available for this server", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Genres
            if (!genres.isNullOrEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.forEach { genre ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(genre.name, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White.copy(0.08f),
                                    labelColor = Color.White.copy(0.8f)
                                ),
                                border = null,
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }

            // Seasons for TV
            if (!seasons.isNullOrEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Seasons", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            IconButton(onClick = { }) { Icon(Icons.Default.ExpandMore, null, tint = Color.White) }
                        }
                        
                        seasons.take(1).forEach { season ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("${season.name} • ${season.episode_count} Episodes", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val isWatchlisted by viewModel.isWatchlisted(mediaId).collectAsState()
                    
                    IconButton(onClick = { viewModel.toggleLibrary(mediaId, mediaType) }, modifier = Modifier.size(54.dp).background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))) {
                        Icon(if (isWatchlisted) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd, null, tint = Color.White)
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun Badge(text: String, containerColor: Color) {
    Box(
        modifier = Modifier.background(containerColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
