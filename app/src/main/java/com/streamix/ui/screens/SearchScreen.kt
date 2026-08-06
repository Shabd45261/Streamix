package com.streamix.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.streamix.ui.components.ContentCard
import com.streamix.ui.components.LoadingShimmer
import com.streamix.ui.components.ErrorFullScreen
import com.streamix.ui.theme.FrostedGlass
import com.streamix.ui.theme.AppColors
import java.net.URLDecoder
import java.net.URLEncoder

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.streamix.core.ProfileManager
import com.streamix.ui.components.YouTubeSearchCard
import com.streamix.ui.components.ContentCard

@Composable
fun SearchScreen(
    currentProfile: ProfileManager.Profile, // Added profile
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val prefs by settingsViewModel.prefs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar (already implemented in TopBar, but keep this for local search if needed or hide if redundant)
        // For now, let's keep it but make it look like YouTube search results
        
        when (val state = uiState) {
            is SearchUiState.Idle    -> {
                Column(Modifier.fillMaxSize()) {
                    if (recentSearches.isNotEmpty()) {
                        RecentSearches(recentSearches, onSelect = viewModel::onQueryChanged, onDelete = viewModel::removeRecentSearch)
                    }
                    SearchSuggestions(viewModel::onQueryChanged)
                }
            }
            is SearchUiState.Loading -> LoadingShimmer()
            is SearchUiState.Error   -> ErrorFullScreen(state.message) { viewModel.searchNow() }
            is SearchUiState.Results -> {
                if (state.results.isEmpty()) {
                    EmptySearchResults(query)
                } else {
                    if (currentProfile == ProfileManager.Profile.ADULT) {
                        // Adult Profile: Grouped Grid Results like in image
                        val groupedResults = state.results.groupBy { it.sourceId }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            groupedResults.forEach { (sourceId, resultList) ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sourceId.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                item {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(resultList) { result ->
                                            ContentCard(
                                                item = result,
                                                prefs = prefs,
                                                onClick = {
                                                    val route = "detail/${URLEncoder.encode(result.id, "UTF-8")}/${result.sourceId}" +
                                                        "?title=${URLEncoder.encode(result.title, "UTF-8")}" +
                                                        "&poster=${URLEncoder.encode(result.posterUrl ?: "", "UTF-8")}" +
                                                        "&desc=${URLEncoder.encode(result.description ?: "", "UTF-8")}"
                                                    navController.navigate(route)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Profile: YouTube Horizontal Results
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.results) { item ->
                                YouTubeSearchCard(
                                    item = item,
                                    onClick = {
                                        val route = "detail/${URLEncoder.encode(item.id, "UTF-8")}/${item.sourceId}" +
                                            "?title=${URLEncoder.encode(item.title, "UTF-8")}" +
                                            "&poster=${URLEncoder.encode(item.posterUrl ?: "", "UTF-8")}" +
                                            "&desc=${URLEncoder.encode(item.description ?: "", "UTF-8")}"
                                        navController.navigate(route)
                                    }
                                )
                            }
                            // Load more trigger
                            if (state.hasMore) {
                                item {
                                    LaunchedEffect(Unit) { viewModel.loadNextPage() }
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentSearches(list: List<String>, onSelect: (String) -> Unit, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Recent Searches", fontWeight = FontWeight.Bold, color = Color.White,
             style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
        
        list.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onSelect(item) },
                        onLongClick = { showDeleteDialog = item }
                    )
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.History, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(item, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { showDeleteDialog = item }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Color.Black,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Delete Search?") },
            text = { Text("Remove \"${showDeleteDialog}\" from history?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(showDeleteDialog!!)
                    showDeleteDialog = null 
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchSuggestions(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf("Action", "Comedy", "Thriller", "Anime", "Horror", "Sci-Fi", "Romance", "Documentary")
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Browse by Genre", fontWeight = FontWeight.Bold, color = Color.White,
             style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            suggestions.forEach { tag ->
                SuggestionChip(
                    onClick = { onSuggestionClick(tag) },
                    label = { Text(tag, color = Color.White) },
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = Color.White
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = Color.White.copy(alpha = 0.1f),
                        enabled = true
                    )
                )
            }
        }
    }
}

@Composable
fun EmptySearchResults(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, contentDescription = null,
                 modifier = Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No results for \"$query\"", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("Try a different search term or install more extensions.",
                 fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                 textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp))
        }
    }
}
