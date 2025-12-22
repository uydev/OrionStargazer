package com.example.orionstargazer.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.ar.ConstellationDrawMode
import com.example.orionstargazer.ar.StarRenderMode

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsPage(
    state: MainUiState,
    onBack: () -> Unit,
    onStarRenderModeChanged: (StarRenderMode) -> Unit,
    onShaderMaxStarsChanged: (Int) -> Unit,
    onConstellationDrawModeChanged: (ConstellationDrawMode) -> Unit
) {
    val shaderSupported = state.starRenderCapabilities?.supportsCustomShaderGlow == true
    val shaderReason = state.starRenderCapabilities?.reasonIfUnsupported ?: "Not supported on this device"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1230)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Star rendering",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAF2FF)
                    )
                    Text(
                        text = "Pick the look you prefer. If your device struggles, use Solid.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCFE0FF)
                    )
                    state.measuredFps?.let { fps ->
                        Text(
                            text = "Estimated FPS: ${"%.0f".format(fps)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    }
                    if (state.starRenderMode == StarRenderMode.AUTO) {
                        val chosen = when (state.effectiveStarRenderMode) {
                            StarRenderMode.SOLID -> "Solid"
                            StarRenderMode.GLOW_TEXTURE -> "Glow (texture)"
                            StarRenderMode.CUSTOM_SHADER_GLOW -> "Custom shader glow"
                            StarRenderMode.AUTO -> "Auto"
                        }
                        Text(
                            text = "Auto chose: $chosen",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    RenderModeRow(
                        title = "Auto",
                        subtitle = "Chooses the best mode based on FPS/device",
                        selected = state.starRenderMode == StarRenderMode.AUTO,
                        enabled = true,
                        onSelect = { onStarRenderModeChanged(StarRenderMode.AUTO) }
                    )
                    HorizontalDivider(color = Color(0x221A2B6B))
                    RenderModeRow(
                        title = "Glow (texture)",
                        subtitle = "Best look, a little more overdraw",
                        selected = state.starRenderMode == StarRenderMode.GLOW_TEXTURE,
                        enabled = true,
                        onSelect = { onStarRenderModeChanged(StarRenderMode.GLOW_TEXTURE) }
                    )
                    HorizontalDivider(color = Color(0x221A2B6B))
                    RenderModeRow(
                        title = "Solid (fast)",
                        subtitle = "Clean dots, lowest GPU cost",
                        selected = state.starRenderMode == StarRenderMode.SOLID,
                        enabled = true,
                        onSelect = { onStarRenderModeChanged(StarRenderMode.SOLID) }
                    )
                    HorizontalDivider(color = Color(0x221A2B6B))
                    RenderModeRow(
                        title = "Custom shader glow (experimental)",
                        subtitle = if (shaderSupported) "Procedural GPU glow (AGSL RuntimeShader)" else shaderReason,
                        selected = state.starRenderMode == StarRenderMode.CUSTOM_SHADER_GLOW,
                        enabled = shaderSupported,
                        onSelect = { onStarRenderModeChanged(StarRenderMode.CUSTOM_SHADER_GLOW) }
                    )

                    val showCap = shaderSupported && (state.starRenderMode == StarRenderMode.CUSTOM_SHADER_GLOW || state.starRenderMode == StarRenderMode.AUTO)
                    if (showCap) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Shader performance cap (${state.shaderMaxStars} stars)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEAF2FF)
                        )
                        Slider(
                            value = state.shaderMaxStars.toFloat(),
                            onValueChange = { onShaderMaxStarsChanged(it.toInt()) },
                            valueRange = 200f..2500f,
                            steps = 23
                        )
                        Text(
                            text = "Lower this if shader mode stutters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCFE0FF)
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Constellation lines",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEAF2FF)
                    )
                    Text(
                        text = "Choose how constellations are drawn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCFE0FF)
                    )
                    Spacer(Modifier.height(6.dp))
                    RenderModeRow(
                        title = "Mode A — Detected (highlight)",
                        subtitle = "Only the best-matching constellation (animated)",
                        selected = state.constellationDrawMode == ConstellationDrawMode.DETECTED,
                        enabled = true,
                        onSelect = { onConstellationDrawModeChanged(ConstellationDrawMode.DETECTED) }
                    )
                    HorizontalDivider(color = Color(0x221A2B6B))
                    RenderModeRow(
                        title = "Mode B — Nearby (atlas)",
                        subtitle = "Draw nearby lines from any constellation",
                        selected = state.constellationDrawMode == ConstellationDrawMode.NEARBY,
                        enabled = true,
                        onSelect = { onConstellationDrawModeChanged(ConstellationDrawMode.NEARBY) }
                    )
                    HorizontalDivider(color = Color(0x221A2B6B))
                    RenderModeRow(
                        title = "Hybrid",
                        subtitle = "Detected highlighted + faint nearby context",
                        selected = state.constellationDrawMode == ConstellationDrawMode.HYBRID,
                        enabled = true,
                        onSelect = { onConstellationDrawModeChanged(ConstellationDrawMode.HYBRID) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderModeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = if (enabled) onSelect else null, enabled = enabled)
        Column(Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFEAF2FF).copy(alpha = alpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCFE0FF).copy(alpha = alpha)
            )
        }
    }
}

