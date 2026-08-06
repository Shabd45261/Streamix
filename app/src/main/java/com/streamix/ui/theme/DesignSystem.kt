package com.streamix.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppColors {
    val black000 = Color(0xFF000000) // Pure AMOLED Black
    val black010 = Color(0xFF080808)
    val black020 = Color(0xFF101010)

    val white100 = Color(0xFFFFFFFF) // Pure White
    val white080 = Color(0xCCFFFFFF)
    val white050 = Color(0x80FFFFFF)
    val white020 = Color(0x33FFFFFF)

    val glassLight = Color(0x1AFFFFFF)
    val glassMedium = Color(0x33FFFFFF)
    val glassStrong = Color(0x4DFFFFFF)
    val glassBorder = Color(0x26FFFFFF)

    val success = Color(0xFF30D158)
    val error = Color(0xFFFF453A)
    val warning = Color(0xFFFFD60A)
}

@Composable
fun FrostedGlass(
    modifier: Modifier = Modifier,
    borderRadius: RoundedCornerShape = RoundedCornerShape(16.dp),
    blur: Dp = 20.dp,
    tint: Color = AppColors.glassMedium,
    hasBorder: Boolean = true,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(borderRadius)
            .blur(blur) // Added actual blur modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.6f),
                        tint.copy(alpha = 0.4f)
                    )
                )
            )
            .then(
                if (hasBorder) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = borderRadius
                    )
                } else {
                    Modifier
                }
            )
            .padding(padding)
    ) {
        // Need to wrap content in a Box without blur to keep text sharp
        Box(modifier = Modifier.blur(0.dp)) {
            content()
        }
    }
}
