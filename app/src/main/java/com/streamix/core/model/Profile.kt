package com.streamix.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Profile(val label: String, val icon: ImageVector, val secondaryColor: Color) {
    MOVIES( "Movies",  Icons.Default.Movie,      Color(0xFF0077B6)),
    YOUTUBE("YouTube", Icons.Default.PlayCircle, Color(0xFFC61A09)),
    ADULT(  "Adult",   Icons.Default.Lock,       Color(0xFFEF164F))
}
