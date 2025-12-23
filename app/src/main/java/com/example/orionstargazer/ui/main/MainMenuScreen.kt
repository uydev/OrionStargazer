package com.example.orionstargazer.ui.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onEnterStargazing: () -> Unit,
    onShowInstructions: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coverImage = remember {
        runCatching {
            context.assets.open("OrionStargazerApp.png").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050915), Color(0xFF0B1328))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Orion Stargazer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFEAF2FF)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Bring the constellations into view with guided AR stargazing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9BC1FF)
                )
            }

            if (coverImage != null) {
                Image(
                    bitmap = coverImage,
                    contentDescription = "Orion Stargazer cover art",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onEnterStargazing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E74FF))
                ) {
                    Text("Stargazing Mode")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onShowInstructions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Instructions")
                    }
                    OutlinedButton(
                        onClick = onShowSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Settings")
                    }
                }
                Text(
                    text = "Swipe up within the live view to open the Visible Stars sheet once you’re aiming toward the sky.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCFE0FF)
                )
            }
        }
    }
}

