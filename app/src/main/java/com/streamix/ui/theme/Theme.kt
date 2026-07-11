package com.streamix.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StreamixTheme(
    viewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val primaryColor by viewModel.primaryColor.collectAsState() // This is the background color
    
    // Enforce black or white background only
    val isLight = primaryColor.luminance() > 0.5f
    val backgroundColor = if (isLight) Color.White else Color.Black
    val textColor = if (isLight) Color.Black else Color.White
    val accentColor = textColor // Strictly Black & White as per request
    
    val customColors = remember(backgroundColor, textColor) {
        CustomThemeColors(
            primary = backgroundColor,
            secondary = textColor,
            tertiary = accentColor
        )
    }

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = textColor,
            secondary = accentColor,
            background = backgroundColor,
            surface = backgroundColor,
            onBackground = textColor,
            onSurface = textColor
        )
    } else {
        darkColorScheme(
            primary = textColor,
            secondary = accentColor,
            background = backgroundColor,
            surface = backgroundColor,
            onBackground = textColor,
            onSurface = textColor
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StreamixTypography,
            content = content
        )
    }
}
