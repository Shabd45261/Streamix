package com.streamix.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector

enum class Profile(val label: String, val icon: ImageVector) {
    MOVIES("Movies",   Icons.Default.Movie),
    YOUTUBE("YouTube", Icons.Default.PlayCircle),
    ADULT("Adult",     Icons.Default.Lock)
}
