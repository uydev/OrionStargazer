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
import androidx.compose.ui.draw.alpha
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
                                        .coerceIn(0f, with(density) { sheetHeight.toPx() })
                                )
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetPx.value > with(density) { sheetHeight.toPx() } / 2) {
                                    offsetPx.animateTo(with(density) { sheetHeight.toPx() }, tween(200))
                                } else {
                                    offsetPx.animateTo(0f, tween(200))
                                }
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
    }
}
