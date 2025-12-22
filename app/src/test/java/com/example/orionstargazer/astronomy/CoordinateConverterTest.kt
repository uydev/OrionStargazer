package com.example.orionstargazer.domain.astronomy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CoordinateConverterTest {
    @Test
    fun testJulianDate() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2023, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val jd = CoordinateConverter.julianDate(cal)
        assertEquals(2459946.0, jd, 0.001)
    }

    @Test
    fun testGreenwichSiderealTime() {
        // Example: JD = 2459946.0 (2023-01-01 12:00 UTC)
        val gst = CoordinateConverter.greenwichSiderealTime(2459946.0)
        // GST in degrees should be between 0 and 360
        assert(gst >= 0.0 && gst < 360.0)
    }

    @Test
    fun testLocalSiderealTime() {
        val jd = 2459946.0
        val longitude = 10.0 // example longitude
        val lst = CoordinateConverter.localSiderealTime(jd, longitude)
        assert(lst >= 0.0 && lst < 360.0)
    }

    @Test
    fun testEquatorialToHorizontal() {
        // Rough test: observer at 52N, target at Dec +30, HA = 2h (30deg)
        val (alt, az) = CoordinateConverter.equatorialToHorizontal(
            latitude = 52.0,
            declination = 30.0,
            hourAngle = 30.0
        )
        // Typical altitude/azimuth values
        assert(alt > -90 && alt < 90)
        assert(az >= 0 && az <= 360)
    }
}

