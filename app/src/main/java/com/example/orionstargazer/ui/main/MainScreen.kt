package com.example.orionstargazer.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.orionstargazer.StarList
import com.example.orionstargazer.SwipeableBottomSheet
import com.example.orionstargazer.ar.ARCoreView
import com.example.orionstargazer.ar.ConstellationDrawMode
import com.example.orionstargazer.ar.StarRenderMode
import com.example.orionstargazer.ui.DetectedStarLabel
import com.example.orionstargazer.ui.FrameRateTracker
import com.example.orionstargazer.ui.HighlightsScreen
import com.example.orionstargazer.ui.LoadingPill
import com.example.orionstargazer.ui.NightSkyBackground
import com.example.orionstargazer.ui.OverlayCompass
import com.example.orionstargazer.ui.PermissionCallout
import com.example.orionstargazer.ui.ReticleStarSelector
import com.example.orionstargazer.ui.ReticleOverlay
import com.example.orionstargazer.ui.SelectedStarCard
import com.example.orionstargazer.ui.SkyStatusBar
import com.example.orionstargazer.ui.OrientationDisplay
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectTransformGestures

@Composable
fun MainScreen(
    state: MainUiState,
    onToggleConstellations: (Boolean) -> Unit,
    onMaxMagnitudeChangeFinished: (Double) -> Unit,
    onMaxMagnitudeChanged: (Double) -> Unit,
    onStarSelected: (Int) -> Unit,
    onStarTapped: (com.example.orionstargazer.data.entities.StarEntity) -> Unit,
    onClearSelection: () -> Unit,
    onReticleStarChanged: (com.example.orionstargazer.domain.astronomy.StarPositionCalculator.VisibleStar?) -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onRequestLocationOnly: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onSetShowSettings: (Boolean) -> Unit = {},
    onSetShowHighlights: (Boolean) -> Unit = {},
    onStarRenderModeChanged: (StarRenderMode) -> Unit = {},
    onShaderMaxStarsChanged: (Int) -> Unit = {},
    onFpsSample: (Float) -> Unit = {},
    onConstellationDrawModeChanged: (ConstellationDrawMode) -> Unit = {},
    onPinchMagnitudeChange: (Float) -> Unit = {},
    onOpenMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showSettings) {
        SettingsPage(
            state = state,
            onBack = { onSetShowSettings(false) },
            onStarRenderModeChanged = onStarRenderModeChanged,
            onShaderMaxStarsChanged = onShaderMaxStarsChanged,
            onConstellationDrawModeChanged = onConstellationDrawModeChanged
        )
        return
    }

    var sceneView by remember { mutableStateOf<com.google.ar.sceneform.ArSceneView?>(null) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        FrameRateTracker(onFps = onFpsSample)
        NightSkyBackground(modifier = Modifier.fillMaxSize())
        SkyStatusBar(
            cameraOk = state.cameraPermissionGranted,
            locationOk = state.locationPermissionGranted,
            magnitude = state.maxMagnitude,
            fps = state.measuredFps,
            constellationMode = state.constellationDrawMode.name.take(6),
            caps = state.shaderMaxStars.toString(),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        IconButton(
            onClick = onOpenMainMenu,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(
                    color = Color(0x220D1A33),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open menu",
                tint = Color(0xFFEAF2FF)
            )
        }

        if (state.cameraPermissionGranted) {
            Box(Modifier.fillMaxSize()) {
                ARCoreView(
                    stars = state.starsInView,
                    constellationPrimarySegments = state.constellationPrimarySegments,
                    constellationSecondarySegments = state.constellationSecondarySegments,
                    showConstellations = state.showConstellations,
                    starRenderMode = state.effectiveStarRenderMode,
                    customShaderGlowEnabled = state.starRenderCapabilities?.supportsCustomShaderGlow == true,
                    shaderMaxStars = state.shaderMaxStars,
                    onStarTapped = onStarTapped,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val delta = (zoom - 1f) * 3f
                                if (kotlin.math.abs(delta) > 0.01f) {
                                    onPinchMagnitudeChange(delta)
                                }
                            }
                        },
                    sceneViewRef = { sceneView = it }
                )
                ReticleOverlay()
                state.highlightedStar?.let { star ->
                    DetectedStarLabel(
                        star = star,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 64.dp)
                    )
                }
            }
        } else {
            PermissionCallout(
                title = "Permissions needed",
                message = "Enable Camera (and Location for accurate sky alignment).",
                primaryActionText = "Grant",
                onPrimaryAction = onRequestPermissions,
                secondaryActionText = "Settings",
                onSecondaryAction = onOpenAppSettings,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(18.dp)
            )
        }

        // Reticle selection (UI-layer, depends on ArSceneView).
        ReticleStarSelector(
            sceneView = sceneView,
            stars = state.starsInView,
            onStarChanged = onReticleStarChanged
        )

        OverlayCompass(
            azimuth = state.azimuth,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
        )

        if (state.isSeeding && state.seedError == null) {
            LoadingPill(
                text = "Loading star catalog…",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )
        } else if (state.seedError != null) {
            LoadingPill(
                text = "Catalog load failed (see details in sheet)",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )
        }

        SwipeableBottomSheet(
            orientationContent = {
                OrientationDisplay(
                    azimuth = state.azimuth,
                    altitude = state.altitude,
                    modifier = Modifier.fillMaxSize()
                )
            },
            starListContent = {
                Column {
                    Spacer(Modifier.height(2.dp))
                    if (!state.locationPermissionGranted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Location is off — using a default location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFE9C7),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRequestLocationOnly) {
                                Text("Enable")
                            }
                        }
                    }
                    Text(
                        text = "Catalog: ${state.dbCount}  •  Mag ≤ ${"%.1f".format(state.maxMagnitude)}  •  Candidates: ${state.candidateStars.size}  •  In view: ${state.starsInView.size}",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEAF2FF)
                    )

                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                        Text(
                            text = "Magnitude limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEAF2FF)
                        )
                        Slider(
                            value = state.maxMagnitude.toFloat(),
                            onValueChange = { onMaxMagnitudeChanged(it.toDouble()) },
                            onValueChangeFinished = { onMaxMagnitudeChangeFinished(state.maxMagnitude) },
                            valueRange = 0f..8f,
                            steps = 15
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Constellations",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEAF2FF),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.showConstellations,
                            onCheckedChange = onToggleConstellations
                        )
                    }
                    if (state.showConstellations) {
                        val name = state.detectedConstellationName
                        val label = when (state.constellationDrawMode) {
                            ConstellationDrawMode.DETECTED -> "Detected"
                            ConstellationDrawMode.HYBRID -> "Detected"
                            ConstellationDrawMode.NEARBY -> "Mode"
                        }
                        Text(
                            text = if (state.constellationDrawMode == ConstellationDrawMode.NEARBY) {
                                "Mode: Nearby (atlas)"
                            } else {
                                if (name != null) "$label: $name" else "$label: —"
                            },
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA9C6FF)
                        )
                    }

                    state.seedError?.let {
                        Text(
                            text = "Seed error: $it",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    val hasSelection = state.highlightedStar != null
                    AnimatedVisibility(
                        visible = hasSelection,
                        enter = fadeIn() + slideInVertically { it / 3 },
                        exit = fadeOut() + slideOutVertically { it / 3 }
                    ) {
                        state.highlightedStar?.let { star ->
                            SelectedStarCard(
                                star = star,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                onClear = onClearSelection
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = !hasSelection,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "Aim the reticle at a star for details.",
                            color = Color(0xFFA9B9FF),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }

                    StarList(
                        stars = state.starsInView,
                        selectedStarId = state.highlightedStar?.star?.id,
                        onStarSelected = onStarSelected
                    )
                }
            }
        )

        if (state.showHighlights) {
            Dialog(onDismissRequest = { onSetShowHighlights(false) }) {
                HighlightsScreen(
                    highlights = if (state.highlights.isNotEmpty()) state.highlights else listOf("Scanning night sky…"),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


