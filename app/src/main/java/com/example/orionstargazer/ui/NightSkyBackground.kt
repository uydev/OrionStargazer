package com.example.orionstargazer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun NightSkyBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 220,
    seed: Int = 42
) {
    val stars = remember(starCount, seed) { generateStars(starCount, seed) }
    val transition = rememberInfiniteTransition(label = "night-sky")
    val t = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle-phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep space gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF050617),
                    Color(0xFF02030A),
                    Color(0xFF000000)
                )
            ),
            style = Fill
        )

        // Soft nebula blobs (very low alpha)
        fun nebula(center: Offset, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        nebula(Offset(size.width * 0.25f, size.height * 0.30f), size.minDimension * 0.55f, Color(0x332E7BFF))
        nebula(Offset(size.width * 0.80f, size.height * 0.35f), size.minDimension * 0.45f, Color(0x33FF3DAA))
        nebula(Offset(size.width * 0.55f, size.height * 0.75f), size.minDimension * 0.60f, Color(0x3325E6C8))

        // Stars
        for (s in stars) {
            val x = s.x * size.width
            val y = s.y * size.height
            val twinkle = 0.62f + 0.38f * sin(t.value * s.twinkleSpeed + s.twinklePhase)
            val a = (s.baseAlpha * twinkle).coerceIn(0.05f, 1f)
            drawCircle(
                color = s.color.copy(alpha = a),
                radius = s.radiusPx,
                center = Offset(x, y)
            )
            // Tiny glow for bright stars
            if (s.radiusPx >= 1.6f) {
                drawCircle(
                    color = s.color.copy(alpha = a * 0.18f),
                    radius = s.radiusPx * 3.2f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private data class SkyStar(
    val x: Float,
    val y: Float,
    val radiusPx: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
    val color: Color
)

private fun generateStars(count: Int, seed: Int): List<SkyStar> {
    val r = Random(seed)
    val palette = listOf(
        Color(0xFFEAF2FF), // blue-white
        Color(0xFFFFF6E3), // warm white
        Color(0xFFD9E7FF), // cool white
        Color(0xFFFFE9C7)  // amber
    )

    return List(count) {
        val y = r.nextFloat()
        // Slightly denser near the top (more "sky")
        val x = r.nextFloat()
        val radius = when {
            r.nextFloat() < 0.06f -> 2.1f + r.nextFloat() * 1.2f
            r.nextFloat() < 0.20f -> 1.4f + r.nextFloat() * 0.8f
            else -> 0.7f + r.nextFloat() * 0.7f
        }
        val alpha = 0.18f + r.nextFloat() * 0.65f
        SkyStar(
            x = x,
            y = y,
            radiusPx = radius,
            baseAlpha = alpha,
            twinkleSpeed = 0.7f + r.nextFloat() * 2.2f,
            twinklePhase = r.nextFloat() * (2f * PI).toFloat(),
            color = palette[r.nextInt(palette.size)]
        )
    }
}


