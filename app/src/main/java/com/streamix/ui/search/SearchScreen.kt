package com.streamix.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.streamix.ui.components.MovieCard
import com.streamix.ui.components.StreamixSearchBar
import com.streamix.ui.theme.LocalCustomColors
import java.net.URLEncoder
import com.streamix.ui.components.ExpandedVideoCard
import com.streamix.core.model.SearchResult
import com.streamix.ui.movies.MovieVerticalCard
import com.streamix.ui.youtube.YoutubeVideoListCard
import com.streamix.ui.adult.AdultVideoListCard
import com.streamix.ui.navigation.LocalProfile
import com.streamix.ui.components.touchBorder
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val history by viewModel.searchHistory.collectAsState()
    val colors = LocalCustomColors.current
    val currentProfile = LocalProfile.current.value
    val hiddenIds by com.streamix.core.utils.SessionVideoManager.hiddenVideoIds.collectAsState()
    val filteredResults = remember(results, hiddenIds) { results.filter { it.id !in hiddenIds } }

    var activeExpandedItem by remember { mutableStateOf<SearchResult?>(null) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && initialQuery != "null") {
            viewModel.onQueryChange(initialQuery)
            viewModel.search()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.primary)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.secondary.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.secondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Search",
                    color = colors.secondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                )
            }

            StreamixSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = { viewModel.search() },
                history = history,
                onRemoveHistoryItem = viewModel::removeHistoryItem,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (isLoading && results.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = colors.tertiary)
                }
            } else if (results.isEmpty() && query.isNotBlank() && !isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No results found for \"$query\"", color = colors.secondary.copy(0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 150.dp, top = 8.dp)
                ) {
                    items(filteredResults, key = { it.id }) { item ->
                        when {
                            item.mediaType == "youtube" || item.mediaType == "youtube_channel" -> {
                                YoutubeVideoListCard(
                                    modifier = Modifier.animateItemPlacement().pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { activeExpandedItem = item })
                                    },
                                    item = item,
                                    onOptionSelect = { _, _ -> },
                                    onClick = {
                                        val encoded = URLEncoder.encode(item.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                            item.mediaType == "adult" -> {
                                AdultVideoListCard(
                                    modifier = Modifier.animateItemPlacement().pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { activeExpandedItem = item })
                                    },
                                    item = item,
                                    onOptionSelect = { _, _ -> },
                                    onClick = {
                                        val encoded = URLEncoder.encode(item.id, "UTF-8")
                                        navController.navigate("adult_detail/$encoded")
                                    }
                                )
                            }
                            else -> { // movie/tv
                                MovieVerticalCard(
                                    modifier = Modifier.animateItemPlacement().pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { activeExpandedItem = item })
                                    },
                                    item = item,
                                    onOptionSelect = { _, _ -> },
                                    onClick = {
                                        val encoded = URLEncoder.encode(item.id, "UTF-8")
                                        val apiEncoded = URLEncoder.encode(item.studio, "UTF-8")
                                        navController.navigate("movies_detail?movieId=$encoded&apiName=$apiEncoded")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        activeExpandedItem?.let { item ->
            ExpandedVideoCard(
                item = item,
                onDismiss = { activeExpandedItem = null },
                onPlay = { /* Handled in component */ },
                onLike = { viewModel.addToLibrary(item, "Liked") },
                onWatched = { viewModel.addToLibrary(item, "Completed") }
            )
        }
    }
}
