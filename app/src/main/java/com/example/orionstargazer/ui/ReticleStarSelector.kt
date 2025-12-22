package com.example.orionstargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.orionstargazer.ar.ScreenProjectionUtil
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * UI-layer helper:
 * periodically computes which visible star is inside the reticle.
 *
 * This is intentionally not in the ViewModel because it needs access to the AR view.
 */
@Composable
fun ReticleStarSelector(
    sceneView: ArSceneView?,
    stars: List<StarPositionCalculator.VisibleStar>,
    reticleSize: Dp = 48.dp,
    onStarChanged: (StarPositionCalculator.VisibleStar?) -> Unit,
) {
    val density = LocalDensity.current

    LaunchedEffect(sceneView, stars, reticleSize) {
        // Use frame clock to avoid running when Compose isn't drawing.
        var lastEmitId: Int? = null
        while (isActive) {
            withFrameNanos { /* sync */ }

            val candidate = computeStarInReticle(sceneView, stars, reticleSize.value * density.density)
            val candidateId = candidate?.star?.id
            if (candidateId != lastEmitId) {
                lastEmitId = candidateId
                onStarChanged(candidate)
            }
        }
    }
}

private fun computeStarInReticle(
    sceneView: ArSceneView?,
    stars: List<StarPositionCalculator.VisibleStar>,
    reticleSizePx: Float
): StarPositionCalculator.VisibleStar? {
    if (sceneView == null) return null
    if (sceneView.width <= 0 || sceneView.height <= 0) return null

    val centerX = sceneView.width / 2f
    val centerY = sceneView.height / 2f
    val half = reticleSizePx / 2f
    val left = centerX - half
    val right = centerX + half
    val top = centerY - half
    val bottom = centerY + half

    var candidate: StarPositionCalculator.VisibleStar? = null
    var minDist = Float.MAX_VALUE

    stars.forEach { star ->
        // Match the sky-dome radius used elsewhere (10f).
        val worldPos = Vector3(
            (10f * cos(Math.toRadians(star.altitude)) * sin(Math.toRadians(star.azimuth))).toFloat(),
            (10f * sin(Math.toRadians(star.altitude))).toFloat(),
            (-10f * cos(Math.toRadians(star.altitude)) * cos(Math.toRadians(star.azimuth))).toFloat()
        )
        val screenPos = ScreenProjectionUtil.projectWorldToScreen(sceneView, worldPos) ?: return@forEach
        val x = screenPos.x.toFloat()
        val y = screenPos.y.toFloat()
        if (x in left..right && y in top..bottom) {
            val dist = ((x - centerX).pow(2) + (y - centerY).pow(2))
            if (dist < minDist) {
                candidate = star
                minDist = dist
            }
        }
    }

    return candidate
}

