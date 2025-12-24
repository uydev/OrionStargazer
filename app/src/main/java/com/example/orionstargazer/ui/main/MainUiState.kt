package com.example.orionstargazer.ui.main

import com.example.orionstargazer.ar.ConstellationRenderer
import com.example.orionstargazer.ar.ConstellationDrawMode
import com.example.orionstargazer.ar.StarRenderCapabilities
import com.example.orionstargazer.ar.StarRenderMode
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import com.example.orionstargazer.data.entities.StarEntity

data class MainUiState(
    val cameraPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val usingFallbackLocation: Boolean = false,
    val isSeeding: Boolean = true,
    val seedError: String? = null,
    val dbCount: Int = 0,
    val maxMagnitude: Double = 6.0,
    val showConstellations: Boolean = true,
    val constellationDrawMode: ConstellationDrawMode = ConstellationDrawMode.DETECTED,
    val showHighlights: Boolean = false,
    val highlights: List<String> = emptyList(),
    val showSettings: Boolean = false,
    val starRenderMode: StarRenderMode = StarRenderMode.GLOW_TEXTURE,
    val starRenderCapabilities: StarRenderCapabilities? = null,
    val shaderMaxStars: Int = 1200,
    val measuredFps: Float? = null,
    val effectiveStarRenderMode: StarRenderMode = StarRenderMode.GLOW_TEXTURE,
    val detectedConstellationName: String? = null,
    val detectedConstellationScore: Float = 0f,
    val azimuth: Float = 0f,
    val altitude: Float = 0f,
    val candidateStars: List<StarEntity> = emptyList(),
    val starsInView: List<StarPositionCalculator.VisibleStar> = emptyList(),
    val constellationPrimarySegments: List<ConstellationRenderer.Segment> = emptyList(),
    val constellationSecondarySegments: List<ConstellationRenderer.Segment> = emptyList(),
    val highlightedStar: StarPositionCalculator.VisibleStar? = null,
    val showXyOverlay: Boolean = false,
    val showCameraBackground: Boolean = false,
    val showCalibrationChallenge: Boolean = false,
    val polarisTargetAzimuth: Float? = null,
    val polarisTargetAltitude: Float? = null
)


