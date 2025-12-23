package com.example.orionstargazer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Subtle X/Y tilt overlay:
 * - X axis: altitude (pitch) slider on left edge (0..90°)
 * - Y axis: azimuth (heading) slider on bottom edge (0..360°)
 */
@Composable
fun XYTiltOverlay(
    azimuthDeg: Float,
    altitudeDeg: Float,
    modifier: Modifier = Modifier
) {
    // clamp + normalize
    val alt = altitudeDeg.coerceIn(-90f, 90f)
    val az = ((azimuthDeg % 360f) + 360f) % 360f

    Box(modifier = modifier) {
        // Bottom-left "L" layout (kept above the compass) with a small gap so X and Y do not touch.
        val baseBottom = 240.dp
        val leftInset = 12.dp
        val gap = 14.dp
        val yStartInset = 34.dp // shift Y to the right so it doesn't sit directly under X (clear separation)
        val yBarHeight = 44.dp

        // Left vertical (X) - altitude
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = leftInset, bottom = baseBottom + yBarHeight + gap)
                .height(220.dp)
                .background(Color(0x22000000), RoundedCornerShape(999.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            val trackHeight = 200f
            // map [-90..90] => [0..1]
            val t = (alt + 90f) / 180f
            val yOff = ((1f - t) * trackHeight).roundToInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("X (Alt)", style = MaterialTheme.typography.labelSmall, color = Color(0x99EAF2FF))
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .size(width = 6.dp, height = 200.dp)
                        .background(Color(0x220D1A33), RoundedCornerShape(999.dp))
                )
            }
            Box(
                modifier = Modifier
                    .offset(y = yOff.dp)
                    .size(10.dp)
                    .align(Alignment.TopCenter)
                    .background(Color(0x66EAF2FF), RoundedCornerShape(999.dp))
            )
            Text(
                text = "${alt.roundToInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0x99EAF2FF),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }

        // Bottom horizontal (Y) - azimuth
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = leftInset + yStartInset, bottom = baseBottom)
                .width(220.dp)
                .height(yBarHeight)
                .background(Color(0x22000000), RoundedCornerShape(999.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            val trackWidth = 220f
            val t = az / 360f
            val xOff = (t * trackWidth).roundToInt()
            // Track
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .background(Color(0x220D1A33), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .offset(x = xOff.dp)
                    .size(10.dp)
                    .align(Alignment.CenterStart)
                    .background(Color(0x66EAF2FF), RoundedCornerShape(999.dp))
            )
            Text(
                text = "${az.roundToInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0x99EAF2FF),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            Text(
                text = "Y (Az)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0x99EAF2FF),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-14).dp)
            )
        }
    }
}


