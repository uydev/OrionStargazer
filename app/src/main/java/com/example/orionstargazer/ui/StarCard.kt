package com.example.orionstargazer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarInfoCard(
    name: String,
    mag: Double,
    spectral: String?,
    constellation: String?,
    ra: Double,
    dec: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            Modifier
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xFF222234),
                        1.0f to Color(0xFF313145)
                    )
                )
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                constellation?.let {
                    Text(
                        text = it,
                        color = Color(0xFFFCDB32),
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "Magnitude: %.2f".format(mag),
                    color = Color(0xFFFFB900),
                    fontSize = 16.sp
                )
                if (!spectral.isNullOrBlank()) {
                    Text(
                        text = "Spectral: $spectral",
                        color = Color(0xFFB2E8F9),
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "RA: %.2f°, Dec: %.2f°".format(ra, dec),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

