package com.example.orionstargazer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun OverlayCompass(
    azimuth: Float,
    modifier: Modifier = Modifier
) {
    val direction = compassDirection(azimuth)
    Surface(
        shape = CircleShape,
        color = Color(0x440D1A33),
        modifier = modifier.size(130.dp),
        shadowElevation = 8.dp,
        border = null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = Color(0x80EAF2FF),
                    radius = radius,
                    style = Stroke(width = 2f)
                )
                rotate(degrees = -azimuth) {
                    drawLine(
                        color = Color(0xFFEAF2FF),
                        start = Offset(0f, -radius + 8f),
                        end = Offset(0f, radius - 8f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF7B8FFF),
                        start = Offset(0f, -radius + 8f),
                        end = Offset(0f, -radius + 28f),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(
                text = direction,
                color = Color(0xFFEAF2FF),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun compassDirection(azimuth: Float): String {
    val normalized = ((azimuth % 360) + 360) % 360
    return when (normalized) {
        in 22.5f..67.5f -> "NE"
        in 67.5f..112.5f -> "E"
        in 112.5f..157.5f -> "SE"
        in 157.5f..202.5f -> "S"
        in 202.5f..247.5f -> "SW"
        in 247.5f..292.5f -> "W"
        in 292.5f..337.5f -> "NW"
        else -> "N"
    }
}
