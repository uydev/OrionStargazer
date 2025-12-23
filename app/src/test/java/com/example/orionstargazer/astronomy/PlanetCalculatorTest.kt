package com.example.orionstargazer.astronomy

import com.example.orionstargazer.domain.astronomy.PlanetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PlanetCalculatorTest {

    @Test
    fun `computePlanets returns seven entries with valid magnitudes`() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.APRIL, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val planets = PlanetCalculator.computePlanets(calendar)

        assertEquals(7, planets.size)
        planets.forEach { planet ->
            assertTrue(planet.magnitude.isFinite())
            assertTrue(planet.distanceAu > 0)
        }
    }

    @Test
    fun `planet ids stay unique`() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2030, Calendar.JULY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val planets = PlanetCalculator.computePlanets(calendar)
        val ids = planets.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }
}
