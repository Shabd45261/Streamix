package com.streamix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreamixSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.Red else Color.White.copy(0.1f),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 0.5.dp,
        label = "borderWidth"
    )

    FrostedGlassBox(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(26.dp))
            .padding(horizontal = 4.dp),
        cornerRadius = 26.dp
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search, 
                        null,
                        tint = if (isFocused) Color.Red else Color.White.copy(0.6f), 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search movies, shows...",
                                color = Color.White.copy(0.4f), 
                                fontSize = 16.sp
                            )
                        }
                        inner()
                    }
                }
            }
        )
    }
}
