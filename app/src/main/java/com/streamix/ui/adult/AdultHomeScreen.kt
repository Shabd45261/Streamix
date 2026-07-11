package com.streamix.ui.adult

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
import coil.request.ImageRequest
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.streamix.core.model.Profile
import com.streamix.core.model.SearchResult
import com.streamix.core.utils.UrlUtils
import com.streamix.core.utils.ShareUtils
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixHeroSection
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.components.VideoOptionsPopup
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdultHomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    onProfileChange: (Profile) -> Unit = {},
    viewModel: AdultHomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val trending   by viewModel.trending.collectAsState()
    val history    by viewModel.history.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isAdultVerified by themeViewModel.isAdultVerified.collectAsState()
    val showRefreshBanner by viewModel.showRefreshBanner.collectAsState()

    val colors = LocalCustomColors.current
    val pullToRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    var showAgePrompt by remember { mutableStateOf(false) }
    
    LaunchedEffect(isAdultVerified) {
        if (!isAdultVerified) showAgePrompt = true
    }

    if (showAgePrompt) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Age Verification", color = colors.secondary) },
            text = { Text("Are you 18 years or older? This section contains adult content.", color = colors.secondary.copy(0.7f)) },
            confirmButton = {
                Button(
                    onClick = { 
                        showAgePrompt = false
                        scope.launch { themeViewModel.setAdultVerified(true) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("I am 18+") }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Go Back", color = colors.secondary.copy(0.5f))
                }
            },
            containerColor = colors.primary,
            shape = RoundedCornerShape(16.dp)
        )
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

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) pullToRefreshState.endRefresh()
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) { viewModel.refresh() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
            .statusBarsPadding()
    ) {
        Box(Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    StreamixHeader(
                        currentProfile = profileState.value,
                        onSettingsTap = { navController.navigate(Screen.Settings.route) },
                        onProfileSelect = onProfileChange,
                        onProfileTripleTap = { navController.navigate(Screen.Passcode.route) }
                    )
                }

                item {
                    StreamixSearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onQueryChange,
                        onSearch = { viewModel.search(searchQuery) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (searchQuery.isBlank()) {
                    // Latest Videos Refresh Banner
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
                            StreamixHeroSection(
                                items = trending.take(5),
                                onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                onClick = { video ->
                                    val encoded = URLEncoder.encode(video.id, "UTF-8")
                                    navController.navigate("adult_detail/$encoded")
                                }
                            )
                        }
                    }

                    // 2. Recent Watch (Horizontally scrollable)
                    if (history.isNotEmpty()) {
                        item { SectionHeader("Recent Watch") }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(history.take(20), key = { "recent_${it.id}" }) { video ->
                                    AdultHorizontalCard(
                                        item = video, 
                                        width = 180.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("adult_detail/$encoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal Block - Just Added
                    if (trending.size > 5) {
                        item { SectionHeader("Just Added") }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(trending.drop(5).take(10)) { video ->
                                    AdultHorizontalCard(
                                        item = video, 
                                        width = 240.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("adult_detail/$encoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal Block - Trending Now
                    if (trending.size > 15) {
                        item { SectionHeader("Trending Now") }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(trending.drop(15).take(12)) { video ->
                                    AdultHorizontalCard(
                                        item = video, 
                                        width = 240.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("adult_detail/$encoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal Block - Recommended
                    if (trending.size > 27) {
                        item { SectionHeader("Recommended") }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(trending.drop(27).take(12)) { video ->
                                    AdultHorizontalCard(
                                        item = video, 
                                        width = 240.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(video.id, "UTF-8")
                                            navController.navigate("adult_detail/$encoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vertical List - All Videos
                    if (trending.size > 39) {
                        item { SectionHeader("More Videos") }
                        val remaining = trending.drop(39)
                        items(remaining) { video ->
                            AdultVideoListCard(
                                item = video,
                                onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                onClick = {
                                    val encoded = URLEncoder.encode(video.id, "UTF-8")
                                    navController.navigate("adult_detail/$encoded")
                                }
                            )
                        }
                    }

                } else {
                    items(searchResults, key = { it.id }) { video ->
                        AdultVideoListCard(
                            item = video,
                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                            onClick = {
                                val encoded = URLEncoder.encode(video.id, "UTF-8")
                                navController.navigate("adult_detail/$encoded")
                            }
                        )
                    }
                }
            }
            
            if (isLoading && trending.isEmpty()) {
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
fun AdultHeroSection(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: (SearchResult) -> Unit) {
    val colors = LocalCustomColors.current
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
        val thumbUrl = remember(item.posterPath, item.id) {
            UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
        }
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbUrl)
                .crossfade(true)
                .setHeader("Referer", UrlUtils.getDomain(item.id).ifEmpty { "https://www.pornhat.com" })
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
                Text(item.rating, color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color.White)
                    Text("Info", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AdultHorizontalCard(item: SearchResult, width: Dp = 180.dp, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
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
        val thumbUrl = remember(item.posterPath, item.id) {
            UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
        }
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .crossfade(true)
                    .setHeader("Referer", UrlUtils.getDomain(item.id).ifEmpty { "https://www.pornhat.com" })
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
fun AdultVideoListCard(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { showMenu = true }
            )
        },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            val thumbUrl = remember(item.posterPath, item.id) {
                UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
            }
            Box(modifier = Modifier.width(140.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .crossfade(true)
                        .setHeader("Referer", UrlUtils.getDomain(item.id).ifEmpty { "https://www.pornhat.com" })
                        .build(),
                    contentDescription = null, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(item.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                VideoOptionsPopup(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onSelect = { status -> onOptionSelect(item, status) },
                    item = item
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.studio.isNotEmpty()) {
                    Text(item.studio, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("Video Description: This is a preview of the content...", color = colors.secondary.copy(0.5f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(item.views, color = colors.secondary.copy(0.7f), fontSize = 11.sp)
            }
        }
    }
}
