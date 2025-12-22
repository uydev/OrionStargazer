package com.example.orionstargazer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.domain.astronomy.AstronomyFacts
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator

@Composable
fun SelectedStarCard(
    star: StarPositionCalculator.VisibleStar,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    val facts = remember(star.star.id, star.altitude, star.azimuth) {
        AstronomyFacts.factsForVisibleStar(star)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1230)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = star.star.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAF2FF),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0x99EAF2FF),
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable { onClear() }
                )
            }
            Text(
                text = listOfNotNull(star.star.constellation, star.star.spectralType).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCFE0FF)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Alt ${"%.1f".format(star.altitude)}°, Az ${"%.1f".format(star.azimuth)}°   •   Mag ${"%.2f".format(star.star.magnitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFF6E3)
            )
            star.star.distance?.let { d ->
                Text(
                    text = "Distance: ${"%.1f".format(d)} ly",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCFE0FF)
                )
            }

            Spacer(Modifier.height(10.dp))
            EducationFactsSection(facts = facts)
        }
    }
}

