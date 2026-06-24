package com.streamix.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object StreamixColors {
    val Background     = Color(0xFF000000)
    val Surface        = Color(0xFF000000)
    val GlassOverlay   = Color(0x1AFFFFFF)
    val GlassBorder    = Color(0x33FFFFFF)
    val TextPrimary    = Color(0xFFFFFFFF)
    val TextSecondary  = Color(0xFFAAAAAA)
    val Accent         = Color(0xFFFFFFFF)
    val ProgressRed    = Color(0xFFFF0000)
}

data class CustomThemeColors(
    val primary: Color,      // Background
    val secondary: Color,    // Text/Overlays
    val tertiary: Color      // Action/Accent (Now Red for progress)
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomThemeColors(
        primary = Color.Black,
        secondary = Color.White,
        tertiary = Color.Red
    )
}

fun Color.toHex(): String {
    return String.format("#%06X", (0xFFFFFF and this.toArgb()))
}
