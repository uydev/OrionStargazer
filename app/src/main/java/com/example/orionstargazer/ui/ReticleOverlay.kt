package com.example.orionstargazer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview

/**
 * A simple aiming reticle for the center of the screen, matching the requested UX.
 */
@Composable
fun ReticleOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension * 0.05f
        val strokeWidth = size.minDimension * 0.005f

        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            center = Offset(centerX, centerY),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(centerX - radius * 1.5f, centerY),
            end = Offset(centerX + radius * 1.5f, centerY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(centerX, centerY - radius * 1.5f),
            end = Offset(centerX, centerY + radius * 1.5f),
            strokeWidth = strokeWidth
        )
    }
}

@Preview
@Composable
fun ReticleOverlayPreview() {
    ReticleOverlay()
}
