package com.example.orionstargazer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NightModeToggle(nightMode: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (nightMode) "Night Mode: On" else "Night Mode: Off",
            fontSize = 16.sp,
            color = if (nightMode) Color(0xFFFF5B5B) else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = nightMode,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFDA3C3C),
                uncheckedThumbColor = Color(0xFF007AFF)
            )
        )
    }
}
