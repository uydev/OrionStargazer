package com.example.orionstargazer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive

/**
 * Lightweight FPS tracker based on Compose frame clock.
 * Reports a smoothed FPS value about once per second.
 */
@Composable
fun FrameRateTracker(
    onFps: (Float) -> Unit
) {
    LaunchedEffect(Unit) {
        var frames = 0
        var startNs = 0L
        var lastReportFps = 60f

        while (isActive) {
            val t = withFrameNanos { it }
            if (startNs == 0L) startNs = t
            frames++
            val dt = t - startNs
            if (dt >= 1_000_000_000L) {
                val fps = frames * 1_000_000_000f / dt.toFloat()
                // simple smoothing to avoid jitter
                lastReportFps = lastReportFps * 0.7f + fps * 0.3f
                onFps(lastReportFps)
                startNs = t
                frames = 0
            }
        }
    }
}

