package com.example.orionstargazer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator

@Composable
fun DetectedStarLabel(
    star: StarPositionCalculator.VisibleStar?,
    modifier: Modifier = Modifier
) {
    if (star == null) return
    val nameText = star.star.name
    val detailText = "Mag ${"%.2f".format(star.star.magnitude)} • Alt ${"%.0f".format(star.altitude)}°"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC050617),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Detected",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9CD0FF)
            )
            Text(
                text = nameText,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFEAF2FF)
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCFE0FF)
            )
        }
    }
}