package com.streamix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.core.storage.WatchlistEntity
import com.streamix.core.utils.UrlUtils
import com.streamix.ui.navigation.LocalProfile
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.core.analytics.AnalyticsManager
import com.streamix.core.analytics.logContentOpened
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.streamix.ui.player.PlayerManager
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.streamix.core.utils.ShareUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentRow(
    title: String,
    items: List<SearchResult>,
    onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> },
    onItemClick: (SearchResult) -> Unit,
    analyticsManager: AnalyticsManager? = null
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
        val hiddenIds by com.streamix.core.utils.SessionVideoManager.hiddenVideoIds.collectAsState()
        val filteredItems = remember(items, hiddenIds) { 
            items.filter { it.id !in hiddenIds }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredItems, key = { it.id }) { item ->
                MovieCard(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onOptionSelect = onOptionSelect,
                    onClick = { 
                        analyticsManager?.logContentOpened(item)
                        onItemClick(item) 
                    }
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    modifier: Modifier = Modifier,
    item: SearchResult, 
    onOptionSelect: (SearchResult, String) -> Unit = { _, _ -> },
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showExpandedCard by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .touchBorder()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showExpandedCard = true }
                )
            }
            .background(Color.White.copy(alpha = 0.05f))
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

        if (showExpandedCard) {
            ExpandedVideoCard(
                item = item,
                onDismiss = { showExpandedCard = false },
                onPlay = { showExpandedCard = false; onClick() },
                onLike = { onOptionSelect(item, "Liked") },
                onWatched = { onOptionSelect(item, "Completed") }
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
            if (item.views.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.views,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            if (item.year.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.year,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ExpandedVideoCard(
    item: SearchResult,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onWatched: () -> Unit,
    onRemoveFromList: () -> Unit = {},
    onRemoveFromHistory: ((String) -> Unit)? = null,
    viewModel: ExpandedVideoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val previewLinks by viewModel.previewLinks.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    var showAddToList by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 1f
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { 
            exoPlayer.stop()
            exoPlayer.release() 
            viewModel.clearPreview()
        }
    }

    LaunchedEffect(item) {
        viewModel.loadPreview(item)
    }

    LaunchedEffect(previewLinks) {
        if (previewLinks.isNotEmpty()) {
            val link = previewLinks.maxByOrNull { it.quality.contains("720") || it.quality.contains("1080") }?.url 
                ?: previewLinks.firstOrNull()?.url
            if (link != null) {
                exoPlayer.stop()
                exoPlayer.setMediaItem(MediaItem.fromUri(link))
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }
    }

    val libraryViewModel: com.streamix.ui.library.LibraryViewModel = hiltViewModel()
    val isLiked by libraryViewModel.isInStatus(item.id, "Liked").collectAsState(initial = false)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(16/9f)) {
                    if (previewLinks.isNotEmpty()) {
                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val posterUrl = remember(item.posterPath, item.mediaType, item.id) {
                            UrlUtils.resolveImageUrl(item.posterPath, item.mediaType, item.id)
                        }
                        coil.compose.AsyncImage(
                            model = posterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White.copy(0.5f))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(2.dp)) {
                            Text("TOP 10", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }

                    if (item.mediaType == "youtube" || item.mediaType == "adult") {
                        Surface(
                            color = Color.Red,
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        ) {
                            Text("Recently Added", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (previewLinks.isNotEmpty()) {
                                        PlayerManager.play(item, previewLinks)
                                        onDismiss()
                                    } else {
                                        onPlay()
                                    }
                                },
                                modifier = Modifier.size(44.dp).background(Color.White, CircleShape)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                            }
                            
                            IconButton(
                                onClick = { 
                                    if (onRemoveFromHistory != null) {
                                        onRemoveFromHistory(item.id)
                                    } else {
                                        com.streamix.core.utils.SessionVideoManager.hideVideo(item.id)
                                    }
                                    onDismiss()
                                },
                                modifier = Modifier.size(44.dp).border(2.dp, Color.White.copy(0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            
                            IconButton(
                                onClick = { 
                                    libraryViewModel.toggleLibrary(WatchlistEntity(
                                        id = item.id,
                                        title = item.title,
                                        posterPath = item.posterPath,
                                        mediaType = item.mediaType,
                                        status = "Liked",
                                        studio = item.studio
                                    ))
                                },
                                modifier = Modifier.size(44.dp).border(2.dp, if (isLiked) Color.Red else Color.White.copy(0.4f), CircleShape)
                            ) {
                                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.White, modifier = Modifier.size(20.dp))
                            }

                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(44.dp).border(2.dp, Color.White.copy(0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Default.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(Color(0xFF1E1E1E)).border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Add to List", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = Color.White) },
                                        onClick = { showMenu = false; showAddToList = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                                        onClick = { 
                                            showMenu = false
                                            val profile = when(item.mediaType) {
                                                "youtube" -> "youtube"
                                                "adult" -> "adult"
                                                else -> "movies"
                                            }
                                            val contentType = when(item.mediaType) {
                                                "youtube" -> if (item.isShort) "short" else "video"
                                                "adult" -> "video"
                                                "tv" -> "episode"
                                                else -> "movie"
                                            }
                                            ShareUtils.shareContent(context, item.title, profile, contentType, item.id)
                                        }
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(44.dp).border(2.dp, Color.White.copy(0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.rating.isNotBlank()) {
                            Text(item.rating, color = Color(0xFF46D369), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (item.year.isNotBlank()) Text(item.year, color = Color.White.copy(0.6f), fontSize = 14.sp)
                        Surface(color = Color.Transparent, border = BorderStroke(1.dp, Color.White.copy(0.3f)), shape = RoundedCornerShape(2.dp)) {
                            Text("HD", color = Color.White.copy(0.8f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddToList) {
        val lists = listOf("Watching", "Liked", "Completed", "Downloads", "Dropped", "Plan To Watch", "Favourites")
        AlertDialog(
            onDismissRequest = { showAddToList = false },
            containerColor = Color(0xFF181818),
            title = { Text("Add to List", color = Color.White) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    lists.forEach { listName ->
                        TextButton(
                            onClick = {
                                libraryViewModel.addToLibrary(WatchlistEntity(
                                    id = item.id,
                                    title = item.title,
                                    posterPath = item.posterPath,
                                    mediaType = item.mediaType,
                                    status = listName,
                                    studio = item.studio
                                ))
                                showAddToList = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(listName, color = Color.White.copy(0.8f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddToList = false }) { Text("CANCEL", color = Color.Red) } }
        )
    }
}
