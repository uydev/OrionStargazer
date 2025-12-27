package com.example.orionstargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import kotlinx.coroutines.isActive

/**
 * Fallback reticle selection when we don't have an [com.google.ar.sceneform.ArSceneView].
 *
 * Picks the closest star to the device pointing (az/alt) within a small angular threshold.
 * Used by the calibration challenge so it can run on the starry background (no AR surface).
 */
@Composable
fun ApproxReticleStarSelector(
    deviceAzimuth: Float,
    deviceAltitude: Float,
    stars: List<StarPositionCalculator.VisibleStar>,
    reticleRadiusDeg: Double = 7.5,
    onStarChanged: (StarPositionCalculator.VisibleStar?) -> Unit,
) {
    LaunchedEffect(deviceAzimuth, deviceAltitude, stars, reticleRadiusDeg) {
        var lastEmitId: Int? = null
        while (isActive) {
            withFrameNanos { /* sync */ }

            var best: StarPositionCalculator.VisibleStar? = null
            var bestDist = Double.MAX_VALUE
            for (s in stars) {
                val d = StarPositionCalculator.viewDistanceDegrees(
                    starAz = s.azimuth,
                    starAlt = s.altitude,
                    deviceAz = deviceAzimuth,
                    deviceAlt = deviceAltitude
                )
                if (d < bestDist) {
                    best = s
                    bestDist = d
                }
            }

            val candidate = if (best != null && bestDist <= reticleRadiusDeg) best else null
            val candidateId = candidate?.star?.id
            if (candidateId != lastEmitId) {
                lastEmitId = candidateId
                onStarChanged(candidate)
            }
        }
    }
}


