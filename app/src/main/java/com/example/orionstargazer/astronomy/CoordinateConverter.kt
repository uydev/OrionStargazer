package com.example.orionstargazer.astronomy

import kotlin.math.*
import java.util.Calendar
import java.util.TimeZone

object CoordinateConverter {
    // Compute Julian Date from calendar/time (UTC)
    fun julianDate(calendar: Calendar): Double {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = y / 100
        val B = 2 - A + (A / 4)
        val dayFraction = (hour + minute / 60.0 + (second + millis / 1000.0) / 3600.0) / 24.0
        val JD = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + dayFraction + B - 1524.5
        return JD
    }

    // Greenwich Sidereal Time in degrees, from Julian Date
    fun greenwichSiderealTime(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        var gst = 280.46061837 +
                360.98564736629 * (jd - 2451545.0) +
                T * T * (0.000387933 - T / 38710000.0)
        gst %= 360.0
        if (gst < 0) gst += 360.0
        return gst // in degrees
    }

    // Local Sidereal Time (degrees)
    fun localSiderealTime(jd: Double, longitude: Double): Double {
        return (greenwichSiderealTime(jd) + longitude) % 360.0
    }

    // Hour Angle (degrees) from LST and Right Ascension
    fun hourAngle(lst: Double, rightAscension: Double): Double {
        var ha = lst - rightAscension
        if (ha < 0) ha += 360.0
        return ha
    }

    // Convert Equatorial coordinates (RA, Dec) to Horizontal (Alt, Az)
    // latitude/declination in degrees, hour angle in degrees
    fun equatorialToHorizontal(
        latitude: Double,
        declination: Double,
        hourAngle: Double
    ): Pair<Double, Double> {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val haRad = Math.toRadians(hourAngle)

        val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
        val altitude = Math.asin(sinAlt)

        val cosAz = (sin(decRad) - sin(altitude) * sin(latRad)) /
                    (cos(altitude) * cos(latRad))
        var azimuth = Math.acos(cosAz)

        if (sin(haRad) > 0) {
            azimuth = 2 * Math.PI - azimuth
        }

        val altitudeDeg = Math.toDegrees(altitude)
        val azimuthDeg = Math.toDegrees(azimuth)

        return Pair(altitudeDeg, azimuthDeg)
    }
}
