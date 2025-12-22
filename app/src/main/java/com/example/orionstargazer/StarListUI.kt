package com.example.orionstargazer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator

@Composable
fun StarList(
    stars: List<StarPositionCalculator.VisibleStar>,
    selectedStarId: Int?,
    onStarSelected: (Int) -> Unit
) {
    if (stars.isEmpty()) {
        Text(
            text = "No visible stars.",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(
                    Color(0x11000000),
                    Color(0x00000000)
                ))
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stars) { star ->
            val isSelected = selectedStarId != null && selectedStarId == star.star.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp)
                    .clickable { onStarSelected(star.star.id) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xCC17245A) else Color(0x99070A18)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 16.dp else 10.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(spectralColor(star.star.spectralType), shape = MaterialTheme.shapes.small)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            star.star.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFFEAF2FF)
                        )
                    }
                    Text(
                        "Alt: %.1f°, Az: %.1f°".format(star.altitude, star.azimuth),
                        fontSize = 16.sp
                    )
                    Text(
                        "Mag: %.2f".format(star.star.magnitude),
                        fontSize = 16.sp,
                        color = Color(0xFFCFE0FF)
                    )
                }
            }
        }
    }
}

private fun spectralColor(spectralType: String?): Color {
    val s = spectralType?.trim()?.uppercase() ?: return Color(0xFFEAF2FF)
    return when {
        s.startsWith("P") -> Color(0xFF7CFFB2) // planets
        s.startsWith("O") -> Color(0xFF9BB9FF)
        s.startsWith("B") -> Color(0xFFA9C6FF)
        s.startsWith("A") -> Color(0xFFD9E7FF)
        s.startsWith("F") -> Color(0xFFFFF6E3)
        s.startsWith("G") -> Color(0xFFFFE9B6)
        s.startsWith("K") -> Color(0xFFFFC07A)
        s.startsWith("M") -> Color(0xFFFF8A7A)
        else -> Color(0xFFEAF2FF)
    }
}

