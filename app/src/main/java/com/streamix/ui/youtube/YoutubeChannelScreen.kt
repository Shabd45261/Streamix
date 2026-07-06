package com.streamix.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
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
import com.streamix.core.model.SearchResult
import com.streamix.ui.theme.LocalCustomColors
import com.streamix.ui.shorts.ShortsPlaylistManager
import com.streamix.core.storage.PreferencesManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun YoutubeChannelScreen(
    navController: NavController,
    channelUrl: String,
    viewModel: YoutubeChannelViewModel = hiltViewModel()
) {
    val channelInfo by viewModel.channelInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val tabItems by viewModel.tabItems.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val subscribedChannels by prefs.subscribedChannels.collectAsState(initial = emptySet())
    val isSubscribed = channelInfo?.url?.let { subscribedChannels.contains(it) } ?: false

    LaunchedEffect(channelUrl) {
        viewModel.loadChannel(channelUrl)
    }

    // Tab ordering logic
    val tabOrderMap = mapOf(
        "home" to 0,
        "videos" to 1,
        "shorts" to 2,
        "live" to 3,
        "streams" to 3,
        "playlists" to 4
    )

    val sortedTabs = remember(channelInfo) {
        channelInfo?.tabs?.sortedBy { tab ->
            val filter = tab.contentFilters?.firstOrNull()?.lowercase() ?: "home"
            tabOrderMap[filter] ?: 99
        } ?: emptyList()
    }

    val pagerState = rememberPagerState(pageCount = { sortedTabs.size })

    LaunchedEffect(pagerState.currentPage, sortedTabs) {
        if (sortedTabs.isNotEmpty()) {
            val currentTab = sortedTabs.getOrNull(pagerState.currentPage)
            val originalIndex = channelInfo?.tabs?.indexOf(currentTab) ?: pagerState.currentPage
            viewModel.loadTab(originalIndex)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            (channelInfo?.name ?: "Channel").uppercase(), 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 8.dp)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Search in channel */ }) {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF8E24AA),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                
                if (sortedTabs.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color(0xFF8E24AA),
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    ) {
                        sortedTabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { 
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { 
                                    val title = (tab.contentFilters?.firstOrNull() ?: "Home").uppercase()
                                    Text(
                                        title, 
                                        fontSize = 13.sp, 
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (isLoading && channelInfo == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalAlignment = Alignment.Top
            ) { page ->
                val currentTab = sortedTabs.getOrNull(page)
                val originalTabIndex = channelInfo?.tabs?.indexOf(currentTab) ?: page
                val currentItems = tabItems[originalTabIndex] ?: emptyList()
                
                val tabName = currentTab?.contentFilters?.firstOrNull()?.lowercase() ?: ""
                val isShortsTab = tabName.contains("shorts")
                val isPlaylistsTab = tabName.contains("playlists")

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        channelInfo?.let { info ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    val bannerUrl = info.banners?.lastOrNull()?.getUrl()
                                    AsyncImage(
                                        model = bannerUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 40.dp, end = 16.dp, bottom = 12.dp)
                                    ) {
                                        Text(text = info.name ?: "", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Button(
                                                onClick = { info.url?.let { url -> scope.launch { prefs.toggleSubscription(url) } } },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isSubscribed) Color.White.copy(0.15f) else Color(0xFF00BCD4)),
                                                shape = RoundedCornerShape(2.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                            ) {
                                                Text(if (isSubscribed) "SUBSCRIBED" else "SUBSCRIBE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Text(text = "${if (info.subscriberCount >= 0) info.subscriberCount else "X"} subscribers", color = Color.White.copy(0.6f), fontSize = 14.sp)
                                        }
                                    }
                                }
                                AsyncImage(
                                    model = info.avatars?.lastOrNull()?.getUrl(),
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 16.dp, top = 70.dp).size(60.dp).clip(CircleShape).border(3.dp, Color.Black, CircleShape).background(Color.Black),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    items(currentItems) { item ->
                        when {
                            isShortsTab -> {
                                YoutubeShortsListCard(
                                    item = item,
                                    onClick = {
                                        ShortsPlaylistManager.setPlaylist(channelUrl, item.id)
                                        navController.navigate(com.streamix.ui.navigation.Screen.Home.route) {
                                            popUpTo(com.streamix.ui.navigation.Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            isPlaylistsTab -> {
                                YoutubePlaylistListCard(item = item, onClick = { })
                            }
                            else -> {
                                YoutubeVideoListCard(
                                    item = item,
                                    onOptionSelect = { _, _ -> },
                                    onClick = {
                                        val encoded = URLEncoder.encode(item.id, "UTF-8")
                                        navController.navigate("youtube_detail?videoId=$encoded")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubePlaylistListCard(item: SearchResult, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(135.dp).aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = null, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.4f)
                        .align(Alignment.CenterEnd)
                        .background(Color.Black.copy(0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.duration, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.PlaylistPlay, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "View full playlist", 
                    color = Color.White.copy(0.5f), 
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
        }
    }
}

@Composable
fun YoutubeShortsListCard(item: SearchResult, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(90.dp).aspectRatio(9f/16f).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = null, 
                    contentScale = ContentScale.Crop, 
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    maxLines = 3, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.views, 
                    color = Color.White.copy(0.5f), 
                    fontSize = 11.sp
                )
            }
        }
    }
}
