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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import com.example.orionstargazer.ui.InstructionPanel
import com.example.orionstargazer.ui.theme.OrionStargazerTheme
import androidx.activity.viewModels
import com.example.orionstargazer.ui.main.MainMenuScreen
import com.example.orionstargazer.ui.main.MainScreen
import com.example.orionstargazer.ui.main.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val camera = results[Manifest.permission.CAMERA] == true ||
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            vm.onPermissionsChanged(cameraGranted = camera, locationGranted = fine || coarse)
        }

        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val locationOnlyPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Robust behavior: don't auto-spam permission dialogs on launch.
        // We surface the callout UI and request when the user taps "Grant".
        val camera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        vm.onPermissionsChanged(cameraGranted = camera, locationGranted = fine || coarse)

        vm.start()

        setContent {
            OrionStargazerTheme(darkTheme = true, dynamicColor = false) {
                val state = vm.state
                var showMainScreen by rememberSaveable { mutableStateOf(false) }
                var showInstructions by remember { mutableStateOf(false) }

                if (!showMainScreen) {
                    MainMenuScreen(
                        onEnterStargazing = { showMainScreen = true },
                        onShowInstructions = { showInstructions = true },
                        onShowSettings = {
                            showMainScreen = true
                            vm.setShowSettings(true)
                        }
                    )
                    if (showInstructions) {
                        Dialog(onDismissRequest = { showInstructions = false }) {
                            InstructionPanel(onClose = { showInstructions = false })
                        }
                    }
                } else {
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
                        onReticleStarChanged = { vm.setHighlightedStar(it) },
                        onRequestPermissions = { permissionLauncher.launch(permissions) },
                        onRequestLocationOnly = { permissionLauncher.launch(locationOnlyPermissions) },
                        onOpenAppSettings = { openAppSettings() },
                        onSetShowSettings = { show -> vm.setShowSettings(show) },
                        onSetShowHighlights = { show -> vm.setShowHighlights(show) },
                        onStarRenderModeChanged = { mode -> vm.setStarRenderMode(mode) },
                        onShaderMaxStarsChanged = { v -> vm.setShaderMaxStars(v) },
                        onFpsSample = { fps -> vm.onFpsSample(fps) },
                        onConstellationDrawModeChanged = { mode -> vm.setConstellationDrawMode(mode) },
                        onPinchMagnitudeChange = { delta ->
                            val next = (state.maxMagnitude + delta).coerceIn(0.0, 8.0)
                            vm.setMaxMagnitude(next)
                        },
                        onOpenMainMenu = { showMainScreen = false },
                        onShowXyOverlayChanged = { enabled -> vm.setShowXyOverlay(enabled) },
                        onShowCameraBackgroundChanged = { enabled -> vm.setShowCameraBackground(enabled) },
                        onStartCalibrationChallenge = { vm.setShowCalibrationChallenge(true) },
                        onDismissCalibrationChallenge = { vm.setShowCalibrationChallenge(false) }
                    )
                }
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

