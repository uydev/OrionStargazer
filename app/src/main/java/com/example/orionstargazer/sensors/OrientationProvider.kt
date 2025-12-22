package com.example.orionstargazer.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.*

class OrientationProvider(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val display = context.display
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var hasGravity = false
    private var hasMagnetometer = false
    private var hasGyro = false
    private var lastGyroTimestampNs: Long? = null
    private var integratedYawDeg: Float = 0f

    private var lastAzimuth: Float? = null
    private var lastAltitude: Float? = null

    var azimuth: Float = 0f
        private set
    var altitude: Float = 0f
        private set

    private val alpha = 0.15f // smoothing factor for low-pass filter

    fun start() {
        // Register the best available orientation sensors.
        // (Some emulators/devices expose GAME_ROTATION_VECTOR but not ROTATION_VECTOR.)
        val delay = SensorManager.SENSOR_DELAY_GAME
        if (rotationVector != null) sensorManager.registerListener(this, rotationVector, delay)
        if (gameRotationVector != null) sensorManager.registerListener(this, gameRotationVector, delay)

        // Fallback path: accel + mag.
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, delay)
        if (magnetometer != null) sensorManager.registerListener(this, magnetometer, delay)
        if (gyroscope != null) sensorManager.registerListener(this, gyroscope, delay)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                updateFromRotationVector(event.values)
                return
            }
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                updateFromRotationVector(event.values)
                return
            }
            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) gravity[i] = alpha * event.values[i] + (1 - alpha) * gravity[i]
                hasGravity = true

                // Emulator often has no magnetometer/rotation-vector. Provide pitch-based altitude anyway.
                updateAltitudeFromGravity()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                for (i in 0..2) geomagnetic[i] = alpha * event.values[i] + (1 - alpha) * geomagnetic[i]
                hasMagnetometer = true
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Optional fallback yaw integration (better than nothing when no magnetometer).
                val prevTs = lastGyroTimestampNs
                lastGyroTimestampNs = event.timestamp
                hasGyro = true
                if (prevTs != null) {
                    val dt = (event.timestamp - prevTs) / 1_000_000_000.0f
                    if (dt > 0f && dt < 1f) {
                        // event.values[2] is rotation around Z axis (rad/s) in device coords.
                        integratedYawDeg = normalizeDegrees(integratedYawDeg + Math.toDegrees((event.values[2] * dt).toDouble()).toFloat())
                        if (!hasMagnetometer && lastAzimuth == null) {
                            // Initialize azimuth if we never had a proper heading.
                            azimuth = integratedYawDeg
                            lastAzimuth = azimuth
                        }
                    }
                }
            }
        }
        if (hasGravity && hasMagnetometer) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(remapForDisplayRotation(R), orientation)
                val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val rawAltitude = Math.toDegrees(orientation[1].toDouble()).toFloat()

                val normalizedAz = normalizeDegrees(rawAzimuth)
                if (!normalizedAz.isNaN()) azimuth = smoothAngle(lastAzimuth, normalizedAz)
                if (!rawAltitude.isNaN()) altitude = lastAltitude?.let { it + (rawAltitude - it) * alpha } ?: rawAltitude
                lastAzimuth = azimuth
                lastAltitude = altitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    private fun normalizeDegrees(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    private fun updateFromRotationVector(values: FloatArray) {
        val R = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(R, values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapForDisplayRotation(R), orientation)

        // orientation[] is in radians: [azimuth(z), pitch(x), roll(y)]
        val rawAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val rawAltitude = Math.toDegrees(orientation[1].toDouble()).toFloat()

        val normalizedAz = normalizeDegrees(rawAzimuth)
        if (!normalizedAz.isNaN()) azimuth = smoothAngle(lastAzimuth, normalizedAz)
        if (!rawAltitude.isNaN()) altitude = lastAltitude?.let { it + (rawAltitude - it) * alpha } ?: rawAltitude
        lastAzimuth = azimuth
        lastAltitude = altitude
    }

    private fun updateAltitudeFromGravity() {
        if (!hasGravity) return
        val gx = gravity[0]
        val gy = gravity[1]
        val gz = gravity[2]
        // Pitch in radians; positive when device tilts forward.
        val pitchRad = atan2(-gx, sqrt(gy * gy + gz * gz))
        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        if (pitchDeg.isNaN()) return
        altitude = lastAltitude?.let { it + (pitchDeg - it) * alpha } ?: pitchDeg
        lastAltitude = altitude

        // If we have no absolute heading, optionally surface gyro yaw.
        if (!hasMagnetometer && hasGyro) {
            azimuth = smoothAngle(lastAzimuth, integratedYawDeg)
            lastAzimuth = azimuth
        }
    }

    private fun smoothAngle(prev: Float?, next: Float): Float {
        if (prev == null) return next
        // shortest signed angular difference in (-180, 180]
        val delta = ((next - prev + 540f) % 360f) - 180f
        return normalizeDegrees(prev + delta * alpha)
    }

    private fun remapForDisplayRotation(inR: FloatArray): FloatArray {
        val outR = FloatArray(9)
        val rotation = display?.rotation ?: Surface.ROTATION_0
        when (rotation) {
            Surface.ROTATION_0 ->
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR)
            Surface.ROTATION_90 ->
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR)
            Surface.ROTATION_180 ->
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, outR)
            Surface.ROTATION_270 ->
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, outR)
            else ->
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR)
        }
        return outR
    }
}

