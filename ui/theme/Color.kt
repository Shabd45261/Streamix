package com.streamix.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object StreamixColors {
    val Background = Color(0xFF000000)
    val Surface    = Color(0xFF121212)
    val AccentBlue = Color(0xFF1E88E5)
    val Red        = Color(0xFFE53935)
}

data class CustomThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val LocalCustomColors = staticCompositionLocalOf {
    CustomThemeColors(
        primary = Color.Black,
        secondary = Color.White,
        tertiary = Color.Red
    )
}
