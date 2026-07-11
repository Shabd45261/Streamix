package com.streamix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamix.core.model.SearchResult
import com.streamix.core.utils.UrlUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamixHeroSection(
    items: List<SearchResult>,
    onOptionSelect: (SearchResult, String) -> Unit,
    onClick: (SearchResult) -> Unit
) {
    if (items.isEmpty()) return
    
    val pagerState = rememberPagerState(pageCount = { items.size })
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        pageSpacing = 12.dp
    ) { page ->
        val item = items[page]
        HeroCard(
            item = item,
            onOptionSelect = onOptionSelect,
            onClick = { onClick(item) }
        )
    }
}

@Composable
private fun HeroCard(
    item: SearchResult,
    onOptionSelect: (SearchResult, String) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    val thumbUrl = remember(item.posterPath, item.mediaType, item.id) {
        UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay at the bottom - subtle like Image 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(0.7f)),
                        startY = 300f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            val typeText = when(item.mediaType) {
                "youtube" -> "TRENDING VIDEO"
                "adult" -> "TRENDING ADULT"
                "tv" -> "TRENDING SERIES"
                else -> "TRENDING MOVIE"
            }
            
            Text(
                text = typeText,
                color = Color.White.copy(0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            val infoText = if (item.year.isNotBlank()) item.year else if (item.views.isNotBlank()) item.views else ""
            if (infoText.isNotBlank()) {
                Text(
                    text = infoText,
                    color = Color.White.copy(0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
}
