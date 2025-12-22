package com.example.orionstargazer.domain.astronomy

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Approximate planet positions (RA/Dec) using simplified orbital elements (J2000 + rates).
 *
 * Accuracy: rough (good enough for MVP/portfolio demo), not for scientific use.
 * Source: Paul Schlyter "Computing planetary positions" style elements/approach.
 */
object PlanetCalculator {

    data class PlanetPosition(
        val id: Int,
        val name: String,
        val raDeg: Double,
        val decDeg: Double,
        val distanceAu: Double,
        val magnitude: Double
    )

    fun computePlanets(calendar: Calendar): List<PlanetPosition> {
        val jd = CoordinateConverter.julianDate(calendar)
        val d = jd - 2451545.0 // days since J2000.0

        val earth = heliocentricEcliptic(elementsEarth(d), d)

        val obliquity = degToRad(23.4393 - 3.563e-7 * d) // Earth's obliquity

        fun toRaDec(xecl: Double, yecl: Double, zecl: Double): Pair<Double, Double> {
            // Ecliptic -> equatorial (rotate around x-axis by obliquity)
            val xeq = xecl
            val yeq = yecl * cos(obliquity) - zecl * sin(obliquity)
            val zeq = yecl * sin(obliquity) + zecl * cos(obliquity)

            val ra = normalizeDeg(radToDeg(atan2(yeq, xeq)))
            val dec = radToDeg(atan2(zeq, sqrt(xeq * xeq + yeq * yeq)))
            return ra to dec
        }

        // Sun (approx): opposite of Earth's heliocentric vector.
        run {
            val x = -earth.x
            val y = -earth.y
            val z = -earth.z
            val (ra, dec) = toRaDec(x, y, z)
            // Not returned as "planet" by default; enable if you want it in AR.
        }

        val planets = listOf(
            planet("Mercury", 1, elementsMercury(d), d, earth, ::toRaDec, mag = -0.2),
            planet("Venus", 2, elementsVenus(d), d, earth, ::toRaDec, mag = -4.0),
            planet("Mars", 3, elementsMars(d), d, earth, ::toRaDec, mag = 0.5),
            planet("Jupiter", 4, elementsJupiter(d), d, earth, ::toRaDec, mag = -2.0),
            planet("Saturn", 5, elementsSaturn(d), d, earth, ::toRaDec, mag = 0.6),
            planet("Uranus", 6, elementsUranus(d), d, earth, ::toRaDec, mag = 5.7),
            planet("Neptune", 7, elementsNeptune(d), d, earth, ::toRaDec, mag = 7.8),
        )

        return planets
    }

    // --- Orbital elements + math helpers ---

    private data class Elements(
        val N: Double, // longitude of ascending node (deg)
        val i: Double, // inclination (deg)
        val w: Double, // argument of perihelion (deg)
        val a: Double, // semi-major axis (AU)
        val e: Double, // eccentricity
        val M: Double  // mean anomaly (deg)
    )

    private data class HelioEcl(val x: Double, val y: Double, val z: Double)

    private fun planet(
        name: String,
        id: Int,
        elements: Elements,
        d: Double,
        earth: HelioEcl,
        toRaDec: (Double, Double, Double) -> Pair<Double, Double>,
        mag: Double
    ): PlanetPosition {
        val helio = heliocentricEcliptic(elements, d)
        val x = helio.x - earth.x
        val y = helio.y - earth.y
        val z = helio.z - earth.z
        val (ra, dec) = toRaDec(x, y, z)
        val dist = sqrt(x * x + y * y + z * z)
        return PlanetPosition(
            id = id,
            name = name,
            raDeg = ra,
            decDeg = dec,
            distanceAu = dist,
            magnitude = mag
        )
    }

    private fun heliocentricEcliptic(e: Elements, d: Double): HelioEcl {
        // Solve Kepler's equation for eccentric anomaly E.
        val M = degToRad(normalizeDeg(e.M))
        var E = M + e.e * sin(M) * (1.0 + e.e * cos(M)) // decent initial guess
        repeat(6) {
            val f = E - e.e * sin(E) - M
            val fPrime = 1.0 - e.e * cos(E)
            E -= f / fPrime
        }

        val xv = e.a * (cos(E) - e.e)
        val yv = e.a * (sqrt(1.0 - e.e * e.e) * sin(E))

        val v = atan2(yv, xv)
        val r = sqrt(xv * xv + yv * yv)

        val N = degToRad(e.N)
        val i = degToRad(e.i)
        val w = degToRad(e.w)

        val vw = v + w
        val xh = r * (cos(N) * cos(vw) - sin(N) * sin(vw) * cos(i))
        val yh = r * (sin(N) * cos(vw) + cos(N) * sin(vw) * cos(i))
        val zh = r * (sin(vw) * sin(i))

        return HelioEcl(xh, yh, zh)
    }

    // Elements for J2000 + linear rates (deg/day where applicable)
    private fun elementsMercury(d: Double) = Elements(
        N = 48.3313 + 3.24587e-5 * d,
        i = 7.0047 + 5.00e-8 * d,
        w = 29.1241 + 1.01444e-5 * d,
        a = 0.387098,
        e = 0.205635 + 5.59e-10 * d,
        M = 168.6562 + 4.0923344368 * d
    )

    private fun elementsVenus(d: Double) = Elements(
        N = 76.6799 + 2.46590e-5 * d,
        i = 3.3946 + 2.75e-8 * d,
        w = 54.8910 + 1.38374e-5 * d,
        a = 0.723330,
        e = 0.006773 - 1.302e-9 * d,
        M = 48.0052 + 1.6021302244 * d
    )

    private fun elementsEarth(d: Double) = Elements(
        N = 0.0,
        i = 0.0,
        w = 282.9404 + 4.70935e-5 * d,
        a = 1.0,
        e = 0.016709 - 1.151e-9 * d,
        M = 356.0470 + 0.9856002585 * d
    )

    private fun elementsMars(d: Double) = Elements(
        N = 49.5574 + 2.11081e-5 * d,
        i = 1.8497 - 1.78e-8 * d,
        w = 286.5016 + 2.92961e-5 * d,
        a = 1.523688,
        e = 0.093405 + 2.516e-9 * d,
        M = 18.6021 + 0.5240207766 * d
    )

    private fun elementsJupiter(d: Double) = Elements(
        N = 100.4542 + 2.76854e-5 * d,
        i = 1.3030 - 1.557e-7 * d,
        w = 273.8777 + 1.64505e-5 * d,
        a = 5.20256,
        e = 0.048498 + 4.469e-9 * d,
        M = 19.8950 + 0.0830853001 * d
    )

    private fun elementsSaturn(d: Double) = Elements(
        N = 113.6634 + 2.38980e-5 * d,
        i = 2.4886 - 1.081e-7 * d,
        w = 339.3939 + 2.97661e-5 * d,
        a = 9.55475,
        e = 0.055546 - 9.499e-9 * d,
        M = 316.9670 + 0.0334442282 * d
    )

    private fun elementsUranus(d: Double) = Elements(
        N = 74.0005 + 1.3978e-5 * d,
        i = 0.7733 + 1.9e-8 * d,
        w = 96.6612 + 3.0565e-5 * d,
        a = 19.18171 - 1.55e-8 * d,
        e = 0.047318 + 7.45e-9 * d,
        M = 142.5905 + 0.011725806 * d
    )

    private fun elementsNeptune(d: Double) = Elements(
        N = 131.7806 + 3.0173e-5 * d,
        i = 1.7700 - 2.55e-7 * d,
        w = 272.8461 - 6.027e-6 * d,
        a = 30.05826 + 3.313e-8 * d,
        e = 0.008606 + 2.15e-9 * d,
        M = 260.2471 + 0.005995147 * d
    )

    private fun degToRad(d: Double) = d * PI / 180.0
    private fun radToDeg(r: Double) = r * 180.0 / PI

    private fun normalizeDeg(deg: Double): Double {
        var x = deg % 360.0
        if (x < 0) x += 360.0
        return x
    }
}

