package com.example.orionstargazer.engine

import android.location.Location
import com.example.orionstargazer.ar.ConstellationRenderer
import com.example.orionstargazer.astronomy.ConstellationCatalog
import com.example.orionstargazer.astronomy.PlanetCalculator
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.example.orionstargazer.data.StarRepository
import com.example.orionstargazer.data.entities.StarEntity
import com.google.ar.sceneform.math.Vector3
import java.util.Calendar

/**
 * “Engine layer” orchestrator:
 * - loads/filters catalog candidates (matching engine)
 * - computes star/planet positions (astronomy engine)
 * - builds constellation line segments (render-prep)
 *
 * UI should call this and render the outputs; it should not own this logic.
 */
class SkyPipeline(
    private val repo: StarRepository,
    private val constellations: List<ConstellationCatalog.Constellation>
) {
    data class ConstellationDetection(
        val name: String,
        val score: Float
    )

    data class DetectedConstellationResult(
        val name: String,
        val score: Float,
        val segments: List<ConstellationRenderer.Segment>
    )
    data class CatalogStatus(
        val dbCount: Int,
        val allStarsById: Map<Int, StarEntity>
    )

    suspend fun ensureCatalogSeeded(
        context: android.content.Context,
        minCount: Int = 2000
    ): CatalogStatus {
        val existing = repo.countStars()
        if (existing < minCount) {
            repo.deleteAll()
            com.example.orionstargazer.data.StarAssetLoader.loadAssetsAndSeedDb(context, repo)
            com.example.orionstargazer.data.HygCsvImporter.importTopBrightest(context, repo, limit = 3000)
        }
        val count = repo.countStars()
        val byId = repo.getAllStars().associateBy { it.id }
        return CatalogStatus(dbCount = count, allStarsById = byId)
    }

    suspend fun loadCandidates(
        maxMagnitude: Double,
        location: Location
    ): List<StarEntity> {
        val minDec = (location.latitude - 90).coerceAtLeast(-90.0)
        val maxDec = (location.latitude + 90).coerceAtMost(90.0)
        return repo.getCandidates(maxMagnitude, minDec, maxDec)
    }

    fun buildSkyEntities(
        calendar: Calendar,
        candidateStars: List<StarEntity>
    ): List<StarEntity> {
        val planets = PlanetCalculator.computePlanets(calendar).map { p ->
            StarEntity(
                id = -1000 - p.id,
                name = p.name,
                ra = p.raDeg,
                dec = p.decDeg,
                magnitude = p.magnitude,
                distance = p.distanceAu * 63241.1,
                spectralType = "P",
                constellation = "Planet"
            )
        }
        return (candidateStars + planets).distinctBy { it.id }
    }

    fun computeVisibleStars(
        calendar: Calendar,
        location: Location,
        azimuth: Float,
        altitude: Float,
        stars: List<StarEntity>,
        fieldOfView: Double = 60.0
    ): List<StarPositionCalculator.VisibleStar> {
        return StarPositionCalculator.calculateVisibleStars(
            calendar = calendar,
            location = location,
            orientationAzimuth = azimuth,
            orientationAltitude = altitude,
            fieldOfView = fieldOfView,
            minAltitude = 0.0,
            stars = stars
        )
    }

    fun buildConstellationSegments(
        calendar: Calendar,
        location: Location,
        azimuth: Float,
        altitude: Float,
        allStarsById: Map<Int, StarEntity>
    ): List<ConstellationRenderer.Segment> {
        val segments = mutableListOf<ConstellationRenderer.Segment>()
        constellations.forEach { constellation ->
            constellation.lines.forEach { line ->
                val a = allStarsById[line.aStarId] ?: return@forEach
                val b = allStarsById[line.bStarId] ?: return@forEach
                val (altA, azA) = StarPositionCalculator.computeAltAz(calendar, location, a)
                val (altB, azB) = StarPositionCalculator.computeAltAz(calendar, location, b)
                if (altA <= 0.0 || altB <= 0.0) return@forEach
                val nearA = StarPositionCalculator.viewDistanceDegrees(azA, altA, azimuth, altitude) < 80.0
                val nearB = StarPositionCalculator.viewDistanceDegrees(azB, altB, azimuth, altitude) < 80.0
                if (!nearA && !nearB) return@forEach
                segments.add(
                    ConstellationRenderer.Segment(
                        key = "${constellation.name}:${line.aStarId}-${line.bStarId}",
                        start = worldPosition(altA, azA),
                        end = worldPosition(altB, azB)
                    )
                )
            }
        }
        return segments
    }

    /**
     * “Detected constellation” logic:
     * - scores each constellation by how many of its lines are fully visible in the current FOV
     * - adds a small bonus if one of its endpoint stars is near the reticle
     *
     * Returns the best candidate (or null).
     */
    fun detectConstellation(
        visibleStars: List<StarPositionCalculator.VisibleStar>,
        deviceAzimuth: Float,
        deviceAltitude: Float
    ): ConstellationDetection? {
        if (visibleStars.isEmpty()) return null

        val byId = visibleStars.associateBy { it.star.id }
        val visibleIds = byId.keys

        var best: ConstellationDetection? = null

        constellations.forEach { constellation ->
            val totalLines = constellation.lines.size
            if (totalLines == 0) return@forEach

            val fullyVisible = constellation.lines.count { line ->
                visibleIds.contains(line.aStarId) && visibleIds.contains(line.bStarId)
            }
            if (fullyVisible <= 0) return@forEach

            // Require a minimum fraction of lines visible to even be considered.
            val required = kotlin.math.max(1, kotlin.math.ceil(totalLines * 0.67).toInt())
            if (fullyVisible < required) return@forEach

            // Reticle proximity bonus: nearest endpoint (that is visible).
            var minDist: Double? = null
            constellation.lines.forEach { line ->
                val a = byId[line.aStarId]
                val b = byId[line.bStarId]
                if (a != null) {
                    val d = StarPositionCalculator.viewDistanceDegrees(a.azimuth, a.altitude, deviceAzimuth, deviceAltitude)
                    minDist = minDist?.let { kotlin.math.min(it, d) } ?: d
                }
                if (b != null) {
                    val d = StarPositionCalculator.viewDistanceDegrees(b.azimuth, b.altitude, deviceAzimuth, deviceAltitude)
                    minDist = minDist?.let { kotlin.math.min(it, d) } ?: d
                }
            }

            val ratio = fullyVisible.toFloat() / totalLines.toFloat()
            val proximity = minDist?.let { (1f - (it / 25.0).toFloat()).coerceIn(0f, 1f) } ?: 0f
            val score = ratio * 0.75f + proximity * 0.25f

            if (best == null || score > best!!.score) {
                best = ConstellationDetection(constellation.name, score)
            }
        }

        return best
    }

    /**
     * Build segments for a single (already detected) constellation.
     * We still filter for "near-ish" so we don't render lines all over the sky.
     */
    fun buildDetectedConstellationSegments(
        constellationName: String,
        calendar: Calendar,
        location: Location,
        azimuth: Float,
        altitude: Float,
        allStarsById: Map<Int, StarEntity>
    ): List<ConstellationRenderer.Segment> {
        val constellation = constellations.firstOrNull { it.name == constellationName } ?: return emptyList()

        val segments = mutableListOf<ConstellationRenderer.Segment>()
        constellation.lines.forEach { line ->
            val a = allStarsById[line.aStarId] ?: return@forEach
            val b = allStarsById[line.bStarId] ?: return@forEach
            val (altA, azA) = StarPositionCalculator.computeAltAz(calendar, location, a)
            val (altB, azB) = StarPositionCalculator.computeAltAz(calendar, location, b)
            if (altA <= 0.0 || altB <= 0.0) return@forEach

            val nearA = StarPositionCalculator.viewDistanceDegrees(azA, altA, azimuth, altitude) < 90.0
            val nearB = StarPositionCalculator.viewDistanceDegrees(azB, altB, azimuth, altitude) < 90.0
            if (!nearA && !nearB) return@forEach

            segments.add(
                ConstellationRenderer.Segment(
                    key = "$constellationName:${line.aStarId}-${line.bStarId}",
                    start = worldPosition(altA, azA),
                    end = worldPosition(altB, azB)
                )
            )
        }
        return segments
    }

    private fun worldPosition(alt: Double, az: Double): Vector3 {
        val radius = 10f
        val altRad = Math.toRadians(alt)
        val azRad = Math.toRadians(az)
        val x = radius * Math.cos(altRad) * Math.sin(azRad)
        val y = radius * Math.sin(altRad)
        val z = -radius * Math.cos(altRad) * Math.cos(azRad)
        return Vector3(x.toFloat(), y.toFloat(), z.toFloat())
    }
}

