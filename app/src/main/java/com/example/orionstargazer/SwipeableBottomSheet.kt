package com.example.orionstargazer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableBottomSheet(
    orientationContent: @Composable () -> Unit,
    starListContent: @Composable () -> Unit
) {
    val sheetHeight = 340.dp
    val scope = rememberCoroutineScope()
    val offsetPx = remember { Animatable(0f) }
    val density = LocalDensity.current
    val sheetHeightPx = with(density) { sheetHeight.toPx() }
    val halfPx = sheetHeightPx * 0.55f

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        orientationContent()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, offsetPx.value.roundToInt()) }
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xCC050617)) // translucent deep navy
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetPx.snapTo(
                                    (offsetPx.value + dragAmount)
                                        .coerceIn(0f, sheetHeightPx)
                                )
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                // Snap to nearest of: expanded (0), mid, collapsed (sheetHeight).
                                val v = offsetPx.value
                                val target = listOf(0f, halfPx, sheetHeightPx).minBy { kotlin.math.abs(it - v) }
                                offsetPx.animateTo(target, tween(220))
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(Modifier.padding(top = 18.dp, bottom = 8.dp)) {
                Box(
                    Modifier
                        .height(4.dp)
                        .fillMaxWidth(0.18f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x55EAF2FF))
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    "Visible Stars",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFEAF2FF),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp)
                )
                starListContent()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (offsetPx.value >= sheetHeightPx - 1f && dragAmount < 0f) {
                                scope.launch {
                                    offsetPx.snapTo(
                                        (offsetPx.value + dragAmount).coerceIn(0f, sheetHeightPx)
                                    )
                                }
                            }
                        }
                    )
                }
        )
    }
}
