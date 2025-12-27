package com.example.orionstargazer.ui.main

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orionstargazer.ar.StarRenderCapabilities
import com.example.orionstargazer.domain.astronomy.ConstellationCatalog
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import com.example.orionstargazer.data.StarRepository
import com.example.orionstargazer.data.UserSettings
import com.example.orionstargazer.ar.ConstellationDrawMode
import com.example.orionstargazer.ar.StarRenderMode
import com.example.orionstargazer.domain.engine.SkyPipeline
import com.example.orionstargazer.sensors.LocationProvider
import com.example.orionstargazer.sensors.OrientationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StarRepository.getInstance(app)
    private val constellations = ConstellationCatalog.load(app)
    private val pipeline = SkyPipeline(repo, constellations)
    private val capabilities = StarRenderCapabilities.detect(app)

    private val orientationProvider = OrientationProvider(app)
    private val locationProvider = LocationProvider(app)

    var state: MainUiState by mutableStateOf(MainUiState())
        private set

    private var lastConstellationUpdateMs: Long = 0L
    private var started: Boolean = false
    private var candidateIndex: SkyPipeline.EquatorialIndex = pipeline.buildEquatorialIndex(emptyList())
    private var cachedPolaris: com.example.orionstargazer.data.entities.StarEntity? = null

    fun onPermissionsChanged(cameraGranted: Boolean, locationGranted: Boolean) {
        state = state.copy(
            cameraPermissionGranted = cameraGranted,
            locationPermissionGranted = locationGranted
        )
        if (locationGranted) runCatching { locationProvider.start() } else runCatching { locationProvider.stop() }
        if (cameraGranted) runCatching { orientationProvider.start() } else runCatching { orientationProvider.stop() }
    }

    fun setShowConstellations(show: Boolean) {
        state = state.copy(showConstellations = show)
    }

    fun setShowSettings(show: Boolean) {
        state = state.copy(showSettings = show)
    }

    fun setStarRenderMode(mode: StarRenderMode) {
        state = state.copy(starRenderMode = mode)
        viewModelScope.launch {
            UserSettings.setStarRenderMode(getApplication(), mode)
        }
    }

    fun setShaderMaxStars(value: Int) {
        state = state.copy(shaderMaxStars = value)
        viewModelScope.launch {
            UserSettings.setShaderMaxStars(getApplication(), value)
        }
    }

    fun setConstellationDrawMode(mode: ConstellationDrawMode) {
        state = state.copy(constellationDrawMode = mode)
        viewModelScope.launch {
            UserSettings.setConstellationDrawMode(getApplication(), mode)
        }
    }

    fun onFpsSample(fps: Float) {
        state = state.copy(measuredFps = fps)
    }

    fun setMaxMagnitude(value: Double) {
        state = state.copy(maxMagnitude = value)
        viewModelScope.launch {
            UserSettings.setMaxMagnitude(getApplication(), value)
        }
    }

    fun setHighlightedStar(star: StarPositionCalculator.VisibleStar?) {
        state = state.copy(highlightedStar = star)
    }

    fun setShowHighlights(show: Boolean) {
        state = state.copy(showHighlights = show)
    }

    fun setShowXyOverlay(show: Boolean) {
        state = state.copy(showXyOverlay = show)
        viewModelScope.launch {
            UserSettings.setShowXyOverlay(getApplication(), show)
        }
    }

    fun setShowCameraBackground(show: Boolean) {
        state = state.copy(showCameraBackground = show)
        viewModelScope.launch {
            UserSettings.setShowCameraBackground(getApplication(), show)
        }
    }

    fun setShowCalibrationChallenge(show: Boolean) {
        state = state.copy(showCalibrationChallenge = show)
    }

    fun start() {
        if (started) return
        started = true
        state = state.copy(starRenderCapabilities = capabilities)
        // Seed + cache catalog
        viewModelScope.launch {
            state = state.copy(isSeeding = true, seedError = null)
            try {
                val status = pipeline.ensureCatalogSeeded(getApplication())
                state = state.copy(dbCount = status.dbCount, isSeeding = false)
                // Stash map internally by reusing pipeline calls on demand; keep count in state.
                // (We avoid putting a huge map into Compose state.)
                cachedAllStarsById = status.allStarsById
            } catch (t: Throwable) {
                state = state.copy(
                    isSeeding = false,
                    seedError = "${t.javaClass.simpleName}: ${t.message ?: "unknown"}"
                )
            }
        }

        // Settings flow
        viewModelScope.launch {
            UserSettings.maxMagnitudeFlow(getApplication()).collectLatest {
                state = state.copy(maxMagnitude = it)
            }
        }
        viewModelScope.launch {
            UserSettings.starRenderModeFlow(getApplication()).collectLatest {
                val mode = if (it == StarRenderMode.CUSTOM_SHADER_GLOW && !capabilities.supportsCustomShaderGlow) {
                    StarRenderMode.GLOW_TEXTURE
                } else it
                state = state.copy(starRenderMode = mode)
            }
        }
        viewModelScope.launch {
            UserSettings.shaderMaxStarsFlow(getApplication()).collectLatest {
                state = state.copy(shaderMaxStars = it)
            }
        }
        viewModelScope.launch {
            UserSettings.constellationDrawModeFlow(getApplication()).collectLatest {
                state = state.copy(constellationDrawMode = it)
            }
        }
        viewModelScope.launch {
            UserSettings.showXyOverlayFlow(getApplication()).collectLatest { enabled ->
                state = state.copy(showXyOverlay = enabled)
            }
        }
        viewModelScope.launch {
            UserSettings.showCameraBackgroundFlow(getApplication()).collectLatest { enabled ->
                state = state.copy(showCameraBackground = enabled)
            }
        }

        // Candidate refresh loop
        viewModelScope.launch {
            while (true) {
                val loc = locationProvider.location
                val effectiveLoc = loc ?: defaultLocation()
                val candidates =
                    runCatching { pipeline.loadCandidates(state.maxMagnitude, effectiveLoc) }.getOrDefault(emptyList())
                candidateIndex = pipeline.buildEquatorialIndex(candidates)
                state = state.copy(
                    candidateStars = candidates,
                    usingFallbackLocation = !state.locationPermissionGranted || loc == null
                )
                delay(2000)
            }
        }

        // Main compute loop
        viewModelScope.launch {
            while (true) {
                // If camera permission is missing, don’t run sensor-driven updates.
                if (!state.cameraPermissionGranted) {
                    state = state.copy(
                        starsInView = emptyList(),
                        constellationPrimarySegments = emptyList(),
                        constellationSecondarySegments = emptyList()
                    )
                    delay(350)
                    continue
                }
                val az = orientationProvider.azimuth
                val alt = orientationProvider.altitude
                val loc = locationProvider.location
                val location = loc ?: defaultLocation()
                val calendar = Calendar.getInstance()

                // Polaris target (always compute so calibration can show exact numbers).
                val polaris = cachedPolaris
                    ?: state.candidateStars.firstOrNull { it.name.contains("Polaris", ignoreCase = true) }
                    ?: cachedAllStarsById.values.firstOrNull { it.name.contains("Polaris", ignoreCase = true) }
                if (cachedPolaris == null && polaris != null) cachedPolaris = polaris
                val polarisAltAz = if (polaris != null) {
                    StarPositionCalculator.computeAltAz(calendar, location, polaris)
                } else null

                // Stars: fast cone-search using equatorial unit-vector cache.
                val visibleStars = pipeline.computeVisibleStarsFast(
                    calendar = calendar,
                    location = location,
                    azimuth = az,
                    altitude = alt,
                    index = candidateIndex,
                    fieldOfView = 60.0,
                    minAltitude = 0.0
                )

                // Planets: small list, compute normally.
                val planetEntities = pipeline.buildSkyEntities(calendar, emptyList())
                val visiblePlanets = pipeline.computeVisibleStars(calendar, location, az, alt, planetEntities)

                val altAzCache = pipeline.buildAltAzCache(calendar, location, cachedAllStarsById.values)
                val visible = (visibleStars + visiblePlanets).sortedWith(
                    compareBy(
                        { StarPositionCalculator.viewDistanceDegrees(it.azimuth, it.altitude, az, alt) },
                        { it.star.magnitude }
                    )
                )

                val effectiveMode = resolveAutoMode(
                    selected = state.starRenderMode,
                    fps = state.measuredFps,
                    capabilities = capabilities
                )

                var primarySegments = state.constellationPrimarySegments
                var secondarySegments = state.constellationSecondarySegments
                if (state.showConstellations) {
                    val now = System.currentTimeMillis()
                    if (now - lastConstellationUpdateMs > 450) {
                        when (state.constellationDrawMode) {
                            ConstellationDrawMode.NEARBY -> {
                                primarySegments = pipeline.buildConstellationSegments(
                                    calendar = calendar,
                                    location = location,
                                    azimuth = az,
                                    altitude = alt,
                                    allStarsById = cachedAllStarsById,
                                    altAzCache = altAzCache
                                )
                                secondarySegments = emptyList()
                                state = state.copy(
                                    detectedConstellationName = null,
                                    detectedConstellationScore = 0f
                                )
                            }

                            ConstellationDrawMode.DETECTED,
                            ConstellationDrawMode.HYBRID -> {
                                val detection = pipeline.detectConstellation(
                                    visibleStars = visible,
                                    deviceAzimuth = az,
                                    deviceAltitude = alt,
                                    altAzCache = altAzCache
                                )

                                // Hysteresis to reduce flicker.
                                val currentName = state.detectedConstellationName
                                val currentScore = state.detectedConstellationScore
                                val nextName: String?
                                val nextScore: Float

                                if (detection == null) {
                                    // Drop only if we were already weak.
                                    nextName = if (currentName != null && currentScore >= 0.45f) currentName else null
                                    nextScore = if (nextName != null) currentScore else 0f
                                } else if (currentName == null) {
                                    nextName = if (detection.score >= 0.67f) detection.name else null
                                    nextScore = if (nextName != null) detection.score else 0f
                                } else if (currentName == detection.name) {
                                    nextName = currentName
                                    nextScore = detection.score
                                } else {
                                    // Switch only if new candidate is clearly better.
                                    val canSwitch = detection.score >= 0.75f && detection.score > currentScore + 0.10f
                                    nextName = if (canSwitch) detection.name else currentName
                                    nextScore = if (canSwitch) detection.score else currentScore
                                }

                                primarySegments = if (nextName != null) {
                                    pipeline.buildDetectedConstellationSegments(
                                        constellationName = nextName,
                                        calendar = calendar,
                                        location = location,
                                        azimuth = az,
                                        altitude = alt,
                                        allStarsById = cachedAllStarsById,
                                        altAzCache = altAzCache
                                    )
                                } else {
                                    emptyList()
                                }

                                secondarySegments = if (state.constellationDrawMode == ConstellationDrawMode.HYBRID) {
                                    val allNearby = pipeline.buildConstellationSegments(
                                        calendar = calendar,
                                        location = location,
                                        azimuth = az,
                                        altitude = alt,
                                        allStarsById = cachedAllStarsById,
                                        altAzCache = altAzCache
                                    )
                                    val filtered = if (nextName != null) {
                                        allNearby.filterNot { it.key.startsWith("$nextName:") }
                                    } else allNearby
                                    // cap to avoid clutter on large datasets
                                    filtered.take(140)
                                } else {
                                    emptyList()
                                }

                                // Update detection state together with segments refresh.
                                state = state.copy(
                                    detectedConstellationName = nextName,
                                    detectedConstellationScore = nextScore
                                )
                            }
                        }
                        lastConstellationUpdateMs = now
                    }
                } else {
                    primarySegments = emptyList()
                    secondarySegments = emptyList()
                    state = state.copy(
                        detectedConstellationName = null,
                        detectedConstellationScore = 0f
                    )
                }

                val highlightTexts = buildHighlights(visible)

                state = state.copy(
                    azimuth = az,
                    altitude = alt,
                    starsInView = visible,
                    constellationPrimarySegments = primarySegments,
                    constellationSecondarySegments = secondarySegments,
                    usingFallbackLocation = !state.locationPermissionGranted || loc == null,
                    effectiveStarRenderMode = effectiveMode
                    ,
                    highlights = highlightTexts,
                    polarisTargetAltitude = polarisAltAz?.first?.toFloat(),
                    polarisTargetAzimuth = polarisAltAz?.second?.toFloat()
                )
                delay(100)
            }
        }
    }

    private var cachedAllStarsById: Map<Int, com.example.orionstargazer.data.entities.StarEntity> = emptyMap()

    override fun onCleared() {
        orientationProvider.stop()
        locationProvider.stop()
        super.onCleared()
    }

    private fun defaultLocation(): Location =
        Location("fallback").apply {
            latitude = 51.4769
            longitude = 0.0005
            accuracy = 10000f
        }

    private fun resolveAutoMode(
        selected: StarRenderMode,
        fps: Float?,
        capabilities: com.example.orionstargazer.ar.StarRenderCapabilities
    ): StarRenderMode {
        if (selected != StarRenderMode.AUTO) return selected

        // Conservative fallback if we don’t yet have FPS.
        val f = fps ?: return if (capabilities.supportsCustomShaderGlow) StarRenderMode.GLOW_TEXTURE else StarRenderMode.SOLID

        // Heuristic:
        // - if fps is low → SOLID
        // - if fps is okay → GLOW_TEXTURE
        // - if fps is high and shader supported → CUSTOM_SHADER_GLOW
        return when {
            f < 35f -> StarRenderMode.SOLID
            f >= 52f && capabilities.supportsCustomShaderGlow -> StarRenderMode.CUSTOM_SHADER_GLOW
            else -> StarRenderMode.GLOW_TEXTURE
        }
    }

    private fun buildHighlights(visible: List<StarPositionCalculator.VisibleStar>): List<String> {
        if (visible.isEmpty()) return emptyList()
        // Fetch device orientation and reticle FOV -- must match ReticleStarSelector logic.
        val az = state.azimuth
        val alt = state.altitude
        val reticleFov = 8.0 // degrees, approximate zone around reticle (tune to taste)
        // Only highlight stars/objects within this range of the reticle center.
        return visible.filter {
            StarPositionCalculator.viewDistanceDegrees(
                it.azimuth, it.altitude, az, alt
            ) <= reticleFov / 2
        }
            .sortedBy { it.star.magnitude }
            .take(4)
            .mapIndexed { index, star ->
                val mag = "%.2f".format(star.star.magnitude)
                val sal = "%.0f".format(star.altitude)
                val saz = "%.0f".format(star.azimuth)
                "${index + 1}. ${star.star.name} • mag $mag • Alt $sal° Az $saz°"
            }
    }
}

