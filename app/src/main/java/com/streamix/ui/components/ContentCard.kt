package com.streamix.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.streamix.core.plugin.SearchResult
import com.streamix.ui.screens.CardDensity
import com.streamix.ui.screens.CornerStyle
import com.streamix.ui.screens.FontSize
import com.streamix.ui.screens.PosterShape
import com.streamix.ui.screens.PosterStyle
import com.streamix.ui.screens.StreamixPrefs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentCard(
    item: SearchResult, 
    prefs: StreamixPrefs, 
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val cardWidth = when(prefs.cardDensity) {
        CardDensity.MINIMAL  -> 100.dp
        CardDensity.BALANCED -> 130.dp
        CardDensity.DETAILED -> 160.dp
    }
    val imageHeight = when(prefs.posterShape) {
        PosterShape.SQUARE   -> cardWidth
        PosterShape.ROUNDED  -> cardWidth * 1.5f
        PosterShape.STANDARD -> cardWidth * 1.4f
        PosterShape.MINIMAL  -> cardWidth * 1.2f
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(if (prefs.cornerStyle == CornerStyle.ROUNDED) 8.dp else if (prefs.cornerStyle == CornerStyle.EXTRA_ROUNDED) 16.dp else 0.dp),
        elevation = CardDefaults.cardElevation(if (prefs.posterStyle == PosterStyle.CARD) 4.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (prefs.posterStyle == PosterStyle.CARD) 
                                MaterialTheme.colorScheme.surfaceVariant 
                             else Color.Transparent
        )
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(
                            topStart = if (prefs.cornerStyle != CornerStyle.SHARP) 8.dp else 0.dp,
                            topEnd = if (prefs.cornerStyle != CornerStyle.SHARP) 8.dp else 0.dp,
                            bottomStart = if (prefs.posterStyle == PosterStyle.CARD) 0.dp else 8.dp,
                            bottomEnd = if (prefs.posterStyle == PosterStyle.CARD) 0.dp else 8.dp
                        ))
                )
                
                // Video quality indicator
                Surface(
                    modifier = Modifier.padding(6.dp).align(Alignment.BottomEnd),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        "HD", 
                        color = Color.White, 
                        fontSize = 8.sp, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (prefs.posterStyle != PosterStyle.LIST) {
                Text(
                    item.title,
                    fontSize = if (prefs.fontSize == FontSize.SMALL) 11.sp else 13.sp,
                    maxLines = if (prefs.cardDensity == CardDensity.DETAILED) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
