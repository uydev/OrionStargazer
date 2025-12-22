package com.example.orionstargazer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orionstargazer.ar.ARCoreView
import com.example.orionstargazer.ar.ScreenProjectionUtil
import com.example.orionstargazer.astronomy.AstronomyFacts
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.example.orionstargazer.ui.EducationFactsSection
import com.example.orionstargazer.ui.theme.OrionStargazerTheme
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.example.orionstargazer.ui.main.MainScreen
import com.example.orionstargazer.ui.main.MainViewModel
import com.example.orionstargazer.ar.StarRenderMode

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var cameraPermissionGranted by mutableStateOf(false)
        var locationPermissionGranted by mutableStateOf(false)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            cameraPermissionGranted = results[Manifest.permission.CAMERA] == true
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            locationPermissionGranted = fine || coarse
            vm.onPermissionsChanged(cameraPermissionGranted, locationPermissionGranted)
        }

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            cameraPermissionGranted = true
            locationPermissionGranted = true
            vm.onPermissionsChanged(cameraPermissionGranted, locationPermissionGranted)
        }

        vm.start()

        setContent {
            OrionStargazerTheme(darkTheme = true, dynamicColor = false) {
                val state = vm.state
                var sceneView by remember { mutableStateOf<ArSceneView?>(null) }
                val reticleSizeDp = 48

                LaunchedEffect(sceneView, state.starsInView) {
                    while (true) {
                        val candidate = computeStarInReticle(sceneView, state.starsInView, reticleSizeDp)
                        vm.setHighlightedStar(candidate)
                        delay(150)
                    }
                }

                MainScreen(
                    state = state,
                    onToggleConstellations = { vm.setShowConstellations(it) },
                    onMaxMagnitudeChanged = { vm.setMaxMagnitude(it) },
                    onMaxMagnitudeChangeFinished = { /* persisted in VM */ },
                    onStarSelected = { id ->
                        vm.setHighlightedStar(state.starsInView.firstOrNull { it.star.id == id })
                    },
                    onStarTapped = { star ->
                        vm.setHighlightedStar(state.starsInView.firstOrNull { it.star.id == star.id })
                    },
                    onClearSelection = { vm.setHighlightedStar(null) },
                    sceneViewRef = { sceneView = it },
                    onRequestPermissions = { permissionLauncher.launch(permissions) },
                    onOpenAppSettings = { openAppSettings() },
                    onSetShowSettings = { show -> vm.setShowSettings(show) },
                    onStarRenderModeChanged = { mode -> vm.setStarRenderMode(mode) },
                    onShaderMaxStarsChanged = { v -> vm.setShaderMaxStars(v) },
                    onFpsSample = { fps -> vm.onFpsSample(fps) },
                    onConstellationDrawModeChanged = { mode -> vm.setConstellationDrawMode(mode) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val camera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        vm.onPermissionsChanged(cameraGranted = camera, locationGranted = fine || coarse)
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}

@Composable
fun SelectedStarCard(
    star: StarPositionCalculator.VisibleStar,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    val facts = remember(star.star.id, star.altitude, star.azimuth) {
        AstronomyFacts.factsForVisibleStar(star)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1230)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = star.star.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAF2FF),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0x99EAF2FF),
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable { onClear() }
                )
            }
            Text(
                text = listOfNotNull(star.star.constellation, star.star.spectralType).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCFE0FF)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Alt ${"%.1f".format(star.altitude)}°, Az ${"%.1f".format(star.azimuth)}°   •   Mag ${"%.2f".format(star.star.magnitude)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFF6E3)
            )
            star.star.distance?.let { d ->
                Text(
                    text = "Distance: ${"%.1f".format(d)} ly",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCFE0FF)
                )
            }

            Spacer(Modifier.height(10.dp))
            EducationFactsSection(facts = facts)
        }
    }
}

@Composable
fun OrientationDisplay(azimuth: Float, altitude: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Card(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .padding(top = 18.dp)
                .padding(horizontal = 14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xAA070A18)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Device Orientation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEAF2FF)
                )
                Text(
                    text = "Azimuth: %.1f°".format(azimuth),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFF6E3)
                )
                Text(
                    text = "Altitude: %.1f°".format(altitude),
                    fontSize = 18.sp,
                    color = Color(0xFFCFE0FF)
                )
            }
        }
    }
}

private fun computeStarInReticle(
    sceneView: ArSceneView?,
    stars: List<StarPositionCalculator.VisibleStar>,
    reticleSizeDp: Int
): StarPositionCalculator.VisibleStar? {
    if (sceneView == null) return null
    val density = Resources.getSystem().displayMetrics.density
    val sizePx = reticleSizeDp * density
    val centerX = sceneView.width / 2
    val centerY = sceneView.height / 2
    val left = (centerX - sizePx / 2)
    val right = (centerX + sizePx / 2)
    val top = (centerY - sizePx / 2)
    val bottom = (centerY + sizePx / 2)
    var candidate: StarPositionCalculator.VisibleStar? = null
    var minDist = Float.MAX_VALUE
    stars.forEach { star ->
        val worldPos = Vector3(
            (10f * cos(Math.toRadians(star.altitude.toDouble())) * sin(Math.toRadians(star.azimuth.toDouble()))).toFloat(),
            (10f * sin(Math.toRadians(star.altitude.toDouble()))).toFloat(),
            (-10f * cos(Math.toRadians(star.altitude.toDouble())) * cos(Math.toRadians(star.azimuth.toDouble()))).toFloat()
        )
        val screenPos = ScreenProjectionUtil.projectWorldToScreen(sceneView, worldPos)
        screenPos?.let {
            val screenX = it.x.toFloat()
            val screenY = it.y.toFloat()
            if (screenX in left..right && screenY in top..bottom) {
                val dist = ((screenX - centerX).pow(2) + (screenY - centerY).pow(2))
                if (dist < minDist) {
                    candidate = star
                    minDist = dist
                }
            }
        }
    }
    return candidate
}
