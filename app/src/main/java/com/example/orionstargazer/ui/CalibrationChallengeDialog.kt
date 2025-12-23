package com.example.orionstargazer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import kotlin.math.abs

@Composable
fun CalibrationChallengeDialog(
    azimuth: Float,
    altitude: Float,
    starsInView: List<StarPositionCalculator.VisibleStar>,
    highlightedStar: StarPositionCalculator.VisibleStar?,
    onMinimize: () -> Unit,
    onEnd: () -> Unit,
    targetAzimuth: Float?,
    targetAltitude: Float?,
    modifier: Modifier = Modifier
) {
    val polaris = starsInView.firstOrNull { it.star.name.contains("Polaris", ignoreCase = true) }
    val isPolarisLocked = highlightedStar?.star?.name?.contains("Polaris", ignoreCase = true) == true

    val flash = rememberInfiniteTransition(label = "flash")
    val flashAlpha = flash.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "flashAlpha"
    ).value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE0C1324)),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Calibration challenge",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFEAF2FF)
                )

                if (isPolarisLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0E2A1C).copy(alpha = flashAlpha), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Success!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF7CFFB2)
                            )
                            Text(
                                text = "Polaris (North Star) is centered in the reticle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCFE0FF)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Goal: center Polaris (North Star) in the reticle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCFE0FF)
                    )

                    if (targetAzimuth != null && targetAltitude != null) {
                        Text(
                            text = "Target (live): Az ${"%.0f".format(targetAzimuth)}° • Alt ${"%.0f".format(targetAltitude)}°",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEAF2FF)
                        )
                        Text(
                            text = "Tip: match the on-screen Azimuth/Altitude numbers to the target, while following the arrows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    }

                    // Provide "exact" reference coords (RA/Dec) plus practical aiming coords (Az/Alt).
                    Text(
                        text = "Polaris coordinates: RA 02h 31m 49s • Dec +89° 15′ 51″ (J2000)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA9C6FF)
                    )
                    Text(
                        text = "Practical aiming: Azimuth ≈ 0° (true North) • Altitude ≈ your latitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA9C6FF)
                    )

                    if (polaris == null) {
                        Text(
                            text = "Polaris isn’t in view yet. Use the target Az/Alt + arrows to guide you. If it still doesn’t show, open Settings and increase max magnitude—this challenge stays active while you do that.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    } else {
                        Text(
                            text = "Target now: Az ${"%.0f".format(polaris.azimuth)}° • Alt ${"%.0f".format(polaris.altitude)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9BC1FF)
                        )
                        val dAz = ((polaris.azimuth.toFloat() - azimuth + 540f) % 360f) - 180f
                        val dAlt = (polaris.altitude.toFloat() - altitude)
                        val azHint = if (abs(dAz) < 4f) "az ok" else if (dAz > 0f) "turn right" else "turn left"
                        val altHint = if (abs(dAlt) < 4f) "alt ok" else if (dAlt > 0f) "tilt up" else "tilt down"
                        Text(
                            text = "Guide: $azHint • $altHint",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onMinimize) { Text("Minimize") }
                    Button(onClick = onEnd) { Text("End") }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


