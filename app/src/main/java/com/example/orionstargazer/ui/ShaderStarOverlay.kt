package com.example.orionstargazer.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.orionstargazer.ar.ScreenProjectionUtil
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.isActive

/**
 * Custom shader glow (AGSL RuntimeShader). API 33+ only.
 *
 * Draws stars as additive “light” glows in screen space by projecting 3D sky-dome positions
 * into pixels each frame.
 */
@Composable
fun ShaderStarOverlay(
    sceneView: ArSceneView?,
    stars: List<StarPositionCalculator.VisibleStar>,
    modifier: Modifier = Modifier,
    maxStars: Int = 1200
) {
    if (Build.VERSION.SDK_INT < 33) return
    ShaderStarOverlay33(sceneView, stars, modifier, maxStars)
}

@RequiresApi(33)
@Composable
private fun ShaderStarOverlay33(
    sceneView: ArSceneView?,
    stars: List<StarPositionCalculator.VisibleStar>,
    modifier: Modifier,
    maxStars: Int
) {
    var frameTick by remember { mutableIntStateOf(0) }

    // Trigger redraw at display frame rate while visible.
    LaunchedEffect(sceneView) {
        while (isActive) {
            withFrameNanos { /* just tick */ }
            frameTick++
        }
    }

    // One reusable shader + paint.
    val shader = remember {
        RuntimeShader(
            // language=AGSL
            """
            uniform float2 center;
            uniform float radius;
            uniform half4 color;

            half4 main(float2 fragCoord) {
              float d = distance(fragCoord, center);
              float t = smoothstep(radius, 0.0, d);
              // Slightly steeper falloff for a “glow core”
              float core = smoothstep(radius * 0.35, 0.0, d);
              float a = t * 0.70 + core * 0.45;
              return half4(color.rgb * a, a);
            }
            """.trimIndent()
        )
    }

    val paint = remember {
        Paint().apply {
            blendMode = BlendMode.Plus
            isAntiAlias = true
        }
    }
    // Hook shader into the framework paint.
    val fwPaint = remember { paint.asFrameworkPaint() }
    fwPaint.shader = shader

    // Force Canvas to re-execute draw block using the tick.
    @Suppress("UNUSED_VARIABLE")
    val _t = frameTick

    Canvas(modifier = modifier) {
        val sv = sceneView ?: return@Canvas
        if (sv.width <= 0 || sv.height <= 0) return@Canvas

        val toDraw = if (stars.size > maxStars) stars.take(maxStars) else stars

        drawIntoCanvas {
            toDraw.forEach { s ->
                val worldPos = worldPosition(s.altitude, s.azimuth)
                val pt = ScreenProjectionUtil.projectWorldToScreen(sv, worldPos) ?: return@forEach

                val x = pt.x.toFloat()
                val y = pt.y.toFloat()
                if (x < -50f || y < -50f || x > size.width + 50f || y > size.height + 50f) return@forEach

                val radiusPx = radiusForMagnitudePx(s.star.magnitude)
                val col = colorForSpectral(s.star.spectralType)

                shader.setFloatUniform("center", x, y)
                shader.setFloatUniform("radius", radiusPx)
                shader.setColorUniform("color", col.toArgb())

                // Draw only a small rect around the glow to reduce work.
                val topLeft = Offset(x - radiusPx, y - radiusPx)
                val rectSize = Size(radiusPx * 2f, radiusPx * 2f)
                it.drawRect(androidx.compose.ui.geometry.Rect(topLeft, rectSize), paint)
            }
        }
    }
}

private fun worldPosition(alt: Double, az: Double): Vector3 {
    val radius = 10f
    val altRad = Math.toRadians(alt)
    val azRad = Math.toRadians(az)
    val x = radius * Math.cos(altRad) * Math.sin(azRad)
    val y = radius * Math.sin(altRad)
    val z = -radius * Math.cos(altRad) * Math.cos(azRad)
    return Vector3(x.toFloat(), y.toFloat(), z.toFloat())
}

private fun radiusForMagnitudePx(mag: Double): Float {
    val m = mag.coerceIn(-1.5, 6.0)
    val t = ((6.0 - m) / 7.5).toFloat()
    // pixels; tuned for typical phone screens
    return 10f + 26f * t
}

private fun colorForSpectral(spectralType: String?): Color {
    val s = spectralType?.trim()?.uppercase() ?: return Color(0xFFEAF2FF)
    return when {
        s.startsWith("P") -> Color(0xFF7CFFB2) // planets
        s.startsWith("O") -> Color(0xFF9BB9FF)
        s.startsWith("B") -> Color(0xFFA9C6FF)
        s.startsWith("A") -> Color(0xFFD9E7FF)
        s.startsWith("F") -> Color(0xFFFFF6E3)
        s.startsWith("G") -> Color(0xFFFFE9B6)
        s.startsWith("K") -> Color(0xFFFFC07A)
        s.startsWith("M") -> Color(0xFFFF8A7A)
        else -> Color(0xFFEAF2FF)
    }
}

