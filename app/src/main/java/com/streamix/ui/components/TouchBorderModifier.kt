package com.streamix.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.streamix.ui.navigation.LocalProfile
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.touchBorder(
    shape: Shape = RoundedCornerShape(12.dp),
    defaultWidth: Dp = 0.dp,
    pressedWidth: Dp = 1.5.dp
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val currentProfile = LocalProfile.current.value
    
    val width by animateDpAsState(
        targetValue = if (isPressed) pressedWidth else defaultWidth,
        label = "borderWidth"
    )
    
    val targetColor = if (isPressed) {
        when (currentProfile) {
            com.streamix.core.model.Profile.YOUTUBE -> Color(0xFFC61A09)
            com.streamix.core.model.Profile.MOVIES -> Color(0xFF0077B6)
            com.streamix.core.model.Profile.ADULT -> Color(0xFFEF164F)
        }
    } else Color.Transparent

    val color by animateColorAsState(targetValue = targetColor, label = "borderColor")
    
    this.then(
        Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            .shadow(
                elevation = if (isPressed) 8.dp else 0.dp,
                shape = shape,
                spotColor = targetColor,
                ambientColor = targetColor
            )
            .border(width, color, shape)
    )
}




