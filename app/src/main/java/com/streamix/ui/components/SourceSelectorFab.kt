package com.streamix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamix.scrapers.BaseScraper
import com.streamix.ui.theme.AppColors
import com.streamix.ui.theme.FrostedGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectorFab(
    availableScrapers: List<BaseScraper>,
    selectedSourceId: String?,            // null = All Sources
    onSourceSelected: (String?) -> Unit,   // null = All Sources
    healthStatus: Map<String, Boolean> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredScrapers = remember(availableScrapers, searchQuery) {
        availableScrapers.filter { 
            it.info.displayName.contains(searchQuery, ignoreCase = true) 
        }.sortedBy { it.info.displayName.lowercase() }
    }

    val alphabet = remember(filteredScrapers) {
        filteredScrapers.map { it.info.displayName.take(1).uppercase() }.distinct().sorted()
    }

    // ── FAB Button ─────────────────────────────────────────────────────────
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { 
                searchQuery = ""
                showSheet = true 
            },
            shape = CircleShape,
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            modifier = Modifier
                .size(64.dp)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        ) {
            // Show selected source icon or default grid icon
            val selected = availableScrapers.firstOrNull { it.info.id == selectedSourceId }
            if (selected?.info?.iconUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = selected.info.iconUrl,
                    contentDescription = selected.info.displayName,
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Apps, contentDescription = "Select Source",
                     modifier = Modifier.size(26.dp))
            }
        }

        // Dot indicator showing selected source count
        if (selectedSourceId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ── Bottom Sheet ────────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.3f)) }
        ) {
            FrostedGlass(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                borderRadius = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                blur = 0.dp, // Blur was hiding the text!
                tint = Color.Black.copy(alpha = 0.95f),
                hasBorder = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Source",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            onSourceSelected(null)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                        }) {
                            Text("Show All", color = Color.White)
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Search sources...", color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )

                    HorizontalDivider(color = AppColors.white020)

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Scraper list
                            if (filteredScrapers.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No sources found.",
                                        color = AppColors.white050
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    // "All Sources" row
                                    item {
                                        SourceRow(
                                            icon = { Icon(Icons.Default.AutoAwesome, null, tint = Color.White) },
                                            name = "All Sources",
                                            subtitle = "Show content from all available scrapers",
                                            isSelected = selectedSourceId == null,
                                            isWorking = true,
                                            onClick = {
                                                onSourceSelected(null)
                                                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                                            }
                                        )
                                    }

                                    items(filteredScrapers) { scraper ->
                                        SourceRow(
                                            icon = {
                                                if (scraper.info.iconUrl.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = scraper.info.iconUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp).clip(CircleShape)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            scraper.info.displayName.take(1),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            },
                                            name = scraper.info.displayName,
                                            subtitle = scraper.info.baseUrl,
                                            isSelected = scraper.info.id == selectedSourceId,
                                            isWorking = healthStatus[scraper.info.id] ?: scraper.info.isWorking,
                                            onClick = {
                                                onSourceSelected(scraper.info.id)
                                                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
                                            }
                                        )
                                    }
                                }
                            }

                            // Alphabet Scroller
                            if (alphabet.isNotEmpty() && searchQuery.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    alphabet.forEach { letter ->
                                        Text(
                                            text = letter,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable {
                                                    val index = filteredScrapers.indexOfFirst { 
                                                        it.info.displayName.startsWith(letter, ignoreCase = true) 
                                                    }
                                                    if (index >= 0) {
                                                        scope.launch {
                                                            listState.animateScrollToItem(index + 1) // +1 for "All Sources"
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 2.dp, horizontal = 4.dp)
                                        )
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

@Composable
private fun SourceRow(
    icon: @Composable () -> Unit,
    name: String,
    subtitle: String,
    isSelected: Boolean,
    isWorking: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            icon()
            // Status Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (isWorking) Color(0xFF00FF00) else Color(0xFFFF0000), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                 fontSize = 16.sp, color = Color.White)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null,
                 tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}
