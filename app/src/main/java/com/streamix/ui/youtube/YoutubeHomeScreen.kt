package com.streamix.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamix.core.model.Profile
import com.streamix.core.model.SearchResult
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.components.VideoOptionsPopup
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YoutubeHomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    viewModel: YoutubeHomeViewModel = hiltViewModel()
) {
    val trending    by viewModel.trending.collectAsState()
    val history     by viewModel.history.collectAsState()
    val subscribed  by viewModel.subscribed.collectAsState()
    val recommended by viewModel.recommended.collectAsState()
    
    val isLoading   by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val showRefreshBanner by viewModel.showRefreshBanner.collectAsState()

    val colors = LocalCustomColors.current
    val pullToRefreshState = rememberPullToRefreshState()

    BackHandler {
        if (searchQuery.isNotBlank()) {
            viewModel.onQueryChange("")
        } else {
            viewModel.onBackPress {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullToRefreshState.endRefresh()
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }

    // Main container with background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        StreamixHeader(
                            currentProfile = profileState.value,
                            onSettingsTap = { navController.navigate(Screen.Settings.route) },
                            onProfileSelect = { profile -> profileState.value = profile },
                            onProfileTripleTap = { navController.navigate(Screen.Passcode.route) }
                        )
                    }

                    item {
                        Box(modifier = Modifier.fillMaxWidth().background(colors.primary).padding(horizontal = 16.dp, vertical = 8.dp)) {
                            StreamixSearchBar(
                                query = searchQuery,
                                onQueryChange = viewModel::onQueryChange,
                                onSearch = { viewModel.search(searchQuery) }
                            )
                        }
                    }

                    if (searchQuery.isBlank()) {
                        if (showRefreshBanner) {
                            item {
                                Button(
                                    onClick = { 
                                        viewModel.refresh()
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

                        // 0. Hero Section
                        if (trending.isNotEmpty()) {
                            item {
                                val heroItems = trending.take(5)
                                val pagerState = rememberPagerState(pageCount = { heroItems.size })
                                
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth().height(480.dp)
                                ) { page ->
                                    val video = heroItems[page]
                                    YoutubeHeroSection(
                                        item = video,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("youtube_detail?videoId=$encoded")
                                        }
                                    )
                                }
                            }
                        }

                        // 1. Recent Watch
                        if (history.isNotEmpty()) {
                            item { SectionHeader("Recent Watch") }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(history.take(12), key = { "history_${it.id}" }) { video ->
                                        YoutubeHorizontalCard(
                                            item = video, 
                                            width = 180.dp,
                                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                            onClick = {
                                                val encoded = URLEncoder.encode(video.id, "UTF-8")
                                                navController.navigate("youtube_detail?videoId=$encoded")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Just Added
                        if (trending.size > 5) {
                            item { SectionHeader("Just Added") }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(trending.drop(5).take(12)) { video ->
                                        YoutubeHorizontalCard(
                                            item = video, 
                                            width = 240.dp,
                                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                            onClick = {
                                                val encoded = URLEncoder.encode(video.id, "UTF-8")
                                                navController.navigate("youtube_detail?videoId=$encoded")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Subscribed
                        if (subscribed.isNotEmpty()) {
                            item { SectionHeader("Subscribed") }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(subscribed.take(12)) { video ->
                                        YoutubeHorizontalCard(
                                            item = video, 
                                            width = 240.dp,
                                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                            onClick = {
                                                val encoded = URLEncoder.encode(video.id, "UTF-8")
                                                navController.navigate("youtube_detail?videoId=$encoded")
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Recommended
                        if (recommended.isNotEmpty()) {
                            item { SectionHeader("Recommended") }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(recommended.take(12)) { video ->
                                        YoutubeHorizontalCard(
                                            item = video, 
                                            width = 240.dp,
                                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                            onClick = {
                                                val encoded = URLEncoder.encode(video.id, "UTF-8")
                                                navController.navigate("youtube_detail?videoId=$encoded")
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Vertical list for Recommended (actual feed recommendations)
                            val feedRecommended = recommended.drop(12)
                            items(feedRecommended) { video ->
                                YoutubeVideoListCard(
                                    item = video,
                                    onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                    onClick = {
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                        }

                        // 5. More Videos (Trending remaining)
                        if (trending.size > 17) {
                            item { SectionHeader("More Videos") }
                            val remaining = trending.drop(17)
                            items(remaining) { video ->
                                YoutubeVideoListCard(
                                    item = video,
                                    onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                    onClick = {
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                        }

                    } else {
                        // Search Results Section
                        items(searchResults, key = { "search_${it.id}" }) { video ->
                            if (video.mediaType == "youtube_channel") {
                                YoutubeChannelListCard(
                                    item = video,
                                    onClick = {
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_channel?channelUrl=$encoded")
                                    }
                                )
                            } else {
                                YoutubeVideoListCard(
                                    item = video,
                                    onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                    onClick = {
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                        }
                        
                        if (!isLoading && searchResults.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("No results found", color = Color.White.copy(0.4f))
                                }
                            }
                        }
                    }
                }
                
                // PullToRefreshContainer placed inside the content area box to avoid overlapping search bar
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                )
            }
        }
        
        if (isLoading && trending.isEmpty() && searchQuery.isBlank()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        }
        
        if (isLoading && searchQuery.isNotBlank() && searchResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = colors.secondary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = colors.secondary.copy(0.5f))
    }
}

@Composable
fun YoutubeHeroSection(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: (SearchResult) -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick(item) },
                onLongPress = { showMenu = true }
            )
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.posterPath)
                .crossfade(true)
                .setHeader("Referer", "https://www.youtube.com")
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        VideoOptionsPopup(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onSelect = { status -> onOptionSelect(item, status) },
            item = item
        )
        
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.5f), Color.Black),
                    startY = 400f
                )
            )
        )
        
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                item.title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.duration, color = Color.White.copy(0.7f), fontSize = 14.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(4.dp).background(Color.White.copy(0.4f), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(item.views + " views", color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { onOptionSelect(item, "Plan to Watch") }) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Text("My List", color = Color.White, fontSize = 11.sp)
                }
                Button(
                    onClick = { onClick(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(44.dp).width(120.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { showMenu = true }) {
                    Icon(Icons.Default.Info, null, tint = Color.White)
                    Text("Info", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun YoutubeHorizontalCard(item: SearchResult, width: Dp = 180.dp, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val modifier = if (width == Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(width)
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = { onClick() },
            onLongPress = { showMenu = true }
        )
    }) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterPath)
                    .crossfade(true)
                    .setHeader("Referer", "https://www.youtube.com")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))
            )
            VideoOptionsPopup(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onSelect = { status -> onOptionSelect(item, status) },
                item = item
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.studio.isNotEmpty()) {
            Text(item.studio, color = Color.Red.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YoutubeVideoListCard(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Redesigned to strictly match Image 1 from requested Batman style
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            }
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(135.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.posterPath)
                        .crossfade(true)
                        .setHeader("Referer", "https://www.youtube.com")
                        .build(),
                    contentDescription = null, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
                if (item.duration.isNotBlank()) {
                    Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                VideoOptionsPopup(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onSelect = { status -> onOptionSelect(item, status) },
                    item = item
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )
                if (item.studio.isNotEmpty()) {
                    Text(
                        text = item.studio, 
                        color = Color.Red.copy(0.85f), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = item.views, 
                    color = Color.White.copy(0.5f), 
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun YoutubeChannelListCard(item: SearchResult, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.posterPath,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(0.1f)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.views, color = Color.White.copy(0.6f), fontSize = 13.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
        }
    }
}
