package com.streamix.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder

@Composable
fun LibraryScreen(
    navController: NavController,
    profileState: MutableState<Profile>,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val colors = LocalCustomColors.current
    val categories = listOf("Watching", "Completed", "Downloads", "Dropped", "Plan to Watch", "Favorites", "Subscribed")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val items by viewModel.items.collectAsState()

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
                            "youtube" -> "youtube_detail/${item.id}"
                            "youtube_channel" -> {
                                val encoded = URLEncoder.encode(item.id, "UTF-8")
                                "youtube_channel?channelUrl=$encoded"
                            }
                            else -> "detail/${item.id}/${item.mediaType}"
                        }
                        navController.navigate(route)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItemCard(item: WatchlistEntity, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.posterPath,
            contentDescription = item.title,
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(2/3f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = colors.secondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.isShort) {
                Text(
                    "(Shorts)",
                    color = Color(0xFF2196F3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.mediaType.uppercase()} • ${item.year}",
                color = colors.secondary.copy(0.5f),
                fontSize = 12.sp
            )
        }
    }
}
