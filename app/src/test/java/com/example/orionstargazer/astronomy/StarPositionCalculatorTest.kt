package com.example.orionstargazer.astronomy

import android.location.Location
import com.example.orionstargazer.data.entities.StarEntity
import com.example.orionstargazer.domain.astronomy.StarPositionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class StarPositionCalculatorTest {

    @Test
    fun `view distance degrees returns zero for identical directions`() {
        val distance = StarPositionCalculator.viewDistanceDegrees(
            starAz = 35.0,
            starAlt = 42.0,
            deviceAz = 35.0f,
            deviceAlt = 42.0f
        )
        assertEquals(0.0, distance, 1e-6)
    }

    @Test
    fun `star below horizon is filtered out`() {
        val star = StarEntity(
            id = 2,
            name = "BelowHorizon",
            ra = 0.0,
            dec = -90.0,
            magnitude = 1.0,
            distance = 50.0,
            spectralType = "K",
            constellation = "S"
        )
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2025, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val location = Location("test").apply {
            latitude = 0.0
            longitude = 0.0
        }

        val visible = StarPositionCalculator.calculateVisibleStars(
            calendar = calendar,
            location = location,
            orientationAzimuth = 0f,
            orientationAltitude = 0f,
            fieldOfView = 120.0,
            minAltitude = 0.0,
            stars = listOf(star)
        )

        assertTrue(visible.isEmpty())
    }

}
