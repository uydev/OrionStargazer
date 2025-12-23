package com.example.orionstargazer.ui.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MainMenuScreen(
    onEnterStargazing: () -> Unit,
    onShowInstructions: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
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
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
                        .clickable { showAbout = true }
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

            // Bottom breathing room so the last line never sits on the gesture bar.
            Spacer(Modifier.height(12.dp))
        }

        if (showAbout) {
            Dialog(onDismissRequest = { showAbout = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE0C1324)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Orion Stargazer",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFEAF2FF)
                        )
                        Text(
                            text = "Developed by Hephaestus Systems (Uner YILMAZ)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCFE0FF)
                        )
                        Text(
                            text = "Tap anywhere outside to close.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9BC1FF)
                        )
                    }
                }
            }
        }
    }
}

