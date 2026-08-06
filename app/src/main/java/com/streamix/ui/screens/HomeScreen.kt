package com.streamix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.ProfileManager
import com.streamix.core.plugin.SearchResult
import com.streamix.ui.components.ContentCard
import com.streamix.ui.components.ErrorFullScreen
import com.streamix.ui.components.LoadingShimmer
import com.streamix.ui.components.SourceSelectorFab
import java.net.URLEncoder

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.streamix.ui.components.YouTubeHomeCard

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    currentProfile: ProfileManager.Profile,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    adultViewModel: AdultHomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // Adult profile → delegate to dedicated ViewModel
    if (currentProfile == ProfileManager.Profile.ADULT) {
        AdultHomeContent(navController, adultViewModel, settingsViewModel)
        return
    }

    // Load movies / songs on first composition or profile change
    LaunchedEffect(currentProfile) { viewModel.loadHome(currentProfile) }

    val uiState         by viewModel.uiState.collectAsState()
    val prefs           by settingsViewModel.prefs.collectAsState()
    val selectedSourceId by viewModel.selectedSourceId.collectAsState()
    val availableScrapers by viewModel.availableScrapers.collectAsState()
    val healthStatus    by viewModel.healthManager.healthStatusMap.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState is HomeUiState.Loading,
        onRefresh = { viewModel.loadHome(currentProfile) }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column {
            // Sideway scrollable movie/video selector (Source/Category selector)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Sources" chip
                item {
                    FilterChip(
                        selected = selectedSourceId == null,
                        onClick = { viewModel.selectSource(null, currentProfile) },
                        label = { Text("All Sources") }
                    )
                }
                // Individual scraper chips
                items(availableScrapers) { scraper ->
                    FilterChip(
                        selected = selectedSourceId == scraper.info.id,
                        onClick = { viewModel.selectSource(scraper.info.id, currentProfile) },
                        label = { Text(scraper.info.displayName) }
                    )
                }
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> LoadingShimmer()
                is HomeUiState.Error   -> ErrorFullScreen(state.message) { viewModel.loadHome(currentProfile) }
                is HomeUiState.Success -> {
                    val allItems = state.rows.flatMap { it.items }
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        // Main Feed with Shorts cards in-between
                        itemsIndexed(allItems) { index, cardItem ->
                            // Show Shorts shelf after every 5th item
                            if (index > 0 && index % 5 == 0) {
                                ShortsShelf(
                                    items = allItems.shuffled().take(5),
                                    onShortClick = { short ->
                                        navController.navigate("shorts?startId=${URLEncoder.encode(short.id, "UTF-8")}")
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            YouTubeHomeCard(
                                item = cardItem,
                                onClick = {
                                    val route = "detail/${URLEncoder.encode(cardItem.id, "UTF-8")}/${cardItem.sourceId}" +
                                        "?title=${URLEncoder.encode(cardItem.title, "UTF-8")}" +
                                        "&poster=${URLEncoder.encode(cardItem.posterUrl ?: "", "UTF-8")}" +
                                        "&desc=${URLEncoder.encode(cardItem.description ?: "", "UTF-8")}"
                                    navController.navigate(route)
                                }
                            )
                        }
                        
                        if (allItems.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("No content. Tap the source button below to choose a source.",
                                         color = Color.White,
                                         textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState is HomeUiState.Loading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )

        // Source Selector FAB — bottom left
        SourceSelectorFab(
            availableScrapers = availableScrapers,
            selectedSourceId  = selectedSourceId,
            onSourceSelected  = { id -> viewModel.selectSource(id, currentProfile) },
            healthStatus      = healthStatus,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdultHomeContent(
    navController: NavController,
    viewModel: AdultHomeViewModel,
    settingsViewModel: SettingsViewModel
) {
    val state            by viewModel.state.collectAsState()
    val prefs            by settingsViewModel.prefs.collectAsState()
    val selectedSourceId by viewModel.selectedSourceId.collectAsState()
    val availableScrapers by viewModel.availableScrapers.collectAsState()
    val healthStatus    by viewModel.healthManager.healthStatusMap.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state is AdultHomeState.Loading,
        onRefresh = { viewModel.loadHome() }
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column {
            // Sideway scrollable selector for Adult too
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSourceId == null,
                        onClick = { viewModel.selectSource(null) },
                        label = { Text("All Sources") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            selectedLabelColor = Color.Black,
                            selectedContainerColor = Color.White
                        )
                    )
                }
                items(availableScrapers) { scraper ->
                    FilterChip(
                        selected = selectedSourceId == scraper.info.id,
                        onClick = { viewModel.selectSource(scraper.info.id) },
                        label = { Text(scraper.info.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            selectedLabelColor = Color.Black,
                            selectedContainerColor = Color.White
                        )
                    )
                }
            }

            when (val s = state) {
                is AdultHomeState.Loading    -> LoadingShimmer()
                is AdultHomeState.NoScrapers -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No adult scrapers registered.", color = Color.White)
                }
                is AdultHomeState.Error      -> ErrorFullScreen(s.message) { viewModel.loadHome() }
                is AdultHomeState.Loaded     -> {
                    val allItems = s.sections.flatMap { it.items }
                    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                        // Main Feed for Adult with Shorts cards in-between
                        itemsIndexed(allItems) { index, cardItem ->
                             if (index > 0 && index % 5 == 0) {
                                ShortsShelf(
                                    items = s.shortsItems.shuffled().take(5),
                                    onShortClick = { short ->
                                        navController.navigate("shorts?startId=${URLEncoder.encode(short.id, "UTF-8")}")
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            YouTubeHomeCard(
                                item = cardItem,
                                onClick = {
                                    val route = "detail/${URLEncoder.encode(cardItem.id, "UTF-8")}/${cardItem.sourceId}" +
                                        "?title=${URLEncoder.encode(cardItem.title, "UTF-8")}" +
                                        "&poster=${URLEncoder.encode(cardItem.posterUrl ?: "", "UTF-8")}" +
                                        "&desc=${URLEncoder.encode(cardItem.description ?: "", "UTF-8")}"
                                    navController.navigate(route)
                                }
                            )
                        }

                        if (allItems.isEmpty() && s.shortsItems.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("No content loaded.", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state is AdultHomeState.Loading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )

        // Source Selector FAB — bottom left
        SourceSelectorFab(
            availableScrapers = availableScrapers,
            selectedSourceId  = selectedSourceId,
            onSourceSelected  = { id -> viewModel.selectSource(id) },
            healthStatus      = healthStatus,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun ShortsShelf(
    items: List<SearchResult>,
    onShortClick: (SearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayCircle, null, tint = Color.Red, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shorts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .width(180.dp) // Slightly wider like TikTok
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShortClick(item) }
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Bottom title overlay like YouTube Shorts shelf
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                    )
                    
                    Text(
                        text = item.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCard(item: SearchResult, prefs: StreamixPrefs, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black
                    )
                )
        )
        // Title + Play button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.year?.let {
                Text(it.toString(), fontSize = 13.sp,
                     color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("Details", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
                
                Button(
                    onClick = onLongClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Play Now", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ContentRow(
    title: String, 
    items: List<SearchResult>, 
    prefs: StreamixPrefs, 
    onItemClick: (SearchResult) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { "${it.sourceId}:${it.id}" }) { item ->
                ContentCard(
                    item = item, 
                    prefs = prefs, 
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
