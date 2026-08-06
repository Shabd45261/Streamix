package com.streamix.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.streamix.core.utils.UrlUtils
import com.streamix.ui.components.*
import com.streamix.ui.navigation.LocalProfile
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YoutubeHomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    onProfileChange: (Profile) -> Unit = {},
    viewModel: YoutubeHomeViewModel = hiltViewModel()
) {
    val trending    by viewModel.trending.collectAsState()
    val history     by viewModel.history.collectAsState()
    val subscribed  by viewModel.subscribed.collectAsState()
    val recommended by viewModel.recommended.collectAsState()
    val liked       by viewModel.liked.collectAsState()
    
    val isLoading   by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val showRefreshBanner by viewModel.showRefreshBanner.collectAsState()
    val hiddenIds by com.streamix.core.utils.SessionVideoManager.hiddenVideoIds.collectAsState()

    val filteredTrending = remember(trending, hiddenIds) { trending.filter { it.id !in hiddenIds } }
    val filteredHistory = remember(history, hiddenIds) { history.filter { it.id !in hiddenIds } }
    val filteredSubscribed = remember(subscribed, hiddenIds) { subscribed.filter { it.id !in hiddenIds } }
    val filteredRecommended = remember(recommended, hiddenIds) { recommended.filter { it.id !in hiddenIds } }
    val filteredLiked = remember(liked, hiddenIds) { liked.filter { it.id !in hiddenIds } }
    val filteredSearchResults = remember(searchResults, hiddenIds) { searchResults.filter { it.id !in hiddenIds } }

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
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    item(key = "header") {
                        StreamixHeader(
                            currentProfile = profileState.value,
                            onSettingsTap = { navController.navigate(Screen.Settings.route) },
                            onProfileSelect = onProfileChange
                        )
                    }

                    item(key = "search_bar") {
                        Box(modifier = Modifier.fillMaxWidth().background(colors.primary).padding(horizontal = 16.dp, vertical = 8.dp)) {
                            StreamixSearchBar(
                                query = searchQuery,
                                onQueryChange = viewModel::onQueryChange,
                                onSearch = { viewModel.search(searchQuery) },
                                history = searchHistory,
                                onRemoveHistoryItem = viewModel::removeHistoryItem
                            )
                        }
                    }

                    if (searchQuery.isBlank()) {
                        if (showRefreshBanner) {
                            item(key = "refresh_banner") {
                                Button(
                                    onClick = { 
                                        viewModel.refresh()
                                        viewModel.dismissRefreshBanner()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, tint = Color.Black)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Latest videos", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 0. Hero Section
                        if (filteredTrending.isNotEmpty()) {
                            item(key = "trending_hero") {
                                StreamixHeroSection(
                                    items = filteredTrending.take(5),
                                    onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                    onRemoveFromHistory = viewModel::removeFromHistory,
                                    onClick = { video ->
                                        val encoded = URLEncoder.encode(video.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                        }

                        // 1. Recent Watch
                        if (filteredHistory.isNotEmpty()) {
                            item(key = "history_header") { SectionHeader("Recent Watch") }
                            item(key = "history_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(filteredHistory.take(12), key = { "history_${it.id}" }) { video ->
                                        YoutubeHorizontalCard(
                                            modifier = Modifier.animateItemPlacement(),
                                            item = video, 
                                            width = 180.dp,
                                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                            onRemoveFromHistory = viewModel::removeFromHistory,
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
                        if (filteredTrending.size > 5) {
                            item(key = "trending_row_header") { SectionHeader("Just Added") }
                            item(key = "trending_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(filteredTrending.drop(5).take(12), key = { it.id }) { video ->
                                        YoutubeHorizontalCard(
                                            modifier = Modifier.animateItemPlacement(),
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
                        if (filteredSubscribed.isNotEmpty()) {
                            item(key = "subscribed_header") { SectionHeader("Subscribed") }
                            item(key = "subscribed_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(filteredSubscribed.take(12), key = { it.id }) { video ->
                                        YoutubeHorizontalCard(
                                            modifier = Modifier.animateItemPlacement(),
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

                        // 3.5 Liked Videos
                        if (filteredLiked.isNotEmpty()) {
                            item(key = "liked_header") { SectionHeader("Liked Videos") }
                            item(key = "liked_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(filteredLiked.take(12), key = { "liked_${it.id}" }) { video ->
                                        YoutubeHorizontalCard(
                                            modifier = Modifier.animateItemPlacement(),
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

                        // 4. Personalized Feed
                        if (filteredRecommended.isNotEmpty()) {
                            item(key = "recommended_header") { SectionHeader("For You") }
                            item(key = "recommended_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(filteredRecommended.take(12), key = { it.id }) { video ->
                                        YoutubeHorizontalCard(
                                            modifier = Modifier.animateItemPlacement(),
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

                            // Vertical list for For You / Mixed feed
                            val feedRecommended = filteredRecommended.drop(12)
                            if (feedRecommended.isNotEmpty()) {
                                item(key = "mixed_for_you_header") { SectionHeader("Mixed for You") }
                            }
                            items(feedRecommended, key = { "rec_list_${it.id}" }) { video ->
                                YoutubeVideoListCard(
                                    modifier = Modifier.animateItemPlacement(),
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
                        if (filteredTrending.size > 17) {
                            item(key = "more_videos_header") { SectionHeader("Subscriptions & More") }
                            val remaining = filteredTrending.drop(17)
                            items(remaining, key = { "more_list_${it.id}" }) { video ->
                                YoutubeVideoListCard(
                                    modifier = Modifier.animateItemPlacement(),
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
                        items(filteredSearchResults, key = { "search_${it.id}" }) { video ->
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
                                    modifier = Modifier.animateItemPlacement(),
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
                    contentColor = Color.White
                )
            }
        }
        
        if (isLoading && trending.isEmpty() && searchQuery.isBlank()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
        if (isLoading && searchQuery.isNotBlank() && searchResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(0.5f))
    }
}

@Composable
fun YoutubeHeroSection(
    item: SearchResult, 
    onOptionSelect: (SearchResult, String) -> Unit, 
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onClick: (SearchResult) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showExpandedCard by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .touchBorder(RoundedCornerShape(0.dp), 0.dp, 3.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick(item) },
                    onLongPress = { showExpandedCard = true }
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

        if (showExpandedCard) {
            ExpandedVideoCard(
                item = item,
                onDismiss = { showExpandedCard = false },
                onPlay = { showExpandedCard = false; onClick(item) },
                onLike = { onOptionSelect(item, "Favorites") },
                onWatched = { onOptionSelect(item, "Completed") },
                onRemoveFromHistory = onRemoveFromHistory
            )
        }
        
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
                Text(item.views + " views", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (item.year.isNotBlank()) {
                    Text(item.year, color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
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
fun YoutubeHorizontalCard(
    modifier: Modifier = Modifier, 
    item: SearchResult, 
    width: Dp = 180.dp, 
    onOptionSelect: (SearchResult, String) -> Unit, 
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val cardModifier = if (width == Dp.Unspecified) modifier.fillMaxWidth() else modifier.width(width)
    var showMenu by remember { mutableStateOf(false) }
    var showExpandedCard by remember { mutableStateOf(false) }

    Column(
        modifier = cardModifier
            .touchBorder()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showExpandedCard = true }
                )
            }
    ) {
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

        if (showExpandedCard) {
            ExpandedVideoCard(
                item = item,
                onDismiss = { showExpandedCard = false },
                onPlay = { showExpandedCard = false; onClick() },
                onLike = { onOptionSelect(item, "Favorites") },
                onWatched = { onOptionSelect(item, "Completed") },
                onRemoveFromHistory = onRemoveFromHistory
            )
        }

        Spacer(Modifier.height(8.dp))
        @Suppress("HardcodedText")
        Text(item.title, color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.studio.isNotEmpty()) {
            Text(item.studio, color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun YoutubeVideoListCard(
    modifier: Modifier = Modifier, 
    item: SearchResult, 
    onOptionSelect: (SearchResult, String) -> Unit, 
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showExpandedCard by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .touchBorder()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showExpandedCard = true }
                )
            }
            .background(Color.White.copy(0.03f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(140.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp))) {
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
                    Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(4.dp))
                if (item.studio.isNotEmpty()) {
                    Text(
                        text = item.studio, 
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.views, 
                        color = Color.White.copy(0.4f), 
                        fontSize = 11.sp
                    )
                    if (item.year.isNotBlank()) {
                        Box(Modifier.padding(horizontal = 4.dp).size(2.dp).background(Color.White.copy(0.3f), CircleShape))
                        Text(
                            text = item.year, 
                            color = Color.White.copy(0.4f), 
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (showExpandedCard) {
            ExpandedVideoCard(
                item = item,
                onDismiss = { showExpandedCard = false },
                onPlay = { showExpandedCard = false; onClick() },
                onLike = { onOptionSelect(item, "Favorites") },
                onWatched = { onOptionSelect(item, "Completed") },
                onRemoveFromHistory = onRemoveFromHistory
            )
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
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.1f)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (item.views.isNotBlank()) {
                    Text(item.views, color = Color.White.copy(0.5f), fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.3f))
        }
    }
}
