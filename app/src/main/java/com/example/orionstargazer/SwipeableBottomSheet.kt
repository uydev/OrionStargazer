package com.example.orionstargazer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
    starListContent: @Composable () -> Unit,
    isExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit
) {
    val sheetHeight = 340.dp
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val sheetHeightPx = with(density) { sheetHeight.toPx() }
    val peekHeightPx = with(density) { 120.dp.toPx() }
    val collapsedOffset = (sheetHeightPx - peekHeightPx).coerceAtLeast(0f)
    val offsetPx = remember { Animatable(if (isExpanded) 0f else collapsedOffset) }

    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 0f else collapsedOffset
        offsetPx.animateTo(target, tween(260))
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        orientationContent()
        val liftAmount = 36.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, offsetPx.value.roundToInt()) }
                .padding(bottom = liftAmount)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xCC050617))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetPx.snapTo(
                                    (offsetPx.value + dragAmount).coerceIn(0f, sheetHeightPx)
                                )
                            }
                        },
                        onDragEnd = {
                            val nextExpanded = offsetPx.value <= collapsedOffset / 2f
                            scope.launch {
                                val target = if (nextExpanded) 0f else collapsedOffset
                                offsetPx.animateTo(target, tween(220))
                            }
                            onExpandChanged(nextExpanded)
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
                        .pointerInput(Unit) {
                            detectTapGestures { onExpandChanged(!isExpanded) }
                        }
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
                                    val nextExpanded = offsetPx.value <= collapsedOffset / 2f
                                    scope.launch {
                                        val target = if (nextExpanded) 0f else collapsedOffset
                                        offsetPx.animateTo(target, tween(220))
                                    }
                                    onExpandChanged(nextExpanded)
                                }
                            )
                        }
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
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {
                    detectTapGestures {
                        onExpandChanged(true)
                    }
                }
        )
    }
}