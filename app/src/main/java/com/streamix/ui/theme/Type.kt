package com.streamix.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val StreamixTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White),
    bodyLarge     = TextStyle(fontSize = 16.sp, color = Color.White),
    bodyMedium    = TextStyle(fontSize = 14.sp, color = Color(0xFFAAAAAA)),
    labelSmall    = TextStyle(fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color.White.copy(alpha = 0.5f))
)
