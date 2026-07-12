package com.streamix.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.streamix.core.model.Profile
import com.streamix.core.model.SearchResult
import com.streamix.core.utils.UrlUtils
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.components.SwipeableProfileHost
import com.streamix.ui.components.VideoOptionsPopup
import com.streamix.ui.navigation.Screen
import com.streamix.ui.youtube.YoutubeHomeScreen
import com.streamix.ui.shorts.ShortsScreen
import com.streamix.ui.shorts.ShortsContext
import com.streamix.ui.navigation.UpdateViewModel
import com.streamix.ui.components.UpdateDialog
import java.net.URLEncoder
import kotlinx.coroutines.delay

import androidx.compose.animation.*
import androidx.compose.animation.core.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    onProfileChange: (Profile) -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    AnimatedContent(
        targetState = profileState.value,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) with fadeOut(animationSpec = tween(400))
        },
        label = "profileTransition"
    ) { targetProfile ->
        when (targetProfile) {
            Profile.MOVIES -> {
                com.streamix.ui.movies.MoviesHomeScreen(navController, profileState, onProfileChange = onProfileChange)
            }
            Profile.YOUTUBE -> {
                YoutubeHomeScreen(navController, profileState, onProfileChange = onProfileChange)
            }
            Profile.ADULT -> {
                com.streamix.ui.adult.AdultHomeScreen(navController, profileState, onProfileChange = onProfileChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MoviesHomeContent(
    navController: NavController,
    profileState: MutableState<Profile>,
    viewModel: HomeViewModel,
    onProfileChange: (Profile) -> Unit = {}
) {
    val trending by viewModel.trending.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val history by viewModel.history.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showRefreshBanner by viewModel.showRefreshBanner.collectAsState()
    
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.onSearchQueryChange("")
            delay(1500)
            pullToRefreshState.endRefresh()
        }
    }

    BackHandler {
        viewModel.onBackPress {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                        onProfileSelect = onProfileChange
                    )
                }

                item {
                    StreamixSearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSearch = { 
                            if (searchQuery.isNotBlank()) {
                                navController.navigate("search?query=${URLEncoder.encode(searchQuery, "UTF-8")}")
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Refresh Banner
                if (showRefreshBanner) {
                    item {
                        Button(
                            onClick = { 
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

                // Hero Section
                if (trending.isNotEmpty()) {
                    item {
                        val heroItems = trending.take(5)
                        val pagerState = rememberPagerState(pageCount = { heroItems.size })
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(500.dp)) { page ->
                            MovieHeroSection(
                                item = heroItems[page],
                                onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                                onClick = { item ->
                                    navController.navigate("detail/${item.id}/${item.mediaType}")
                                }
                            )
                        }
                    }
                }

                // Continue Watching
                if (history.isNotEmpty()) {
                    item { SectionHeader("Continue Watching") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(history) { item ->
                                MovieHorizontalCard(
                                    item = item,
                                    width = 180.dp,
                                    onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                                    onClick = {
                                        if (it.mediaType == "adult") {
                                            val encoded = URLEncoder.encode(it.id, "UTF-8")
                                            navController.navigate("adult_detail/$encoded")
                                        } else {
                                            navController.navigate("detail/${it.id}/${it.mediaType}")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Trending Row
                if (trending.size > 5) {
                    item { SectionHeader("Trending Now") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(trending.drop(5).take(10)) { item ->
                                MovieHorizontalCard(
                                    item = item,
                                    onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                                    onClick = {
                                        navController.navigate("detail/${it.id}/${it.mediaType}")
                                    }
                                )
                            }
                        }
                    }
                }

                // Top Rated Row
                if (topRated.isNotEmpty()) {
                    item { SectionHeader("Top Rated") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(topRated.take(10)) { item ->
                                MovieHorizontalCard(
                                    item = item,
                                    onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                                    onClick = {
                                        navController.navigate("detail/${it.id}/${it.mediaType}")
                                    }
                                )
                            }
                        }
                    }
                }

                // All Videos Vertical List
                if (trending.size > 15) {
                    item { SectionHeader("More to Explore") }
                    items(trending.drop(15)) { item ->
                        MovieVerticalListCard(
                            item = item,
                            onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                            onClick = {
                                navController.navigate("detail/${it.id}/${it.mediaType}")
                            }
                        )
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
fun MovieHeroSection(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit, onClick: (SearchResult) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick(item) },
                onLongPress = { showMenu = true }
            )
        }
    ) {
        val backdropUrl = remember(item.posterPath) { "https://image.tmdb.org/t/p/w780${item.posterPath}" }
        AsyncImage(model = backdropUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        VideoOptionsPopup(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onSelect = { status -> onOptionSelect(item, status) },
            item = item
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.5f), Color.Black), startY = 400f)))
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.mediaType.uppercase(), color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(4.dp).background(Color.White.copy(0.4f), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(item.year ?: "", color = Color.White.copy(0.7f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Text("My List", color = Color.White, fontSize = 11.sp)
                }
                Button(onClick = { onClick(item) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(44.dp).width(120.dp)) {
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
fun MovieHorizontalCard(
    item: SearchResult, 
    width: androidx.compose.ui.unit.Dp = 150.dp,
    onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> }, 
    onClick: (SearchResult) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.width(width).pointerInput(Unit) {
        detectTapGestures(
            onTap = { onClick(item) },
            onLongPress = { showMenu = true }
        )
    }) {
        val posterUrl = remember(item.posterPath) { "https://image.tmdb.org/t/p/w342${item.posterPath}" }
        Box {
            AsyncImage(model = posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(2/3f).clip(RoundedCornerShape(8.dp)))
            VideoOptionsPopup(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onSelect = { status -> onOptionSelect(item, status) },
                item = item
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MovieVerticalListCard(item: SearchResult, onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> }, onClick: (SearchResult) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick(item) },
                onLongPress = { showMenu = true }
            )
        },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            val posterUrl = remember(item.posterPath) { "https://image.tmdb.org/t/p/w185${item.posterPath}" }
            Box {
                AsyncImage(model = posterUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.width(100.dp).aspectRatio(2/3f).clip(RoundedCornerShape(8.dp)))
                VideoOptionsPopup(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    onSelect = { status -> onOptionSelect(item, status) },
                    item = item
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text("Movie Description: A gripping tale of adventure and mystery. Explore the unknown in this latest release.", color = Color.White.copy(0.5f), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("${item.year} • ${item.mediaType.uppercase()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
