package com.example.orionstargazer.ui

import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.example.orionstargazer.ar.ScreenProjectionUtil
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Provides a shared function for finding all stars/objects inside the reticle zone, sorted by distance to the center.
 */
object ReticleHitTestUtil {
    /**
     * Finds all visible stars in the AR reticle (rectangular/circular region at center of screen).
     * Returns a list sorted by pixel distance from the reticle center (closest first).
     */
    fun getStarsInReticle(
        sceneView: ArSceneView?,
        stars: List<StarPositionCalculator.VisibleStar>,
        reticleSizePx: Float
    ): List<Pair<StarPositionCalculator.VisibleStar, Float>> {
        if (sceneView == null || sceneView.width <= 0 || sceneView.height <= 0) return emptyList()
        val centerX = sceneView.width / 2f
        val centerY = sceneView.height / 2f
        val half = reticleSizePx / 2f
        val left = centerX - half
        val right = centerX + half
        val top = centerY - half
        val bottom = centerY + half
        val inZone = mutableListOf<Pair<StarPositionCalculator.VisibleStar, Float>>()
        for (star in stars) {
            // World position on unit sky dome, Z is forward
            val worldPos = Vector3(
                (10f * cos(Math.toRadians(star.altitude)) * sin(Math.toRadians(star.azimuth))).toFloat(),
                (10f * sin(Math.toRadians(star.altitude))).toFloat(),
                (-10f * cos(Math.toRadians(star.altitude)) * cos(Math.toRadians(star.azimuth))).toFloat()
            )
            val screenPos = ScreenProjectionUtil.projectWorldToScreen(sceneView, worldPos) ?: continue
            val x = screenPos.x.toFloat()
            val y = screenPos.y.toFloat()
            if (x in left..right && y in top..bottom) {
                val dist = ((x - centerX).pow(2) + (y - centerY).pow(2))
                inZone.add(star to dist)
            }
        }
        return inZone.sortedBy { it.second }
    }
}

