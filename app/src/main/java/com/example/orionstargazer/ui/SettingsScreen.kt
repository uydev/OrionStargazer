package com.example.orionstargazer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.data.UserSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    nightMode: Boolean,
    onNightModeToggle: (Boolean) -> Unit,
    maxMagnitude: Float,
    onMaxMagnitudeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Night Mode (red UI)")
            Switch(
                checked = nightMode,
                onCheckedChange = onNightModeToggle
            )
        }
        Column {
            Text(text = "Star Visibility Limit (Magnitude: %.1f)".format(maxMagnitude))
            Slider(
                value = maxMagnitude,
                onValueChange = onMaxMagnitudeChange,
                valueRange = 0f..7f,
                steps = 13
            )
        }
    }
}