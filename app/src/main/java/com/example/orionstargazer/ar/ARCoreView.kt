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
import com.google.ar.sceneform.ArSceneView

@Composable
fun ARCoreView(
    stars: List<StarPositionCalculator.VisibleStar>,
    constellationSegments: List<ConstellationRenderer.Segment>,
    showConstellations: Boolean,
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
    val constellationRenderer = remember(sceneView) {
        sceneView?.let { ConstellationRenderer(it) }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ArSceneView(ctx).apply {
                sceneView = this
                sceneViewRef(this)
                sessionManager.attachSceneView(this)
                this.planeRenderer.isVisible = false
            }
        },
        update = {
            renderer?.updateStars(stars)
            if (showConstellations) {
                constellationRenderer?.updateSegments(constellationSegments)
            } else {
                constellationRenderer?.clear()
            }
        }
    )

    LaunchedEffect(sceneView) {
        sceneView?.resume()
    }

    DisposableEffect(sessionManager) {
        onDispose {
            renderer?.clearNodes()
            constellationRenderer?.clear()
            sceneView?.pause()
            sessionManager.destroy()
        }
    }
}

