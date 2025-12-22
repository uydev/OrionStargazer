package com.example.orionstargazer.astronomy

import android.location.Location
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot

object StarPositionCalculator {
    data class VisibleStar(
        val star: com.example.orionstargazer.data.entities.StarEntity,
        val altitude: Double,
        val azimuth: Double
    )

    fun calculateVisibleStars(
        calendar: Calendar,
        location: Location,
        orientationAzimuth: Float,
        orientationAltitude: Float,
        fieldOfView: Double = 40.0,
        minAltitude: Double = 0.0,
        stars: List<com.example.orionstargazer.data.entities.StarEntity>
    ): List<VisibleStar> {
        val jd = CoordinateConverter.julianDate(calendar)
        val lst = CoordinateConverter.localSiderealTime(jd, location.longitude)
        val latitude = location.latitude

        return stars.mapNotNull { star ->
            val ha = CoordinateConverter.hourAngle(lst, star.ra)
            val (alt, az) = CoordinateConverter.equatorialToHorizontal(latitude, star.dec, ha)
            if (alt > minAltitude && isWithinView(az, alt, orientationAzimuth, orientationAltitude, fieldOfView)) {
                VisibleStar(star, alt, az)
            } else null
        }
    }

    fun computeAltAz(
        calendar: Calendar,
        location: Location,
        star: com.example.orionstargazer.data.entities.StarEntity
    ): Pair<Double, Double> {
        val jd = CoordinateConverter.julianDate(calendar)
        val lst = CoordinateConverter.localSiderealTime(jd, location.longitude)
        val latitude = location.latitude
        val ha = CoordinateConverter.hourAngle(lst, star.ra)
        return CoordinateConverter.equatorialToHorizontal(latitude, star.dec, ha)
    }

    /**
     * Angular distance in degrees between a star (az/alt) and the device pointing (az/alt),
     * using a simple az/alt Euclidean approximation.
     *
     * This is good enough for ranking/sorting within moderate FOVs.
     */
    fun viewDistanceDegrees(
        starAz: Double,
        starAlt: Double,
        deviceAz: Float,
        deviceAlt: Float
    ): Double {
        val dAz = angleDistance(starAz, deviceAz.toDouble())
        val dAlt = starAlt - deviceAlt.toDouble()
        return hypot(dAz, dAlt)
    }

    private fun isWithinView(
        starAz: Double,
        starAlt: Double,
        deviceAz: Float,
        deviceAlt: Float,
        fieldOfView: Double
    ): Boolean {
        val dAz = angleDistance(starAz, deviceAz.toDouble())
        val dAlt = starAlt - deviceAlt
        return abs(dAz) <= fieldOfView / 2 && abs(dAlt) <= fieldOfView / 2
    }

    private fun angleDistance(a: Double, b: Double): Double {
        val diff = (a - b + 540) % 360 - 180
        return diff
    }
}
