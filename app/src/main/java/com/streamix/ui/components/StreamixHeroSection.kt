package com.streamix.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import com.streamix.ui.navigation.LocalProfile

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamixHeroSection(
    items: List<SearchResult>,
    onOptionSelect: (SearchResult, String) -> Unit,
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onClick: (SearchResult) -> Unit
) {
    if (items.isEmpty()) return
    
    val hiddenIds by com.streamix.core.utils.SessionVideoManager.hiddenVideoIds.collectAsState()
    val filteredItems = remember(items, hiddenIds) { 
        items.filter { it.id !in hiddenIds }
    }
    
    if (filteredItems.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { filteredItems.size })
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        pageSpacing = 12.dp
    ) { page ->
        val item = filteredItems[page]
        HeroCard(
            item = item,
            onOptionSelect = onOptionSelect,
            onRemoveFromHistory = onRemoveFromHistory,
            onClick = { onClick(item) }
        )
    }
}

@Composable
private fun HeroCard(
    item: SearchResult,
    onOptionSelect: (SearchResult, String) -> Unit,
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var showExpandedCard by remember { mutableStateOf(false) }
    
    val currentProfile = LocalProfile.current.value
    val borderSize by animateDpAsState(targetValue = if (isPressed) 2.dp else 0.dp, label = "border")
    val borderColor = if (isPressed) currentProfile.secondaryColor else Color.Transparent

    val thumbUrl = remember(item.posterPath, item.mediaType, item.id) {
        UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showExpandedCard = true }
                )
            }
            .border(borderSize, borderColor, RoundedCornerShape(16.dp))
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

        if (showExpandedCard) {
            ExpandedVideoCard(
                item = item,
                onDismiss = { showExpandedCard = false },
                onPlay = { showExpandedCard = false; onClick() },
                onLike = { onOptionSelect(item, "Favorites") },
                onWatched = { onOptionSelect(item, "Completed") },
                onRemoveFromHistory = onRemoveFromHistory
            )
        }
    }
}
