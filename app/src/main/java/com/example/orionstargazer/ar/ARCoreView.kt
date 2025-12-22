package com.example.orionstargazer.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.example.orionstargazer.ui.ShaderStarOverlay
import com.google.ar.sceneform.ArSceneView

@Composable
fun ARCoreView(
    stars: List<StarPositionCalculator.VisibleStar>,
    constellationPrimarySegments: List<ConstellationRenderer.Segment>,
    constellationSecondarySegments: List<ConstellationRenderer.Segment>,
    showConstellations: Boolean,
    starRenderMode: StarRenderMode,
    customShaderGlowEnabled: Boolean,
    shaderMaxStars: Int,
    onStarTapped: (com.example.orionstargazer.data.entities.StarEntity) -> Unit,
    modifier: Modifier = Modifier,
    sceneViewRef: (ArSceneView?) -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { ARSessionManager(context) }
    var sceneView: ArSceneView? by remember { mutableStateOf(null) }
    val renderer = remember(sceneView) {
        sceneView?.let { ARStarDomeRenderer(it, onStarTapped) }
    }
    val constellationPrimaryRenderer = remember(sceneView) {
        sceneView?.let { ConstellationRenderer(it, lineRadius = 0.012f, lineColor = com.google.ar.sceneform.rendering.Color(0.35f, 0.90f, 1.00f)) }
    }
    val constellationSecondaryRenderer = remember(sceneView) {
        // Dimmer + thinner context layer for HYBRID.
        sceneView?.let { ConstellationRenderer(it, lineRadius = 0.007f, lineColor = com.google.ar.sceneform.rendering.Color(0.12f, 0.45f, 0.80f)) }
    }
    var lastShowConstellations by remember { mutableStateOf(true) }

    val shaderActive = starRenderMode == StarRenderMode.CUSTOM_SHADER_GLOW && customShaderGlowEnabled

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                ArSceneView(ctx).apply {
                    sceneView = this
                    sceneViewRef(this)
                    sessionManager.attachSceneView(this)
                    this.planeRenderer.isVisible = false
                }
            },
            update = {
                if (shaderActive) {
                    // Hide 3D star sprites when using the shader overlay.
                    renderer?.updateStars(emptyList(), StarRenderMode.SOLID)
                } else {
                    renderer?.updateStars(stars, starRenderMode)
                }
                if (showConstellations) {
                    constellationPrimaryRenderer?.updateSegments(constellationPrimarySegments)
                    constellationSecondaryRenderer?.updateSegments(constellationSecondarySegments)
                } else if (lastShowConstellations) {
                    // Only clear once when the toggle flips off (avoid doing work every frame).
                    constellationPrimaryRenderer?.clear()
                    constellationSecondaryRenderer?.clear()
                }
                lastShowConstellations = showConstellations
            }
        )

        if (shaderActive) {
            ShaderStarOverlay(
                sceneView = sceneView,
                stars = stars,
                modifier = Modifier.matchParentSize(),
                maxStars = shaderMaxStars
            )
        }
    }

    LaunchedEffect(sceneView) {
        sceneView?.resume()
    }

    DisposableEffect(sessionManager) {
        onDispose {
            renderer?.clearNodes()
            constellationPrimaryRenderer?.clear()
            constellationSecondaryRenderer?.clear()
            sceneView?.pause()
            sessionManager.destroy()
        }
    }
}

