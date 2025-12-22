package com.example.orionstargazer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SkyStatusBar(
    cameraOk: Boolean,
    locationOk: Boolean,
    magnitude: Double,
    fps: Float?,
    constellationMode: String,
    caps: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xBB0C1324)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(label = "Camera", value = if (cameraOk) "On" else "Off")
            StatusPill(label = "Location", value = if (locationOk) "On" else "Off")
            StatusPill(label = "Mag", value = "%.1f".format(magnitude))
            StatusPill(label = "Const", value = constellationMode)
            StatusPill(label = "Cap", value = caps)
            fps?.let {
                StatusPill(label = "FPS", value = "%.0f".format(it))
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B2435),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9EC5FF)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}
