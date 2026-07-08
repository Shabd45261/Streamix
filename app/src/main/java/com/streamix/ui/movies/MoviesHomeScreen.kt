package com.streamix.ui.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.components.VideoOptionsPopup
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoviesHomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    onProfileChange: (Profile) -> Unit = {},
    viewModel: MoviesHomeViewModel = hiltViewModel()
) {
    val homeRows by viewModel.homeRows.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
    ) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
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
                    // Refresh Banner
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
                                Text("Latest content", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Hero Section
                    val heroItems = homeRows.firstOrNull { it.name.contains("Trending", true) }?.items?.take(5)
                        ?: homeRows.firstOrNull()?.items?.take(5) ?: emptyList()
                    
                    if (heroItems.isNotEmpty()) {
                        item {
                            val pagerState = rememberPagerState(pageCount = { heroItems.size })
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().height(480.dp)
                            ) { page ->
                                val movie = heroItems[page]
                                MoviesHeroSection(
                                    item = movie,
                                    onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                    onClick = {
                                        val encoded = URLEncoder.encode(movie.id, "UTF-8")
                                        val apiEncoded = URLEncoder.encode(movie.studio, "UTF-8")
                                        navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                    }
                                )
                            }
                        }
                    }

                    // History Section
                    if (history.isNotEmpty()) {
                        item { MoviesSectionHeader("Continue Watching") }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(history.take(20), key = { "history_${it.id}" }) { movie ->
                                    MoviesHorizontalCard(
                                        item = movie, 
                                        width = 180.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(movie.id, "UTF-8")
                                            val apiEncoded = URLEncoder.encode(movie.studio, "UTF-8")
                                            navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Rows
                    homeRows.forEach { row ->
                        item { MoviesSectionHeader(row.name) }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(row.items) { movie ->
                                    MoviesHorizontalCard(
                                        item = movie, 
                                        width = 240.dp,
                                        onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                                        onClick = {
                                            val encoded = URLEncoder.encode(movie.id, "UTF-8")
                                            val apiEncoded = URLEncoder.encode(movie.studio, "UTF-8")
                                            navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (homeRows.isEmpty() && !isLoading && history.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize().padding(bottom = 150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Movie, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Text("No content found", color = Color.Gray, fontSize = 18.sp)
                                    Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(top = 16.dp)) {
                                        Text("Refresh")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (searchResults.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No results for \"$searchQuery\"", color = colors.secondary.copy(0.5f))
                            }
                        }
                    }
                    items(searchResults, key = { "search_${it.id}" }) { movie ->
                        MovieVerticalCard(
                            item = movie,
                            onOptionSelect = { item, status -> viewModel.addToLibrary(item, status) },
                            onClick = {
                                val encoded = URLEncoder.encode(movie.id, "UTF-8")
                                val apiEncoded = URLEncoder.encode(movie.studio, "UTF-8")
                                navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                            }
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.Transparent,
                contentColor = colors.tertiary
            )
        }
    }

    if (isLoading && homeRows.isEmpty() && searchQuery.isBlank()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = colors.tertiary)
        }
    }
}

@Composable
fun MoviesSectionHeader(title: String) {
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
fun MoviesHeroSection(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: (SearchResult) -> Unit) {
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
                Text(item.year.ifBlank { "2024" }, color = Color.White.copy(0.7f), fontSize = 14.sp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(4.dp).background(Color.White.copy(0.4f), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (item.mediaType == "tv") "TV Series" else "Movie",
                    color = colors.tertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (item.rating.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(4.dp).background(Color.White.copy(0.4f), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(item.rating, color = Color.White.copy(0.7f), fontSize = 14.sp)
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
fun MoviesHorizontalCard(item: SearchResult, width: Dp = 180.dp, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
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
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))
            )
            
            if (item.rating.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(colors.tertiary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.rating,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
            Text(item.studio, color = colors.tertiary.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MovieVerticalCard(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: () -> Unit) {
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
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.studio.isNotEmpty()) {
                    Text(item.studio, color = colors.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                if (item.year.isNotBlank() || item.views.isNotBlank()) {
                    val info = listOf(item.views, item.year).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(text = info, color = colors.secondary.copy(0.5f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                if (item.rating.isNotBlank()) {
                    Text(item.rating, color = colors.secondary.copy(0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}
