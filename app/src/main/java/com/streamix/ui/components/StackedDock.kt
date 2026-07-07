package com.streamix.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

enum class DockFront { NAV, PLAYER }

@Composable
fun StackedDock(
    navBar: @Composable () -> Unit,
    playerBar: @Composable () -> Unit,
    frontCard: DockFront,
    onFrontCardChange: (DockFront) -> Unit
) {
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Visibility offset: how much of the back card is visible
    val visibleGap = 18.dp

    // Offsets and scales for both cards
    val navOffsetY by animateFloatAsState(
        targetValue = if (frontCard == DockFront.NAV) 0f else -visibleGap.value,
        animationSpec = springSpec,
        label = "navOffset"
    )
    val navScale by animateFloatAsState(
        targetValue = if (frontCard == DockFront.NAV) 1.0f else 0.97f,
        animationSpec = springSpec,
        label = "navScale"
    )
    val navZIndex = if (frontCard == DockFront.NAV) 2f else 1f

    val playerOffsetY by animateFloatAsState(
        targetValue = if (frontCard == DockFront.PLAYER) 0f else -visibleGap.value,
        animationSpec = springSpec,
        label = "playerOffset"
    )
    val playerScale by animateFloatAsState(
        targetValue = if (frontCard == DockFront.PLAYER) 1.0f else 0.97f,
        animationSpec = springSpec,
        label = "playerScale"
    )
    val playerZIndex = if (frontCard == DockFront.PLAYER) 2f else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            .height(90.dp), 
        contentAlignment = Alignment.BottomCenter
    ) {
        // Player Card
        Box(
            modifier = Modifier
                .zIndex(playerZIndex)
                .offset { IntOffset(0, playerOffsetY.dp.roundToPx()) }
                .scale(playerScale)
                .clip(RoundedCornerShape(35.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y < -10) onFrontCardChange(DockFront.PLAYER)
                            else if (dragAmount.y > 10) onFrontCardChange(DockFront.NAV)
                            
                            if (dragAmount.x < -10) onFrontCardChange(DockFront.PLAYER)
                            else if (dragAmount.x > 10) onFrontCardChange(DockFront.NAV)
                        }
                    )
                }
                .clickable(enabled = frontCard != DockFront.PLAYER) { onFrontCardChange(DockFront.PLAYER) }
        ) {
            playerBar()
        }

        // Nav Card
        Box(
            modifier = Modifier
                .zIndex(navZIndex)
                .offset { IntOffset(0, navOffsetY.dp.roundToPx()) }
                .scale(navScale)
                .clip(RoundedCornerShape(35.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y < -10) onFrontCardChange(DockFront.PLAYER)
                            else if (dragAmount.y > 10) onFrontCardChange(DockFront.NAV)
                            
                            if (dragAmount.x < -10) onFrontCardChange(DockFront.NAV)
                            else if (dragAmount.x > 10) onFrontCardChange(DockFront.PLAYER)
                        }
                    )
                }
                .clickable(enabled = frontCard != DockFront.NAV) { onFrontCardChange(DockFront.NAV) }
        ) {
            navBar()
        }
    }
}
