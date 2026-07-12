package com.streamix.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.streamix.ui.theme.CustomThemeColors
import androidx.compose.foundation.clickable
import java.net.URLEncoder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

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

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && initialQuery != "null") {
            viewModel.onQueryChange(initialQuery)
            viewModel.search()
        }
    }

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
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (query.isBlank() && !isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(history) { item ->
                    SearchHistoryItem(
                        text = item,
                        onClick = {
                            viewModel.onQueryChange(item)
                            viewModel.search()
                        },
                        onDelete = { viewModel.removeHistoryItem(item) },
                        colors = colors
                    )
                }
            }
        } else if (isLoading && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = colors.tertiary)
            }
        } else if (results.isEmpty() && query.isNotBlank() && !isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No results found for \"$query\"", color = colors.secondary.copy(0.4f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 100.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { item ->
                    MovieCard(
                        item = item,
                        onOptionSelect = { result, status -> viewModel.addToLibrary(result, status) },
                        onClick = {
                            if (item.mediaType == "adult") {
                                val encoded = URLEncoder.encode(item.id, "UTF-8")
                                navController.navigate("adult_detail/$encoded")
                            } else {
                                navController.navigate("detail/${item.id}/${item.mediaType}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryItem(text: String, onClick: () -> Unit, onDelete: () -> Unit, colors: CustomThemeColors) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = colors.secondary,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = colors.secondary.copy(0.4f), modifier = Modifier.size(18.dp))
        }
    }
}
