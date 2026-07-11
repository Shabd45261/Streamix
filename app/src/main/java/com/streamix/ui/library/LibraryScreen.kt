package com.streamix.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streamix.core.model.Profile
import com.streamix.core.storage.WatchlistEntity
import com.streamix.ui.components.StreamixHeader
import com.streamix.ui.navigation.Screen
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@Composable
fun LibraryScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    onProfileChange: (Profile) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val colors = LocalCustomColors.current
    val categories = listOf("Watching", "Completed", "Downloads", "Dropped", "Plan to Watch", "Favorites", "Subscribed")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val items by viewModel.items.collectAsState()
    val groupedHistory by viewModel.groupedHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(profileState.value) {
        viewModel.setProfile(profileState.value)
    }

    LaunchedEffect(selectedTabIndex) {
        viewModel.loadStatus(categories[selectedTabIndex])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.primary)
    ) {
        StreamixHeader(
            currentProfile = profileState.value,
            onSettingsTap = { navController.navigate(Screen.Settings.route) },
            onProfileSelect = onProfileChange,
            onProfileTripleTap = { navController.navigate(Screen.Passcode.route) }
        )

        if (categories[selectedTabIndex] == "Watching") {
            // Search Bar for History
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search watch history", color = colors.secondary.copy(0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.secondary.copy(0.5f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.tertiary,
                    unfocusedBorderColor = colors.secondary.copy(0.1f),
                    focusedContainerColor = colors.secondary.copy(0.05f),
                    unfocusedContainerColor = colors.secondary.copy(0.05f),
                    focusedTextColor = colors.secondary,
                    unfocusedTextColor = colors.secondary
                ),
                singleLine = true
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = colors.tertiary,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = colors.tertiary
                )
            }
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) colors.secondary else colors.secondary.copy(0.5f)
                        )
                    }
                )
            }
        }

        if (categories[selectedTabIndex] == "Watching") {
            if (groupedHistory.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No watch history", color = colors.secondary.copy(0.3f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    groupedHistory.forEach { (date, historyList) ->
                        item {
                            Text(
                                text = date,
                                color = colors.secondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        val shorts = historyList.filter { it.isShort }
                        val videos = historyList.filter { !it.isShort }

                        if (shorts.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    items(shorts) { short ->
                                        HistoryShortCard(short) {
                                            val route = if (short.mediaType == "youtube") "youtube_detail?videoId=${short.id}" else "adult_detail/${URLEncoder.encode(short.id, "UTF-8")}"
                                            navController.navigate(route)
                                        }
                                    }
                                }
                            }
                        }

                        items(videos) { video ->
                            LibraryItemCard(
                                item = WatchlistEntity(
                                    id = video.id,
                                    title = video.title,
                                    posterPath = video.posterPath,
                                    mediaType = video.mediaType,
                                    isShort = video.isShort,
                                    studio = video.studio,
                                    views = video.views,
                                    year = video.year
                                )
                            ) {
                                val route = when (video.mediaType) {
                                    "adult" -> "adult_detail/${URLEncoder.encode(video.id, "UTF-8")}"
                                    "youtube" -> "youtube_detail?videoId=${video.id}"
                                    else -> {
                                        val encodedId = URLEncoder.encode(video.id, "UTF-8")
                                        if (profileState.value == Profile.MOVIES) {
                                            val apiEncoded = URLEncoder.encode(video.studio, "UTF-8")
                                            "movies_detail?movieId=$encodedId&apiName=$apiEncoded"
                                        } else {
                                            "detail/$encodedId/${video.mediaType}"
                                        }
                                    }
                                }
                                navController.navigate(route)
                            }
                        }
                    }
                }
            }
        } else {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items in this category", color = colors.secondary.copy(0.3f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items) { item ->
                        LibraryItemCard(item) {
                            val route = when (item.mediaType) {
                                "adult" -> {
                                    val encoded = URLEncoder.encode(item.id, "UTF-8")
                                    "adult_detail/$encoded"
                                }
                                "youtube" -> "youtube_detail?videoId=${item.id}"
                                "youtube_channel" -> {
                                    val encoded = URLEncoder.encode(item.id, "UTF-8")
                                    "youtube_channel?channelUrl=$encoded"
                                }
                                else -> {
                                    val encodedId = URLEncoder.encode(item.id, "UTF-8")
                                    if (profileState.value == Profile.MOVIES) {
                                        val apiEncoded = URLEncoder.encode(item.studio, "UTF-8")
                                        "movies_detail?movieId=$encodedId&apiName=$apiEncoded"
                                    } else {
                                        "detail/$encodedId/${item.mediaType}"
                                    }
                                }
                            }
                            navController.navigate(route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryShortCard(item: com.streamix.core.storage.WatchHistoryEntity, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.secondary.copy(0.1f))
        ) {
            AsyncImage(
                model = item.posterPath,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Progress bar at the bottom
            if (item.totalDuration > 0) {
                val progress = item.progress.toFloat() / item.totalDuration.toFloat()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Color.Red)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            color = colors.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LibraryItemCard(item: WatchlistEntity, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.studio.isNotEmpty()) {
                    Text(
                        item.studio,
                        color = Color.Red.copy(0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (item.views.isNotEmpty()) {
                    Text(
                        item.views,
                        color = Color.White.copy(0.5f),
                        fontSize = 11.sp
                    )
                } else if (item.year.isNotEmpty()) {
                    Text(
                        item.year,
                        color = Color.White.copy(0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
