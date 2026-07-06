package com.streamix.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.streamix.core.model.SearchResult
import com.streamix.core.utils.UrlUtils

@Composable
fun ContentRow(
    title: String,
    items: List<SearchResult>,
    onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> },
    onItemClick: (SearchResult) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                MovieCard(
                    item = item,
                    onOptionSelect = onOptionSelect,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    item: SearchResult, 
    onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> },
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            }
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        val posterUrl = remember(item.posterPath, item.mediaType, item.id) {
            UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
        }
        
        Box {
            SubcomposeAsyncImage(
                model = posterUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White.copy(0.2f), modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null, tint = Color.White.copy(0.1f))
                    }
                }
            )
            VideoOptionsPopup(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onSelect = { status -> onOptionSelect(item, status) },
                item = item
            )
        }
        Column(Modifier.padding(8.dp)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.year ?: "",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}
