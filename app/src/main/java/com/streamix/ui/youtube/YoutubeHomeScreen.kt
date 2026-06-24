package com.streamix.ui.youtube

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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.model.Profile
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.navigation.Screen
import java.net.URLEncoder
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeHomeScreen(
    navController: NavController,
    profileState:  MutableState<Profile>,
    viewModel:     YoutubeHomeViewModel = hiltViewModel()
) {
    val trending     by viewModel.trending.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val showRefreshBanner by viewModel.showRefreshBanner.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadTrending()
            delay(1000)
            pullToRefreshState.endRefresh()
        }
    }

    BackHandler {
        if (searchQuery.isNotBlank()) {
            viewModel.onQueryChange("")
        } else {
            viewModel.onBackPress {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        StreamixHeader(
            currentProfile     = profileState.value,
            onSettingsTap      = { navController.navigate(Screen.Settings.route) },
            onProfileSelect    = { profile -> profileState.value = profile },
            onProfileTripleTap = { navController.navigate(Screen.Passcode.route) }
        )

        StreamixSearchBar(
            query         = searchQuery,
            onQueryChange = viewModel::onQueryChange,
            onSearch      = { viewModel.search(searchQuery) },
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            if (isLoading && trending.isEmpty() && searchResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.Red)
                }
            } else {
                val displayItems = if (searchQuery.isNotBlank()) searchResults else trending

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        // Refresh Banner
                        if (showRefreshBanner) {
                            item {
                                Button(
                                    onClick = { 
                                        viewModel.loadTrending()
                                        viewModel.dismissRefreshBanner()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Latest videos", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Featured Video
                        if (displayItems.isNotEmpty()) {
                            item {
                                YoutubeBiggerVideoCard(displayItems[0]) {
                                    val encoded = URLEncoder.encode(displayItems[0].id, "UTF-8")
                                    navController.navigate("youtube_detail/$encoded")
                                }
                            }
                        }

                        // Shorts Section
                        item {
                            Column(Modifier.padding(vertical = 12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayCircleFilled, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Shorts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(0.5f))
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(displayItems.drop(1).take(6)) { video ->
                                        YoutubeShortCard(video) {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("youtube_detail/$encoded")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Grid / List of videos
                    val startIdx = if (searchQuery.isBlank()) 7 else 0
                    val remaining = displayItems.drop(startIdx)
                    val chunks = remaining.chunked(2)
                    items(chunks) { chunk ->
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            chunk.forEach { video ->
                                Box(Modifier.weight(1f)) {
                                    YoutubeVideoCard(video) {
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_detail/$encoded")
                                    }
                                }
                            }
                            if (chunk.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.Black,
                contentColor = Color.White
            )
        }
    }
}

@Composable
fun YoutubeVideoCard(item: YoutubeVideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model              = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                item.title,
                color    = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.channelName,
                color    = Color.White.copy(0.6f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun YoutubeBiggerVideoCard(item: YoutubeVideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model              = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxWidth().height(220.dp)
        )
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.1f)), contentAlignment = Alignment.Center) {
                Text(item.channelName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${item.channelName} • ${item.viewCount}", color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
            Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun YoutubeShortCard(item: YoutubeVideoItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model              = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)), startY = 400f)))
        Text(
            item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        )
    }
}
