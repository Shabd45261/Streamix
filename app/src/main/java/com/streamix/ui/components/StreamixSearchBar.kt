package com.streamix.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamix.ui.theme.LocalCustomColors

@Composable
fun StreamixSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    history: List<String> = emptyList(),
    onRemoveHistoryItem: (String) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }
    
    val colors = LocalCustomColors.current

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.White.copy(0.15f),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 0.5.dp,
        label = "borderWidth"
    )

    Column(modifier = modifier) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(borderWidth, borderColor, RoundedCornerShape(27.dp)),
            cornerRadius = 27.dp
        ) {
            BasicTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    showHistory = it.isEmpty() && isFocused
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { 
                    onSearch(query)
                    showHistory = false
                }),
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { 
                        isFocused = it.isFocused 
                        showHistory = it.isFocused && query.isEmpty()
                    },
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            null,
                            tint = if (isFocused) Color.White else Color.White.copy(0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    "Search movies, shows...",
                                    color = Color.White.copy(0.4f), 
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange(""); showHistory = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showHistory && history.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(history) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onQueryChange(item)
                                    onSearch(item)
                                    showHistory = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = item,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { itemToDelete = item }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete search?", color = Color.White) },
            text = { Text("Are you sure you want to remove \"${itemToDelete}\" from history?", color = Color.White.copy(0.7f)) },
            confirmButton = {
                TextButton(onClick = { 
                    onRemoveHistoryItem(itemToDelete!!)
                    itemToDelete = null 
                }) {
                    Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = Color(0xFF1C1C1C),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
