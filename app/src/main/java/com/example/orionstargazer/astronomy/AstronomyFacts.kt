package com.example.orionstargazer.astronomy

import com.example.orionstargazer.data.entities.StarEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Small “education layer” that turns raw catalog fields into human-friendly facts.
 *
 * This is intentionally lightweight (no network, no huge DB). It’s meant for a portfolio MVP:
 * - clear explanations
 * - deterministic output
 * - works offline
 */
object AstronomyFacts {
    data class Fact(val title: String, val body: String)

    fun factsForVisibleStar(
        visible: StarPositionCalculator.VisibleStar
    ): List<Fact> = factsFor(
        star = visible.star,
        altitudeDeg = visible.altitude,
        azimuthDeg = visible.azimuth
    )

    fun factsFor(
        star: StarEntity,
        altitudeDeg: Double? = null,
        azimuthDeg: Double? = null
    ): List<Fact> {
        val facts = mutableListOf<Fact>()

        // Where in the sky?
        if (altitudeDeg != null && azimuthDeg != null) {
            val alt = altitudeDeg
            val az = azimuthDeg
            facts += Fact(
                title = "Where you’re looking",
                body = buildString {
                    append("Altitude is how high above the horizon something is. ")
                    append("Azimuth is the compass direction.\n\n")
                    append("This object is at Alt ${alt.format1()}° and Az ${az.format1()}° ")
                    append(directionName(az)).append(".")
                }
            )
        }

        // Magnitude (brightness)
        facts += Fact(
            title = "Brightness (magnitude)",
            body = buildString {
                append("Astronomers use “magnitude” for brightness. Lower numbers mean brighter.\n\n")
                append("This object is magnitude ${star.magnitude.format2()}. ")
                append(
                    when {
                        star.magnitude <= 0.0 -> "That’s extremely bright."
                        star.magnitude <= 2.0 -> "That’s bright and easy to spot."
                        star.magnitude <= 4.0 -> "That’s moderately bright."
                        else -> "That’s faint—dark skies help."
                    }
                )
            }
        )

        // Distance
        star.distance?.let { ly ->
            val years = ly.roundToInt().coerceAtLeast(1)
            facts += Fact(
                title = "Distance (light-years)",
                body = buildString {
                    append("A light‑year is the distance light travels in one year.\n\n")
                    append("Distance is about ${ly.format1()} ly, so the light you see left ~${years} years ago.")
                }
            )
        }

        // Spectral class / temperature
        spectralFacts(star)?.let { facts += it }

        // Coordinates
        facts += Fact(
            title = "Sky coordinates (RA/Dec)",
            body = buildString {
                append("Right Ascension (RA) and Declination (Dec) are like longitude/latitude on the sky.\n\n")
                append("RA ${star.ra.format2()}°, Dec ${star.dec.format2()}°.")
            }
        )

        // Constellations
        if (!star.constellation.isNullOrBlank() && star.constellation != "Planet") {
            facts += Fact(
                title = "Constellation",
                body = buildString {
                    append("Constellations are cultural patterns—stars that only look close together from Earth.\n\n")
                    append("This object is labeled in: ${star.constellation}.")
                }
            )
        }

        // Planets: quick context
        if (isPlanet(star)) {
            facts += Fact(
                title = "Planet (not a star)",
                body = buildString {
                    append("${star.name} is a planet in our Solar System. ")
                    append("Unlike stars, planets shine by reflecting sunlight.\n\n")
                    append("In this app, planet positions are approximate (good for an MVP).")
                }
            )
        }

        return facts
    }

    private fun isPlanet(star: StarEntity): Boolean {
        return star.spectralType?.trim()?.startsWith("P", ignoreCase = true) == true ||
            star.constellation == "Planet" ||
            star.id < 0
    }

    private fun spectralFacts(star: StarEntity): Fact? {
        val s = star.spectralType?.trim().orEmpty()
        val cls = s.firstOrNull()?.uppercaseChar() ?: return null
        if (cls !in listOf('O', 'B', 'A', 'F', 'G', 'K', 'M', 'P')) return null
        if (cls == 'P') return null // handled separately

        val (color, tempRange) = when (cls) {
            'O' -> "blue" to "30,000K+"
            'B' -> "blue‑white" to "10,000–30,000K"
            'A' -> "white" to "7,500–10,000K"
            'F' -> "yellow‑white" to "6,000–7,500K"
            'G' -> "yellow" to "5,200–6,000K"
            'K' -> "orange" to "3,700–5,200K"
            'M' -> "red" to "2,400–3,700K"
            else -> return null
        }

        val subtype = s.drop(1).takeWhile { it.isDigit() }.toIntOrNull()
        val extra = if (subtype != null) " (subclass $subtype, finer temperature/color)" else ""

        return Fact(
            title = "Spectral type",
            body = buildString {
                append("Spectral class estimates a star’s surface temperature and color.\n\n")
                append("Type $cls stars are typically $color and about $tempRange$extra.")
            }
        )
    }

    private fun directionName(azimuthDeg: Double): String {
        // 8-point compass, centered on each direction (N=0).
        val a = ((azimuthDeg % 360) + 360) % 360
        val dirs = listOf("north", "north‑east", "east", "south‑east", "south", "south‑west", "west", "north‑west")
        val idx = ((a / 45.0).roundToInt()) % 8
        return "toward ${dirs[idx]}"
    }

    private fun Double.format1(): String = "%.1f".format(this)
    private fun Double.format2(): String = "%.2f".format(this)
}

