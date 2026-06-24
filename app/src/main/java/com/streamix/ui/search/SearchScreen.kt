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
import java.net.URLEncoder

@Composable
fun SearchScreen(
    navController: NavController,
    initialQuery: String? = null,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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

        if (isLoading && results.isEmpty()) {
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
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { item ->
                    MovieCard(item) {
                        if (item.mediaType == "adult") {
                            val encoded = URLEncoder.encode(item.id, "UTF-8")
                            navController.navigate("adult_detail/$encoded")
                        } else {
                            navController.navigate("detail/${item.id}/${item.mediaType}")
                        }
                    }
                }
            }
        }
    }
}
