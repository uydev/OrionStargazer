package com.example.orionstargazer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Resources
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orionstargazer.StarList
import com.example.orionstargazer.ar.ARCoreView
import com.example.orionstargazer.ar.ConstellationRenderer
import com.example.orionstargazer.ar.ScreenProjectionUtil
import com.example.orionstargazer.astronomy.ConstellationCatalog
import com.example.orionstargazer.astronomy.PlanetCalculator
import com.example.orionstargazer.astronomy.StarPositionCalculator
import com.example.orionstargazer.data.StarRepository
import com.example.orionstargazer.data.UserSettings
import com.example.orionstargazer.sensors.LocationProvider
import com.example.orionstargazer.sensors.OrientationProvider
import com.example.orionstargazer.ui.NightSkyBackground
import com.example.orionstargazer.ui.ReticleOverlay
import com.example.orionstargazer.SwipeableBottomSheet
import com.example.orionstargazer.ui.theme.OrionStargazerTheme
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var orientationProvider: OrientationProvider
    private lateinit var locationProvider: LocationProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orientationProvider = OrientationProvider(this)
        locationProvider = LocationProvider(this)

        var cameraPermissionGranted by mutableStateOf(false)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            cameraPermissionGranted = results[Manifest.permission.CAMERA] == true
            if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                locationProvider.start()
            }
        }

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            cameraPermissionGranted = true
            locationProvider.start()
        }

        orientationProvider.start()

        setContent {
            OrionStargazerTheme(darkTheme = true, dynamicColor = false) {
                var azimuth by remember { mutableStateOf(0f) }
                var altitude by remember { mutableStateOf(0f) }
                var starsInView by remember { mutableStateOf(listOf<StarPositionCalculator.VisibleStar>()) }
                var candidateStars by remember { mutableStateOf(emptyList<com.example.orionstargazer.data.entities.StarEntity>()) }
                var dbCount by remember { mutableStateOf(0) }
                var seedError by remember { mutableStateOf<String?>(null) }
                var maxMagnitude by remember { mutableStateOf(UserSettings.DEFAULT_MAX_MAGNITUDE) }
                var showConstellations by remember { mutableStateOf(true) }
                var constellationSegments by remember { mutableStateOf(emptyList<ConstellationRenderer.Segment>()) }
                var sceneView by remember { mutableStateOf<ArSceneView?>(null) }
                var highlightedStar by remember { mutableStateOf<StarPositionCalculator.VisibleStar?>(null) }

                val repo = remember { StarRepository.getInstance(this@MainActivity) }
                val constellations = remember { ConstellationCatalog.load(this@MainActivity) }
                val reticleSizeDp = 48

                LaunchedEffect(Unit) {
                    try {
                        val existing = repo.countStars()
                        if (existing < 2000) {
                            repo.deleteAll()
                            com.example.orionstargazer.data.StarAssetLoader.loadAssetsAndSeedDb(this@MainActivity, repo)
                            com.example.orionstargazer.data.HygCsvImporter.importTopBrightest(this@MainActivity, repo, limit = 3000)
                        }
                    } catch (t: Throwable) {
                        Log.e("MainActivity", "Star seed failed", t)
                        seedError = "${t.javaClass.simpleName}: ${t.message ?: "unknown"}"
                    }
                    dbCount = try { repo.countStars() } catch (_: Throwable) { -1 }
                }

                LaunchedEffect(Unit) {
                    UserSettings.maxMagnitudeFlow(this@MainActivity).collectLatest {
                        maxMagnitude = it
                    }
                }

                LaunchedEffect(maxMagnitude) {
                    while (true) {
                        val loc = locationProvider.location ?: defaultLocation()
                        val minDec = (loc.latitude - 90).coerceAtLeast(-90.0)
                        val maxDec = (loc.latitude + 90).coerceAtMost(90.0)
                        candidateStars = repo.getCandidates(maxMagnitude, minDec, maxDec)
                        delay(2000)
                    }
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        azimuth = orientationProvider.azimuth
                        altitude = orientationProvider.altitude
                        val location = locationProvider.location ?: defaultLocation()
                        val calendar = Calendar.getInstance()

                        val planetEntities = PlanetCalculator.computePlanets(calendar).map { p ->
                            com.example.orionstargazer.data.entities.StarEntity(
                                id = -1000 - p.id,
                                name = p.name,
                                ra = p.raDeg,
                                dec = p.decDeg,
                                magnitude = p.magnitude,
                                distance = p.distanceAu * 63241.1,
                                spectralType = "P",
                                constellation = "Planet"
                            )
                        }

                        val skyEntities = (candidateStars + planetEntities).distinctBy { it.id }

                        starsInView = StarPositionCalculator.calculateVisibleStars(
                            calendar,
                            location,
                            azimuth,
                            altitude,
                            fieldOfView = 60.0,
                            minAltitude = 0.0,
                            stars = skyEntities
                        )

                        val listCandidates = StarPositionCalculator.calculateVisibleStars(
                            calendar,
                            location,
                            azimuth,
                            altitude,
                            fieldOfView = 360.0,
                            minAltitude = 0.0,
                            stars = skyEntities
                        )

                        highlightedStar = computeStarInReticle(sceneView, starsInView, reticleSizeDp)

                        if (showConstellations) {
                            constellationSegments = buildConstellations(
                                constellations,
                                repo,
                                calendar,
                                location,
                                azimuth,
                                altitude
                            )
                        } else {
                            constellationSegments = emptyList()
                        }

                        delay(100)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NightSkyBackground(modifier = Modifier.fillMaxSize())

                        if (cameraPermissionGranted) {
                            Box(Modifier.fillMaxSize()) {
                                ARCoreView(
                                    stars = starsInView,
                                    constellationSegments = constellationSegments,
                                    showConstellations = showConstellations,
                                    onStarTapped = { star ->
                                        highlightedStar = starsInView.firstOrNull { visible -> visible.star.id == star.id }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    sceneViewRef = { sceneView = it }
                                )
                                ReticleOverlay()
                            }
                        }

                        SwipeableBottomSheet(
                            orientationContent = {
                                OrientationDisplay(
                                    azimuth = azimuth,
                                    altitude = altitude,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            },
                            starListContent = {
                                Column {
                                    Text(
                                        text = "Catalog: $dbCount  •  Mag ≤ ${"%.1f".format(maxMagnitude)}  •  Candidates: ${candidateStars.size}  •  In view: ${starsInView.size}",
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                                        Text(
                                            text = "Magnitude limit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFEAF2FF)
                                        )
                                        val scope = rememberCoroutineScope()
                                        Slider(
                                            value = maxMagnitude.toFloat(),
                                            onValueChange = { maxMagnitude = it.toDouble() },
                                            onValueChangeFinished = {
                                                scope.launch {
                                                    UserSettings.setMaxMagnitude(this@MainActivity, maxMagnitude)
                                                }
                                            },
                                            valueRange = 0f..8f,
                                            steps = 15
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Constellations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFEAF2FF),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = showConstellations,
                                            onCheckedChange = { showConstellations = it }
                                        )
                                    }
                                    seedError?.let {
                                        Text(
                                            text = "Seed error: $it",
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    highlightedStar?.let { star ->
                                        SelectedStarCard(
                                            star = star,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                            onClear = { highlightedStar = null }
                                        )
                                    } ?: Text(
                                        text = "Aim the reticle at a star for details.",
                                        color = Color(0xFFA9B9FF),
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                    StarList(
                                        stars = starsInView,
                                        selectedStarId = highlightedStar?.star?.id,
                                        onStarSelected = { id ->
                                            highlightedStar = starsInView.firstOrNull { visible -> visible.star.id == id }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        orientationProvider.stop()
        locationProvider.stop()
        super.onDestroy()
    }

    private suspend fun buildConstellations(
        constellations: List<com.example.orionstargazer.astronomy.ConstellationCatalog.Constellation>,
        repo: StarRepository,
        calendar: Calendar,
        location: Location,
        azimuth: Float,
        altitude: Float
    ): List<ConstellationRenderer.Segment> {
        val byId = repo.getAllStars().associateBy { it.id }
        val segments = mutableListOf<ConstellationRenderer.Segment>()
        constellations.forEach { constellation ->
            constellation.lines.forEach { line ->
                val a = byId[line.aStarId] ?: return@forEach
                val b = byId[line.bStarId] ?: return@forEach
                val (altA, azA) = StarPositionCalculator.computeAltAz(calendar, location, a)
                val (altB, azB) = StarPositionCalculator.computeAltAz(calendar, location, b)
                if (altA <= 0.0 || altB <= 0.0) return@forEach
                val nearA = StarPositionCalculator.viewDistanceDegrees(azA, altA, azimuth, altitude) < 80.0
                val nearB = StarPositionCalculator.viewDistanceDegrees(azB, altB, azimuth, altitude) < 80.0
                if (!nearA && !nearB) return@forEach
                segments.add(
                    ConstellationRenderer.Segment(
                        key = "${constellation.name}:${line.aStarId}-${line.bStarId}",
                        start = worldPosition(altA, azA),
                        end = worldPosition(altB, azB)
                    )
                )
            }
        }
        return segments
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

    private fun defaultLocation(): Location {
        return Location("fallback").apply {
            latitude = 51.4769
            longitude = 0.0005
            accuracy = 10000f
        }
    }
}

@Composable
fun SelectedStarCard(
    star: StarPositionCalculator.VisibleStar,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
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
