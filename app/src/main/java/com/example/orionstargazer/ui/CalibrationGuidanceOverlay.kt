package com.example.orionstargazer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun CalibrationGuidanceOverlay(
    deviceAzimuth: Float,
    deviceAltitude: Float,
    targetAzimuth: Float?,
    targetAltitude: Float?,
    modifier: Modifier = Modifier
) {
    if (targetAzimuth == null || targetAltitude == null) return

    val dAz = angleDeltaDeg(targetAzimuth, deviceAzimuth)
    val dAlt = targetAltitude - deviceAltitude

    val azArrow = arrowsForDelta(dAz, horizontal = true)
    val altArrow = arrowsForDelta(dAlt, horizontal = false)

    val azColor = colorForDelta(abs(dAz))
    val altColor = colorForDelta(abs(dAlt))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Arrows around the reticle center (parking-assist style).
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (altArrow.isNotEmpty()) {
                Text(
                    text = altArrow,
                    color = altColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
            } else {
                Spacer(Modifier.height(40.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = if (azArrow.startsWith("⇦")) azArrow else "",
                    color = azColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.width(80.dp))
                Text(
                    text = if (azArrow.startsWith("⇨")) azArrow else "",
                    color = azColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private fun angleDeltaDeg(target: Float, current: Float): Float {
    // shortest signed delta in [-180, 180]
    val diff = ((target - current + 540f) % 360f) - 180f
    return diff
}

private fun arrowsForDelta(delta: Float, horizontal: Boolean): String {
    val mag = abs(delta)
    val count = when {
        mag >= 25f -> 3
        mag >= 10f -> 2
        mag >= 4f -> 1
        else -> 0
    }
    if (count == 0) return ""
    return if (horizontal) {
        if (delta > 0) "⇨".repeat(count) else "⇦".repeat(count)
    } else {
        if (delta > 0) "⇧".repeat(count) else "⇩".repeat(count)
    }
}

private fun colorForDelta(magnitude: Float): Color {
    return when {
        magnitude >= 25f -> Color(0xFFFF5A5A) // far: red
        magnitude >= 10f -> Color(0xFFFFB020) // mid: amber
        magnitude >= 4f -> Color(0xFFFFE36E) // near: yellow
        else -> Color(0xFF7CFFB2) // locked: green
    }
}


